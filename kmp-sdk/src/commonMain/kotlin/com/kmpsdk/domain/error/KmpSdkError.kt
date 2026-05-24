package com.kmpsdk.domain.error

/**
 * Errors originating from a non-2xx HTTP response.
 */
sealed interface HttpFailure {
    val httpCode: Int
    val responseBody: String?
}

/**
 * Global error model exposed consistently to presenters and UI.
 */
sealed class KmpSdkError {
    abstract val message: String
    open val cause: Throwable? = null

    data class Network(
        override val message: String = "Network unavailable or unreachable",
        override val cause: Throwable? = null,
    ) : KmpSdkError()

    data class Auth(
        override val message: String = "Authentication failed",
        override val httpCode: Int = 401,
        override val responseBody: String? = null,
        override val cause: Throwable? = null,
    ) : KmpSdkError(), HttpFailure

    data class Business(
        override val message: String,
        val code: String? = null,
        override val httpCode: Int,
        override val responseBody: String? = null,
        val fieldErrors: Map<String, String> = emptyMap(),
        val apiError: ApiError? = null,
        override val cause: Throwable? = null,
    ) : KmpSdkError(), HttpFailure

    data class Server(
        override val message: String = "Server error",
        override val httpCode: Int,
        override val responseBody: String? = null,
        override val cause: Throwable? = null,
    ) : KmpSdkError(), HttpFailure

    data class Parse(
        override val message: String = "Failed to parse response",
        override val cause: Throwable? = null,
    ) : KmpSdkError()

    data class Unknown(
        override val message: String = "Unexpected error",
        val httpCode: Int? = null,
        val responseBody: String? = null,
        override val cause: Throwable? = null,
    ) : KmpSdkError()
}

/** HTTP status when this error came from an API response; null for offline/parse errors. */
val KmpSdkError.httpStatusCode: Int?
    get() = when (this) {
        is HttpFailure -> httpCode
        is KmpSdkError.Unknown -> httpCode
        else -> null
    }

/** Raw response body from a failed HTTP call, when available. */
val KmpSdkError.responseBodyOrNull: String?
    get() = when (this) {
        is HttpFailure -> responseBody
        is KmpSdkError.Unknown -> responseBody
        else -> null
    }

fun KmpSdkError.isHttpStatus(code: Int): Boolean = httpStatusCode == code

fun KmpSdkError.isClientError(): Boolean = httpStatusCode?.let { it in 400..499 } == true

fun KmpSdkError.isServerError(): Boolean = httpStatusCode?.let { it in 500..599 } == true

sealed class KmpSdkResult<out T> {
    data class Success<T>(val data: T) : KmpSdkResult<T>()
    data class Failure(val error: KmpSdkError) : KmpSdkResult<Nothing>() {
        val httpStatusCode: Int? get() = error.httpStatusCode
        val responseBody: String? get() = error.responseBodyOrNull
    }

    inline fun <R> map(transform: (T) -> R): KmpSdkResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Failure -> this
    }

    inline fun onSuccess(block: (T) -> Unit): KmpSdkResult<T> {
        if (this is Success) block(data)
        return this
    }

    inline fun onFailure(block: (KmpSdkError) -> Unit): KmpSdkResult<T> {
        if (this is Failure) block(error)
        return this
    }

    inline fun onHttpFailure(block: (httpCode: Int, body: String?) -> Unit): KmpSdkResult<T> {
        if (this is Failure) {
            error.httpStatusCode?.let { code -> block(code, error.responseBodyOrNull) }
        }
        return this
    }
}

inline fun <T> kmpSdkResultOf(block: () -> T): KmpSdkResult<T> =
    try {
        KmpSdkResult.Success(block())
    } catch (throwable: Throwable) {
        KmpSdkResult.Failure(KmpSdkError.Unknown(cause = throwable))
    }
