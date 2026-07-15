package com.kmpsdk.data.sync

import com.kmpsdk.core.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

data class BackgroundWorkConfig(
    val periodicSync: Duration? = null,
    val requireNetworkHint: Boolean = true,
    val syncOnConnectivityRestored: Boolean = true,
)

/**
 * Schedules periodic / one-shot sync work.
 * Uses common coroutine loop + [registerPlatformBackgroundSync] (WorkManager/BGTasks hook).
 */
class BackgroundWorkBridge(
    private val scope: CoroutineScope,
    private val onSync: suspend () -> Unit,
    private val logger: Logger = Logger.create("BackgroundWork"),
) {
    private var periodicJob: Job? = null
    private val oneShotJobs = mutableMapOf<String, Job>()

    fun configure(config: BackgroundWorkConfig) {
        periodicJob?.cancel()
        val every = config.periodicSync ?: return
        val millis = every.inWholeMilliseconds
        logger.i("Periodic background sync every ${every}")
        registerPlatformBackgroundSync(millis) { onSync() }
        periodicJob = scope.launch {
            while (isActive) {
                delay(millis)
                runCatching { onSync() }
                    .onFailure { logger.w("Periodic sync failed: ${it.message}") }
            }
        }
    }

    fun enqueueOneShotSync(id: String, delayMillis: Long = 0) {
        oneShotJobs.remove(id)?.cancel()
        oneShotJobs[id] = scope.launch {
            if (delayMillis > 0) delay(delayMillis)
            runCatching { onSync() }
                .onFailure { logger.w("One-shot sync '$id' failed: ${it.message}") }
        }
    }

    fun cancel(id: String) {
        oneShotJobs.remove(id)?.cancel()
    }

    fun cancelAll() {
        periodicJob?.cancel()
        oneShotJobs.keys.toList().forEach { cancel(it) }
    }
}

class BackgroundWorkDsl {
    var periodicSync: Duration? = 15.minutes
    var requireNetworkHint: Boolean = true
    var syncOnConnectivityRestored: Boolean = true

    fun periodicSync(every: Duration) {
        periodicSync = every
    }

    internal fun build() = BackgroundWorkConfig(
        periodicSync = periodicSync,
        requireNetworkHint = requireNetworkHint,
        syncOnConnectivityRestored = syncOnConnectivityRestored,
    )
}
