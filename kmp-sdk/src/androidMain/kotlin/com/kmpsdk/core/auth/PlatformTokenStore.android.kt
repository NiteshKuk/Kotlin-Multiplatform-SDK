package com.kmpsdk.core.auth

import android.content.Context
import com.kmpsdk.KmpSdkAndroid

actual class PlatformTokenStore actual constructor() : TokenStore {
    private val prefs by lazy {
        KmpSdkAndroid.requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)

    override suspend fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    override suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .putString(KEY_REFRESH, refreshToken)
            .apply()
    }

    override suspend fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "kmpsdk_auth"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
    }
}
