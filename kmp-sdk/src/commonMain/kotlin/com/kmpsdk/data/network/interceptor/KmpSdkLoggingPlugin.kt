package com.kmpsdk.data.network.interceptor

import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.logger.Logger
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining

private val RequestBodyKey = io.ktor.util.AttributeKey<String>("KmpSdkRequestBody")

/**
 * Factory avoids [pluginConfig] receiver issues in Ktor 3 + Kotlin 2.x and lateinit init-order bugs.
 */
fun createKmpSdkLoggingPlugin(
    logger: Logger,
    config: KmpSdkConfig,
): ClientPlugin<Unit> = createClientPlugin("KmpSdkLogging") {
    onRequest { request, body ->
        val bodyText = extractRequestBody(body)
        if (bodyText != null) {
            request.attributes.put(RequestBodyKey, bodyText)
        }

        if (config.enableRequestLogging) {
            logger.d(buildRequestLog(request, bodyText, config))
            if (config.enableCurlLogging) {
                logger.d(buildCurlCommand(request, bodyText, config))
            }
        }
    }

    onResponse { response ->
        if (config.enableRequestLogging) {
            val requestBody = response.call.request.attributes.getOrNull(RequestBodyKey)
            logger.d(buildResponseLog(response, requestBody, config))
        }
    }
}

private suspend fun extractRequestBody(body: Any?): String? = when (body) {
    is OutgoingContent.ByteArrayContent -> body.bytes().decodeToString()
    is OutgoingContent.NoContent -> null
    is OutgoingContent.ReadChannelContent -> runCatching {
        body.readFrom().readRemaining().readText(Charsets.UTF_8)
    }.getOrNull()
    else -> null
}

private fun buildRequestLog(
    request: HttpRequestBuilder,
    body: String?,
    config: KmpSdkConfig,
): String = buildString {
    appendLine("─── HTTP REQUEST ───")
    appendLine("${request.method.value} ${request.url.buildString()}")
    appendLine("Headers:")
    appendLine(redactHeaders(request.headers.build(), config.redactedHeaderKeys))
    if (config.enableResponseBodyLogging && !body.isNullOrBlank()) {
        appendLine("Body: $body")
    }
}

private suspend fun buildResponseLog(
    response: HttpResponse,
    requestBody: String?,
    config: KmpSdkConfig,
): String = buildString {
    appendLine("─── HTTP RESPONSE ───")
    appendLine("${response.call.request.method.value} ${response.call.request.url}")
    appendLine("Status: ${response.status.value} ${response.status.description}")
    appendLine("Response Headers:")
    appendLine(redactHeaders(response.headers, config.redactedHeaderKeys))
    if (config.enableResponseBodyLogging) {
        val responseBody = runCatching { response.bodyAsText() }.getOrNull()
        if (!responseBody.isNullOrBlank()) {
            appendLine("Response Body: $responseBody")
        }
    }
    if (!requestBody.isNullOrBlank()) {
        appendLine("Request Body (ref): $requestBody")
    }
}

private fun buildCurlCommand(
    request: HttpRequestBuilder,
    body: String?,
    config: KmpSdkConfig,
): String = buildString {
    append("curl -X ${request.method.value}")
    request.headers.build().entries().forEach { (key, values) ->
        values.forEach { value ->
            val headerValue = if (key.lowercase() in config.redactedHeaderKeys) "***" else value
            append(" -H \"$key: $headerValue\"")
        }
    }
    val contentType = request.contentType()?.toString()
    if (contentType != null) {
        append(" -H \"Content-Type: $contentType\"")
    }
    if (!body.isNullOrBlank()) {
        append(" -d '${body.replace("'", "'\\''")}'")
    }
    append(" \"${request.url.buildString()}\"")
}

private fun redactHeaders(headers: Headers, redactedKeys: Set<String>): String =
    headers.entries().joinToString("\n") { (key, values) ->
        val display = if (key.lowercase() in redactedKeys) "***" else values.joinToString(", ")
        "  $key: $display"
    }
