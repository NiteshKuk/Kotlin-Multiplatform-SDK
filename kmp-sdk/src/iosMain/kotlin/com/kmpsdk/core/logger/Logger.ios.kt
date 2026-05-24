package com.kmpsdk.core.logger

actual object PlatformLogger {
    actual fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val prefix = when (level) {
            LogLevel.VERBOSE -> "💬"
            LogLevel.DEBUG -> "🐛"
            LogLevel.INFO -> "ℹ️"
            LogLevel.WARN -> "⚠️"
            LogLevel.ERROR -> "❌"
            LogLevel.NONE -> return
        }
        val output = "[$tag] $prefix $message"
        if (throwable != null) {
            println("$output\n${throwable.stackTraceToString()}")
        } else {
            println(output)
        }
    }
}
