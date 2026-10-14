package com.kmpsdk.data.network.error

import com.kmpsdk.domain.error.ApiError
import com.kmpsdk.domain.error.KmpSdkError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Parses common API error JSON shapes into [ApiError] and [KmpSdkError].
 *
 * Supported shapes:
 * - `{ "code": "USER_NOT_FOUND", "message": "..." }`
 * - `{ "error": "invalid_grant", "error_description": "..." }`
 * - `{ "message": "Validation failed", "errors": { "email": "invalid" } }`
 * - `{ "errors": [{ "field": "email", "message": "required" }] }`
 */
class ApiErrorParser(
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {
    fun parse(httpCode: Int, responseBody: String?): ApiError {
        if (responseBody.isNullOrBlank()) {
            return ApiError(
                code = null,
                message = "HTTP $httpCode",
                rawBody = responseBody,
            )
        }

        return runCatching { parseJson(responseBody) }
            .getOrElse {
                ApiError(
                    code = null,
                    message = responseBody.take(200),
                    rawBody = responseBody,
                )
            }
    }

    fun toKmpSdkError(httpCode: Int, responseBody: String?, fallbackMessage: String): KmpSdkError {
        val apiError = parse(httpCode, responseBody)
        val message = apiError.message.ifBlank { fallbackMessage }
        val jamBusinessFailure = isJamApiBusinessFailure(responseBody)

        return when {
            httpCode == 401 && !jamBusinessFailure -> KmpSdkError.Auth(
                message = message,
                httpCode = httpCode,
                responseBody = responseBody,
            )
            httpCode == 403 && !jamBusinessFailure -> KmpSdkError.Auth(
                message = message,
                httpCode = httpCode,
                responseBody = responseBody,
            )
            httpCode in 500..599 -> KmpSdkError.Server(
                message = message,
                httpCode = httpCode,
                responseBody = responseBody,
            )
            httpCode in 400..499 || jamBusinessFailure -> KmpSdkError.Business(
                message = message,
                code = apiError.code,
                httpCode = httpCode,
                responseBody = responseBody,
                fieldErrors = apiError.fieldErrors,
                apiError = apiError,
            )
            else -> KmpSdkError.Unknown(
                message = message,
                httpCode = httpCode,
                responseBody = responseBody,
            )
        }
    }

    /**
     * JAM APIs return `{ "success": false, "error": { "code", "message" } }` for business failures,
     * including many HTTP 403 cases that must not clear the auth session.
     */
    private fun isJamApiBusinessFailure(responseBody: String?): Boolean {
        if (responseBody.isNullOrBlank()) return false
        val root = runCatching { json.parseToJsonElement(responseBody) }.getOrNull() as? JsonObject
            ?: return false
        val success = (root["success"] as? JsonPrimitive)?.contentOrNull
        if (success != "false") return false
        return root.obj("error")?.string("code") != null ||
            root.obj("error")?.string("message") != null
    }

    private fun parseJson(body: String): ApiError {
        val root = json.parseToJsonElement(body)
        if (root !is JsonObject) {
            return ApiError(code = null, message = body.take(200), rawBody = body)
        }

        val code = root.string("code")
            ?: root.obj("error")?.string("code")
            ?: root.string("error_code")
            ?: root.string("error")
        val message = root.string("message")
            ?: root.obj("error")?.string("message")
            ?: root.string("error_description")
            ?: root.string("detail")
            ?: root.string("title")
            ?: code
            ?: "Request failed"

        val fieldErrors = parseFieldErrors(root)

        return ApiError(
            code = code,
            message = message,
            fieldErrors = fieldErrors,
            rawBody = body,
        )
    }

    private fun parseFieldErrors(root: JsonObject): Map<String, String> {
        val errorsElement = root["errors"] ?: return emptyMap()

        return when (errorsElement) {
            is JsonObject -> errorsElement.mapValues { (_, value) ->
                value.toErrorMessage()
            }
            is JsonArray -> errorsElement.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val field = obj.string("field") ?: obj.string("name") ?: return@mapNotNull null
                field to obj.string("message").orEmpty()
            }.toMap()
            else -> emptyMap()
        }
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.obj(key: String): JsonObject? =
        this[key] as? JsonObject

    private fun JsonElement.toErrorMessage(): String = when (this) {
        is JsonPrimitive -> contentOrNull ?: toString()
        is JsonArray -> joinToString(", ") { it.toErrorMessage() }
        else -> toString()
    }
}
