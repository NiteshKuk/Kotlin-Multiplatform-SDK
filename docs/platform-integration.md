# Platform integration (DI, deep links, push, background)

## DI adapters

```kotlin
// Koin — after KmpSdk.init, before first inject
startKoin { modules(kmpSdkKoinModule()) }

// Hilt (host @Module)
@Provides fun api(): RestResourceApi<Product> = hiltKmpGet()

// Kodein
bindSingleton { kmpSdkKodeinGet<RestResourceApi<Product>>() }
```

Host → SDK modules: still `register` in `KmpSdk.init` (see [getting-started.md](getting-started.md)).

## Deep links

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    deepLinks {
        route("orders/{id}") { args, _ ->
            KmpSdk.syncCoordinator.refreshTarget("orders")
        }
    }
}
// Platform:
KmpSdk.deepLinks.handle("myapp://orders/42")
```

## Push data payloads

```kotlin
push {
    on("order_updated") { KmpSdk.syncCoordinator.refreshTarget("orders") }
}
// FCM / APNs:
KmpSdk.push.handle(remoteMessage.data)
```

## Background work

```kotlin
backgroundWork {
    periodicSync(15.minutes)
    syncOnConnectivityRestored = true
}
KmpSdk.backgroundWork.enqueueOneShotSync("manual")
```

Also: `backgroundSyncIntervalMillis` on init for the built-in scheduler loop.

## Messages / UI

SDK emits events; **you** show toast/snackbar:

```kotlin
KmpSdk.messageEventBus.events.collect { Snackbar.make(…, it.message, …).show() }
```
