package com.kmpsdk.data.network.interceptor

import com.kmpsdk.core.auth.SessionManager
import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.logger.Logger
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

/**
 * Attaches Bearer token to outgoing requests and handles 401 session expiry.
 */
fun createKmpSdkAuthPlugin(
    sessionManager: SessionManager,
    config: KmpSdkConfig,
    logger: Logger,
): ClientPlugin<Unit> = createClientPlugin("KmpSdkAuth") {
    onRequest { request, _ ->
        val authConfig = config.auth
        if (!authConfig.enabled) return@onRequest
        if (request.headers.contains(HttpHeaders.Authorization)) return@onRequest

        val token = sessionManager.cachedAccessToken
        if (!token.isNullOrBlank()) {
            request.headers.append(
                authConfig.headerName,
                "${authConfig.tokenPrefix} $token",
            )
        }
    }

    onResponse { response ->
        val authConfig = config.auth
        if (!authConfig.enabled) return@onResponse
        if (response.status != HttpStatusCode.Unauthorized &&
            response.status != HttpStatusCode.Forbidden
        ) {
            return@onResponse
        }

        val recovered = sessionManager.handleUnauthorized(response.status.value)
        if (!recovered) {
            logger.w("Auth failure for ${response.request.url} (${response.status.value})")
        }
    }
}
