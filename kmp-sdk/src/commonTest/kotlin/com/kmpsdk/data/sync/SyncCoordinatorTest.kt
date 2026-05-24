package com.kmpsdk.data.sync

import com.kmpsdk.core.logger.Logger
import com.kmpsdk.domain.error.KmpSdkResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncCoordinatorTest {

    @Test
    fun syncAllReplaysQueueThenRefreshesTargets() = runTest {
        var replayCalls = 0
        val coordinator = SyncCoordinator(
            replayOfflineQueue = {
                replayCalls++
                KmpSdkResult.Success(2)
            },
            logger = Logger.create("Test"),
        )

        var refreshedA = false
        var refreshedB = false
        coordinator.register("users") {
            refreshedA = true
            KmpSdkResult.Success(Unit)
        }
        coordinator.register("products") {
            refreshedB = true
            KmpSdkResult.Success(Unit)
        }

        val result = coordinator.syncAll()

        assertEquals(1, replayCalls)
        assertTrue(refreshedA)
        assertTrue(refreshedB)
        assertEquals(2, result.replayedOfflineRequests)
        assertEquals(2, result.refreshedRepositories)
        assertEquals(SyncPhase.COMPLETED, coordinator.state.value.phase)
    }

    @Test
    fun syncAllRecordsFailedTargets() = runTest {
        val coordinator = SyncCoordinator(
            replayOfflineQueue = { KmpSdkResult.Success(0) },
            logger = Logger.create("Test"),
        )
        coordinator.register("broken") { KmpSdkResult.Failure(com.kmpsdk.domain.error.KmpSdkError.Network()) }

        val result = coordinator.syncAll()

        assertEquals(listOf("broken"), result.failures)
        assertEquals(SyncPhase.FAILED, coordinator.state.value.phase)
    }
}
