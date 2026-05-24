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

        return when (httpCode) {
            401, 403 -> KmpSdkError.Auth(
                message = message,
                httpCode = httpCode,
                responseBody = responseBody,
            )
            in 500..599 -> KmpSdkError.Server(
                message = message,
                httpCode = httpCode,
                responseBody = responseBody,
            )
            in 400..499 -> KmpSdkError.Business(
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

    private fun parseJson(body: String): ApiError {
        val root = json.parseToJsonElement(body)
        if (root !is JsonObject) {
            return ApiError(code = null, message = body.take(200), rawBody = body)
        }

        val code = root.string("code")
            ?: root.string("error_code")
            ?: root.string("error")
        val message = root.string("message")
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

    private fun JsonElement.toErrorMessage(): String = when (this) {
        is JsonPrimitive -> contentOrNull ?: toString()
        is JsonArray -> joinToString(", ") { it.toErrorMessage() }
        else -> toString()
    }
}
