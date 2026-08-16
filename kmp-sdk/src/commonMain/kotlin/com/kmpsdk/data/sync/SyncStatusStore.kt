package com.kmpsdk.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * Per-feature sync lifecycle for UI (banners, pull-to-refresh, offline hints).
 */
enum class FeatureSyncPhase {
    /** No sync in progress; last attempt succeeded online (or never synced). */
    Idle,

    /** Refresh / sync currently running. */
    Syncing,

    /** Offline (or remote failed) but local cache is being served. */
    OfflineCached,

    /** Last sync failed and no usable cache was applied. */
    Failed,
}

data class FeatureSyncStatus(
    val name: String,
    val phase: FeatureSyncPhase = FeatureSyncPhase.Idle,
    val lastErrorMessage: String? = null,
    val lastSyncedAtMillis: Long? = null,
    val localCount: Long? = null,
)

/**
 * Observable store of [FeatureSyncStatus] keyed by sync target name
 * (same name passed to [SyncCoordinator.register] / `installRestListFeature`).
 */
class SyncStatusStore {
    private val _statuses = MutableStateFlow<Map<String, FeatureSyncStatus>>(emptyMap())
    val statuses: StateFlow<Map<String, FeatureSyncStatus>> = _statuses.asStateFlow()

    fun get(name: String): FeatureSyncStatus =
        _statuses.value[name] ?: FeatureSyncStatus(name = name)

    fun observe(name: String): Flow<FeatureSyncStatus> =
        statuses.map { it[name] ?: FeatureSyncStatus(name = name) }.distinctUntilChanged()

    fun ensureRegistered(name: String) {
        if (_statuses.value.containsKey(name)) return
        update(FeatureSyncStatus(name = name))
    }

    fun markSyncing(name: String) {
        val previous = get(name)
        update(
            previous.copy(
                phase = FeatureSyncPhase.Syncing,
                lastErrorMessage = null,
            ),
        )
    }

    fun markSuccess(name: String, localCount: Long? = null) {
        update(
            FeatureSyncStatus(
                name = name,
                phase = FeatureSyncPhase.Idle,
                lastErrorMessage = null,
                lastSyncedAtMillis = Clock.System.now().toEpochMilliseconds(),
                localCount = localCount,
            ),
        )
    }

    fun markOfflineCached(name: String, localCount: Long? = null) {
        val previous = get(name)
        update(
            previous.copy(
                phase = FeatureSyncPhase.OfflineCached,
                lastErrorMessage = null,
                localCount = localCount ?: previous.localCount,
            ),
        )
    }

    fun markFailed(name: String, errorMessage: String?, localCount: Long? = null) {
        val previous = get(name)
        update(
            previous.copy(
                phase = FeatureSyncPhase.Failed,
                lastErrorMessage = errorMessage,
                localCount = localCount ?: previous.localCount,
            ),
        )
    }

    private fun update(status: FeatureSyncStatus) {
        _statuses.value = _statuses.value + (status.name to status)
    }
}
