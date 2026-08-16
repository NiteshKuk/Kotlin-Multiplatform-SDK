package com.kmpsdk.data.offline

import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.connectivity.ConnectivityMonitor
import com.kmpsdk.core.connectivity.ConnectivityStatus
import com.kmpsdk.core.logger.Logger
import com.kmpsdk.data.db.KmpSdkDatabase
import com.kmpsdk.data.db.Offline_queue
import com.kmpsdk.data.network.KmpNetworkClient
import com.kmpsdk.domain.error.KmpSdkError
import com.kmpsdk.domain.error.KmpSdkResult
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class OfflineReplayStrategy {
    FIFO,
    LIFO,
    PRIORITY,
}

enum class OfflineQueueStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}

@Serializable
data class OfflineRequestPayload(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val priority: Int = 0,
)

class OfflineQueueManager(
    private val database: KmpSdkDatabase,
    private val networkClient: KmpNetworkClient,
    private val connectivityMonitor: ConnectivityMonitor,
    private val config: KmpSdkConfig,
    private val scope: CoroutineScope,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val logger: Logger = Logger.create("OfflineQueue"),
) {
    private val _pendingCount = MutableStateFlow(0L)
    val pendingCount: StateFlow<Long> = _pendingCount.asStateFlow()

    init {
        scope.launch {
            connectivityMonitor.status.collect { status ->
                if (status == ConnectivityStatus.Online) {
                    if (!config.autoSyncOnReconnect) {
                        replayQueue()
                    }
                }
            }
        }
        refreshPendingCount()
    }

    suspend fun enqueue(request: OfflineRequestPayload): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        database.kmpSdkDatabaseQueries.insertQueueItem(
            method = request.method,
            url = request.url,
            headers_json = json.encodeToString(request.headers),
            body = request.body,
            priority = request.priority.toLong(),
            created_at = now,
            retry_count = 0,
            max_retries = config.maxOfflineRetries.toLong(),
            status = OfflineQueueStatus.PENDING.name,
        )
        refreshPendingCount()
        return database.kmpSdkDatabaseQueries.countPendingQueueItems().executeAsOne()
    }

    suspend fun replayQueue(): KmpSdkResult<Int> {
        if (!connectivityMonitor.isOnline()) {
            return KmpSdkResult.Failure(KmpSdkError.Network("Cannot replay while offline"))
        }

        val items = loadPendingItems()
        var replayed = 0

        for (item in items) {
            val result = replayItem(item)
            if (result is KmpSdkResult.Success) {
                replayed++
            }
        }

        refreshPendingCount()
        logger.i("Offline replay finished. Success count=$replayed")
        return KmpSdkResult.Success(replayed)
    }

    suspend fun inspectQueue(): List<OfflineQueueItem> =
        loadPendingItems().map { it.toDomain(json) }

    suspend fun clearQueue() {
        database.kmpSdkDatabaseQueries.clearQueue()
        refreshPendingCount()
    }

    private suspend fun replayItem(item: Offline_queue): KmpSdkResult<Unit> {
        updateStatus(item.id, OfflineQueueStatus.IN_PROGRESS, item.retry_count)

        return try {
            val headers = json.decodeFromString<Map<String, String>>(item.headers_json)
            val method = HttpMethod.parse(item.method)
            val response = when (method) {
                HttpMethod.Get -> networkClient.httpClient.get(item.url) {
                    applyHeaders(headers, item.body)
                }
                HttpMethod.Post -> networkClient.httpClient.post(item.url) {
                    applyHeaders(headers, item.body)
                }
                HttpMethod.Put -> networkClient.httpClient.put(item.url) {
                    applyHeaders(headers, item.body)
                }
                HttpMethod.Patch -> networkClient.httpClient.patch(item.url) {
                    applyHeaders(headers, item.body)
                }
                HttpMethod.Delete -> networkClient.httpClient.delete(item.url) {
                    applyHeaders(headers, item.body)
                }
                else -> error("Unsupported method ${item.method}")
            }

            if (response.status.isSuccess()) {
                updateStatus(item.id, OfflineQueueStatus.COMPLETED, item.retry_count)
                database.kmpSdkDatabaseQueries.deleteQueueItem(item.id)
                KmpSdkResult.Success(Unit)
            } else {
                handleFailure(item)
            }
        } catch (throwable: Throwable) {
            logger.e("Replay failed for queue item ${item.id}", throwable)
            handleFailure(item)
        }
    }

    private suspend fun handleFailure(item: Offline_queue): KmpSdkResult.Failure {
        val nextRetry = item.retry_count + 1
        return if (nextRetry >= item.max_retries) {
            updateStatus(item.id, OfflineQueueStatus.FAILED, nextRetry)
            KmpSdkResult.Failure(KmpSdkError.Network("Offline replay exhausted retries"))
        } else {
            updateStatus(item.id, OfflineQueueStatus.PENDING, nextRetry)
            KmpSdkResult.Failure(KmpSdkError.Network("Offline replay will retry"))
        }
    }

    private fun loadPendingItems(): List<Offline_queue> {
        val all = database.kmpSdkDatabaseQueries.selectAllPendingQueueItems().executeAsList()
        return when (config.offlineReplayStrategy) {
            OfflineReplayStrategy.FIFO -> all.sortedBy { it.created_at }
            OfflineReplayStrategy.LIFO -> all.sortedByDescending { it.created_at }
            OfflineReplayStrategy.PRIORITY -> all.sortedWith(
                compareByDescending<Offline_queue> { it.priority }
                    .thenBy { it.created_at },
            )
        }
    }

    private fun updateStatus(id: Long, status: OfflineQueueStatus, retryCount: Long) {
        database.kmpSdkDatabaseQueries.updateQueueItemStatus(
            status = status.name,
            retry_count = retryCount,
            id = id,
        )
    }

    private fun refreshPendingCount() {
        val count = database.kmpSdkDatabaseQueries.countPendingQueueItems().executeAsOne()
        _pendingCount.value = count
    }
}

data class OfflineQueueItem(
    val id: Long,
    val request: OfflineRequestPayload,
    val createdAt: Long,
    val retryCount: Int,
    val status: OfflineQueueStatus,
)

private fun Offline_queue.toDomain(json: Json): OfflineQueueItem = OfflineQueueItem(
    id = id,
    request = OfflineRequestPayload(
        method = method,
        url = url,
        headers = json.decodeFromString(headers_json),
        body = body,
        priority = priority.toInt(),
    ),
    createdAt = created_at,
    retryCount = retry_count.toInt(),
    status = OfflineQueueStatus.valueOf(status),
)

private fun io.ktor.client.request.HttpRequestBuilder.applyHeaders(
    headers: Map<String, String>,
    body: String?,
) {
    headers.forEach { (key, value) -> header(key, value) }
    body?.let { setBody(it) }
}
