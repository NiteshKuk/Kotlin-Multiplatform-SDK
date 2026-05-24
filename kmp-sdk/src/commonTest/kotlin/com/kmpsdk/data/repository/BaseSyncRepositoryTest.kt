package com.kmpsdk.data.repository

import com.kmpsdk.core.connectivity.ConnectivityMonitor
import com.kmpsdk.core.connectivity.ConnectivityStatus
import com.kmpsdk.core.logger.Logger
import com.kmpsdk.domain.error.KmpSdkError
import com.kmpsdk.domain.error.KmpSdkResult
import com.kmpsdk.domain.sync.SyncPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BaseSyncRepositoryTest {

    private class FakeConnectivity(initial: ConnectivityStatus) : ConnectivityMonitor {
        private val state = MutableStateFlow(initial)
        override val status: Flow<ConnectivityStatus> = state
    }

    @Test
    fun offlineWithCacheReturnsSuccessForStaleWhileRevalidate() = runTest {
        val connectivity = FakeConnectivity(ConnectivityStatus.Offline)
        var remoteCalled = false
        val repo = BaseSyncRepository(
            tag = "Test",
            observeLocal = { flowOf(listOf("cached")) },
            countLocal = { 1 },
            syncRemote = {
                remoteCalled = true
                KmpSdkResult.Success(Unit)
            },
            connectivityMonitor = connectivity,
            syncPolicy = SyncPolicy.STALE_WHILE_REVALIDATE,
            logger = Logger.create("Test"),
        )

        val result = repo.refresh()

        assertTrue(result is KmpSdkResult.Success)
        assertEquals(false, remoteCalled)
    }

    @Test
    fun offlineWithoutCacheReturnsNetworkFailure() = runTest {
        val connectivity = FakeConnectivity(ConnectivityStatus.Offline)
        val repo = BaseSyncRepository(
            tag = "Test",
            observeLocal = { flowOf(emptyList()) },
            countLocal = { 0 },
            syncRemote = { KmpSdkResult.Success(Unit) },
            connectivityMonitor = connectivity,
            syncPolicy = SyncPolicy.STALE_WHILE_REVALIDATE,
            logger = Logger.create("Test"),
        )

        val result = repo.refresh()

        assertTrue(result is KmpSdkResult.Failure)
        assertTrue((result as KmpSdkResult.Failure).error is KmpSdkError.Network)
    }

    @Test
    fun networkFirstFailsWhenOffline() = runTest {
        val connectivity = FakeConnectivity(ConnectivityStatus.Offline)
        val repo = BaseSyncRepository(
            tag = "Test",
            observeLocal = { flowOf(listOf("cached")) },
            countLocal = { 1 },
            syncRemote = { KmpSdkResult.Success(Unit) },
            connectivityMonitor = connectivity,
            syncPolicy = SyncPolicy.NETWORK_FIRST,
            logger = Logger.create("Test"),
        )

        val result = repo.refresh()

        assertTrue(result is KmpSdkResult.Failure)
    }
}
