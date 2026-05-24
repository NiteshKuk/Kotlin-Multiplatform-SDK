package com.kmpsdk.data.db

/**
 * SQLDelight migration guidance for host apps and SDK consumers.
 *
 * Setup (already configured in [com.kmpsdk build.gradle]):
 * - `schemaOutputDirectory` stores versioned schema snapshots under `sqldelight/databases/`
 * - `verifyMigrations.set(true)` validates `.sqm` files at compile time
 *
 * When you change [KmpSdkDatabase.sq]:
 * 1. Run `./gradlew :kmp-sdk:generateSqlDelightSchema`
 * 2. Add `sqldelight/com/kmpsdk/data/db/migrations/<N+1>.sqm` with ALTER statements
 * 3. Ship the migration before changing the `.sq` CREATE statements for existing users
 */
object DatabaseMigrationGuide {
    const val CURRENT_VERSION: Int = 1
    const val DATABASE_NAME: String = "kmpsdk.db"
}
