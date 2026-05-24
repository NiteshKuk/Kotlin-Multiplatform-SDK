package com.kmpsdk.platform

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Swift-friendly wrapper around Kotlin [StateFlow].
 */
class ObservableState<T> internal constructor(
    private val source: StateFlow<T>,
) {
    val value: T get() = source.value

    fun observe(scope: CoroutineScope, onChange: (T) -> Unit): Cancellable {
        val job = scope.launch {
            source.collect { onChange(it) }
        }
        return Cancellable(job)
    }
}

class ObservableEvents<T> internal constructor(
    private val source: SharedFlow<T>,
) {
    fun observe(scope: CoroutineScope, onEvent: (T) -> Unit): Cancellable {
        val job = scope.launch {
            source.collect { onEvent(it) }
        }
        return Cancellable(job)
    }
}

class Cancellable internal constructor(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}

fun <T> StateFlow<T>.asObservable(): ObservableState<T> = ObservableState(this)

fun <T> SharedFlow<T>.asObservableEvents(): ObservableEvents<T> = ObservableEvents(this)
