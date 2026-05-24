package com.kmpsdk.data.sync

import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

expect fun registerPlatformBackgroundSync(intervalMillis: Long, onTick: suspend () -> Unit)

class BackgroundSyncScheduler(
    private val config: KmpSdkConfig,
    private val scope: CoroutineScope,
    private val onSync: suspend () -> Unit,
    private val logger: Logger = Logger.create("BackgroundSync"),
) {
    fun start() {
        val interval = config.backgroundSyncIntervalMillis ?: return
        logger.i("Background sync enabled every ${interval}ms")
        registerPlatformBackgroundSync(interval) { onSync() }
        scope.launch {
            while (isActive) {
                delay(interval)
                runCatching { onSync() }
                    .onFailure { logger.w("Background sync failed: ${it.message}") }
            }
        }
    }
}
