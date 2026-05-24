package com.kmpsdk.data.sync

import com.kmpsdk.core.logger.Logger
import com.kmpsdk.domain.error.KmpSdkError
import com.kmpsdk.domain.error.KmpSdkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SyncPhase {
    IDLE,
    REPLAYING_OFFLINE_QUEUE,
    REFRESHING_REPOSITORIES,
    COMPLETED,
    FAILED,
}

data class SyncState(
    val phase: SyncPhase = SyncPhase.IDLE,
    val replayedCount: Int = 0,
    val refreshedCount: Int = 0,
    val failedTargets: List<String> = emptyList(),
    val lastError: KmpSdkError? = null,
)

data class SyncResult(
    val replayedOfflineRequests: Int,
    val refreshedRepositories: Int,
    val failures: List<String>,
)

data class SyncTarget(
    val name: String,
    val refresh: suspend () -> KmpSdkResult<Unit>,
)

/**
 * Orchestrates offline queue replay, then refreshes registered repositories.
 */
class SyncCoordinator(
    private val replayOfflineQueue: suspend () -> KmpSdkResult<Int>,
    private val logger: Logger = Logger.create("SyncCoordinator"),
) {
    private val targets = mutableListOf<SyncTarget>()
    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()

    fun register(name: String, refresh: suspend () -> KmpSdkResult<Unit>) {
        targets.removeAll { it.name == name }
        targets.add(SyncTarget(name, refresh))
    }

    fun register(target: SyncTarget) {
        register(target.name, target.refresh)
    }

    suspend fun syncAll(): SyncResult {
        _state.value = SyncState(phase = SyncPhase.REPLAYING_OFFLINE_QUEUE)
        logger.i("Starting full sync")

        val replayResult = replayOfflineQueue()
        val replayed = (replayResult as? KmpSdkResult.Success)?.data ?: 0

        _state.value = SyncState(
            phase = SyncPhase.REFRESHING_REPOSITORIES,
            replayedCount = replayed,
        )

        var refreshed = 0
        val failures = mutableListOf<String>()

        for (target in targets) {
            when (val result = target.refresh()) {
                is KmpSdkResult.Success -> refreshed++
                is KmpSdkResult.Failure -> {
                    failures.add(target.name)
                    logger.w("Sync failed for ${target.name}: ${result.error.message}")
                }
            }
        }

        val finalState = SyncState(
            phase = if (failures.isEmpty()) SyncPhase.COMPLETED else SyncPhase.FAILED,
            replayedCount = replayed,
            refreshedCount = refreshed,
            failedTargets = failures,
            lastError = if (failures.isNotEmpty()) {
                KmpSdkError.Network("Sync failed for: ${failures.joinToString()}")
            } else {
                null
            },
        )
        _state.value = finalState

        return SyncResult(
            replayedOfflineRequests = replayed,
            refreshedRepositories = refreshed,
            failures = failures,
        )
    }

    fun clearTargets() {
        targets.clear()
    }
}
