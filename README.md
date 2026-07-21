# KmpSDK

Headless **Kotlin Multiplatform** SDK for Android and iOS.

Networking, auth, cache/offline sync, and MVI **contracts** — **you own all UI** (Compose, SwiftUI, XML, …).

| | |
|--|--|
| **Maven** | `in.co.niteshkukreja:kmp-sdk` |
| **Version** | **[Releases]** (also `sdkVersion` in `gradle.properties`) |

---

## Find it in the docs

Scan **I want to…**, check **What’s inside** for keywords (UseCase, ViewModel, …), then open **Go to**.

### Get running

| I want to… | What’s inside | Go to |
|------------|---------------|-------|
| Install & `KmpSdk.init` in 5 minutes | Dependency, `init`, `KmpSdk.get`, feature modules, host `register` | [getting-started](docs/getting-started.md) |
| See Path A vs B vs C | Comparison table, Path A UseCase sample, Path B cache flags, Path C + ViewModel / `bindSyncList` | [integration-paths](docs/integration-paths.md) |
| Copy-paste a working snippet | Path A/B/C, analytics, auth, push, upload, sync banner, SSL, drafts, deep link, env switch | [recipes](docs/recipes.md) |
| Fix a build / runtime error | Gradle, iOS publish, logging plugin, QueryKit, host `register`, TLS / pinning | [troubleshooting](docs/troubleshooting.md) |

### Features & data

| I want to… | What’s inside | Go to |
|------------|---------------|-------|
| Offline list + CRUD | Feature Kit, `RestResourceApi`, generator, OpenAPI import, `installRestResourceFeature` | [feature-kit](docs/feature-kit.md) |
| UseCase + feature module | `KmpSdkModule`, `register` / `get` UseCase, Path A UseCase example | [getting-started](docs/getting-started.md) · [integration-paths](docs/integration-paths.md) · [recipes](docs/recipes.md) |
| ViewModel / MVI / list binding | Path C notes: host ViewModel + `bindSyncList`; MVI contracts in SDK (`MviViewModel`) | [integration-paths](docs/integration-paths.md) · [api-reference](docs/api-reference.md) |
| HTTP cache, upload, realtime, resilience | Path B cache, retry / circuit breaker, `fileUpload`, WebSocket / SSE, dedup / rate-limit | [networking](docs/networking.md) |
| SSL / certificate pinning | `CertificateParams`, `certificateBuilder`, openssl, Android-only, captive-portal vs pin fail | [networking § SSL](docs/networking.md#ssl--certificate-pinning) |
| Offline queue & sync UI | Offline mutations, `syncCoordinator`, `syncStatus` banner, dirty sync, drafts, QueryKit | [offline-sync](docs/offline-sync.md) |

### Host app wiring

| I want to… | What’s inside | Go to |
|------------|---------------|-------|
| AppAnalytics / custom classes / telemetry | Custom interface/class, `expect`/`actual`, `register`, feature UseCase wiring, `KmpSdk.telemetry` bridge | [getting-started §4](docs/getting-started.md#4-inject-host-types--appanalytics--custom-classes--telemetry) |
| Auth & session | `auth { }`, `tokenRefreshHandler`, `sessionManager.login` / events | [auth-and-config](docs/auth-and-config.md) |
| Firebase Remote Config | Host Firebase fetch → `remoteConfig { }` map, `getBoolean` / TTL key | [auth-and-config § RC](docs/auth-and-config.md#remote-config--firebase-in-the-host-app) |
| Environments / tenant | Env vault (`dev`/`staging`/`prod`), `switchTo`, `tenantManager` | [auth-and-config](docs/auth-and-config.md) |
| DI, deep links, push, background | Koin / Hilt / Kodein bridges, deep link routes, push router, `backgroundWork` | [platform-integration](docs/platform-integration.md) |
| Messages → snackbar / toast | `messageEventBus` collect → your UI | [platform-integration § Messages](docs/platform-integration.md#messages--ui) |

### Reference & tools

| I want to… | What’s inside | Go to |
|------------|---------------|-------|
| API / `KmpSdk.*` cheat sheet | Globals, init flags (`certificateBuilder`, …), debugger snapshot | [api-reference](docs/api-reference.md) |
| What changed recently | Feature Kit, sync, upload, DI, docs notes | [changelog](docs/changelog.md) |
| Consumer / sample checklist | Path A/B/C demos, host injection, sync banner checklist | [sample-app](docs/sample-app.md) |
| Full docs index | Same goals, single index page | [docs/README](docs/README.md) |
| Generate a feature from YAML | CLI, `product.yaml` / `order.yaml`, SQL option | [feature-generator](tools/feature-generator/README.md) |
| Import OpenAPI → Feature Kit | Spec → YAML → generated feature | [openapi-import](tools/openapi-import/README.md) |
| SQLDelight migration helper | Host schema migration tips | [migration-helper](tools/migration-helper/README.md) |

---

## Not sure which path? (30 seconds)

```
Need YOUR SQL offline for this feature?
  YES → Path C  → docs/feature-kit.md
  NO  → Last HTTP response offline OK?
         YES → Path B  → docs/networking.md + docs/integration-paths.md
         NO  → Path A  → docs/getting-started.md
```

More detail: [docs/integration-paths.md](docs/integration-paths.md).

---

## Do / don’t (quick)

| Do | Don’t |
|----|--------|
| Pick **one** path (A/B/C) per feature | Mix Path C SQL + Path B cache for the same list without a reason |
| `register` host types in `KmpSdk.init` | Expect Koin/Hilt alone to feed feature modules |
| Keep Firebase, Analytics, UI in the **host** app | Expect the SDK to ship screens or Firebase |
| Use recipes + the table above to find docs | Treat this README as the full API manual |
| Pin with `certificateBuilder` on **prod Android** when you have real pins | Debug pinning on captive portal / wrong-host certs |

---

## 60-second install

```kotlin
// shared/build.gradle.kts
implementation("in.co.niteshkukreja:kmp-sdk:[Releases]")

// Application.onCreate
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    install(YourFeatureModule)
}
```

Repositories: `mavenCentral()` (add `mavenLocal()` only when testing a local publish).

**Next (optional):** SSL pinning → [networking](docs/networking.md#ssl--certificate-pinning) · Analytics → [getting-started §4](docs/getting-started.md#4-inject-host-types--appanalytics--custom-classes--telemetry) · Firebase RC → [auth-and-config](docs/auth-and-config.md#remote-config--firebase-in-the-host-app)

---

## Sample app?

No in-repo Android/iOS sample yet. Use your consumer test app (e.g. TestingKmpSdk) — checklist: [docs/sample-app.md](docs/sample-app.md).

---

## Build (SDK maintainers)

```powershell
.\gradlew.bat :kmp-sdk:compileDebugKotlinAndroid :kmp-sdk:allTests --no-daemon
```

Mac (incl. iOS targets for publish):

```bash
./gradlew :kmp-sdk:assemble --no-daemon
```

Publish iOS artifacts from a **Mac** (or macOS CI). See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## What is NOT in the SDK

- Compose / SwiftUI screens or themes  
- Platform toasts/snackbars  
- Your domain entities & use cases live in the **host** (SDK only provides contracts + `install` / `get`)  
- iOS certificate pinning (host Darwin / `NSURLSession` if needed)  
