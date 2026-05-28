# Contributing

## Workflow

1. Fork or branch from `master` (use a feature branch, e.g. `feature/my-change`).
2. Open a **Pull Request** into `master`.
3. Wait for review and approval from the repository owner.
4. The owner merges the PR.

Do **not** push directly to `master` (branch protection blocks this for collaborators).

## Publishing

Only the repository owner publishes to Maven Central.

- Collaborators **cannot** run a successful `publishAndReleaseToMavenCentral` in CI without owner approval (protected GitHub Environment).
- Publishing credentials live only in the owner’s `~/.gradle/gradle.properties` and GitHub Actions secrets — never commit them.

Release process (owner only):

1. Merge approved PR(s) on `master`.
2. Bump `sdkVersion` in `gradle.properties` if needed.
3. Test: `.\gradlew.bat :kmp-sdk:publishToMavenLocal`
4. Tag and push, or create a GitHub Release.
5. Approve the **maven-central-release** environment deployment in GitHub Actions (if triggered from CI), or run locally:
   `.\gradlew.bat :kmp-sdk:publishAndReleaseToMavenCentral`
