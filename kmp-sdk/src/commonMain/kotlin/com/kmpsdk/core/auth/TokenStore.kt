package com.kmpsdk.core.auth

/**
 * Persists auth tokens. Replace [InMemoryTokenStore] with [PlatformTokenStore] in production apps.
 */
interface TokenStore {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun saveTokens(accessToken: String, refreshToken: String? = null)
    suspend fun clear()
}

class InMemoryTokenStore : TokenStore {
    private var accessToken: String? = null
    private var refreshToken: String? = null

    override suspend fun getAccessToken(): String? = accessToken
    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
    }

    override suspend fun clear() {
        accessToken = null
        refreshToken = null
    }
}

expect class PlatformTokenStore() : TokenStore
