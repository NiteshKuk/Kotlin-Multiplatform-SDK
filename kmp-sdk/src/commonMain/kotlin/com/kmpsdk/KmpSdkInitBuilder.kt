package com.kmpsdk

import com.kmpsdk.core.auth.TokenRefreshHandler
import com.kmpsdk.core.config.EnvironmentDsl
import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.config.RemoteConfigStore
import com.kmpsdk.core.config.SdkProfile
import com.kmpsdk.core.config.applyProfile
import com.kmpsdk.core.config.buildConfigForEnvironment
import com.kmpsdk.core.config.kmpSdkConfig
import com.kmpsdk.core.di.KmpSdkModule
import com.kmpsdk.core.logger.LogLevel
import com.kmpsdk.data.offline.OfflineReplayStrategy
import com.kmpsdk.domain.sync.SyncPolicy

/**
 * One-call init DSL for [KmpSdk.init].
 */
class KmpSdkInitBuilder {
    var baseUrl: String = ""
    var logLevel: LogLevel = LogLevel.INFO
    var enableRequestLogging: Boolean = true
    var enableResponseBodyLogging: Boolean = false
    var enableCurlLogging: Boolean = false
    var defaultCacheTtlMillis: Long = 5 * 60 * 1000L
    var offlineReplayStrategy: OfflineReplayStrategy = OfflineReplayStrategy.FIFO
    var maxOfflineRetries: Int = 3
    var syncPolicy: SyncPolicy = SyncPolicy.STALE_WHILE_REVALIDATE
    var enableHttpCache: Boolean = true
    var autoSyncOnReconnect: Boolean = true
    var autoRefreshOnObserve: Boolean = false
    var queueMutationsWhenOffline: Boolean = true
    var enableRequestDeduplication: Boolean = true
    var enableRateLimitBackoff: Boolean = true
    var maxRateLimitRetries: Int = 3
    var certificatePins: List<String> = emptyList()
    var backgroundSyncIntervalMillis: Long? = null
    var validateOnStartup: Boolean = true
    var tokenRefreshHandler: TokenRefreshHandler? = null
    var profile: SdkProfile? = null
    var environmentName: String? = null
    var remoteConfigFetcher: (suspend () -> Map<String, String>)? = null

    private val modules = mutableListOf<KmpSdkModule>()
    private var authConfigBlock: (com.kmpsdk.core.config.AuthConfigBuilder.() -> Unit)? = null
    private val environmentBlocks = linkedMapOf<String, com.kmpsdk.core.config.KmpSdkConfigBuilder.() -> Unit>()

    fun auth(block: com.kmpsdk.core.config.AuthConfigBuilder.() -> Unit) {
        authConfigBlock = block
    }

    fun install(module: KmpSdkModule) {
        modules += module
    }

    fun install(vararg module: KmpSdkModule) {
        modules += module
    }

    fun environments(block: EnvironmentDsl.() -> Unit) {
        environmentBlocks.putAll(EnvironmentDsl().apply(block).entries())
    }

    fun remoteConfig(fetcher: suspend () -> Map<String, String>) {
        remoteConfigFetcher = fetcher
    }

    internal fun buildConfig(): KmpSdkConfig {
        val config = if (environmentName != null && environmentBlocks.isNotEmpty()) {
            buildConfigForEnvironment(
                environmentName = environmentName!!,
                environments = environmentBlocks,
                fallback = { this@KmpSdkInitBuilder.applyToConfigBuilder(this) },
            )
        } else {
            kmpSdkConfig { this@KmpSdkInitBuilder.applyToConfigBuilder(this) }
        }

        return remoteConfigFetcher?.let { fetcher ->
            // applied after RemoteConfigStore refresh in KmpSdk.init
            config
        } ?: config
    }

    internal fun modules(): List<KmpSdkModule> = modules.toList()

    internal fun remoteConfigFetcherOrNull(): (suspend () -> Map<String, String>)? = remoteConfigFetcher

    private fun applyToConfigBuilder(target: com.kmpsdk.core.config.KmpSdkConfigBuilder) {
        profile?.let { target.applyProfile(it) }
        target.baseUrl = baseUrl
        target.logLevel = logLevel
        target.enableRequestLogging = enableRequestLogging
        target.enableResponseBodyLogging = enableResponseBodyLogging
        target.enableCurlLogging = enableCurlLogging
        target.defaultCacheTtlMillis = defaultCacheTtlMillis
        target.offlineReplayStrategy = offlineReplayStrategy
        target.maxOfflineRetries = maxOfflineRetries
        target.syncPolicy = syncPolicy
        target.enableHttpCache = enableHttpCache
        target.autoSyncOnReconnect = autoSyncOnReconnect
        target.autoRefreshOnObserve = autoRefreshOnObserve
        target.queueMutationsWhenOffline = queueMutationsWhenOffline
        target.enableRequestDeduplication = enableRequestDeduplication
        target.enableRateLimitBackoff = enableRateLimitBackoff
        target.maxRateLimitRetries = maxRateLimitRetries
        target.certificatePins = certificatePins
        target.backgroundSyncIntervalMillis = backgroundSyncIntervalMillis
        target.validateOnStartup = validateOnStartup
        authConfigBlock?.let { block -> target.auth(block) }
    }
}
