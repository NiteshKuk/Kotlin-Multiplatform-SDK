package com.kmpsdk

import com.kmpsdk.core.auth.TokenRefreshHandler
import com.kmpsdk.core.config.EnvironmentDsl
import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.core.config.SdkProfile
import com.kmpsdk.core.config.applyProfile
import com.kmpsdk.core.config.buildConfigForEnvironment
import com.kmpsdk.core.config.kmpSdkConfig
import com.kmpsdk.core.di.KmpSdkContext
import com.kmpsdk.core.di.KmpSdkModule
import com.kmpsdk.core.di.KmpSdkRegistry
import com.kmpsdk.core.logger.LogLevel
import com.kmpsdk.core.resilience.ResilienceDsl
import com.kmpsdk.core.routing.DeepLinkRouter
import com.kmpsdk.core.routing.PushPayloadRouter
import com.kmpsdk.data.offline.OfflineReplayStrategy
import com.kmpsdk.data.sync.BackgroundWorkDsl
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
    private val hostRegistrations = mutableListOf<KmpSdkRegistry.() -> Unit>()
    private var authConfigBlock: (com.kmpsdk.core.config.AuthConfigBuilder.() -> Unit)? = null
    private val environmentBlocks = linkedMapOf<String, com.kmpsdk.core.config.KmpSdkConfigBuilder.() -> Unit>()
    private var resilienceBlock: (ResilienceDsl.() -> Unit)? = null
    private var deepLinkBlock: (DeepLinkRouter.() -> Unit)? = null
    private var pushBlock: (PushPayloadRouter.() -> Unit)? = null
    private var backgroundWorkBlock: (BackgroundWorkDsl.() -> Unit)? = null

    fun auth(block: com.kmpsdk.core.config.AuthConfigBuilder.() -> Unit) {
        authConfigBlock = block
    }

    /**
     * Register a host-owned type into the SDK registry (analytics, flags, etc.).
     */
    fun <T : Any> register(type: kotlin.reflect.KClass<T>, factory: (KmpSdkContext) -> T) {
        hostRegistrations += { register(type, factory) }
    }

    inline fun <reified T : Any> register(noinline factory: (KmpSdkContext) -> T) {
        register(T::class, factory)
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

    fun resilience(block: ResilienceDsl.() -> Unit) {
        resilienceBlock = block
    }

    fun deepLinks(block: DeepLinkRouter.() -> Unit) {
        deepLinkBlock = block
    }

    fun push(block: PushPayloadRouter.() -> Unit) {
        pushBlock = block
    }

    fun backgroundWork(block: BackgroundWorkDsl.() -> Unit) {
        backgroundWorkBlock = block
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
        return config
    }

    internal fun modules(): List<KmpSdkModule> = modules.toList()

    internal fun applyRegistry(registry: KmpSdkRegistry) {
        hostRegistrations.forEach { it(registry) }
        modules.forEach { registry.install(it) }
    }

    internal fun remoteConfigFetcherOrNull(): (suspend () -> Map<String, String>)? = remoteConfigFetcher

    internal fun environmentBlocksOrEmpty(): Map<String, com.kmpsdk.core.config.KmpSdkConfigBuilder.() -> Unit> =
        environmentBlocks.toMap()

    internal fun deepLinkBlockOrNull(): (DeepLinkRouter.() -> Unit)? = deepLinkBlock

    internal fun pushBlockOrNull(): (PushPayloadRouter.() -> Unit)? = pushBlock

    internal fun backgroundWorkBlockOrNull(): (BackgroundWorkDsl.() -> Unit)? = backgroundWorkBlock

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
        resilienceBlock?.let { block -> target.resilience(block) }
    }
}
