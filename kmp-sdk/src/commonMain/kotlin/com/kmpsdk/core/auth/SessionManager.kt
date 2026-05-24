package com.kmpsdk.core.auth

import com.kmpsdk.core.logger.Logger
import com.kmpsdk.domain.error.KmpSdkResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(
    private val tokenStore: TokenStore,
    private val refreshHandler: TokenRefreshHandler? = null,
    private val logger: Logger = Logger.create("SessionManager"),
) {
    var cachedAccessToken: String? = null
        private set

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unknown)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    suspend fun initialize() {
        val token = tokenStore.getAccessToken()
        cachedAccessToken = token
        _sessionState.value = if (token.isNullOrBlank()) {
            SessionState.LoggedOut
        } else {
            SessionState.Authenticated(token.preview())
        }
    }

    suspend fun login(accessToken: String, refreshToken: String? = null) {
        tokenStore.saveTokens(accessToken, refreshToken)
        cachedAccessToken = accessToken
        _sessionState.value = SessionState.Authenticated(accessToken.preview())
        _events.emit(SessionEvent.LoggedIn(accessToken.preview()))
        logger.i("Session established")
    }

    suspend fun logout() {
        tokenStore.clear()
        cachedAccessToken = null
        _sessionState.value = SessionState.LoggedOut
        _events.emit(SessionEvent.LoggedOut)
        logger.i("Session cleared")
    }

    /**
     * Called on 401/403. Returns true when session was recovered via [TokenRefreshHandler].
     */
    suspend fun handleUnauthorized(httpCode: Int): Boolean {
        val refreshToken = tokenStore.getRefreshToken()
        val handler = refreshHandler

        if (refreshToken.isNullOrBlank() || handler == null) {
            awaitLogout(httpCode)
            return false
        }

        return when (val result = handler.refresh(refreshToken)) {
            is KmpSdkResult.Success -> {
                tokenStore.saveTokens(result.data.accessToken, result.data.refreshToken ?: refreshToken)
                cachedAccessToken = result.data.accessToken
                _sessionState.value = SessionState.Authenticated(result.data.accessToken.preview())
                _events.emit(SessionEvent.TokenRefreshed)
                logger.i("Access token refreshed")
                true
            }
            is KmpSdkResult.Failure -> {
                awaitLogout(httpCode)
                false
            }
        }
    }

    private suspend fun awaitLogout(httpCode: Int) {
        tokenStore.clear()
        cachedAccessToken = null
        _sessionState.value = SessionState.LoggedOut
        _events.emit(SessionEvent.SessionExpired(httpCode))
        logger.w("Session expired (HTTP $httpCode)")
    }

    private fun String.preview(): String = take(6) + "…"
}
