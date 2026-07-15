# API reference (cheat sheet)

| API | Purpose |
|-----|---------|
| `KmpSdk.init { }` | Initialize + install modules |
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
| `KmpSdk.telemetry` | Analytics hooks |
| `KmpSdk.messageEventBus` | UI message stream |
| `KmpSdk.debugger` | Diagnostics |
| `KmpSdk.connectivityMonitor` | Online/offline |
| `KmpSdk.scope` | Shared coroutine scope |

Init flag kitchen sink examples: see older monolithic README in git history, or Step 20 style block in team notes.
