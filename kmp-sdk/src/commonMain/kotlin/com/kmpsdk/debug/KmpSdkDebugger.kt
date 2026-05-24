package com.kmpsdk.debug

import com.kmpsdk.core.auth.SessionManager
import com.kmpsdk.core.auth.SessionState
import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.connectivity.ConnectivityMonitor
import com.kmpsdk.data.cache.CacheStore
import com.kmpsdk.data.cache.TieredCacheStore
import com.kmpsdk.data.offline.OfflineQueueItem
import com.kmpsdk.data.offline.OfflineQueueManager
import com.kmpsdk.data.sync.SyncCoordinator
import com.kmpsdk.data.sync.SyncState
import com.kmpsdk.domain.error.KmpSdkResult
import kotlinx.coroutines.flow.StateFlow

data class KmpSdkDebugSnapshot(
    val isOnline: Boolean,
    val connectivityStatus: String,
    val pendingOfflineRequests: Long,
    val offlineQueuePreview: List<OfflineQueueItem>,
    val sessionState: String,
    val syncState: SyncState?,
    val baseUrl: String,
    val logLevel: String,
)

class KmpSdkDebugger internal constructor(
    private val config: KmpSdkConfig,
    private val connectivityMonitor: ConnectivityMonitor,
    private val offlineQueue: OfflineQueueManager,
    private val cacheStore: CacheStore,
    private val sessionManager: SessionManager,
    private val syncCoordinator: SyncCoordinator,
) {
    val syncState: StateFlow<SyncState> get() = syncCoordinator.state

    suspend fun snapshot(): KmpSdkDebugSnapshot = KmpSdkDebugSnapshot(
        isOnline = connectivityMonitor.isOnline(),
        connectivityStatus = connectivityMonitor.status.value.name,
        pendingOfflineRequests = offlineQueue.pendingCount.value,
        offlineQueuePreview = offlineQueue.inspectQueue(),
        sessionState = sessionManager.sessionState.value.toDebugLabel(),
        syncState = syncCoordinator.state.value,
        baseUrl = config.baseUrl,
        logLevel = config.logLevel.name,
    )

    suspend fun inspectOfflineQueue(): List<OfflineQueueItem> = offlineQueue.inspectQueue()

    suspend fun clearOfflineQueue() {
        offlineQueue.clearQueue()
    }

    suspend fun purgeExpiredCache() {
        (cacheStore as? TieredCacheStore)?.purgeExpired()
    }

    suspend fun triggerFullSync(): com.kmpsdk.data.sync.SyncResult = syncCoordinator.syncAll()
}

private fun SessionState.toDebugLabel(): String = when (this) {
    is SessionState.Unknown -> "Unknown"
    is SessionState.LoggedOut -> "LoggedOut"
    is SessionState.Authenticated -> "Authenticated(${accessTokenPreview})"
}
