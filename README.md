# KmpSDK

Headless, plug-and-play **Kotlin Multiplatform SDK** for Android and iOS.

KmpSDK gives your host app ready-made **infrastructure** — networking, auth, optional caching/offline sync, logging, and MVI **contracts**. **You own all UI** (Compose, SwiftUI, XML, etc.).

Not every screen needs SQL, local data sources, or sync repositories. Pick an integration path per feature (see below).

---

## What you get vs what you build

| KmpSDK provides (infrastructure) | Host app provides (your code) |
|-----------------------------------|------------------------------|
| `KmpSdk.init`, registry, modules | Feature modules, DTOs, use cases |
| Ktor client, auth plugin, error parsing | ViewModels, screens, navigation |
| Optional offline queue, HTTP cache, sync helpers | **Path C only:** your SQL schema, local/remote sources, repos |
| `MviViewModel`, `DataState`, message bus | Toast/snackbar/alert UI |

---

## Choose your integration path

Use **one path per feature** (e.g. login = Path A, product catalog = Path C).

### Quick decision

```
Need this feature's data in YOUR SQL when the device is offline?
  YES → Path C (full offline-first)
  NO  → Is showing the last API response offline OK?
         YES → Path B (network-first + SDK HTTP cache)
         NO  → Path A (online-only)
```

### Path comparison

