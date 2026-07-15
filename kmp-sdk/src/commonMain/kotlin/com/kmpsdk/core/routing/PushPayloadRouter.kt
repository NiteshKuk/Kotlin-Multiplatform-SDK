package com.kmpsdk.core.routing

import com.kmpsdk.core.logger.Logger

typealias PushHandler = suspend (data: Map<String, String>) -> Unit

/**
 * Routes silent/data push payloads by `type` (or custom [typeKey]).
 * Platform FCM/APNs code forwards the data map into [handle].
 */
class PushPayloadRouter(
    private val typeKey: String = "type",
    private val logger: Logger = Logger.create("PushRouter"),
) {
    private val handlers = mutableMapOf<String, PushHandler>()
    private var defaultHandler: PushHandler? = null

    fun on(type: String, handler: PushHandler) {
        handlers[type] = handler
    }

    fun onDefault(handler: PushHandler) {
        defaultHandler = handler
    }

    fun routes(block: PushPayloadRouter.() -> Unit) = apply(block)

    /**
     * @return true if a typed or default handler ran
     */
    suspend fun handle(data: Map<String, String>): Boolean {
        val type = data[typeKey]
        val handler = type?.let { handlers[it] } ?: defaultHandler
        if (handler == null) {
            logger.w("No push handler for type=$type")
            return false
        }
        logger.i("Handling push type=$type")
        handler(data)
        return true
    }
}
