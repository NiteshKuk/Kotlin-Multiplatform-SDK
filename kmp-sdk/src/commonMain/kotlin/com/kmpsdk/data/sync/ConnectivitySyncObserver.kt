package com.kmpsdk.data.sync

import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.connectivity.ConnectivityMonitor
import com.kmpsdk.core.connectivity.ConnectivityStatus
import com.kmpsdk.core.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Triggers [SyncCoordinator.syncAll] when connectivity is restored.
 */
class ConnectivitySyncObserver(
    private val connectivityMonitor: ConnectivityMonitor,
    private val syncCoordinator: SyncCoordinator,
    private val config: KmpSdkConfig,
    private val scope: CoroutineScope,
    private val logger: Logger = Logger.create("ConnectivitySync"),
) {
    init {
        if (config.autoSyncOnReconnect) {
            scope.launch {
                connectivityMonitor.status.collect { status ->
                    if (status == ConnectivityStatus.Online) {
                        logger.i("Network restored — running full sync")
                        syncCoordinator.syncAll()
                    }
                }
            }
        }
    }
}
