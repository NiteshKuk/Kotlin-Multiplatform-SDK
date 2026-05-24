package com.kmpsdk.data.network

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.http.ContentType
import kotlinx.serialization.json.Json

internal val OfflineBodyKey = io.ktor.util.AttributeKey<String>("KmpSdkOfflineBody")
internal val OfflineHeadersKey = io.ktor.util.AttributeKey<Map<String, String>>("KmpSdkOfflineHeaders")

fun HttpRequestBuilder.captureForOffline(jsonBody: String? = null) {
    jsonBody?.let { attributes.put(OfflineBodyKey, it) }
}

fun HttpRequestBuilder.setJsonBodyWithOfflineCapture(body: String, contentType: ContentType = ContentType.Application.Json) {
    attributes.put(OfflineBodyKey, body)
    contentType(contentType)
    setBody(TextContent(body, contentType))
}

internal fun HttpRequestBuilder.extractOfflinePayload(): Pair<String?, Map<String, String>> {
    val body = attributes.getOrNull(OfflineBodyKey)
    val headers = attributes.getOrNull(OfflineHeadersKey) ?: buildMap {
        headers.entries().forEach { (key, values) ->
            put(key, values.joinToString(","))
        }
    }
    return body to headers
}
