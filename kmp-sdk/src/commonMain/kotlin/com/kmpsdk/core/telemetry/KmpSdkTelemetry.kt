package com.kmpsdk.core.telemetry

sealed class TelemetryEvent {
    data class ApiCallCompleted(
        val method: String,
        val path: String,
        val statusCode: Int?,
        val durationMs: Long,
        val success: Boolean,
    ) : TelemetryEvent()

    data class SyncCompleted(
        val replayedOffline: Int,
        val refreshedRepos: Int,
        val failures: List<String>,
    ) : TelemetryEvent()

    data class SessionEvent(val name: String, val detail: String? = null) : TelemetryEvent()

    data class OfflineQueued(val method: String, val path: String) : TelemetryEvent()

    data class ValidationWarning(val message: String) : TelemetryEvent()
}

fun interface TelemetryListener {
    fun onEvent(event: TelemetryEvent)
}

object KmpSdkTelemetry {
    private val listeners = mutableListOf<TelemetryListener>()

    fun addListener(listener: TelemetryListener) {
        listeners += listener
    }

    fun removeListener(listener: TelemetryListener) {
        listeners -= listener
    }

    internal fun emit(event: TelemetryEvent) {
        listeners.forEach { it.onEvent(event) }
    }
}
