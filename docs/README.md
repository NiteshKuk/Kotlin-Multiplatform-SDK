# KmpSDK documentation

Start here after the root [README](../README.md).

## Read by goal

| I want to… | Open |
|------------|------|
| Install & init in 5 minutes | [getting-started.md](getting-started.md) |
| Pick Path A / B / C | [integration-paths.md](integration-paths.md) |
| Offline list + CRUD quickly | [feature-kit.md](feature-kit.md) |
| Auth, Firebase remote config, env, tenant | [auth-and-config.md](auth-and-config.md) |
| Cache, retry, upload, realtime, **SSL pinning** | [networking.md](networking.md) |
| Offline queue, sync status, dirty sync | [offline-sync.md](offline-sync.md) |
| Deep links, push, background work, DI | [platform-integration.md](platform-integration.md) |
| Copy-paste scenarios | [recipes.md](recipes.md) |
| Fix common errors | [troubleshooting.md](troubleshooting.md) |
| Use / create a sample (consumer) app | [sample-app.md](sample-app.md) |
| API / globals cheat sheet | [api-reference.md](api-reference.md) |
| What changed recently | [changelog.md](changelog.md) |

## Decision (30 seconds)

```
Need YOUR SQL offline for this feature?
  YES → Path C  (see integration-paths.md + feature-kit.md)
  NO  → Last HTTP response offline OK?
         YES → Path B
         NO  → Path A
```
