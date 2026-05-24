package com.kmpsdk.data.source

import kotlinx.coroutines.flow.Flow

/**
 * Generic local store contract. Implement with SQLDelight in the host app module.
 */
interface LocalListDataSource<T> {
    fun observeAll(): Flow<List<T>>

    suspend fun count(): Long
}

/**
 * Generic remote fetch contract. Implement with [com.kmpsdk.data.network.KmpNetworkClient] in the host app.
 */
interface RemoteListDataSource<TDto> {
    suspend fun fetchAll(): com.kmpsdk.domain.error.KmpSdkResult<List<TDto>>
}
