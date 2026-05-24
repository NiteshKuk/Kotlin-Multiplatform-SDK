package com.kmpsdk.`data`.db

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.kmpsdk.`data`.db.kmpsdk.newInstance
import com.kmpsdk.`data`.db.kmpsdk.schema
import kotlin.Unit

public interface KmpSdkDatabase : Transacter {
  public val kmpSdkDatabaseQueries: KmpSdkDatabaseQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = KmpSdkDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): KmpSdkDatabase =
        KmpSdkDatabase::class.newInstance(driver)
  }
}
