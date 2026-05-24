package com.kmpsdk.domain.pagination

data class PageRequest(
    val page: Int,
    val pageSize: Int,
) {
    init {
        require(page >= 0) { "page must be >= 0" }
        require(pageSize > 0) { "pageSize must be > 0" }
    }

    val offset: Int get() = page * pageSize
}

data class PaginatedResult<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean,
) {
    val isEmpty: Boolean get() = items.isEmpty()
}

fun <T> List<T>.toPaginatedResult(page: PageRequest): PaginatedResult<T> =
    PaginatedResult(
        items = this,
        page = page.page,
        pageSize = page.pageSize,
        hasMore = size >= page.pageSize,
    )
