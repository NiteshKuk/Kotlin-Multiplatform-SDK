package com.kmpsdk.data.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

expect fun createDatabaseDriverFactory(): DatabaseDriverFactory
