package com.kmpsdk.core.messaging

import kotlinx.coroutines.flow.SharedFlow

enum class MessageDuration {
    SHORT,
    LONG,
}

/**
 * Host apps implement their own toast/snackbar/alert UI and wire this contract,
 * or collect [MessageEventBus.events] directly.
 */
interface MessageNotifier {
    suspend fun show(message: String, duration: MessageDuration = MessageDuration.SHORT)
}

interface MessageEventBus {
    val events: SharedFlow<MessageEvent>
    suspend fun emit(message: String, duration: MessageDuration = MessageDuration.SHORT)
}

data class MessageEvent(
    val message: String,
    val duration: MessageDuration,
)

class SharedMessageEventBus : MessageEventBus {
    private val _events = kotlinx.coroutines.flow.MutableSharedFlow<MessageEvent>(extraBufferCapacity = 32)
    override val events: SharedFlow<MessageEvent> = _events

    override suspend fun emit(message: String, duration: MessageDuration) {
        _events.emit(MessageEvent(message, duration))
    }
}

class MessageNotifierAdapter(
    private val bus: MessageEventBus,
) : MessageNotifier {
    override suspend fun show(message: String, duration: MessageDuration) {
        bus.emit(message, duration)
    }
}
