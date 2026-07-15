# KmpSDK

Headless **Kotlin Multiplatform** SDK for Android and iOS.

Infrastructure for networking, auth, cache/offline sync, and MVI **contracts**.  
**You own all UI** (Compose, SwiftUI, XML, …).

Current Maven version (see `gradle.properties`): **`1.0.1`**  
Coordinates: `in.co.niteshkukreja:kmp-sdk`

---

## Start here (pick one)

| I need… | Go to |
|---------|--------|
| Install in 5 minutes | [docs/getting-started.md](docs/getting-started.md) |
| Path A / B / C explained | [docs/integration-paths.md](docs/integration-paths.md) |
| Offline list + CRUD | [docs/feature-kit.md](docs/feature-kit.md) |
| Copy-paste recipes | [docs/recipes.md](docs/recipes.md) |
| Something broken | [docs/troubleshooting.md](docs/troubleshooting.md) |
| Full docs index | [docs/README.md](docs/README.md) |

### Quick path decision

```
Need YOUR SQL offline for this feature?
  YES → Path C  → docs/feature-kit.md
  NO  → Last HTTP response offline OK?
         YES → Path B  → docs/integration-paths.md
         NO  → Path A  → docs/getting-started.md
```

---

## 60-second install

```kotlin
// shared/build.gradle.kts
implementation("in.co.niteshkukreja:kmp-sdk:1.0.1")

// Application.onCreate
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    install(YourFeatureModule)
}
```

Repositories: `mavenCentral()` (add `mavenLocal()` only when testing a local publish).

---

## Sample app?

This repo **does not** ship an in-repo Android/iOS sample yet.

Use your **consumer test app** (e.g. TestingKmpSdk) as the living sample — see **[docs/sample-app.md](docs/sample-app.md)**.

---

## Docs map

| Topic | Doc |
|-------|-----|
| Getting started + host injection | [docs/getting-started.md](docs/getting-started.md) |
| Paths A/B/C | [docs/integration-paths.md](docs/integration-paths.md) |
| Feature Kit / generator / OpenAPI | [docs/feature-kit.md](docs/feature-kit.md) |
| Auth, Firebase remote config, env, tenant | [docs/auth-and-config.md](docs/auth-and-config.md) |
| Cache, resilience, upload, realtime | [docs/networking.md](docs/networking.md) |
| Offline queue, sync status | [docs/offline-sync.md](docs/offline-sync.md) |
| DI, deep links, push, background | [docs/platform-integration.md](docs/platform-integration.md) |
| Recipes | [docs/recipes.md](docs/recipes.md) |
| Troubleshooting | [docs/troubleshooting.md](docs/troubleshooting.md) |
| API cheat sheet | [docs/api-reference.md](docs/api-reference.md) |
| Recent changes | [docs/changelog.md](docs/changelog.md) |

Tools: [feature-generator](tools/feature-generator/README.md) · [openapi-import](tools/openapi-import/README.md) · [migration-helper](tools/migration-helper/README.md)

---

## Build (SDK)

```powershell
.\gradlew.bat :kmp-sdk:compileDebugKotlinAndroid :kmp-sdk:allTests --no-daemon
```

Mac (incl. iOS targets for publish):

```bash
./gradlew :kmp-sdk:assemble --no-daemon
```

---

## What is NOT in the SDK

- Compose / SwiftUI screens or themes  
- Platform toasts/snackbars  
- Your domain entities (User, Product, …) — those stay in the host app  

---

## Contributing / release

See [CONTRIBUTING.md](CONTRIBUTING.md). Publish iOS artifacts from a **Mac** (or macOS CI).
