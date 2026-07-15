package com.kmpsdk.data.repository

import com.kmpsdk.core.connectivity.ConnectivityMonitor
import com.kmpsdk.core.logger.Logger
import com.kmpsdk.data.sync.SyncStatusStore
import com.kmpsdk.domain.error.KmpSdkError
import com.kmpsdk.domain.error.KmpSdkResult
import com.kmpsdk.domain.repository.SyncableRepository
import com.kmpsdk.domain.sync.SyncPolicy
import kotlinx.coroutines.flow.Flow

/**
 * Generic offline-first repository base.
 *
 * Host apps subclass this (or wrap it) and only provide:
 * - how to observe local SQL data
 * - how to count local rows
 * - how to fetch from API and persist to SQL
 *
 * SyncPolicy handling (offline, stale cache, network-first) is built in.
 * When [syncStatusStore] + [syncTargetName] are set, UI can observe [SyncStatusStore].
 */
open class BaseSyncRepository<T>(
    private val tag: String,
    private val observeLocal: () -> Flow<List<T>>,
    countLocal: suspend () -> Long,
    private val syncRemote: suspend () -> KmpSdkResult<Unit>,
    private val connectivityMonitor: ConnectivityMonitor,
    private val syncPolicy: SyncPolicy,
    private val logger: Logger,
    private val syncStatusStore: SyncStatusStore? = null,
    private val syncTargetName: String? = null,
) : SyncableRepository<T> {

    /** Avoid name clash with [countLocal] — override would recurse into itself. */
    private val countLocalBlock: suspend () -> Long = countLocal

    override fun observeAll(): Flow<List<T>> = observeLocal()

    override suspend fun countLocal(): Long = countLocalBlock()

    override suspend fun refresh(): KmpSdkResult<Unit> {
        val cachedCount = countLocalBlock()
        reportSyncing()

        if (!connectivityMonitor.isOnline()) {
            return reportOffline(handleOffline(cachedCount), cachedCount)
        }

        return when (val result = syncRemote()) {
            is KmpSdkResult.Success -> {
                logger.i("$tag sync completed")
                reportSuccess(countLocalBlock())
                KmpSdkResult.Success(Unit)
            }
            is KmpSdkResult.Failure -> {
                val handled = handleRemoteFailure(result, cachedCount)
                reportAfterRemoteFailure(handled, result, cachedCount)
                handled
            }
        }
    }

    private fun handleOffline(localCount: Long): KmpSdkResult<Unit> = when (syncPolicy) {
        SyncPolicy.NETWORK_FIRST -> KmpSdkResult.Failure(
            KmpSdkError.Network("$tag requires network connection"),
        )
        SyncPolicy.CACHE_FIRST,
        SyncPolicy.STALE_WHILE_REVALIDATE,
        -> if (localCount > 0) {
            logger.i("$tag offline — serving $localCount cached item(s)")
            KmpSdkResult.Success(Unit)
        } else {
            KmpSdkResult.Failure(KmpSdkError.Network("$tag has no cached data offline"))
        }
    }

    private fun handleRemoteFailure(
        result: KmpSdkResult.Failure,
        localCount: Long,
    ): KmpSdkResult<Unit> = when (syncPolicy) {
        SyncPolicy.NETWORK_FIRST -> result
        SyncPolicy.CACHE_FIRST,
        SyncPolicy.STALE_WHILE_REVALIDATE,
        -> if (localCount > 0) {
            logger.w("$tag remote sync failed — serving $localCount cached item(s)")
            KmpSdkResult.Success(Unit)
        } else {
            result
        }
    }

    private fun reportSyncing() {
        val name = syncTargetName ?: return
        syncStatusStore?.markSyncing(name)
    }

    private fun reportSuccess(localCount: Long) {
        val name = syncTargetName ?: return
        // After online sync, recount may differ; caller often refresh-replaces rows first.
        syncStatusStore?.markSuccess(name, localCount)
    }

    private fun reportOffline(result: KmpSdkResult<Unit>, localCount: Long): KmpSdkResult<Unit> {
        val name = syncTargetName ?: return result
        val store = syncStatusStore ?: return result
        when (result) {
            is KmpSdkResult.Success -> store.markOfflineCached(name, localCount)
            is KmpSdkResult.Failure -> store.markFailed(name, result.error.message, localCount)
        }
        return result
    }

    private fun reportAfterRemoteFailure(
        handled: KmpSdkResult<Unit>,
        original: KmpSdkResult.Failure,
        localCount: Long,
    ) {
        val name = syncTargetName ?: return
        val store = syncStatusStore ?: return
        when (handled) {
            is KmpSdkResult.Success -> store.markOfflineCached(name, localCount)
            is KmpSdkResult.Failure -> store.markFailed(name, original.error.message, localCount)
        }
    }
}
