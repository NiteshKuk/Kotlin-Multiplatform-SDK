package com.kmpsdk.data.draft

import com.kmpsdk.data.db.KmpSdkDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Persist in-progress form payloads for resume-after-kill.
 */
class DraftStore(
    private val database: KmpSdkDatabase,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) {
    suspend fun <T : Any> save(key: String, data: T, serializer: kotlinx.serialization.KSerializer<T>, ttlMillis: Long? = null) {
        val now = Clock.System.now().toEpochMilliseconds()
        database.kmpSdkDatabaseQueries.insertDraft(
            draft_key = key,
            payload_json = json.encodeToString(serializer, data),
            updated_at = now,
            ttl_millis = ttlMillis,
        )
    }

    suspend inline fun <reified T : Any> save(key: String, data: T, ttlMillis: Long? = null) {
        save(key, data, serializer(), ttlMillis)
    }

    suspend fun <T : Any> load(key: String, serializer: kotlinx.serialization.KSerializer<T>): T? {
        purgeExpired()
        val row = database.kmpSdkDatabaseQueries.selectDraft(key).executeAsOneOrNull() ?: return null
        return json.decodeFromString(serializer, row.payload_json)
    }

    suspend inline fun <reified T : Any> load(key: String): T? = load(key, serializer())

    suspend fun clear(key: String) {
        database.kmpSdkDatabaseQueries.deleteDraft(key)
    }

    suspend fun purgeExpired() {
        val now = Clock.System.now().toEpochMilliseconds()
        database.kmpSdkDatabaseQueries.deleteExpiredDrafts(now)
    }

    /**
     * Periodically persist the latest value from [provider].
     * Cancel by cancelling [scope] or the returned job.
     */
    fun <T : Any> bindAutosave(
        scope: CoroutineScope,
        key: String,
        intervalMs: Long = 1_000,
        ttlMillis: Long? = null,
        serializer: kotlinx.serialization.KSerializer<T>,
        provider: () -> T?,
    ) = scope.launch {
        while (isActive) {
            delay(intervalMs)
            val value = provider() ?: continue
            save(key, value, serializer, ttlMillis)
        }
    }

    inline fun <reified T : Any> bindAutosave(
        scope: CoroutineScope,
        key: String,
        intervalMs: Long = 1_000,
        ttlMillis: Long? = null,
        noinline provider: () -> T?,
    ) = bindAutosave(scope, key, intervalMs, ttlMillis, serializer(), provider)
}