| | **Path A — Online only** | **Path B — SDK HTTP cache** | **Path C — Full offline-first** |
|---|--------------------------|-------------------------------|----------------------------------|
| **Best for** | Login, forms, one-shot screens | Lists that can show last fetch offline | Feeds, catalogs, field apps |
| **Your SQL tables** | Not required | Not required | Required (`AppDatabase.sq`) |
| **Local data source** | Not required | Not required | Required |
| **Remote + sync repository** | Not required | Not required | Required (or `installRestListFeature`) |
| **Typical API** | `KmpSdk.networkClient.get/post` | Same as A | `BaseSyncRepository`, `bindSyncList` |
| **README steps** | 1–4, then [Path A example](#path-a--online-only-no-your-sql) | 1–4 + [Path B init](#path-b--network-first-sdk-http-cache) | 1–10 (full guide) |

### SDK internal storage (all paths)

`KmpSdk.init` always opens the SDK database (`api_cache`, `offline_queue`, `offline_action`). That is separate from **your** app tables. You control behaviour with init flags (see [Path B init](#path-b--network-first-sdk-http-cache) and [Step 20](#step-20--full-configuration-reference)).

---

### Path A — Online only (no your SQL)

**You write:** use case or ViewModel calling `KmpSdk.networkClient`, plus UI.

**You do not write:** `AppDatabase` table, `LocalDataSource`, or `BaseSyncRepository` for this feature.

**Example init:**

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    syncPolicy = SyncPolicy.NETWORK_FIRST
    enableHttpCache = false
    queueMutationsWhenOffline = false
    install(AuthFeatureModule) // modules without SQL are fine
}
```

**Example use case:**

```kotlin
class GetAboutUseCase {
    suspend fun load(): KmpSdkResult<AboutDto> =
        KmpSdk.networkClient.get("/about")
}
```

**Example feature module (minimal):**

```kotlin
object AboutFeatureModule : KmpSdkModule {
    override fun register(registry: KmpSdkRegistry) {
        registry.register<GetAboutUseCase> { GetAboutUseCase() }
    }
}
```

Wire loading/error/state in your ViewModel (standard `StateFlow` / `DataState`).

---

### Path B — Network-first + SDK HTTP cache

Same app code as Path A for the feature (no your SQL). Offline GET may return the last cached **HTTP body** from the SDK `api_cache` table.

**Example init:**

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    syncPolicy = SyncPolicy.NETWORK_FIRST
    enableHttpCache = true
    queueMutationsWhenOffline = false // or true if mutations should queue
    install(ProductListModule)
}
```

---

### Path C — Full offline-first (your SQL)

Use when the feature must read/write **your** persisted entities offline.

Follow **Steps 5–10** below (SQL → local → remote → repository → use case → ViewModel).

Shortcuts: `SqlDelightListLocalDataSource`, [`installRestListFeature`](#rest-list-installer-no-custom-repository-class) (no custom repository **class**, but local lambdas still required), `RestMutationUseCase`, feature generator CLI.

---

## Prerequisites

- **JDK 17+**
- Android Studio / Xcode for platform apps
- Kotlin Multiplatform project with a `shared` module

---

## Step-by-step integration guide

| Steps | Applies to |
|-------|------------|
| **1–4** | All paths (dependency, init, resolve, feature module) |
| **5–10** | **[Path C](#path-c--full-offline-first-your-sql) only** — SQL, local, remote, repository, use case, ViewModel |
| **11–20** | Optional/advanced (auth, cache, offline queue, config, v1.4) |

**New to the SDK?** Start with [Path A](#path-a--online-only-no-your-sql). Move to Path C only when you need offline data in **your** database.

**Step 20** lists every init flag in one place (core + v1.4).  
The **v1.4 — Rich SDK additions** section below Step 20 documents profiles, telemetry, REST installers, dirty sync, tools, and other advanced features.

### Step 1 — Add the dependency

Add these repositories in your host app **`settings.gradle.kts`**:

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

`mavenLocal()` is used when testing a SDK build published with `publishToMavenLocal`. After Maven Central publish, `mavenCentral()` is enough for users.

Add `kmp-sdk` to your host **shared** module:

```kotlin
implementation("in.co.niteshkukreja:kmp-sdk:1.0.0")
```

**Maven coordinates:**

| Field | Value |
|-------|--------|
| Group | `in.co.niteshkukreja` |
| Artifact | `kmp-sdk` |
| Version | `1.0.0` (see [GitHub Releases](https://github.com/NiteshKuk/Kotlin-Multiplatform-SDK/releases)) |

**Example (`shared/build.gradle.kts`):**

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("in.co.niteshkukreja:kmp-sdk:1.0.0")
        }
    }
}
```

---

### Step 2 — Initialize the SDK

Call `KmpSdk.init` once at app startup. Install only the feature modules you need.

**Example — Android (`Application.onCreate`):**

```kotlin
import com.kmpsdk.KmpSdk
import com.kmpsdk.init
import com.kmpsdk.core.config.SdkProfile
import com.kmpsdk.core.logger.LogLevel
import com.kmpsdk.core.telemetry.KmpSdkTelemetry
import com.yourapp.feature.user.UserFeatureModule

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        KmpSdk.init(this) {
            profile = if (BuildConfig.DEBUG) SdkProfile.DEVELOPMENT else SdkProfile.STAGING
            baseUrl = "https://api.example.com"
            logLevel = LogLevel.DEBUG
            enableRequestLogging = true
            autoSyncOnReconnect = true
            validateOnStartup = true
            install(UserFeatureModule)
        }
        KmpSdk.telemetry.addListener { event -> /* analytics */ }
    }
}
```

**Example — iOS / common only (no Android `Context`):**

```kotlin
KmpSdk.init {
    baseUrl = "https://api.example.com"
    logLevel = LogLevel.INFO
    install(UserFeatureModule)
}
```

**Example — Advanced (custom config object):**

```kotlin
val config = KmpSdkConfig(baseUrl = "https://api.example.com")
KmpSdk.init(config = config) {
    register<OrderRepository> { ctx -> OrderRepositoryImpl(ctx) }
}
```

---

### Step 3 — Resolve dependencies anywhere

After init, use `KmpSdk.get<T>()` to resolve registered types.

**Example:**

```kotlin
val getUsers = KmpSdk.get<GetUsersUseCase>()
val userRepo = KmpSdk.get<UserRepository>()
```

---

### Step 4 — Create a feature module

Group registrations per domain (User, Product, Order…) in a `KmpSdkModule`.

- **[Path A/B](#choose-your-integration-path):** register use cases that call `KmpSdk.networkClient` (see [minimal module example](#path-a--online-only-no-your-sql)).
- **[Path C](#path-c--full-offline-first-your-sql):** register database, repositories, and use cases (example below).

**Example (`UserFeatureModule.kt`):**

```kotlin
object UserFeatureModule : KmpSdkModule {
    override fun register(registry: KmpSdkRegistry) {
        registry.register<AppDatabase> {
            AppDatabase(createAppDatabaseDriver())
        }
        registry.register<UserRepository> { ctx ->
            UserRepositoryImpl(
                localDataSource = UserLocalDataSource(registry.resolve()),
                remoteDataSource = UserRemoteDataSource(ctx.networkClient),
                ctx = ctx,
            )
        }
        registry.register<GetUsersUseCase> {
            GetUsersUseCase(registry.resolve())
        }
        registry.registerSyncTarget("users", registry.resolve<UserRepository>())
    }
}
```

Then install it in init:

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    install(UserFeatureModule)
}
```

---

### Step 5 — Define your SQLDelight schema (host DB)

> **Path C only.** Skip Steps 5–10 if this feature uses [Path A or B](#choose-your-integration-path).

Your app tables live in **your** `AppDatabase.sq` — not in the SDK database.

**Example (`AppDatabase.sq`):**

```sql
CREATE TABLE user_entity (
    id TEXT NOT NULL PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    updated_at INTEGER NOT NULL,
    synced_at INTEGER,
    is_dirty INTEGER NOT NULL DEFAULT 0
);

selectAllUsers:
SELECT * FROM user_entity;

upsertUser:
INSERT OR REPLACE INTO user_entity(id, name, email, updated_at, synced_at, is_dirty)
VALUES ?;
```

**Example — Android driver (`AppDatabaseDriverFactory.android.kt`):**

```kotlin
actual fun createAppDatabaseDriver(): SqlDriver {
    val context = KmpSdkAndroid.requireContext()
    return AndroidSqliteDriver(AppDatabase.Schema, context, "host_app.db")
}
```

---

### Step 6 — Create a local data source

Use `SqlDelightListLocalDataSource` to avoid boilerplate.

**Example (`UserLocalDataSource.kt`):**

```kotlin
class UserLocalDataSource(
    private val database: AppDatabase,
) : LocalListDataSource<User> {
    private val store = SqlDelightListLocalDataSource(
        observeRows = {
            database.appDatabaseQueries.selectAllUsers()
                .asFlow()
                .mapToList(Dispatchers.Default)
        },
        toDomain = { it.toDomain() },
        countRows = { database.appDatabaseQueries.countUsers().executeAsOne() },
        replaceRows = { entities ->
            database.transaction {
                database.appDatabaseQueries.deleteAllUsers()
                entities.forEach { database.appDatabaseQueries.upsertUser(/* … */) }
            }
        },
    )

    override fun observeAll() = store.observeAll()
    override suspend fun count() = store.count()
    suspend fun replaceAll(entities: List<User_entity>) = store.replaceAll(entities)
}
```

---

### Step 7 — Create a remote data source

Use `KmpNetworkClient` from the SDK — no direct Ktor dependency needed in the host module.

**Example (`UserRemoteDataSource.kt`):**

```kotlin
class UserRemoteDataSource(
    private val networkClient: KmpNetworkClient,
) : RemoteListDataSource<UserDto> {
    override suspend fun fetchAll(): KmpSdkResult<List<UserDto>> =
        networkClient.get("/users")
}
```

---

### Step 8 — Create a sync repository

Extend `BaseSyncRepository` for offline-first list sync.

**Example (`UserRepositoryImpl.kt`):**

```kotlin
class UserRepositoryImpl(
    private val localDataSource: UserLocalDataSource,
    private val remoteDataSource: UserRemoteDataSource,
    ctx: KmpSdkContext,
) : BaseSyncRepository<User>(
    tag = "UserRepository",
    observeLocal = { localDataSource.observeAll() },
    countLocal = { localDataSource.count() },
    syncRemote = {
        when (val result = remoteDataSource.fetchAll()) {
            is KmpSdkResult.Success -> {
                localDataSource.replaceAll(result.data.map { it.toEntity() })
                KmpSdkResult.Success(Unit)
            }
            is KmpSdkResult.Failure -> result
        }
    },
    connectivityMonitor = ctx.connectivityMonitor,
    syncPolicy = ctx.config.syncPolicy,
    logger = ctx.logger,
), UserRepository
```

**Sync policy options:**

| Policy | Behaviour |
|--------|-----------|
| `STALE_WHILE_REVALIDATE` | Show SQL cache; refresh when online (default) |
| `CACHE_FIRST` | Prefer local; refresh when online |
| `NETWORK_FIRST` | Require network; fail when offline |

---

### Step 9 — Create a use case

Thin wrapper over the repository.

**Example (`GetUsersUseCase.kt`):**

```kotlin
class GetUsersUseCase(
    private val repository: UserRepository,
) {
    fun observe() = repository.observeUsers()
    suspend fun refresh() = repository.refreshUsers()
}
```

---

### Step 10 — Build a ViewModel (headless MVI)

Use `bindSyncList` to wire observe + refresh + error handling in one call.

**Example (`UserListViewModel.kt`):**

```kotlin
class UserListViewModel(
    scope: CoroutineScope,
    getUsersUseCase: GetUsersUseCase,
    userRepository: UserRepository,
) : MviViewModel<UserListState, UserListIntent, UserListEffect>(
    initialState = UserListState(),
    reducer = UserListReducer(),
    scope = scope,
) {
    private val usersController = bindSyncList(
        scope = scope,
        stateUpdater = { state, users -> state.copy(users = users) },
        observe = getUsersUseCase::observe,
        refresh = getUsersUseCase::refresh,
        countLocal = userRepository::countLocal,
        messageNotifier = KmpSdk.messageNotifier,
        config = KmpSdk.config,
        connectivityMonitor = KmpSdk.connectivityMonitor,
    )

    override fun dispatch(intent: UserListIntent) {
        super.dispatch(intent)
        if (intent == UserListIntent.Refresh) {
            usersController.refreshNow(showLoading = true)
        }
    }
}
```

Factory:

```kotlin
fun createUserListViewModel(scope: CoroutineScope) = UserListViewModel(
    scope = scope,
    getUsersUseCase = KmpSdk.get(),
    userRepository = KmpSdk.get(),
)
```

---

### Step 11 — Render state in your UI (client-owned)

Map `DataState` to your platform UI. The SDK does **not** ship widgets.

**Example — Android Compose:**

```kotlin
when (val users = state.users) {
    is DataState.Loading -> CircularProgressIndicator()
    is DataState.Success -> Text(users.data.joinToString { it.name })
    is DataState.Failure -> Text(users.toErrorMessage())
    is DataState.NoNetwork -> Text("Offline")
    is DataState.Idle -> Unit
}
```

**Example — iOS SwiftUI (bridge Kotlin state):**

```swift
KmpSdk.shared.messageEventBus.events
    .asObservableEvents()
    .observe(scope: KmpSdk.shared.scope) { event in
        print("[SDK] \(event.message)")
    }

viewModel.state.asObservable().observe(scope: KmpSdk.shared.scope) { state in
    // map UserListState → SwiftUI
}
```

---

### Step 12 — Show messages (event bus)

SDK emits events; **you** show toast/snackbar/alert.

**Example — collect in Android:**

```kotlin
lifecycleScope.launch {
    KmpSdk.messageEventBus.events.collect { event ->
        Snackbar.make(rootView, event.message, Snackbar.LENGTH_SHORT).show()
    }
}
```

Wire a `Lifecycle`-aware collector in your Activity/Fragment, or a dedicated presenter class in your app module.

---

### Step 13 — Add authentication

Enable auth in init and provide a token refresh handler.

**Example:**

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    auth {
        enabled = true
        useSecureTokenStore = true
    }
    tokenRefreshHandler = TokenRefreshHandler { refreshToken ->
        // call your refresh API
        KmpSdkResult.Success(TokenPair(newAccessToken, refreshToken))
    }
    install(UserFeatureModule)
}

// After login
KmpSdk.sessionManager.login("access-token", "refresh-token")

// Listen for session events
KmpSdk.sessionManager.events.collect { event ->
    when (event) {
        is SessionEvent.SessionExpired -> navigateToLogin()
        is SessionEvent.LoggedOut -> navigateToLogin()
        else -> Unit
    }
}
```

401/403 responses automatically trigger token refresh and **one retry** when a handler is configured.

---

### Step 14 — Handle API errors

Non-2xx responses map to `KmpSdkError` with HTTP metadata.

**Example:**

```kotlin
when (val result = userRepository.refreshUsers()) {
    is KmpSdkResult.Success -> Unit
    is KmpSdkResult.Failure -> {
        val status = result.httpStatusCode          // e.g. 422
        val rawJson = result.responseBody           // raw body
        val apiError = result.error.apiErrorOrNull  // typed parse
        val emailErr = result.error.fieldErrors["email"]
    }
}
```

---

### Step 15 — Use HTTP cache (GET)

Enabled by default (`enableHttpCache = true`). Offline GET falls back to cache.

**Example:**

```kotlin
// Cached automatically
networkClient.get<List<UserDto>>("/users")

// Skip cache for this call
networkClient.get<UserDto>("/users/1", useCache = false)
```

---

### Step 16 — Queue offline mutations

POST/PUT/PATCH/DELETE are queued when offline if `queueMutationsWhenOffline = true`.

**Example:**

```kotlin
networkClient.post(
    path = "/users",
    offlineBody = """{"name":"Jane"}""",
    offlineHeaders = mapOf("Authorization" to "Bearer $token"),
) {
    setJsonBody(payload, networkClient.json)
}
```

Or use the executor directly:

```kotlin
KmpSdk.offlineExecutor.executeOrQueue(
    payload = OfflineRequestPayload(
        method = "POST",
        url = "/users",
        body = """{"name":"Jane"}""",
    ),
) {
    networkClient.post("/users") { setBody(payload) }
}
```

Queue replays automatically when connectivity returns.

---

### Step 17 — Sync on reconnect

When `autoSyncOnReconnect = true`, network restore runs full sync (offline HTTP queue + registered sync targets + pending domain actions).

**Example — manual sync:**

```kotlin
KmpSdk.syncCoordinator.syncAll()

// Or via debugger helper
KmpSdk.debugger.triggerFullSync()
```

Register sync targets in your feature module:

```kotlin
registry.registerSyncTarget("users", userRepository)
```

---

### Step 18 — Paginated lists

Use `BasePaginatedRepository`, `SqlDelightPaginatedLocalDataSource`, and `PaginatedListController` for page-based APIs.

**Example — load pages:**

```kotlin
val getProducts = KmpSdk.get<GetProductsUseCase>()

getProducts.loadInitial(pageSize = 10)
getProducts.loadMore()
getProducts.observe().collect { products ->
    // list grows in SQL as pages append
}
```

**Example — paginated ViewModel binder:**

```kotlin
val productsController = PaginatedListController(
    scope = scope,
    repository = productRepository,
    onStateChange = { paginatedState ->
        setState { it.copy(products = paginatedState) }
    },
)
productsController.start()
productsController.loadMore()
```

Use `SqlDelightPaginatedLocalDataSource` for local storage with `replaceAll` + `appendAll`.

---

### Step 19 — Debug & diagnostics (headless)

Inspect SDK state from your own debug menu.

**Example:**

```kotlin
val snapshot = KmpSdk.debugger.snapshot()
// snapshot.isOnline, snapshot.pendingOfflineRequests, snapshot.sessionState, …

KmpSdk.debugger.inspectOfflineQueue()
KmpSdk.debugger.clearOfflineQueue()
KmpSdk.debugger.purgeCache()
```

Build your own debug screen in the host app using `KmpSdk.debugger` APIs.

---

### Step 20 — Full configuration reference

**Example — all common init options (core + v1.4):**

```kotlin
KmpSdk.init(this) {
    // Core
    baseUrl = "https://api.example.com"
    logLevel = LogLevel.INFO
    enableRequestLogging = true
    enableResponseBodyLogging = false
    enableCurlLogging = false
    defaultCacheTtlMillis = 300_000
    offlineReplayStrategy = OfflineReplayStrategy.PRIORITY
    maxOfflineRetries = 3
    syncPolicy = SyncPolicy.STALE_WHILE_REVALIDATE
    enableHttpCache = true
    autoSyncOnReconnect = true
    autoRefreshOnObserve = false
    queueMutationsWhenOffline = true
    auth { enabled = true; useSecureTokenStore = true }
    tokenRefreshHandler = myRefreshHandler

    // v1.4
    profile = SdkProfile.ENTERPRISE
    environmentName = "staging"
    environments {
        dev { baseUrl = "https://dev.api.com"; enableCurlLogging = true }
        staging { baseUrl = "https://staging.api.com" }
        prod { baseUrl = "https://api.com"; enableRequestLogging = false }
    }
    remoteConfig { fetchRemoteConfigMap() }
    enableRequestDeduplication = true
    enableRateLimitBackoff = true
    maxRateLimitRetries = 3
    certificatePins = listOf("api.example.com/abcdef1234567890=")
    backgroundSyncIntervalMillis = 900_000
    validateOnStartup = true

    install(UserFeatureModule)
}

// After init — telemetry (not part of init DSL)
KmpSdk.telemetry.addListener { event -> /* analytics */ }
```

---

## v1.4 — Rich SDK additions

Each item includes **why** it exists and **one example**. Cross-reference **Step 20** for a single init block with all flags.

| Feature | Section |
|---------|---------|
| SDK profiles | [SDK profiles](#sdk-profiles) |
| Multi-environment init | [Multi-environment init](#multi-environment-init) |
| Startup validation | [Startup validation](#startup-validation) |
| Telemetry hooks | [Telemetry hooks](#telemetry-hooks) |
| Multi-tenant switching | [Multi-tenant switching](#multi-tenant-switching) |
| Remote config | [Remote config block](#remote-config-block) |
| REST list installer | [REST list installer](#rest-list-installer-no-custom-repository-class) |
| REST mutation use case | [REST mutation use case](#rest-mutation-use-case) |
| Dirty record sync | [Dirty record sync](#dirty-record-sync) |
| Offline domain actions | [Offline domain actions](#offline-domain-actions) |
| Request deduplication | [Request deduplication](#request-deduplication) |
| Rate-limit backoff | [Rate-limit backoff](#rate-limit-backoff) |
| Certificate pinning | [Certificate pinning](#certificate-pinning-android) |
| File upload helper | [File upload helper](#file-upload-helper) |
| Background sync scheduler | [Background sync scheduler](#background-sync-scheduler) |
| Feature generator CLI | [Feature generator CLI](#feature-generator-cli) |
| Migration helper | [Migration helper](#migration-helper) |

### SDK profiles

**Why:** Sensible defaults per environment without tuning 20 flags.

```kotlin
KmpSdk.init(this) {
    profile = SdkProfile.ENTERPRISE
    baseUrl = "https://api.example.com"
    install(UserFeatureModule)
}
```

Profiles: `DEVELOPMENT`, `STAGING`, `PRODUCTION`, `ENTERPRISE`.

---

### Multi-environment init

**Why:** Dev/stage/prod configs in one place.

```kotlin
KmpSdk.init(this) {
    environmentName = "staging"
    environments {
        dev { baseUrl = "https://dev.api.com"; enableCurlLogging = true }
        staging { baseUrl = "https://staging.api.com" }
        prod { baseUrl = "https://api.com"; enableRequestLogging = false }
    }
    install(UserFeatureModule)
}
```

---

### Startup validation

**Why:** Fail fast in debug when config/modules are wrong.

```kotlin
val result = KmpSdk.validate()
result.issues.forEach { println("${it.level}: ${it.message}") }
```

Enabled by default via `validateOnStartup = true`.

---

### Telemetry hooks

**Why:** Pipe API/sync/session events to Firebase, Datadog, etc. without wrapping every repo.

```kotlin
KmpSdk.telemetry.addListener { event ->
    when (event) {
        is TelemetryEvent.ApiCallCompleted -> analytics.log("api", event.path)
        is TelemetryEvent.SyncCompleted -> analytics.log("sync", event.refreshedRepos.toString())
        else -> Unit
    }
}
```

---

### Multi-tenant switching

**Why:** B2B apps swap API base URL at runtime.

```kotlin
KmpSdk.tenantManager.switchTenant(
    tenantId = "acme",
    baseUrl = "https://acme.api.example.com",
    headers = mapOf("X-Tenant" to "acme"),
)
```

---

### Remote config block

**Why:** Tune cache TTL / flags from server without app update.

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    remoteConfig { fetchRemoteConfigMap() }
    install(UserFeatureModule)
}

// Read values after init (RemoteConfigStore applies supported keys to config)
val ttl = KmpSdk.remoteConfig.getLong("default_cache_ttl_millis")
val flag = KmpSdk.remoteConfig.getBoolean("feature_x_enabled", default = false)
KmpSdk.remoteConfig.values.collect { map -> /* react to updates */ }
```

---

### REST list installer (no custom repository class)

**Why:** Standard list APIs without writing a custom `Repository` implementation class.

**Path:** [Path C](#path-c--full-offline-first-your-sql) — you still provide **local** observe/count/replace (SQL or in-memory store you own). For online-only lists, use [Path A](#path-a--online-only-no-your-sql) with `networkClient.get` instead.

```kotlin
KmpSdk.init(this) {
    baseUrl = "https://api.example.com"
    install(UserFeatureModule) // register installRestListFeature inside module
}

// Inside KmpSdkModule.register (after UserLocalDataSource exists):
installRestListFeature(
    RestListFeatureConfig<User, UserDto>(
        name = "users",
        path = "/users",
        observeLocal = { userLocal.observeAll() },
        countLocal = { userLocal.count() },
        replaceLocal = { dtos -> userLocal.replaceAll(dtos.map { it.toEntity() }) },
    ),
)
```

---

### REST mutation use case

**Why:** Standard POST/PUT/PATCH/DELETE with optional offline queue ([Path B/C](#choose-your-integration-path)).

```kotlin
val createUser = RestMutationUseCase.create<CreateUserBody>(
    networkClient = KmpSdk.networkClient,
    path = "/users",
    method = HttpMethod.Post,
    onSuccess = { userRepository.refresh() },
)
createUser.execute(CreateUserBody(name = "Jane"))
```

---

### Dirty record sync

**Why:** Push local SQL edits marked `is_dirty = 1` without custom outbox code.

```kotlin
KmpSdk.dirtySyncCoordinator.syncDirty(object : DirtySyncTarget<UserEntity> {
    override suspend fun loadDirty() = local.getDirtyUsers()
    override suspend fun push(record: UserEntity) = remote.update(record)
    override suspend fun markClean(id: String) = local.markClean(id)
})
```

---

### Offline domain actions

**Why:** Queue business actions separately from raw HTTP replay.

```kotlin
// Register handler once (e.g. in feature module or after init)
KmpSdk.offlineActions.registerHandler("FAVORITE_POST") { payload ->
    networkClient.post("/posts/${payload.entityId}/favorite") {
        setJsonBodyWithOfflineCapture(payload.payloadJson)
    }
}

// Enqueue when offline or for deferred work
KmpSdk.offlineActions.enqueue(
    actionType = "FAVORITE_POST",
    entityId = postId,
    payloadJson = """{"postId":"$postId"}""",
)

// Replay runs automatically during background sync / full sync; manual:
KmpSdk.offlineActions.replayPending()
```

---

### Request deduplication

**Why:** Prevent duplicate in-flight GETs from list + detail screens.

```kotlin
KmpSdk.init(this) {
    enableRequestDeduplication = true
    baseUrl = "https://api.example.com"
}
// GET /users called twice concurrently → one network call
```

---

### Rate-limit backoff

**Why:** Handle 429/503 automatically.

```kotlin
KmpSdk.init(this) {
    enableRateLimitBackoff = true
    maxRateLimitRetries = 3
    baseUrl = "https://api.example.com"
}
```

---

### Certificate pinning (Android)

**Why:** Enterprise security without custom OkHttp setup.

```kotlin
KmpSdk.init(this) {
    certificatePins = listOf("api.example.com/abcdef1234567890=")
    baseUrl = "https://api.example.com"
}
```

---

### File upload helper

**Why:** Multipart uploads without Ktor boilerplate in host module.

```kotlin
KmpSdk.networkClient.uploadMultipart<UploadResponse>(
    MultipartUploadRequest(
        path = "/upload",
        parts = listOf(FileUploadPart("file", "photo.jpg", imageBytes, "image/jpeg")),
        fields = mapOf("userId" to "123"),
    ),
)
```

---

### Background sync scheduler

**Why:** Refresh data periodically, not only on reconnect. Each tick runs `syncAll()` and replays pending offline domain actions.

```kotlin
KmpSdk.init(this) {
    backgroundSyncIntervalMillis = 15 * 60 * 1000L
    autoSyncOnReconnect = true
    baseUrl = "https://api.example.com"
}
```

---

### Feature generator CLI

**Why:** New entity scaffold in seconds.

```bash
python tools/feature-generator/generate.py \
  --config tools/feature-generator/examples/order.yaml \
  --output shared/src/commonMain/kotlin \
  --package com.yourapp.feature
```

See `tools/feature-generator/README.md`.

---

### Migration helper

**Why:** Safe SQLDelight schema upgrades in host apps.

See `tools/migration-helper/README.md`.

---

## Quick reference — `KmpSdk` globals (updated)

| API | Purpose |
|-----|---------|
| `KmpSdk.init { }` | Initialize SDK + install modules |
| `KmpSdk.validate()` | Startup health check |
| `KmpSdk.get<T>()` | Resolve registered dependency |
| `KmpSdk.networkClient` | Ktor HTTP client + cache + dedup |
| `KmpSdk.sessionManager` | Login / logout / token refresh |
| `KmpSdk.syncCoordinator` | Full sync orchestration |
| `KmpSdk.dirtySyncCoordinator` | Push dirty SQL records |
| `KmpSdk.offlineExecutor` | Queue/replay offline HTTP |
| `KmpSdk.offlineActions` | Domain offline action queue |
| `KmpSdk.tenantManager` | Runtime tenant/base URL switch |
| `KmpSdk.remoteConfig` | Server-driven config values |
| `KmpSdk.telemetry` | Analytics/diagnostics hooks |
| `KmpSdk.messageEventBus` | Toast/snackbar event stream |
| `KmpSdk.debugger` | Diagnostics snapshot & actions |
| `KmpSdk.connectivityMonitor` | Online/offline status |
| `KmpSdk.scope` | Shared coroutine scope |

---

## Project structure

```
KmpSDK/
├── kmp-sdk/                    # Published SDK (headless)
│   └── com/kmpsdk/
│       ├── KmpSdk.kt           # Global entry point
│       ├── KmpSdkInitBuilder.kt
│       ├── core/               # Config, auth, DI, messaging, connectivity
│       ├── data/               # Network, cache, offline, SQLDelight, repos
│       ├── domain/             # Errors, contracts, pagination, sync policy
│       ├── presentation/       # MVI, DataState, binders (no UI widgets)
│       ├── debug/              # Headless debug API
│       └── platform/           # Swift Flow bridges
├── tools/
│   ├── feature-generator/      # YAML → feature scaffold CLI
│   └── migration-helper/       # SQLDelight migration guide
└── settings.gradle.kts
```

### SDK layers

| Layer | Package | Responsibility |
|-------|---------|----------------|
| **Core** | `core.*` | Init, config, auth, registry, logging, message bus |
| **Domain** | `domain.*` | Errors, repository contracts, sync policy |
| **Data** | `data.*` | Network, cache, offline queue, base repositories |
| **Presentation** | `presentation.*` | MVI base, `DataState`, `bindSyncList` (no widgets) |

---

## Adding a new feature (checklist)

**First:** pick a path in [Choose your integration path](#choose-your-integration-path).

### Path A — Online only

1. Create DTO (+ mapper if needed)
2. Create use case using `KmpSdk.networkClient`
3. Register in `XxxFeatureModule`; `install` in `KmpSdk.init`
4. Build ViewModel + platform UI

### Path B — Network-first + SDK cache

Same as Path A; set `enableHttpCache = true` and `SyncPolicy.NETWORK_FIRST` in init.

### Path C — Full offline-first

1. Add table to **your** `AppDatabase.sq`
2. Create DTO + mapper + domain model
3. Create local source (`SqlDelightListLocalDataSource` or paginated variant)
4. Create remote source (`networkClient.get/post/…`) — or use `installRestListFeature` / `RestMutationUseCase` for standard REST
5. Create repository (`BaseSyncRepository` or `BasePaginatedRepository`) unless using `installRestListFeature`
6. Create use case
7. Create `XxxFeatureModule` and `registerSyncTarget`
8. `install(XxxFeatureModule)` in `KmpSdk.init`
9. Build ViewModel with `bindSyncList` or `PaginatedListController`
10. Build your platform UI

**Path C shortcuts (v1.4):** run `tools/feature-generator/generate.py` to scaffold steps 3–7; register offline action handlers with `KmpSdk.offlineActions.registerHandler`.

---

## Build

Requires **JDK 17+** (Android Studio’s bundled JBR works).

**Windows:** use `.\gradlew.bat` instead of `./gradlew`.  
`gradlew.bat` auto-detects Android Studio’s JDK. If you still see a Java 8 error, copy `gradle/jdk.home.example` → `gradle/jdk.home` and set your JDK path, or run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

### Verify SDK build (step by step)

Run these from the repo root to confirm the SDK compiles, packages, and tests pass.

**Step 1 — Android compile**

```bash
# Android
./gradlew :kmp-sdk:compileDebugKotlinAndroid
```

```powershell
# Windows
.\gradlew.bat :kmp-sdk:compileDebugKotlinAndroid
```

**Step 2 — iOS compile (simulator)**

Requires a Mac with Xcode for this target.

```bash
# iOS (simulator)
./gradlew :kmp-sdk:compileKotlinIosSimulatorArm64
```

```powershell
# Windows (skipped automatically if iOS targets are unavailable)
.\gradlew.bat :kmp-sdk:compileKotlinIosSimulatorArm64
```

**Step 3 — Full release AAR**

```bash
# Full release AAR
./gradlew :kmp-sdk:assembleRelease
```

```powershell
# Windows
.\gradlew.bat :kmp-sdk:assembleRelease
```

Output: `kmp-sdk/build/outputs/aar/`

**Step 4 — Unit tests**

```bash
# Unit tests
./gradlew :kmp-sdk:cleanTest :kmp-sdk:allTests
```

```powershell
# Windows
.\gradlew.bat :kmp-sdk:cleanTest :kmp-sdk:allTests
```

**Step 5 — Publish to Maven Central (maintainer)**

```powershell
.\gradlew.bat :kmp-sdk:publishToMavenLocal
.\gradlew.bat :kmp-sdk:publishToMavenCentral
```

See [Publishing to Maven Central](#publishing-to-maven-central) for GPG + Sonatype setup.

### iOS framework (optional, Mac only)

To produce an Xcode framework bundle instead of compile-only:

```bash
./gradlew :kmp-sdk:linkDebugFrameworkIosSimulatorArm64
```

Output: `kmp-sdk/build/xcode-frameworks`

---

## What is NOT in the SDK

- Compose / SwiftUI / Material components
- Shared screens, themes, navigation
- Platform toast/snackbar implementations
- Hardcoded domain entities (User, Product, etc.) — those belong in your host app

Your app stays in control of UX; KmpSDK handles infrastructure.
