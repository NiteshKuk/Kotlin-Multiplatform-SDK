package com.kmpsdk.presentation.state

import com.kmpsdk.domain.error.KmpSdkError

/** Headless paginated list state for host apps. */
sealed class PaginatedDataState<out T> {
    data object Idle : PaginatedDataState<Nothing>()
    data object Loading : PaginatedDataState<Nothing>()
    data class Success<T>(
        val items: List<T>,
        val hasMore: Boolean,
    ) : PaginatedDataState<T>()
    data class LoadingMore<T>(
        val items: List<T>,
    ) : PaginatedDataState<T>()
    data object EndReached : PaginatedDataState<Nothing>()
    data class Failure(val error: KmpSdkError) : PaginatedDataState<Nothing>()
    data object NoNetwork : PaginatedDataState<Nothing>()

    val isLoading: Boolean get() = this is Loading || this is LoadingMore<*>
}

@Suppress("UNCHECKED_CAST")
fun <T> PaginatedDataState<T>.itemsOrEmpty(): List<T> = when (this) {
    is PaginatedDataState.Success -> items
    is PaginatedDataState.LoadingMore -> items
    else -> emptyList()
}
