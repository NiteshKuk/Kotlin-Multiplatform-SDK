package com.kmpsdk.core.config

data class AuthConfig(
    val enabled: Boolean = true,
    val headerName: String = "Authorization",
    val tokenPrefix: String = "Bearer",
    val useSecureTokenStore: Boolean = true,
)
