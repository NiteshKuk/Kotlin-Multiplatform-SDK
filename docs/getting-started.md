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
    // certificatePins = listOf("api.example.com/<base64>")
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

## 4) Inject host types into the SDK

Register in init, resolve in modules:

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    register<AppAnalytics> { AppAnalytics() }
    install(UserFeatureModule)
}

// In UserFeatureModule:
// analytics = registry.resolve<AppAnalytics>()
```

| Goal | API |
|------|-----|
| Register host service | `register<MyType> { … }` in `KmpSdk.init` |
| Read later | `KmpSdk.get<MyType>()` |
| Use in feature module | `registry.resolve<MyType>()` |

Host Koin/Hilt **alone** does not feed `KmpSdkModule` — still `register` into the SDK. See [platform-integration.md](platform-integration.md)#di-adapters.

## Next

- Pick a path → [integration-paths.md](integration-paths.md)
- Offline CRUD → [feature-kit.md](feature-kit.md)
- Sample consumer app → [sample-app.md](sample-app.md)
