# Recipes (copy-paste)

Short scenarios. Expand in linked docs when needed.

## 1) Online-only GET (Path A)

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://jsonplaceholder.typicode.com"
    enableHttpCache = false
    install(AboutFeatureModule)
}
val about = KmpSdk.get<GetAboutUseCase>().load()
```

## 2) Cached GET (Path B)

```kotlin
enableHttpCache = true
syncPolicy = SyncPolicy.NETWORK_FIRST
```

## 3) Offline list + CRUD (Path C Feature Kit)

Generate → `install(ProductFeatureModule)` →:

```kotlin
val api = KmpSdk.get<RestResourceApi<Product>>()
api.refresh()
api.observeAll()
api.create(CreateProductBody(...))
```

See [feature-kit.md](feature-kit.md).

## 4) Host analytics (custom class) + telemetry

```kotlin
// 1) Your type (any custom class / interface)
interface AppAnalytics {
    fun event(name: String, params: Map<String, String> = emptyMap())
}

// 2) Register
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    register<AppAnalytics> { MyFirebaseAnalytics() } // your implementation
    install(UserFeatureModule)
}

// 3) Feature module
val analytics = registry.resolve<AppAnalytics>()
// or UI: KmpSdk.get<AppAnalytics>().event("tap")

// 4) Optional — forward SDK telemetry
KmpSdk.telemetry.addListener { event ->
    when (event) {
        is TelemetryEvent.ApiCallCompleted ->
            KmpSdk.get<AppAnalytics>().event("sdk_api", mapOf("path" to event.path))
        else -> Unit
    }
}
```

Full walkthrough (expect/actual, verify on Android/iOS): [getting-started.md](getting-started.md#4-inject-host-types--appanalytics--custom-classes--telemetry).

## 5) Login + session

```kotlin
auth { enabled = true }
tokenRefreshHandler = …
KmpSdk.sessionManager.login(access, refresh)
```

## 6) Push → refresh list

```kotlin
push { on("order_updated") { KmpSdk.syncCoordinator.refreshTarget("orders") } }
```

## 7) Upload file

```kotlin
KmpSdk.fileUpload.upload<UploadResponse>(path = "/upload", fileName = "a.jpg", bytes = bytes)
```

## 8) Offline banner from sync status

```kotlin
KmpSdk.syncStatus.observe("products").collect { /* OfflineCached → banner */ }
```

## 9) Certificate pinning (Android)

```kotlin
import com.kmpsdk.core.config.CertificateParams

KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    certificateBuilder = CertificateParams(
        hostname = "api.example.com",
        certificatePins = listOf(
            "<sha256Base64>",
            "<backupSha256Base64>",
        ),
    )
}
```

`CertificateParams(hostname, certificatePins)` — details in [networking.md](networking.md#ssl--certificate-pinning).

## 10) Drafts + local query

```kotlin
KmpSdk.drafts.save("create_product", CreateProductBody(...))
val draft = KmpSdk.drafts.load<CreateProductBody>("create_product")

KmpSdk.query.sources.register(
    name = "products",
    observeAll = { store.observeAll() },
) { item, field ->
    when (field) {
        "title" -> item.title
        else -> null
    }
}
```

## 11) Deep link → refresh

```kotlin
deepLinks {
    route("orders/{id}") { _, _ ->
        KmpSdk.syncCoordinator.refreshTarget("orders")
    }
}
// later:
KmpSdk.deepLinks.handle("myapp://orders/42")
```

## 12) Environment switch (debug)

```kotlin
KmpSdk.environments?.switchTo("staging")
```
