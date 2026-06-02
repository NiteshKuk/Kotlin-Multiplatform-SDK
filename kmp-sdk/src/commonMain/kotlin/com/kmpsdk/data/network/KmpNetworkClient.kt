package com.kmpsdk.data.network

import com.kmpsdk.core.auth.SessionManager
import com.kmpsdk.core.auth.TokenStore
import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.connectivity.ConnectivityMonitor
import com.kmpsdk.core.tenant.TenantContext
import com.kmpsdk.core.logger.Logger
import com.kmpsdk.data.cache.CacheStore
import com.kmpsdk.data.network.interceptor.createKmpSdkAuthPlugin
import com.kmpsdk.data.network.interceptor.createKmpSdkLoggingPlugin
import com.kmpsdk.core.telemetry.KmpSdkTelemetry
import com.kmpsdk.core.telemetry.TelemetryEvent
import com.kmpsdk.data.network.error.ApiErrorParser
import com.kmpsdk.data.offline.OfflineQueueManager
import com.kmpsdk.data.offline.OfflineRequestPayload
import com.kmpsdk.domain.error.KmpSdkError
import com.kmpsdk.domain.error.KmpSdkResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
import kotlinx.datetime.Clock
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

expect fun createPlatformHttpClient(config: KmpSdkConfig, logger: Logger): HttpClient

