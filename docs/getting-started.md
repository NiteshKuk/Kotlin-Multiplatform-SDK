# Getting started

**You need this if:** you are adding KmpSDK to a host app for the first time.

## Prerequisites

- JDK 17+
- KMP host with a `shared` module
- Android Studio / Xcode for platform UI

## 1) Dependency

`settings.gradle.kts` — include `mavenCentral()` (and `mavenLocal()` only if testing a local publish).

`shared/build.gradle.kts`:

```kotlin
implementation("in.co.niteshkukreja:kmp-sdk:1.2.0") // match gradle.properties sdkVersion
```

## 2) Initialize once

Android `Application.onCreate`:

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    logLevel = LogLevel.DEBUG
    // optional Android SSL pins — see networking.md#ssl--certificate-pinning
    // certificateBuilder = CertificateParams(hostname = "api.example.com", certificatePins = listOf("<base64>"))
    install(AboutFeatureModule) // your module
}
```

Common / iOS:

```kotlin
KmpSdk.init {
    baseUrl = "https://api.example.com"
    install(AboutFeatureModule)
}
```

## 3) Resolve

```kotlin
val useCase = KmpSdk.get<GetAboutUseCase>()
```

## 4) Inject host types — AppAnalytics + custom classes + telemetry

The SDK does **not** ship Firebase Analytics. You own a custom class (or interface), `register` it at init, then resolve it from feature modules / UI. Same call sites on **Android and iOS**; use `expect`/`actual` if backends differ.

### Custom class (common)

```kotlin
interface AppAnalytics {
    fun screen(name: String)
    fun event(name: String, params: Map<String, String> = emptyMap())
}

// Or a single concrete class if one implementation is enough
class LoggingAppAnalytics : AppAnalytics {
    override fun screen(name: String) = println("screen=$name")
    override fun event(name: String, params: Map<String, String>) =
        println("event=$name params=$params")
}
```

### Optional: platform backends (Firebase, etc.)

```kotlin
// commonMain
expect fun createAppAnalytics(): AppAnalytics

// androidMain — wrap FirebaseAnalytics / your SDK
actual fun createAppAnalytics(): AppAnalytics = AndroidAppAnalytics(/* context */)

// iosMain — wrap Firebase iOS / your tracker
actual fun createAppAnalytics(): AppAnalytics = IosAppAnalytics()
```

### Register at init

```kotlin
KmpSdk.init(this) { // Android: pass Application/Context
    baseUrl = "https://api.example.com"
    register<AppAnalytics> { createAppAnalytics() } // or { LoggingAppAnalytics() }
    install(UserFeatureModule)
}
```

You can register **any** host type the same way (`AppAnalytics`, `CrashReporter`, feature flags helper, …).

### Use in a feature module

```kotlin
object UserFeatureModule : KmpSdkModule {
    override fun register(registry: KmpSdkRegistry) {
        val analytics = registry.resolve<AppAnalytics>()
        registry.register<GetUsersUseCase> { ctx ->
            GetUsersUseCase(ctx.networkClient, analytics)
        }
    }
}

class GetUsersUseCase(
    private val network: KmpNetworkClient,
    private val analytics: AppAnalytics,
) {
    suspend fun load() {
        analytics.screen("users")
        network.get<List<UserDto>>("/users")
            .onSuccess { analytics.event("users_loaded", mapOf("count" to it.size.toString())) }
            .onFailure { analytics.event("users_failed") }
    }
}
```

### Use from UI / anywhere after init

```kotlin
KmpSdk.get<AppAnalytics>().event("button_tap", mapOf("id" to "refresh"))
```

### How to verify on Android & iOS

1. Put a `println` / `Log.d` / `NSLog` inside your `AppAnalytics` implementation.  
2. Open a screen that calls `analytics.event(...)`.  
3. Confirm the log on each platform.  
4. Optional: Firebase Analytics **DebugView** (Android `adb setprop debug.firebase.analytics.app …`, iOS `-FIRDebugEnabled`).

### Bridge SDK telemetry → AppAnalytics

`KmpSdk.telemetry` emits SDK lifecycle events (API, sync, offline queue, session, validation). Forward them into your custom analytics **after** init:

```kotlin
KmpSdk.telemetry.addListener { event ->
    val analytics = KmpSdk.get<AppAnalytics>()
    when (event) {
        is TelemetryEvent.ApiCallCompleted -> analytics.event(
            "sdk_api_call",
            mapOf(
                "method" to event.method,
                "path" to event.path,
                "ok" to event.success.toString(),
                "ms" to event.durationMs.toString(),
            ),
        )
        is TelemetryEvent.SyncCompleted -> analytics.event(
            "sdk_sync",
            mapOf(
                "replayed" to event.replayedOffline.toString(),
                "refreshed" to event.refreshedRepos.toString(),
            ),
        )
        is TelemetryEvent.OfflineQueued -> analytics.event(
            "sdk_offline_queued",
            mapOf("method" to event.method, "path" to event.path),
        )
        is TelemetryEvent.SessionEvent ->
            analytics.event("sdk_session", mapOf("name" to event.name))
        is TelemetryEvent.ValidationWarning ->
            analytics.event("sdk_validation", mapOf("msg" to event.message))
    }
}
```

| Goal | API |
|------|-----|
| Register host service | `register<MyType> { … }` in `KmpSdk.init` |
| Read later | `KmpSdk.get<MyType>()` |
| Use in feature module | `registry.resolve<MyType>()` |
| SDK → your tracker | `KmpSdk.telemetry.addListener { … }` |

Host Koin/Hilt **alone** does not feed `KmpSdkModule` — still `register` into the SDK. See [platform-integration.md](platform-integration.md) and [recipes.md](recipes.md) recipe 4.

## Next

- Pick a path → [integration-paths.md](integration-paths.md)
- Offline CRUD → [feature-kit.md](feature-kit.md)
- Sample consumer app → [sample-app.md](sample-app.md)
