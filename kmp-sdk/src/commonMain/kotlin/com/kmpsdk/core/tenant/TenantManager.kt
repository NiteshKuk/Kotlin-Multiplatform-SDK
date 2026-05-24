package com.kmpsdk.core.tenant

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TenantContext(
    val tenantId: String,
    val baseUrl: String,
    val headers: Map<String, String> = emptyMap(),
)

/**
 * Runtime tenant / environment switching for B2B apps.
 */
class TenantManager(
    initialBaseUrl: String,
) {
    private var onSwitch: ((TenantContext) -> Unit)? = null
    private val _current = MutableStateFlow(
        TenantContext(tenantId = "default", baseUrl = initialBaseUrl),
    )
    val current: StateFlow<TenantContext> = _current.asStateFlow()

    fun onTenantSwitch(handler: (TenantContext) -> Unit) {
        onSwitch = handler
    }

    fun switchTenant(tenantId: String, baseUrl: String, headers: Map<String, String> = emptyMap()) {
        val context = TenantContext(tenantId, baseUrl, headers)
        _current.value = context
        onSwitch?.invoke(context)
    }

    val activeBaseUrl: String get() = _current.value.baseUrl
    val activeHeaders: Map<String, String> get() = _current.value.headers
}