class KmpNetworkClient(
    private val config: KmpSdkConfig,
    private val connectivityMonitor: ConnectivityMonitor,
    private val tokenStore: TokenStore,
    private val sessionManager: SessionManager,
    private val cacheStore: CacheStore,
    private val offlineQueueProvider: () -> OfflineQueueManager,
    private val logger: Logger = Logger.create("KmpNetworkClient"),
) {
    private val offlineQueue: OfflineQueueManager get() = offlineQueueProvider()

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = config.enableRequestLogging
    }

    private val apiErrorParser = ApiErrorParser(json)
    private val deduplicator = RequestDeduplicator()
    private val rateLimitHandler = RateLimitHandler(config)
    private var activeBaseUrl: String = config.baseUrl
    private var activeTenantHeaders: Map<String, String> = emptyMap()

    val httpClient: HttpClient = createPlatformHttpClient(config, logger).config {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        install(createKmpSdkLoggingPlugin(logger, config))
        if (config.auth.enabled) {
            install(createKmpSdkAuthPlugin(sessionManager, config, logger))
        }
        defaultRequest {
            url(activeBaseUrl)
            activeTenantHeaders.forEach { (key, value) -> header(key, value) }
            contentType(ContentType.Application.Json)
        }
    }

    fun applyTenant(context: TenantContext) {
        activeBaseUrl = context.baseUrl
        activeTenantHeaders = context.headers
    }

    suspend inline fun <reified T> get(
        path: String,
        useCache: Boolean = true,
        cacheTtlMillis: Long? = null,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): KmpSdkResult<T> = safeRequest(
        method = HttpMethod.Get,
        path = path,
        serializer = serializer(),
        useCache = useCache,
        cacheTtlMillis = cacheTtlMillis,
        block = block,
    )

    suspend inline fun <reified T> post(
        path: String,
        offlineBody: String? = null,
        offlineHeaders: Map<String, String> = emptyMap(),
        priority: Int = 0,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): KmpSdkResult<T> = safeRequest(
        method = HttpMethod.Post,
        path = path,
        serializer = serializer(),
        useCache = false,
        cacheTtlMillis = null,
        block = block,
        offlineBody = offlineBody,
        offlineHeaders = offlineHeaders,
        priority = priority,
    )

    suspend inline fun <reified T> put(
        path: String,
        offlineBody: String? = null,
        offlineHeaders: Map<String, String> = emptyMap(),
        priority: Int = 0,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): KmpSdkResult<T> = safeRequest(
        method = HttpMethod.Put,
        path = path,
        serializer = serializer(),
        useCache = false,
        cacheTtlMillis = null,
        block = block,
        offlineBody = offlineBody,
        offlineHeaders = offlineHeaders,
        priority = priority,
    )

    suspend inline fun <reified T> patch(
        path: String,
        offlineBody: String? = null,
        offlineHeaders: Map<String, String> = emptyMap(),
        priority: Int = 0,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): KmpSdkResult<T> = safeRequest(
        method = HttpMethod.Patch,
        path = path,
        serializer = serializer(),
        useCache = false,
        cacheTtlMillis = null,
        block = block,
        offlineBody = offlineBody,
        offlineHeaders = offlineHeaders,
        priority = priority,
    )

    suspend inline fun <reified T> delete(
        path: String,
        offlineHeaders: Map<String, String> = emptyMap(),
        priority: Int = 0,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): KmpSdkResult<T> = safeRequest(
        method = HttpMethod.Delete,
        path = path,
        serializer = serializer(),
        useCache = false,
        cacheTtlMillis = null,
        block = block,
        offlineHeaders = offlineHeaders,
        priority = priority,
    )

    suspend fun <T> safeRequest(
        method: HttpMethod,
        path: String,
        serializer: KSerializer<T>,
        useCache: Boolean = method == HttpMethod.Get,
        cacheTtlMillis: Long? = null,
        block: HttpRequestBuilder.() -> Unit = {},
        offlineBody: String? = null,
        offlineHeaders: Map<String, String> = emptyMap(),
        priority: Int = 0,
    ): KmpSdkResult<T> {
        val cacheKey = buildHttpCacheKey(method, path)
        val capture = captureRequest(block)
        val effectiveBody = offlineBody ?: capture.body
        val effectiveHeaders = offlineHeaders.ifEmpty { capture.headers }

        if (!connectivityMonitor.isOnline()) {
            if (method == HttpMethod.Get && config.enableHttpCache && useCache) {
                readCachedResponse(cacheKey, serializer)?.let { return it }
            }
            if (method != HttpMethod.Get && config.queueMutationsWhenOffline) {
                offlineQueue.enqueue(
                    OfflineRequestPayload(
                        method = method.value,
                        url = path,
                        headers = effectiveHeaders,
                        body = effectiveBody,
                        priority = priority,
                    ),
                )
                KmpSdkTelemetry.emit(TelemetryEvent.OfflineQueued(method.value, path))
                return KmpSdkResult.Failure(KmpSdkError.Network("Queued for offline replay"))
            }
            return KmpSdkResult.Failure(KmpSdkError.Network("Device is offline"))
        }

        val started = Clock.System.now().toEpochMilliseconds()
        return try {
            val bodyText = if (method == HttpMethod.Get && config.enableRequestDeduplication) {
                deduplicator.execute(cacheKey) {
                    rateLimitHandler.executeWithBackoff {
                        executeWithAuthRetry(method, path, capture.block)
                    }
                }
            } else {
                rateLimitHandler.executeWithBackoff {
                    executeWithAuthRetry(method, path, capture.block)
                }
            }
            if (method == HttpMethod.Get && config.enableHttpCache && useCache && bodyText != null) {
                cacheStore.put(cacheKey, bodyText, cacheTtlMillis)
            }
            val result = KmpSdkResult.Success(json.decodeFromString(serializer, bodyText ?: "null"))
            emitApiTelemetry(method, path, statusCode = 200, started = started, success = true)
            result
        } catch (exception: KmpHttpException) {
            emitApiTelemetry(method, path, exception.httpCode, started, success = false)
            if (method == HttpMethod.Get && config.enableHttpCache && useCache) {
                readCachedResponse(cacheKey, serializer)?.let { return it }
            }
            KmpSdkResult.Failure(exception.toKmpSdkError(apiErrorParser))
        } catch (exception: Throwable) {
            emitApiTelemetry(method, path, statusCode = null, started = started, success = false)
            logger.e("Request failed: $method $path", exception)
            if (method == HttpMethod.Get && config.enableHttpCache && useCache) {
                readCachedResponse(cacheKey, serializer)?.let { return it }
            }
            KmpSdkResult.Failure(mapThrowable(exception))
        }
    }

    suspend inline fun <reified T> uploadMultipart(
        request: MultipartUploadRequest,
    ): KmpSdkResult<T> = safeRequest(
        method = HttpMethod.Post,
        path = request.path,
        serializer = serializer(),
        useCache = false,
        block = {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        request.fields.forEach { (key, value) -> append(key, value) }
                        request.parts.forEach { part ->
                            append(
                                key = part.fieldName,
                                value = buildPacket { writeFully(part.bytes) },
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, part.contentType)
                                    append(HttpHeaders.ContentDisposition, "filename=\"${part.fileName}\"")
                                },
                            )
                        }
                    },
                ),
            )
        },
    )

    private data class CapturedRequest(
        val block: HttpRequestBuilder.() -> Unit,
        val body: String?,
        val headers: Map<String, String>,
    )

    private fun captureRequest(block: HttpRequestBuilder.() -> Unit): CapturedRequest {
        val probe = HttpRequestBuilder()
        probe.apply(block)
        val (body, headers) = probe.extractOfflinePayload()
        return CapturedRequest(block, body, headers)
    }

    private fun emitApiTelemetry(
        method: HttpMethod,
        path: String,
        statusCode: Int?,
        started: Long,
        success: Boolean,
    ) {
        val duration = Clock.System.now().toEpochMilliseconds() - started
        KmpSdkTelemetry.emit(
            TelemetryEvent.ApiCallCompleted(
                method = method.value,
                path = path,
                statusCode = statusCode,
                durationMs = duration,
                success = success,
            ),
        )
    }

    private suspend fun <T> readCachedResponse(
        cacheKey: String,
        serializer: KSerializer<T>,
    ): KmpSdkResult.Success<T>? {
        val cached = cacheStore.get(cacheKey) ?: return null
        return runCatching {
            KmpSdkResult.Success(json.decodeFromString(serializer, cached))
        }.getOrNull()
    }

    private suspend fun executeWithAuthRetry(
        method: HttpMethod,
        path: String,
        block: HttpRequestBuilder.() -> Unit,
    ): String? {
        return try {
            readSuccessfulBody(executeOnce(method, path, block))
        } catch (exception: KmpHttpException) {
            if (config.auth.enabled && (exception.httpCode == 401 || exception.httpCode == 403)) {
                if (sessionManager.handleUnauthorized(exception.httpCode)) {
                    return readSuccessfulBody(executeOnce(method, path, block))
                }
            }
            throw exception
        }
    }

    private suspend fun executeOnce(
        method: HttpMethod,
        path: String,
        block: HttpRequestBuilder.() -> Unit,
    ): HttpResponse = when (method) {
        HttpMethod.Get -> httpClient.get(path, block)
        HttpMethod.Post -> httpClient.post(path, block)
        HttpMethod.Put -> httpClient.put(path, block)
        HttpMethod.Patch -> httpClient.patch(path, block)
        HttpMethod.Delete -> httpClient.delete(path, block)
        else -> error("Unsupported HTTP method: $method")
    }

    private suspend fun readSuccessfulBody(response: HttpResponse): String? {
        val bodyText = runCatching { response.bodyAsText() }.getOrNull()
        if (!response.status.isSuccess()) {
            throw KmpHttpException(
                httpCode = response.status.value,
                responseBody = bodyText,
                message = bodyText?.takeIf { it.isNotBlank() }
                    ?: "HTTP ${response.status.value}: ${response.status.description}",
            )
        }
        return bodyText
    }

    fun mapThrowable(throwable: Throwable): KmpSdkError = when (throwable) {
        is KmpHttpException -> throwable.toKmpSdkError(apiErrorParser)
        else -> KmpSdkError.Network(cause = throwable)
    }
}

class KmpHttpException(
    val httpCode: Int,
    val responseBody: String?,
    override val message: String,
) : Exception(message) {
    fun toKmpSdkError(parser: ApiErrorParser = ApiErrorParser()): KmpSdkError =
        parser.toKmpSdkError(
            httpCode = httpCode,
            responseBody = responseBody,
            fallbackMessage = message,
        )

    companion object {
        suspend fun fromResponse(response: HttpResponse): KmpHttpException {
            val body = runCatching { response.bodyAsText() }.getOrNull()
            return KmpHttpException(
                httpCode = response.status.value,
                responseBody = body,
                message = body?.takeIf { it.isNotBlank() }
                    ?: "HTTP ${response.status.value}: ${response.status.description}",
            )
        }
    }
}

suspend inline fun <reified T> HttpRequestBuilder.setJsonBody(body: T, json: Json) {
    val encoded = json.encodeToString(serializer(), body)
    setJsonBodyWithOfflineCapture(encoded)
}

fun HttpRequestBuilder.authToken(token: String) {
    header("Authorization", "Bearer $token")
}
