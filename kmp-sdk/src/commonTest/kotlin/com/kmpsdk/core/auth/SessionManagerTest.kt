package com.kmpsdk.core.auth

import com.kmpsdk.domain.error.KmpSdkResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SessionManagerTest {

    @Test
    fun loginStoresTokensAndUpdatesState() = runTest {
        val store = InMemoryTokenStore()
        val manager = SessionManager(store)

        manager.login("access-123", "refresh-456")

        assertEquals("access-123", store.getAccessToken())
        assertEquals("refresh-456", store.getRefreshToken())
        assertTrue(manager.sessionState.value is SessionState.Authenticated)
    }

    @Test
    fun logoutClearsSession() = runTest {
        val store = InMemoryTokenStore()
        val manager = SessionManager(store)
        manager.login("access-123", "refresh-456")

        manager.logout()

        assertEquals(null, store.getAccessToken())
        assertEquals(SessionState.LoggedOut, manager.sessionState.value)
    }

    @Test
    fun refreshHandlerRecoversUnauthorized() = runTest {
        val store = InMemoryTokenStore()
        store.saveTokens("old-access", "refresh-token")
        val manager = SessionManager(
            tokenStore = store,
            refreshHandler = TokenRefreshHandler {
                KmpSdkResult.Success(TokenPair("new-access", "refresh-token"))
            },
        )

        val recovered = manager.handleUnauthorized(401)

        assertTrue(recovered)
        assertEquals("new-access", store.getAccessToken())
    }
}
