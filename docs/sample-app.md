# Sample / consumer app (no in-repo sample yet)

This repo publishes **`kmp-sdk` only**. There is **no** full Android/iOS sample module here yet.

## What to do instead (recommended)

Use your **existing consumer / test app** (e.g. `TestingKmpSdk` / `com.testingkmpsdk`) as the living sample:

1. Depend on a published or `mavenLocal()` SDK version  
2. Implement Path A (or Feature Kit Path C) against a public API (JSONPlaceholder is fine)  
3. Keep that app as the place reviewers/devs run demos  

Treat that app as the **reference consumer**, even if it lives in another folder/repo.

## Checklist — “official sample behaviour”

Your test app should demonstrate at least:

| # | Scenario | Path |
|---|----------|------|
| 1 | Init + one GET screen | A |
| 2 | Optional HTTP cache offline GET | B |
| 3 | One list: refresh + observe (+ create if you use Feature Kit) | C |
| 4 | Host injection example (`register<AppAnalytics>`) | any |
| 5 | (Optional) sync status banner | C |

Link that project from your team wiki or README “Consumer sample” line once stable.

## Optional later: add `samples/` inside this repo

When you are ready:

```text
samples/
  android-smoke/     # or kmp-app/
    README.md        # how to run
```

Minimum viable sample:

- One `Application` + `KmpSdk.init`  
- One screen calling `networkClient.get` **or** generated Feature Kit  
- README: `publishToMavenLocal` → run sample  

Until then, **do not block** docs on an in-repo sample — use the external test app + [recipes.md](recipes.md).

## How SDK maintainers demo today

```text
1. .\gradlew.bat :kmp-sdk:publishToMavenLocal
2. Open consumer test app → dependency mavenLocal() + matching sdkVersion
3. Run Path A or Feature Kit flow
```
