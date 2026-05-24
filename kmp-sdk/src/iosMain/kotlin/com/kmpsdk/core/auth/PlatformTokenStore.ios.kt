package com.kmpsdk.core.auth

import platform.Foundation.NSUserDefaults

actual class PlatformTokenStore actual constructor() : TokenStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual override suspend fun getAccessToken(): String? =
        defaults.stringForKey(KEY_ACCESS)

    actual override suspend fun getRefreshToken(): String? =
        defaults.stringForKey(KEY_REFRESH)

    actual override suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        defaults.setObject(accessToken, KEY_ACCESS)
        if (refreshToken != null) {
            defaults.setObject(refreshToken, KEY_REFRESH)
        } else {
            defaults.removeObjectForKey(KEY_REFRESH)
        }
        defaults.synchronize()
    }

    actual override suspend fun clear() {
        defaults.removeObjectForKey(KEY_ACCESS)
        defaults.removeObjectForKey(KEY_REFRESH)
        defaults.synchronize()
    }

    private companion object {
        const val KEY_ACCESS = "kmpsdk_access_token"
        const val KEY_REFRESH = "kmpsdk_refresh_token"
    }
}
