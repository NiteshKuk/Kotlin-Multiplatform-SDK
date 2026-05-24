package com.kmpsdk.data.cache

import com.kmpsdk.core.config.KmpSdkConfig
import com.kmpsdk.data.db.KmpSdkDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

interface CacheStore {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String, ttlMillis: Long? = null)
    suspend fun remove(key: String)
    suspend fun clear()
}

/**
 * Two-tier cache: in-memory LRU + SQLDelight disk persistence with TTL.
 */
class TieredCacheStore(
    private val database: KmpSdkDatabase,
    private val config: KmpSdkConfig,
    private val memoryCapacity: Int = 128,
) : CacheStore {
    private val mutex = Mutex()
    private val memory = LinkedHashMap<String, CacheEntry>(memoryCapacity, 0.75f, true)

    override suspend fun get(key: String): String? = mutex.withLock {
        val now = Clock.System.now().toEpochMilliseconds()
        memory[key]?.takeIf { !it.isExpired(now) }?.value
            ?: loadFromDisk(key, now)?.also { memory[key] = it }
            ?.value
    }

    override suspend fun put(key: String, value: String, ttlMillis: Long?) {
        val ttl = ttlMillis ?: config.defaultCacheTtlMillis
        val now = Clock.System.now().toEpochMilliseconds()
        val entry = CacheEntry(value, now, ttl)
        mutex.withLock {
            memory[key] = entry
            database.kmpSdkDatabaseQueries.insertCacheEntry(
                cache_key = key,
                response_body = value,
                created_at = now,
                ttl_millis = ttl,
            )
        }
    }

    override suspend fun remove(key: String) {
        mutex.withLock {
            memory.remove(key)
            database.kmpSdkDatabaseQueries.deleteCacheEntry(key)
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            memory.clear()
        }
    }

    suspend fun purgeExpired() {
        val now = Clock.System.now().toEpochMilliseconds()
        mutex.withLock {
            memory.entries.removeIf { (_, entry) -> entry.isExpired(now) }
            database.kmpSdkDatabaseQueries.deleteExpiredCache(now)
        }
    }

    private fun loadFromDisk(key: String, now: Long): CacheEntry? {
        val row = database.kmpSdkDatabaseQueries.selectCacheEntry(key).executeAsOneOrNull()
            ?: return null
        val entry = CacheEntry(row.response_body, row.created_at, row.ttl_millis)
        return entry.takeIf { !it.isExpired(now) }
    }

    private data class CacheEntry(
        val value: String,
        val createdAt: Long,
        val ttlMillis: Long,
    ) {
        fun isExpired(now: Long): Boolean = (createdAt + ttlMillis) < now
    }
}
