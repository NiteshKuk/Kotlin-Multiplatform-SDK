package com.kmpsdk.core.config

class AuthConfigBuilder {
    var enabled: Boolean = true
    var headerName: String = "Authorization"
    var tokenPrefix: String = "Bearer"
    var useSecureTokenStore: Boolean = true

    fun build() = AuthConfig(
        enabled = enabled,
        headerName = headerName,
        tokenPrefix = tokenPrefix,
        useSecureTokenStore = useSecureTokenStore,
    )
}
