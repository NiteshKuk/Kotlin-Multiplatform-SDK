package com.kmpsdk.`data`.db.kmpsdk

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.kmpsdk.`data`.db.KmpSdkDatabase
import com.kmpsdk.`data`.db.KmpSdkDatabaseQueries
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass

internal val KClass<KmpSdkDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = KmpSdkDatabaseImpl.Schema

internal fun KClass<KmpSdkDatabase>.newInstance(driver: SqlDriver): KmpSdkDatabase =
    KmpSdkDatabaseImpl(driver)

private class KmpSdkDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), KmpSdkDatabase {
  override val kmpSdkDatabaseQueries: KmpSdkDatabaseQueries = KmpSdkDatabaseQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE api_cache (
          |    cache_key TEXT NOT NULL PRIMARY KEY,
          |    response_body TEXT NOT NULL,
          |    created_at INTEGER NOT NULL,
          |    ttl_millis INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE offline_queue (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    method TEXT NOT NULL,
          |    url TEXT NOT NULL,
          |    headers_json TEXT NOT NULL,
          |    body TEXT,
          |    priority INTEGER NOT NULL DEFAULT 0,
          |    created_at INTEGER NOT NULL,
          |    retry_count INTEGER NOT NULL DEFAULT 0,
          |    max_retries INTEGER NOT NULL DEFAULT 3,
          |    status TEXT NOT NULL DEFAULT 'PENDING'
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE offline_action (
          |    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
          |    action_type TEXT NOT NULL,
          |    payload_json TEXT NOT NULL,
          |    created_at INTEGER NOT NULL,
          |    status TEXT NOT NULL DEFAULT 'PENDING'
          |)
          """.trimMargin(), 0)
      driver.execute(null, "CREATE INDEX api_cache_created_at ON api_cache(created_at)", 0)
      driver.execute(null,
          "CREATE INDEX offline_queue_status_priority ON offline_queue(status, priority DESC, created_at ASC)",
          0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
