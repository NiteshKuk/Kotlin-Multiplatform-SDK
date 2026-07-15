package com.kmpsdk.core.config

import com.kmpsdk.core.logger.Logger
import com.kmpsdk.core.tenant.TenantManager
import com.kmpsdk.data.network.KmpNetworkClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Named environment packs (dev/staging/prod) with optional runtime switch.
 */
class EnvironmentVault(
    private val environmentBlocks: Map<String, KmpSdkConfigBuilder.() -> Unit>,
    private val fallback: KmpSdkConfigBuilder.() -> Unit,
    initialName: String,
    private val onConfigRebuilt: (KmpSdkConfig) -> Unit,
    private val tenantManager: TenantManager,
    private val networkClient: KmpNetworkClient,
    private val logger: Logger = Logger.create("EnvironmentVault"),
) {
    private val _active = MutableStateFlow(initialName)
    val active: StateFlow<String> = _active.asStateFlow()

    fun available(): Set<String> = environmentBlocks.keys

    fun requirePinsFor(name: String): Boolean {
        val cfg = build(name)
        return cfg.certificatePins.isNotEmpty()
    }

    fun build(name: String): KmpSdkConfig =
        buildConfigForEnvironment(name, environmentBlocks, fallback)

    /**
     * Switch active environment (typically debug/staging tools).
     * Rebuilds config and applies base URL via [TenantManager].
     */
    fun switchTo(name: String) {
        require(environmentBlocks.containsKey(name)) { "Unknown environment: $name" }
        val cfg = build(name)
        if (name == "prod" || name.contains("prod", ignoreCase = true)) {
            require(cfg.certificatePins.isNotEmpty() || !cfg.validateOnStartup) {
                "Environment '$name' should define certificatePins for production"
            }
        }
        onConfigRebuilt(cfg)
        tenantManager.switchTenant(
            tenantId = name,
            baseUrl = cfg.baseUrl,
            headers = emptyMap(),
        )
        networkClient.applyTenant(tenantManager.current.value)
        _active.value = name
        logger.i("Switched environment to '$name' (${cfg.baseUrl})")
    }
}
