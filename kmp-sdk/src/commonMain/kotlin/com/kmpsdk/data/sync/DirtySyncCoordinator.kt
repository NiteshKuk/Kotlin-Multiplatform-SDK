package com.kmpsdk.data.sync

import com.kmpsdk.domain.error.KmpSdkError
import com.kmpsdk.domain.error.KmpSdkResult

enum class DirtyConflictPolicy {
    SERVER_WINS,
    CLIENT_WINS,
}

interface DirtyRecord {
    val id: String
    val isDirty: Boolean
}

interface DirtySyncTarget<T : DirtyRecord> {
    suspend fun loadDirty(): List<T>
    suspend fun push(record: T): KmpSdkResult<Unit>
    suspend fun markClean(id: String)
}

class DirtySyncCoordinator(
    private val conflictPolicy: DirtyConflictPolicy = DirtyConflictPolicy.SERVER_WINS,
) {
    suspend fun <T : DirtyRecord> syncDirty(target: DirtySyncTarget<T>): KmpSdkResult<Int> {
        val dirty = target.loadDirty()
        var synced = 0

        for (record in dirty) {
            when (val result = target.push(record)) {
                is KmpSdkResult.Success -> {
                    target.markClean(record.id)
                    synced++
                }
                is KmpSdkResult.Failure -> {
                    when (conflictPolicy) {
                        DirtyConflictPolicy.SERVER_WINS -> return KmpSdkResult.Failure(result.error)
                        DirtyConflictPolicy.CLIENT_WINS -> {
                            target.markClean(record.id)
                            synced++
                        }
                    }
                }
            }
        }
        return KmpSdkResult.Success(synced)
    }
}
