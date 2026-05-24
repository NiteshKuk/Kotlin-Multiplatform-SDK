package com.kmpsdk.domain.repository

import com.kmpsdk.domain.error.KmpSdkResult
import com.kmpsdk.domain.pagination.PaginatedResult
import com.kmpsdk.domain.pagination.PageRequest
import kotlinx.coroutines.flow.Flow

interface PaginatedRepository<T> {
    fun observeAll(): Flow<List<T>>

    suspend fun loadInitial(pageSize: Int = 20): KmpSdkResult<Unit>

    suspend fun loadNextPage(): KmpSdkResult<Unit>

    suspend fun refresh(pageSize: Int = 20): KmpSdkResult<Unit>

    suspend fun countLocal(): Long

    val hasMore: Boolean
}

interface PaginatedRemoteDataSource<TDto> {
    suspend fun fetchPage(request: PageRequest): KmpSdkResult<PaginatedResult<TDto>>
}
