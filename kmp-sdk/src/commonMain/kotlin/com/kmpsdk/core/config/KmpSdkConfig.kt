package com.kmpsdk.core.config

import com.kmpsdk.core.logger.LogLevel
import com.kmpsdk.core.resilience.ResilienceConfig
import com.kmpsdk.data.offline.OfflineReplayStrategy
import com.kmpsdk.domain.sync.SyncPolicy

/**
 * DSL-friendly configuration for [com.kmpsdk.KmpSdk.init].
 */
data class KmpSdkConfig(
    val baseUrl: String,
    val logLevel: LogLevel = LogLevel.INFO,
    val enableRequestLogging: Boolean = true,
    val enableResponseBodyLogging: Boolean = false,
    val enableCurlLogging: Boolean = false,
    val redactedHeaderKeys: Set<String> = DEFAULT_REDACTED_HEADERS,
    val defaultCacheTtlMillis: Long = 5 * 60 * 1000L,
    val offlineReplayStrategy: OfflineReplayStrategy = OfflineReplayStrategy.FIFO,
    val maxOfflineRetries: Int = 3,
    val syncPolicy: SyncPolicy = SyncPolicy.STALE_WHILE_REVALIDATE,
    val auth: AuthConfig = AuthConfig(),
    val enableHttpCache: Boolean = true,
    val autoSyncOnReconnect: Boolean = true,
    val autoRefreshOnObserve: Boolean = false,
    val queueMutationsWhenOffline: Boolean = true,
    val enableRequestDeduplication: Boolean = true,
    val enableRateLimitBackoff: Boolean = true,
    val maxRateLimitRetries: Int = 3,
    val certificateBuilder: CertificateParams = CertificateParams(),
    val backgroundSyncIntervalMillis: Long? = null,
    val validateOnStartup: Boolean = true,
    val resilience: ResilienceConfig = ResilienceConfig(),
) {
    companion object {
        val DEFAULT_REDACTED_HEADERS = setOf(
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "x-auth-token",
        )
    }
}

data class CertificateParams(
    val hostname: String = "",
    val certificatePins: List<String> = emptyList(),
)



class KmpSdkConfigBuilder {
    var baseUrl: String = ""
    var logLevel: LogLevel = LogLevel.INFO
    var enableRequestLogging: Boolean = true
    var enableResponseBodyLogging: Boolean = false
    var enableCurlLogging: Boolean = false
    var redactedHeaderKeys: Set<String> = KmpSdkConfig.DEFAULT_REDACTED_HEADERS
    var defaultCacheTtlMillis: Long = 5 * 60 * 1000L
    var offlineReplayStrategy: OfflineReplayStrategy = OfflineReplayStrategy.FIFO
    var maxOfflineRetries: Int = 3
    var syncPolicy: SyncPolicy = SyncPolicy.STALE_WHILE_REVALIDATE
    var auth: AuthConfig = AuthConfig()
    var enableHttpCache: Boolean = true
    var autoSyncOnReconnect: Boolean = true
    var autoRefreshOnObserve: Boolean = false
    var queueMutationsWhenOffline: Boolean = true
    var enableRequestDeduplication: Boolean = true
    var enableRateLimitBackoff: Boolean = true
    var maxRateLimitRetries: Int = 3
    var certificateBuilder: CertificateParams = CertificateParams()
    var backgroundSyncIntervalMillis: Long? = null
    var validateOnStartup: Boolean = true
    var resilience: ResilienceConfig = ResilienceConfig()

    fun auth(block: AuthConfigBuilder.() -> Unit) {
        auth = AuthConfigBuilder().apply(block).build()
    }

    fun resilience(block: com.kmpsdk.core.resilience.ResilienceDsl.() -> Unit) {
        resilience = com.kmpsdk.core.resilience.ResilienceDsl().apply(block).build()
    }

    fun build(): KmpSdkConfig = KmpSdkConfig(
        baseUrl = baseUrl.also { require(it.isNotBlank()) { "baseUrl is required" } },
        logLevel = logLevel,
        enableRequestLogging = enableRequestLogging,
        enableResponseBodyLogging = enableResponseBodyLogging,
        enableCurlLogging = enableCurlLogging,
        redactedHeaderKeys = redactedHeaderKeys,
        defaultCacheTtlMillis = defaultCacheTtlMillis,
        offlineReplayStrategy = offlineReplayStrategy,
        maxOfflineRetries = maxOfflineRetries,
        syncPolicy = syncPolicy,
        auth = auth,
        enableHttpCache = enableHttpCache,
        autoSyncOnReconnect = autoSyncOnReconnect,
        autoRefreshOnObserve = autoRefreshOnObserve,
        queueMutationsWhenOffline = queueMutationsWhenOffline,
        enableRequestDeduplication = enableRequestDeduplication,
        enableRateLimitBackoff = enableRateLimitBackoff,
        maxRateLimitRetries = maxRateLimitRetries,
        certificateBuilder = certificateBuilder,
        backgroundSyncIntervalMillis = backgroundSyncIntervalMillis,
        validateOnStartup = validateOnStartup,
        resilience = resilience,
    )
}

fun kmpSdkConfig(block: KmpSdkConfigBuilder.() -> Unit): KmpSdkConfig =
    KmpSdkConfigBuilder().apply(block).build()
