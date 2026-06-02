package com.kmpsdk.data.network.interceptor

import com.kmpsdk.core.auth.SessionManager
import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.logger.Logger
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class AuthPluginConfig {
    lateinit var sessionManager: SessionManager
    lateinit var config: KmpSdkConfig
    lateinit var logger: Logger
}

/**
 * Attaches Bearer token to outgoing requests and handles 401 session expiry.
 */
val KmpSdkAuthPlugin = createClientPlugin("KmpSdkAuth", ::AuthPluginConfig) {
    onRequest { request, _ ->
        val authConfig = pluginConfig.config.auth
        if (!authConfig.enabled) return@onRequest
        if (request.headers.contains(HttpHeaders.Authorization)) return@onRequest

        val token = pluginConfig.sessionManager.cachedAccessToken
        if (!token.isNullOrBlank()) {
            request.headers.append(
                authConfig.headerName,
                "${authConfig.tokenPrefix} $token",
            )
        }
    }

    onResponse { response ->
        val authConfig = pluginConfig.config.auth
        if (!authConfig.enabled) return@onResponse
        if (response.status != HttpStatusCode.Unauthorized &&
            response.status != HttpStatusCode.Forbidden
        ) {
            return@onResponse
        }

        val recovered = pluginConfig.sessionManager.handleUnauthorized(response.status.value)
        if (!recovered) {
            pluginConfig.logger.w("Auth failure for ${response.request.url} (${response.status.value})")
        }
    }
}
