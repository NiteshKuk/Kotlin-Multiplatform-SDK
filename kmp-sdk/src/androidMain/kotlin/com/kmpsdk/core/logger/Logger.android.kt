package com.kmpsdk.core.logger

import android.util.Log

actual object PlatformLogger {
    actual fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
            LogLevel.NONE -> Unit
        }
    }
}
