package com.kmpsdk.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(KmpSdkDatabase.Schema, "kmpsdk.db")
}

actual fun createDatabaseDriverFactory(): DatabaseDriverFactory = DatabaseDriverFactory()
