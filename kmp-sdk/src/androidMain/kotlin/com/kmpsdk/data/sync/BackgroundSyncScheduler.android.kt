package com.kmpsdk.data.sync

actual fun registerPlatformBackgroundSync(intervalMillis: Long, onTick: suspend () -> Unit) {
    // Host apps can wire WorkManager here; coroutine loop in BackgroundSyncScheduler covers KMP common path.
}
