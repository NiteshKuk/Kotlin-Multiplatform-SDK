package com.kmpsdk.data.repository

import com.kmpsdk.core.connectivity.ConnectivityMonitor
import com.kmpsdk.core.logger.Logger
import com.kmpsdk.domain.error.KmpSdkError
import com.kmpsdk.domain.error.KmpSdkResult
import com.kmpsdk.domain.pagination.PageRequest
import com.kmpsdk.domain.pagination.PaginatedResult
import com.kmpsdk.domain.repository.PaginatedRemoteDataSource
import com.kmpsdk.domain.repository.PaginatedRepository
import com.kmpsdk.domain.sync.SyncPolicy
import kotlinx.coroutines.flow.Flow

/**
 * Generic paginated repository base for host-app copy/paste.
 */
open class BasePaginatedRepository<TDomain, TDto>(
    private val tag: String,
    private val pageSize: Int,
    private val observeLocal: () -> Flow<List<TDomain>>,
    countLocal: suspend () -> Long,
    private val replaceAll: suspend (List<TDomain>) -> Unit,
    private val appendPage: suspend (List<TDomain>) -> Unit,
    private val remote: PaginatedRemoteDataSource<TDto>,
    private val mapDto: (TDto, page: Int) -> TDomain,
    private val connectivityMonitor: ConnectivityMonitor,
    private val syncPolicy: SyncPolicy,
    private val logger: Logger,
) : PaginatedRepository<TDomain> {

    private val countLocalBlock: suspend () -> Long = countLocal

    private var currentPage = 0
    override var hasMore: Boolean = true
        protected set

    override fun observeAll(): Flow<List<TDomain>> = observeLocal()

    override suspend fun countLocal(): Long = countLocalBlock()

    override suspend fun loadInitial(pageSize: Int): KmpSdkResult<Unit> {
        currentPage = 0
        hasMore = true
        return loadPage(page = 0, pageSize = pageSize, replace = true)
    }

    override suspend fun loadNextPage(): KmpSdkResult<Unit> {
        if (!hasMore) return KmpSdkResult.Success(Unit)
        return loadPage(page = currentPage + 1, pageSize = pageSize, replace = false)
    }

    override suspend fun refresh(pageSize: Int): KmpSdkResult<Unit> = loadInitial(pageSize)

    private suspend fun loadPage(
        page: Int,
        pageSize: Int,
        replace: Boolean,
    ): KmpSdkResult<Unit> {
        val cachedCount = countLocalBlock()

        if (!connectivityMonitor.isOnline()) {
            return handleOffline(cachedCount)
        }

        val request = PageRequest(page = page, pageSize = pageSize)
        return when (val result = remote.fetchPage(request)) {
            is KmpSdkResult.Success -> {
                val mapped = result.data.items.map { dto -> mapDto(dto, page) }
                if (replace) {
                    replaceAll(mapped)
                } else {
                    appendPage(mapped)
                }
                currentPage = page
                hasMore = result.data.hasMore
                logger.i("$tag loaded page $page (${mapped.size} items, hasMore=$hasMore)")
                KmpSdkResult.Success(Unit)
            }
            is KmpSdkResult.Failure -> handleRemoteFailure(result, cachedCount)
        }
    }

    private fun handleOffline(localCount: Long): KmpSdkResult<Unit> = when (syncPolicy) {
        SyncPolicy.NETWORK_FIRST -> KmpSdkResult.Failure(
            KmpSdkError.Network("$tag requires network connection"),
        )
        SyncPolicy.CACHE_FIRST,
        SyncPolicy.STALE_WHILE_REVALIDATE,
        -> if (localCount > 0) {
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
        -> if (localCount > 0) KmpSdkResult.Success(Unit) else result
    }
}
