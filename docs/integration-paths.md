# Integration paths (A / B / C)

Use **one path per feature** (login can be A while catalog is C).

## Quick decision

```
Need this feature's data in YOUR SQL when offline?
  YES → Path C
  NO  → Is showing the last API response offline OK?
         YES → Path B
         NO  → Path A
```

## Comparison

| | Path A | Path B | Path C |
|---|--------|--------|--------|
| Best for | Login, forms | Lists OK with last GET body | Feeds, catalogs, field apps |
| Your SQL | No | No | Yes |
| Typical API | `networkClient.get/post` | Same + HTTP cache | `BaseSyncRepository` / Feature Kit |

SDK still opens its own DB (`api_cache`, `offline_queue`, …) on every path — that is **not** your app tables.

## Path A — Online only

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    syncPolicy = SyncPolicy.NETWORK_FIRST
    enableHttpCache = false
    queueMutationsWhenOffline = false
    install(AboutFeatureModule)
}

class GetAboutUseCase {
    suspend fun load() = KmpSdk.networkClient.get<AboutDto>("/about")
}
```

## Path B — SDK HTTP cache

Same as Path A, but:

```kotlin
enableHttpCache = true
syncPolicy = SyncPolicy.NETWORK_FIRST
```

Offline GET may return the last cached HTTP body from SDK `api_cache`.

## Path C — Full offline-first

Prefer the [Feature Kit](feature-kit.md) for list CRUD.

Manual Path C checklist:

1. Host `AppDatabase.sq` table  
2. Local + remote sources  
3. `BaseSyncRepository` **or** `installRestResourceFeature`  
4. Use case + ViewModel (`bindSyncList`) + your UI  

Details and samples: historical sections also lived in the old monolithic README (recover via git if needed). See also [recipes.md](recipes.md).
