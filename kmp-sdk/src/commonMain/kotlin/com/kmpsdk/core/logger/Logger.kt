package com.kmpsdk.core.logger

enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    NONE,
    ;

    fun isEnabled(minLevel: LogLevel): Boolean = ordinal >= minLevel.ordinal && this != NONE
}

expect object PlatformLogger {
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
}

class Logger(
    private val tag: String,
    private val minLevel: LogLevel,
) {
    fun v(message: String, throwable: Throwable? = null) = log(LogLevel.VERBOSE, message, throwable)
    fun d(message: String, throwable: Throwable? = null) = log(LogLevel.DEBUG, message, throwable)
    fun i(message: String, throwable: Throwable? = null) = log(LogLevel.INFO, message, throwable)
    fun w(message: String, throwable: Throwable? = null) = log(LogLevel.WARN, message, throwable)
    fun e(message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, message, throwable)

    private fun log(level: LogLevel, message: String, throwable: Throwable?) {
        if (level.isEnabled(minLevel)) {
            PlatformLogger.log(level, tag, message, throwable)
        }
    }

    companion object {
        private var globalMinLevel: LogLevel = LogLevel.INFO

        fun configure(minLevel: LogLevel) {
            globalMinLevel = minLevel
        }

        fun create(tag: String): Logger = Logger(tag, globalMinLevel)
    }
}
