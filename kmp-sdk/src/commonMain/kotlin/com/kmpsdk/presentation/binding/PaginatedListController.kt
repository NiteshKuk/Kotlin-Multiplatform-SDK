package com.kmpsdk.presentation.binding

import com.kmpsdk.domain.error.KmpSdkError
import com.kmpsdk.domain.error.KmpSdkResult
import com.kmpsdk.domain.repository.PaginatedRepository
import com.kmpsdk.presentation.state.PaginatedDataState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Headless paginated list binder for host ViewModels.
 */
class PaginatedListController<T>(
    private val scope: CoroutineScope,
    private val repository: PaginatedRepository<T>,
    private val onStateChange: (PaginatedDataState<T>) -> Unit,
    private val onError: ((KmpSdkError) -> Unit)? = null,
    private val pageSize: Int = 20,
    private val autoLoadOnStart: Boolean = true,
) {
    private var started = false
    private var latestItems: List<T> = emptyList()

    fun start() {
        if (started) return
        started = true

        scope.launch {
            repository.observeAll().collectLatest { items ->
                latestItems = items
                onStateChange(
                    PaginatedDataState.Success(
                        items = items,
                        hasMore = repository.hasMore,
                    ),
                )
            }
        }

        if (autoLoadOnStart) {
            loadInitial()
        }
    }

    fun loadInitial() {
        scope.launch {
            onStateChange(PaginatedDataState.Loading)
            when (val result = repository.loadInitial(pageSize)) {
                is KmpSdkResult.Success -> {
                    if (repository.countLocal() == 0L) {
                        onStateChange(
                            PaginatedDataState.Success(
                                items = emptyList(),
                                hasMore = repository.hasMore,
                            ),
                        )
                    }
                }
                is KmpSdkResult.Failure -> handleError(result.error)
            }
        }
    }

    fun loadMore() {
        if (!repository.hasMore) {
            onStateChange(PaginatedDataState.EndReached)
            return
        }

        scope.launch {
            onStateChange(PaginatedDataState.LoadingMore(latestItems))

            when (val result = repository.loadNextPage()) {
                is KmpSdkResult.Success -> {
                    if (!repository.hasMore) {
                        onStateChange(PaginatedDataState.EndReached)
                    }
                }
                is KmpSdkResult.Failure -> handleError(result.error)
            }
        }
    }

    fun refresh() = loadInitial()

    private fun handleError(error: KmpSdkError) {
        val dataState = when (error) {
            is KmpSdkError.Network -> PaginatedDataState.NoNetwork
            else -> PaginatedDataState.Failure(error)
        }
        onStateChange(dataState)
        onError?.invoke(error)
    }
}
