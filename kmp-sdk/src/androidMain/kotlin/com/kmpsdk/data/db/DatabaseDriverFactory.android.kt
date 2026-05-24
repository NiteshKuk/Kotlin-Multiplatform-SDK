package com.kmpsdk.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.kmpsdk.KmpSdkAndroid

actual class DatabaseDriverFactory(
    private val context: Context,
) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(KmpSdkDatabase.Schema, context, "kmpsdk.db")
}

actual fun createDatabaseDriverFactory(): DatabaseDriverFactory =
    DatabaseDriverFactory(KmpSdkAndroid.requireContext())
