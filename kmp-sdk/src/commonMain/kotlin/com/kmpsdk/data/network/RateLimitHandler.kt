package com.kmpsdk.data.network

import com.kmpsdk.core.config.KmpSdkConfig
import kotlinx.coroutines.delay

internal class RateLimitHandler(
    private val config: KmpSdkConfig,
) {
    suspend fun <T> executeWithBackoff(block: suspend () -> T): T {
        if (!config.enableRateLimitBackoff) return block()

        var attempt = 0
        var lastError: Throwable? = null

        while (attempt <= config.maxRateLimitRetries) {
            try {
                return block()
            } catch (ex: KmpHttpException) {
                if (ex.httpCode != 429 && ex.httpCode != 503) throw ex
                lastError = ex
                val backoffMs = (500L * (1 shl attempt)).coerceAtMost(30_000L)
                delay(backoffMs)
                attempt++
            }
        }
        throw lastError ?: IllegalStateException("Rate limit retries exhausted")
    }
}
