package com.kmpsdk.core.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Headless remote-config holder. Host app supplies fetch logic from Firebase/your API.
 */
class RemoteConfigStore {
    private val _values = MutableStateFlow<Map<String, String>>(emptyMap())
    val values: StateFlow<Map<String, String>> = _values.asStateFlow()

    fun getString(key: String, default: String? = null): String? =
        _values.value[key] ?: default

    fun getLong(key: String, default: Long? = null): Long? =
        _values.value[key]?.toLongOrNull() ?: default

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        _values.value[key]?.toBooleanStrictOrNull() ?: default

    suspend fun refresh(fetcher: suspend () -> Map<String, String>) {
        _values.value = fetcher()
    }

    fun apply(config: KmpSdkConfig): KmpSdkConfig {
        val ttl = getLong("default_cache_ttl_millis") ?: return config
        return config.copy(defaultCacheTtlMillis = ttl)
    }
}
