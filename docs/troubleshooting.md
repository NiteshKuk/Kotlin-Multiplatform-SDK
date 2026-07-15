# Troubleshooting

## `gradlew.bat` not recognized (PowerShell)

Use:

```powershell
.\gradlew.bat …
```

## `countLocal` stack overflow

Fixed in SDK: constructor lambda must not share the name `countLocal` with `countLocal()`. Upgrade SDK; keep `countLocal = { local.count() }` in subclasses.

## `Could not find … kmp-sdk-iossimulatorarm64`

iOS artifacts were not published (Windows/Ubuntu-only publish). Publish from **Mac** / `macos-latest` CI.

## Logging plugin / `lateinit` logger

Use factory plugins (`createKmpSdkLoggingPlugin`). Clean caches; one SDK version on the classpath.

## Maven publish: `maven-central-build-service` / no property

Set `mavenCentralUsername` / `mavenCentralPassword` (+ signing) in **user** `~/.gradle/gradle.properties`.

## Circuit open / retries

Check `resilience { }` and path prefixes in `protectPath`.

## QueryKit visibility / compile errors

Ensure you are on a build that marks `QueryPredicate` as `internal` (not private).

## Host type not found in feature module

You must `register<MyType>` in `KmpSdk.init` — Koin/Hilt alone is not enough.
