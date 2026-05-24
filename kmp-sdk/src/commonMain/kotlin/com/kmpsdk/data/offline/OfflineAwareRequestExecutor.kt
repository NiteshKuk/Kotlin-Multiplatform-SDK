package com.kmpsdk.data.offline

import com.kmpsdk.core.connectivity.ConnectivityMonitor
import com.kmpsdk.data.network.KmpNetworkClient
import com.kmpsdk.domain.error.KmpSdkError
import com.kmpsdk.domain.error.KmpSdkResult
import io.ktor.http.HttpMethod

/**
 * Executes live requests when online, otherwise persists them for replay.
 */
class OfflineAwareRequestExecutor(
    private val networkClient: KmpNetworkClient,
    private val connectivityMonitor: ConnectivityMonitor,
    private val offlineQueue: OfflineQueueManager,
) {
    suspend fun <T> executeOrQueue(
        payload: OfflineRequestPayload,
        liveCall: suspend () -> KmpSdkResult<T>,
    ): KmpSdkResult<T> {
        if (!connectivityMonitor.isOnline()) {
            offlineQueue.enqueue(payload)
            return KmpSdkResult.Failure(KmpSdkError.Network("Queued for offline replay"))
        }

        val result = liveCall()
        if (result is KmpSdkResult.Failure && result.error is KmpSdkError.Network) {
            offlineQueue.enqueue(payload)
        }
        return result
    }

    suspend fun enqueueMutation(
        method: HttpMethod,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
        priority: Int = 0,
    ): KmpSdkResult<Long> {
        val id = offlineQueue.enqueue(
            OfflineRequestPayload(
                method = method.value,
                url = path,
                headers = headers,
                body = body,
                priority = priority,
            ),
        )
        return KmpSdkResult.Success(id)
    }

    suspend fun replayPendingQueue() = offlineQueue.replayQueue()
}
