# API reference (cheat sheet)

| API | Purpose |
|-----|---------|
| `KmpSdk.init { }` | Initialize + install modules (+ optional `register<T>`) |
| `KmpSdk.init(context) { }` | Android: sets platform context then same DSL |
| `KmpSdk.validate()` | Startup health check |
| `KmpSdk.get<T>()` | Resolve registered dependency |
| `KmpSdk.networkClient` | HTTP + cache + resilience |
| `KmpSdk.sessionManager` | Login / logout / refresh |
| `KmpSdk.syncCoordinator` | Full / per-target sync |
| `KmpSdk.syncStatus` | Per-feature sync UI state |
| `KmpSdk.fileUpload` | Multipart upload |
| `KmpSdk.drafts` | Form draft autosave |
| `KmpSdk.query` | Local query kit |
| `KmpSdk.realtime` | WebSocket / SSE |
| `KmpSdk.deepLinks` | Deep link router |
| `KmpSdk.push` | Push data router |
| `KmpSdk.backgroundWork` | Background sync bridge |
| `KmpSdk.environments` | Environment vault (if configured) |
| `KmpSdk.dirtySyncCoordinator` | Dirty SQL push |
| `KmpSdk.offlineExecutor` | Offline HTTP queue |
| `KmpSdk.offlineActions` | Domain offline actions |
| `KmpSdk.tenantManager` | Tenant / base URL switch |
| `KmpSdk.remoteConfig` | Remote config map |
| `KmpSdk.telemetry` | Analytics hooks — `addListener` / `removeListener`; forward to host `AppAnalytics` |
| `KmpSdk.messageEventBus` | UI message stream |
| `KmpSdk.debugger` | Diagnostics (`snapshot()`, queue inspect, full sync) |
| `KmpSdk.connectivityMonitor` | Online/offline |
| `KmpSdk.scope` | Shared coroutine scope |

### Init flags (common)

| Flag | Purpose |
|------|---------|
| `certificatePins` | Android SSL public-key pins (`host/base64`) — [networking.md](networking.md#ssl--certificate-pinning) |
| `enableHttpCache` | Path B HTTP response cache |
| `syncPolicy` | Offline / cache strategy |
| `resilience { }` | Retry + circuit breaker |
| `environments { }` / `environmentName` | Named env packs |
| `deepLinks { }` / `push { }` / `backgroundWork { }` | Platform routers / sync |

### Debugger (quick)

```kotlin
val snap = KmpSdk.debugger.snapshot()
// snap.isOnline, pendingOfflineRequests, baseUrl, syncState, …
KmpSdk.debugger.triggerFullSync()
```
