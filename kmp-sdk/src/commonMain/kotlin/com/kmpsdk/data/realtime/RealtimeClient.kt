package com.kmpsdk.data.realtime

import com.kmpsdk.core.logger.Logger
import com.kmpsdk.data.network.KmpNetworkClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readUTF8Line
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RealtimeEvent(
    val type: String,
    val payload: String,
)

typealias RealtimeHandler = suspend (RealtimeEvent) -> Unit

/**
 * Headless WebSocket + SSE helpers with reconnect backoff.
 */
class RealtimeClient(
    private val networkClient: KmpNetworkClient,
    private val scope: CoroutineScope,
    private val logger: Logger = Logger.create("Realtime"),
) {
    private val mutex = Mutex()
    private val jobs = mutableMapOf<String, Job>()

    fun connectWebSocket(
        key: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        reconnectDelayMillis: Long = 2_000,
        onEvent: RealtimeHandler,
    ) {
        disconnect(key)
        jobs[key] = scope.launch {
            while (isActive) {
                try {
                    networkClient.httpClient.webSocket(
                        urlString = url,
                        request = {
                            headers.forEach { (k, v) -> header(k, v) }
                        },
                    ) {
                        logger.i("WebSocket connected: $url")
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                val type = text.substringBefore(':').ifBlank { "message" }
                                onEvent(RealtimeEvent(type = type, payload = text))
                            }
                        }
                    }
                } catch (ex: Throwable) {
                    logger.w("WebSocket error ($url): ${ex.message}")
                }
                delay(reconnectDelayMillis)
            }
        }
    }

    fun connectSse(
        key: String,
        path: String,
        headers: Map<String, String> = emptyMap(),
        reconnectDelayMillis: Long = 2_000,
        onEvent: RealtimeHandler,
    ) {
        disconnect(key)
        jobs[key] = scope.launch {
            while (isActive) {
                try {
                    networkClient.httpClient.prepareGet(path) {
                        header(HttpHeaders.Accept, "text/event-stream")
                        headers.forEach { (k, v) -> header(k, v) }
                    }.execute { response ->
                        val channel = response.bodyAsChannel()
                        var eventType = "message"
                        val data = StringBuilder()
                        while (!channel.isClosedForRead) {
                            val line = channel.readUTF8Line() ?: break
                            when {
                                line.startsWith("event:") -> eventType = line.removePrefix("event:").trim()
                                line.startsWith("data:") -> data.append(line.removePrefix("data:").trim())
                                line.isBlank() && data.isNotEmpty() -> {
                                    onEvent(RealtimeEvent(eventType, data.toString()))
                                    eventType = "message"
                                    data.clear()
                                }
                            }
                        }
                    }
                } catch (ex: Throwable) {
                    logger.w("SSE error ($path): ${ex.message}")
                }
                delay(reconnectDelayMillis)
            }
        }
    }

    fun disconnect(key: String) {
        jobs.remove(key)?.cancel()
    }

    suspend fun disconnectAll() = mutex.withLock {
        jobs.keys.toList().forEach { disconnect(it) }
    }
}
