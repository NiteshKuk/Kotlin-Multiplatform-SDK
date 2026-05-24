package com.kmpsdk.data.sync

actual fun registerPlatformBackgroundSync(intervalMillis: Long, onTick: suspend () -> Unit) {
    // Host apps can wire BGTaskScheduler here; coroutine loop covers KMP common path.
}
