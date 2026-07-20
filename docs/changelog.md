# Changelog (docs)

High-level recent SDK additions. Prefer GitHub Releases for versioned notes.

- **Docs** — AppAnalytics + custom host classes + `KmpSdk.telemetry` bridge (getting-started §4, recipes, platform-integration)
- **Docs** — SSL / certificate pinning guide (format, openssl, Android-only, captive-portal vs pin mismatch); README version notes
- **Init** — Android `KmpSdk.init(context)` delegates to common init (deep links / push / env / background / remote config apply); `register<T>` on init DSL
- **OpenAPI → Feature Kit** — `tools/openapi-import`
- **Circuit breaker + smart retry** — `resilience { }`
- **WebSocket / SSE** — `KmpSdk.realtime`
- **Local query kit** — `KmpSdk.query`
- **Draft / autosave** — `KmpSdk.drafts`
- **DI adapters** — Koin / Hilt / Kodein bridges
- **Environment vault** — `KmpSdk.environments`
- **Background work** — `KmpSdk.backgroundWork`
- **Deep link + push routers** — `KmpSdk.deepLinks` / `KmpSdk.push`
- **List + Mutation Feature Kit** — `installRestResourceFeature` / `RestResourceApi`
- **Sync status API** — `KmpSdk.syncStatus`
- **File upload helper** — `KmpSdk.fileUpload`
- **Firebase Remote Config** host example (app-owned fetch)
- **Fixes** — `countLocal` recursion; RestList reified GET; logging/auth factory plugins
