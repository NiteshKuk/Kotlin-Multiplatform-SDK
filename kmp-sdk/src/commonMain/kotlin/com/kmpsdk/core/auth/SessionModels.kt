package com.kmpsdk.core.auth

import com.kmpsdk.domain.error.KmpSdkResult

/**
 * Host apps implement this to refresh tokens on 401 responses.
 */
fun interface TokenRefreshHandler {
    suspend fun refresh(refreshToken: String): KmpSdkResult<TokenPair>
}

data class TokenPair(
    val accessToken: String,
    val refreshToken: String? = null,
)

sealed class SessionState {
    data object Unknown : SessionState()
    data object LoggedOut : SessionState()
    data class Authenticated(val accessTokenPreview: String) : SessionState()
}

sealed class SessionEvent {
    data class LoggedIn(val accessTokenPreview: String) : SessionEvent()
    data object LoggedOut : SessionEvent()
    data class SessionExpired(val httpCode: Int) : SessionEvent()
    data object TokenRefreshed : SessionEvent()
}
