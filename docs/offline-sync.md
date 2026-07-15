# Offline & sync

## Queue mutations when offline

```kotlin
queueMutationsWhenOffline = true
// POST/PUT/PATCH/DELETE enqueue when offline; replay on reconnect
```

## Sync on reconnect / manual

```kotlin
autoSyncOnReconnect = true
KmpSdk.syncCoordinator.syncAll()
KmpSdk.syncCoordinator.refreshTarget("users")
```

## Sync status API (UI)

Phases: `Idle` | `Syncing` | `OfflineCached` | `Failed`

```kotlin
KmpSdk.syncStatus.observe("users").collect { status ->
    when (status.phase) {
        FeatureSyncPhase.Syncing -> showRefreshing()
        FeatureSyncPhase.OfflineCached -> showOfflineBanner()
        FeatureSyncPhase.Failed -> showError(status.lastErrorMessage)
        FeatureSyncPhase.Idle -> hideBanners()
    }
}
```

Name must match Feature Kit / `registerSyncTarget` name.

## Dirty SQL sync

```kotlin
KmpSdk.dirtySyncCoordinator.syncDirty(object : DirtySyncTarget<UserEntity> {
    override suspend fun loadDirty() = local.getDirtyUsers()
    override suspend fun push(record: UserEntity) = remote.update(record)
    override suspend fun markClean(id: String) = local.markClean(id)
})
```

## Domain offline actions

```kotlin
KmpSdk.offlineActions.registerHandler("FAVORITE_POST") { payload -> /* network call */ }
KmpSdk.offlineActions.enqueue(actionType = "FAVORITE_POST", entityId = id, payloadJson = "{}")
```

## Drafts / local query

```kotlin
KmpSdk.drafts.save("create_product", body)
KmpSdk.query.sources.register("products", observeAll = { store.observeAll() }) { item, field -> … }
```
