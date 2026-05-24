package com.kmpsdk.presentation.mvi

import com.kmpsdk.core.logger.Logger
import com.kmpsdk.core.messaging.MessageDuration
import com.kmpsdk.core.messaging.MessageNotifier
import com.kmpsdk.presentation.state.DataState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface MviIntent

interface MviState

/** One-off side effects for the host app to handle (navigation, messages, etc.). */
interface MviEffect

fun interface MviReducer<S : MviState, I : MviIntent> {
    fun reduce(state: S, intent: I): S
}

abstract class MviViewModel<S : MviState, I : MviIntent, E : MviEffect>(
    initialState: S,
    private val reducer: MviReducer<S, I>,
    protected val scope: CoroutineScope,
    private val logger: Logger = Logger.create("MviViewModel"),
) {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<E>(extraBufferCapacity = 64)
    val effects: SharedFlow<E> = _effects.asSharedFlow()

    val currentState: S get() = _state.value

    open fun dispatch(intent: I) {
        logger.d("Intent: $intent")
        _state.update { current -> reducer.reduce(current, intent) }
    }

    fun setState(transform: (S) -> S) {
        _state.update(transform)
    }

    fun emitMessage(
        notifier: MessageNotifier,
        message: String,
        duration: MessageDuration = MessageDuration.SHORT,
    ) {
        scope.launch { notifier.show(message, duration) }
    }

    protected fun sendEffect(effect: E) {
        scope.launch { _effects.emit(effect) }
    }

    protected fun <T> bindDataState(
        dataState: DataState<T>,
        onSuccess: (T) -> Unit,
        onLoading: () -> Unit = {},
        onIdle: () -> Unit = {},
        onNoNetwork: () -> Unit = {},
        onFailure: (com.kmpsdk.domain.error.KmpSdkError) -> Unit = {},
    ) {
        when (dataState) {
            is DataState.Idle -> onIdle()
            is DataState.Loading -> onLoading()
            is DataState.Success -> onSuccess(dataState.data)
            is DataState.NoNetwork -> onNoNetwork()
            is DataState.Failure -> onFailure(dataState.error)
        }
    }
}
