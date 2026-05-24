package com.kmpsdk.data.offline

import com.kmpsdk.core.logger.Logger
import com.kmpsdk.data.db.KmpSdkDatabase
import com.kmpsdk.domain.error.KmpSdkResult
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class OfflineActionPayload(
    val actionType: String,
    val entityId: String,
    val payloadJson: String,
)

enum class OfflineActionStatus {
    PENDING,
    COMPLETED,
    FAILED,
}

typealias OfflineActionHandler = suspend (OfflineActionPayload) -> KmpSdkResult<Unit>

class OfflineActionManager(
    private val database: KmpSdkDatabase,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val logger: Logger = Logger.create("OfflineAction"),
) {
    private val handlers = mutableMapOf<String, OfflineActionHandler>()

    fun registerHandler(actionType: String, handler: OfflineActionHandler) {
        handlers[actionType] = handler
    }

    suspend fun enqueue(actionType: String, entityId: String, payloadJson: String): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        return database.transactionWithResult {
            database.kmpSdkDatabaseQueries.insertOfflineAction(
                action_type = actionType,
                payload_json = json.encodeToString(
                    OfflineActionPayload(actionType, entityId, payloadJson),
                ),
                created_at = now,
                status = OfflineActionStatus.PENDING.name,
            )
            database.kmpSdkDatabaseQueries.selectLastInsertRowId().executeAsOne()
        }
    }

    suspend fun pendingActions(): List<OfflineActionPayload> =
        database.kmpSdkDatabaseQueries.selectPendingActions()
            .executeAsList()
            .map { row -> json.decodeFromString<OfflineActionPayload>(row.payload_json) }

    suspend fun replayPending(): KmpSdkResult<Int> {
        val rows = database.kmpSdkDatabaseQueries.selectPendingActions().executeAsList()
        var replayed = 0
        for (row in rows) {
            val payload = json.decodeFromString<OfflineActionPayload>(row.payload_json)
            val handler = handlers[payload.actionType]
            if (handler == null) {
                logger.w("No handler registered for offline action ${payload.actionType}")
                continue
            }
            when (val result = handler(payload)) {
                is KmpSdkResult.Success -> {
                    markCompleted(row.id)
                    replayed++
                }
                is KmpSdkResult.Failure -> {
                    logger.w("Offline action replay failed for ${payload.actionType}: ${result.error.message}")
                }
            }
        }
        return KmpSdkResult.Success(replayed)
    }

    suspend fun markCompleted(id: Long) {
        database.kmpSdkDatabaseQueries.updateOfflineActionStatus(
            status = OfflineActionStatus.COMPLETED.name,
            id = id,
        )
    }
}
