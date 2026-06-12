# CLAUDE.md

Guidance for working in this repository (paywall experiment for a media company).

## Version policy — check on every dependency change

- **Latest *stable* versions only.** No alpha, beta, RC, dev, or SNAPSHOT versions
  in `gradle/libs.versions.toml` or the Gradle wrapper — ever.
- When asked to upgrade (or when adding a dependency), look up the latest version on
  Maven Central / services.gradle.org first, take the newest **stable**, and skip
  prereleases even if they are newer.
- The root Gradle task `verifyStableVersions` enforces this and runs with `check`;
  it must stay green.
- An upgrade is only done when the full verification passes:
  `./gradlew verifyStableVersions :shared:jvmTest :backend:test :shared:compileKotlinWasmJs :composeApp:wasmJsBrowserDistribution :composeApp:compileKotlinJvm`

## Project layout

- `shared/` — pure-Kotlin KMP domain core (jvm + wasmJs): access decision engine,
  DCB event model, metering/entitlements/grants/experiments. No infra dependencies.
- `backend/` — Ktor origin: `POST /api/v1/decide` (API-05 budget: < 50 ms p95,
  guarded by `DecideApiSpeedTest`).
- `composeApp/` — Compose Multiplatform admin console (wasmJs + jvm), wall designer.
- `requirements/` — the specification (Docs 1–7); reference requirement IDs
  (e.g. `PW-20`, `MT-03`, `NFR-12`) in commits and code comments.
- `architecture/` — reference architecture; follow its module and naming conventions.
- `design/` — design package; UI reads tokens from `CrmTheme` only (no hard-coded hex/dp).

## Domain boundaries to respect

- Subscription administration is an **external system**: the paywall enforces access
  and drives conversion; it only ingests entitlement changes (`EntitlementGranted/Revoked`).
- Access decisions are server-side (AC-01): premium content never appears in any
  response to an unentitled request — also not in test fixtures for API responses.

## Testing

- The NFR-12 decision matrix test (`DecisionMatrixTest`) must stay exhaustive
  (4 paywall types × 4 visitor states × 2 tiers = 32 cases).
- Speed tests assert the API-05 budget; keep their assertions, adjust fixtures only
  with good reason.
