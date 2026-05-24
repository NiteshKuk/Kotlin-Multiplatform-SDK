package com.kmpsdk.presentation.state

import com.kmpsdk.domain.error.KmpSdkError
import com.kmpsdk.domain.error.httpStatusCode

/**
 * Headless loading/result model for host apps to bind to their own UI.
 */
sealed class DataState<out T> {
    data object Idle : DataState<Nothing>()
    data object Loading : DataState<Nothing>()
    data class Success<T>(val data: T) : DataState<T>()
    data class Failure(val error: KmpSdkError) : DataState<Nothing>()
    data object NoNetwork : DataState<Nothing>()

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure || this is NoNetwork

    fun getOrNull(): T? = (this as? Success)?.data

    inline fun <R> map(transform: (T) -> R): DataState<R> = when (this) {
        is Idle -> Idle
        is Loading -> Loading
        is Success -> Success(transform(data))
        is Failure -> this
        is NoNetwork -> NoNetwork
    }
}

fun <T> T.asSuccess(): DataState<T> = DataState.Success(this)

fun KmpSdkError.asFailure(): DataState<Nothing> = DataState.Failure(this)

fun DataState<*>.toErrorMessage(): String = when (this) {
    is DataState.Idle -> ""
    is DataState.Loading -> "Loading..."
    is DataState.Success -> ""
    is DataState.NoNetwork -> "No internet connection"
    is DataState.Failure -> {
        val code = error.httpStatusCode
        if (code != null) "${error.message} (HTTP $code)" else error.message
    }
}
