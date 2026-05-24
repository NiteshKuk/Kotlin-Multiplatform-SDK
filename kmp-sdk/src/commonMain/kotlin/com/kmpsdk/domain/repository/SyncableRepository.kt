package com.kmpsdk.domain.repository

import com.kmpsdk.domain.error.KmpSdkResult
import kotlinx.coroutines.flow.Flow

/**
 * Base contract for entities that sync between SQLDelight and a remote API.
 * Copy this pattern for Products, Orders, etc.
 */
interface SyncableRepository<T> {
    fun observeAll(): Flow<List<T>>

    suspend fun refresh(): KmpSdkResult<Unit>

    suspend fun countLocal(): Long
}
