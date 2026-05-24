# SQLDelight migration helper

Before shipping schema changes in your **host app**:

1. Update your `AppDatabase.sq`
2. Generate a schema snapshot from your shared module:
   ```bash
   ./gradlew :shared:generateCommonMainAppDatabaseSchema
   ```
   (Replace `:shared` with your host module name.)
3. Add `.sqm` migration files under `shared/src/commonMain/sqldelight/migrations/` (or your chosen migrations folder)
4. Enable verification in your module `build.gradle.kts`:
   ```kotlin
   sqldelight {
       databases {
           create("AppDatabase") {
               packageName.set("com.yourapp.db")
               schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
               verifyMigrations.set(true)
           }
       }
   }
   ```

SDK infra DB migrations follow the same pattern in `kmp-sdk` — see `DatabaseMigrationGuide.kt`.
