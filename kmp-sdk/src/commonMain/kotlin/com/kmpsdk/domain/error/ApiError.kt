package com.kmpsdk.domain.error

/**
 * Structured API error parsed from a non-2xx JSON response body.
 */
data class ApiError(
    val code: String?,
    val message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
    val rawBody: String? = null,
)

val KmpSdkError.apiErrorOrNull: ApiError?
    get() = when (this) {
        is KmpSdkError.Business -> apiError ?: ApiError(
            code = code,
            message = message,
            fieldErrors = fieldErrors,
            rawBody = responseBody,
        )
        is KmpSdkError.Auth -> ApiError(
            code = "AUTH_ERROR",
            message = message,
            rawBody = responseBody,
        )
        is KmpSdkError.Server -> ApiError(
            code = "SERVER_ERROR",
            message = message,
            rawBody = responseBody,
        )
        is KmpSdkError.Unknown -> ApiError(
            code = null,
            message = message,
            rawBody = responseBody,
        )
        else -> null
    }

val KmpSdkError.fieldErrors: Map<String, String>
    get() = when (this) {
        is KmpSdkError.Business -> fieldErrors
        else -> apiErrorOrNull?.fieldErrors.orEmpty()
    }
