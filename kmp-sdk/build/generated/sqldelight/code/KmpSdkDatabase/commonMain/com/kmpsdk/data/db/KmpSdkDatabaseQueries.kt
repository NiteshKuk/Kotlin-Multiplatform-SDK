package com.kmpsdk.`data`.db

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class KmpSdkDatabaseQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllPendingQueueItems(mapper: (
    id: Long,
    method: String,
    url: String,
    headers_json: String,
    body: String?,
    priority: Long,
    created_at: Long,
    retry_count: Long,
    max_retries: Long,
    status: String,
  ) -> T): Query<T> = Query(1_208_184_611, arrayOf("offline_queue"), driver, "KmpSdkDatabase.sq",
      "selectAllPendingQueueItems", """
  |SELECT offline_queue.id, offline_queue.method, offline_queue.url, offline_queue.headers_json, offline_queue.body, offline_queue.priority, offline_queue.created_at, offline_queue.retry_count, offline_queue.max_retries, offline_queue.status
  |FROM offline_queue
  |WHERE status = 'PENDING'
  |ORDER BY priority DESC, created_at ASC
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9)!!
    )
  }

  public fun selectAllPendingQueueItems(): Query<Offline_queue> = selectAllPendingQueueItems { id,
      method, url, headers_json, body, priority, created_at, retry_count, max_retries, status ->
    Offline_queue(
      id,
      method,
      url,
      headers_json,
      body,
      priority,
      created_at,
      retry_count,
      max_retries,
      status
    )
  }

  public fun <T : Any> selectQueueItemById(id: Long, mapper: (
    id: Long,
    method: String,
    url: String,
    headers_json: String,
    body: String?,
    priority: Long,
    created_at: Long,
    retry_count: Long,
    max_retries: Long,
    status: String,
  ) -> T): Query<T> = SelectQueueItemByIdQuery(id) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9)!!
    )
  }

  public fun selectQueueItemById(id: Long): Query<Offline_queue> = selectQueueItemById(id) { id_,
      method, url, headers_json, body, priority, created_at, retry_count, max_retries, status ->
    Offline_queue(
      id_,
      method,
      url,
      headers_json,
      body,
      priority,
      created_at,
      retry_count,
      max_retries,
      status
    )
  }

  public fun countPendingQueueItems(): Query<Long> = Query(431_398_745, arrayOf("offline_queue"),
      driver, "KmpSdkDatabase.sq", "countPendingQueueItems",
      "SELECT COUNT(*) FROM offline_queue WHERE status = 'PENDING'") { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> selectCacheEntry(cache_key: String, mapper: (
    cache_key: String,
    response_body: String,
    created_at: Long,
    ttl_millis: Long,
  ) -> T): Query<T> = SelectCacheEntryQuery(cache_key) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!,
      cursor.getLong(3)!!
    )
  }

  public fun selectCacheEntry(cache_key: String): Query<Api_cache> = selectCacheEntry(cache_key) {
      cache_key_, response_body, created_at, ttl_millis ->
    Api_cache(
      cache_key_,
      response_body,
      created_at,
      ttl_millis
    )
  }

  public fun <T : Any> selectPendingActions(mapper: (
    id: Long,
    action_type: String,
    payload_json: String,
    created_at: Long,
    status: String,
  ) -> T): Query<T> = Query(-1_370_957_180, arrayOf("offline_action"), driver, "KmpSdkDatabase.sq",
      "selectPendingActions", """
  |SELECT offline_action.id, offline_action.action_type, offline_action.payload_json, offline_action.created_at, offline_action.status
  |FROM offline_action
  |WHERE status = 'PENDING'
  |ORDER BY created_at ASC
  """.trimMargin()) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getString(4)!!
    )
  }

  public fun selectPendingActions(): Query<Offline_action> = selectPendingActions { id, action_type,
      payload_json, created_at, status ->
    Offline_action(
      id,
      action_type,
      payload_json,
      created_at,
      status
    )
  }

  public fun selectLastInsertRowId(): ExecutableQuery<Long> = Query(1_511_637_384, driver,
      "KmpSdkDatabase.sq", "selectLastInsertRowId", "SELECT last_insert_rowid()") { cursor ->
    cursor.getLong(0)!!
  }

  public fun insertQueueItem(
    method: String,
    url: String,
    headers_json: String,
    body: String?,
    priority: Long,
    created_at: Long,
    retry_count: Long,
    max_retries: Long,
    status: String,
  ) {
    driver.execute(-808_154_935, """
        |INSERT INTO offline_queue(method, url, headers_json, body, priority, created_at, retry_count, max_retries, status)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 9) {
          bindString(0, method)
          bindString(1, url)
          bindString(2, headers_json)
          bindString(3, body)
          bindLong(4, priority)
          bindLong(5, created_at)
          bindLong(6, retry_count)
          bindLong(7, max_retries)
          bindString(8, status)
        }
    notifyQueries(-808_154_935) { emit ->
      emit("offline_queue")
    }
  }

  public fun updateQueueItemStatus(
    status: String,
    retry_count: Long,
    id: Long,
  ) {
    driver.execute(-1_103_531_509, """
        |UPDATE offline_queue
        |SET status = ?, retry_count = ?
        |WHERE id = ?
        """.trimMargin(), 3) {
          bindString(0, status)
          bindLong(1, retry_count)
          bindLong(2, id)
        }
    notifyQueries(-1_103_531_509) { emit ->
      emit("offline_queue")
    }
  }

  public fun deleteQueueItem(id: Long) {
    driver.execute(1_906_701_847, """
        |DELETE FROM offline_queue
        |WHERE id = ?
        """.trimMargin(), 1) {
          bindLong(0, id)
        }
    notifyQueries(1_906_701_847) { emit ->
      emit("offline_queue")
    }
  }

  public fun clearQueue() {
    driver.execute(-741_709_754, """DELETE FROM offline_queue""", 0)
    notifyQueries(-741_709_754) { emit ->
      emit("offline_queue")
    }
  }

  public fun insertCacheEntry(
    cache_key: String,
    response_body: String,
    created_at: Long,
    ttl_millis: Long,
  ) {
    driver.execute(-1_179_551_317, """
        |INSERT OR REPLACE INTO api_cache(cache_key, response_body, created_at, ttl_millis)
        |VALUES (?, ?, ?, ?)
        """.trimMargin(), 4) {
          bindString(0, cache_key)
          bindString(1, response_body)
          bindLong(2, created_at)
          bindLong(3, ttl_millis)
        }
    notifyQueries(-1_179_551_317) { emit ->
      emit("api_cache")
    }
  }

  public fun deleteExpiredCache(`value`: Long) {
    driver.execute(1_087_562_794, """
        |DELETE FROM api_cache
        |WHERE (created_at + ttl_millis) < ?
        """.trimMargin(), 1) {
          bindLong(0, value)
        }
    notifyQueries(1_087_562_794) { emit ->
      emit("api_cache")
    }
  }

  public fun deleteCacheEntry(cache_key: String) {
    driver.execute(1_376_630_301, """
        |DELETE FROM api_cache
        |WHERE cache_key = ?
        """.trimMargin(), 1) {
          bindString(0, cache_key)
        }
    notifyQueries(1_376_630_301) { emit ->
      emit("api_cache")
    }
  }

  public fun insertOfflineAction(
    action_type: String,
    payload_json: String,
    created_at: Long,
    status: String,
  ) {
    driver.execute(-123_950_978, """
        |INSERT INTO offline_action(action_type, payload_json, created_at, status)
        |VALUES (?, ?, ?, ?)
        """.trimMargin(), 4) {
          bindString(0, action_type)
          bindString(1, payload_json)
          bindLong(2, created_at)
          bindString(3, status)
        }
    notifyQueries(-123_950_978) { emit ->
      emit("offline_action")
    }
  }

  public fun updateOfflineActionStatus(status: String, id: Long) {
    driver.execute(100_711_936, """
        |UPDATE offline_action
        |SET status = ?
        |WHERE id = ?
        """.trimMargin(), 2) {
          bindString(0, status)
          bindLong(1, id)
        }
    notifyQueries(100_711_936) { emit ->
      emit("offline_action")
    }
  }

  private inner class SelectQueueItemByIdQuery<out T : Any>(
    public val id: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("offline_queue", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("offline_queue", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-192_454_952, """
    |SELECT offline_queue.id, offline_queue.method, offline_queue.url, offline_queue.headers_json, offline_queue.body, offline_queue.priority, offline_queue.created_at, offline_queue.retry_count, offline_queue.max_retries, offline_queue.status
    |FROM offline_queue
    |WHERE id = ?
    """.trimMargin(), mapper, 1) {
      bindLong(0, id)
    }

    override fun toString(): String = "KmpSdkDatabase.sq:selectQueueItemById"
  }

  private inner class SelectCacheEntryQuery<out T : Any>(
    public val cache_key: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("api_cache", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("api_cache", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_070_320_594, """
    |SELECT api_cache.cache_key, api_cache.response_body, api_cache.created_at, api_cache.ttl_millis
    |FROM api_cache
    |WHERE cache_key = ?
    """.trimMargin(), mapper, 1) {
      bindString(0, cache_key)
    }

    override fun toString(): String = "KmpSdkDatabase.sq:selectCacheEntry"
  }
}
