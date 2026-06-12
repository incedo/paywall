# CLAUDE.md

Guidance for working in this repository (paywall experiment for a media company).
Adapted from the CRM project's reference instructions; core working agreements
carried over, names and facts updated for the paywall.

## Architecture

DDD Hexagonal + CQRS + Dynamic Consistency Boundaries (DCB). See `architecture/`
for the full reference; `requirements/` (Docs 1–7) is the specification — reference
requirement IDs (e.g. `PW-20`, `MT-03`, `NFR-12`) in commits and code comments.

- **shared/** — Domain core (events, commands, decision models, ports) + application
  handlers. Pure Kotlin (KMP: `jvm` + `wasmJs`), zero infra deps.
- **backend/** — Ktor REST controllers (inbound adapters) + `PostgresEventStore`
  (outbound adapter). `POST /api/v1/decide` carries the API-05 budget (< 50 ms p95,
  guarded by the speed tests).
- **composeApp/** — Compose Multiplatform admin console (wall designer), `wasmJs` + `jvm`.
  The target end-state module split (designsystem/, frontend/common|web|desktop|android|ios)
  is documented in `architecture/module-structure.md`; grow toward it, don't pre-build it.

## Multiplatform Rule (non-negotiable)

The frontend is **Kotlin Multiplatform**: web (Wasm) and desktop (JVM) today,
Android (JVM) and iOS (Kotlin/Native) later. Every frontend design decision must
work on all targets — "web only" or "JVM only" solutions are rejected. Concretely:

- **Prefer server-side solutions** when a client capability isn't portable
  (OAuth/OIDC PKCE, JWT validation, crypto). A BFF flow that works for every client
  beats four per-platform native integrations.
- **When a capability truly must live on the client**, use `expect`/`actual` with
  actuals in every platform source set.
- **When asking the user to choose between approaches**, always flag the
  multiplatform impact.
- **Use design-system components only.** Screens render identically on every target
  because `CrmTheme` tokens are commonMain.

## Key Patterns

- **Events are source of truth** — never query the DB directly; query the EventStore by tags.
- **Decision models** replace aggregates — built on the fly from queried events.
- **AppendCondition** enforces optimistic concurrency (DCB); retry 3× on conflict (Q-7).
- New domain events must be registered in `paywallSerializersModule` (the round-trip
  test enforces this).
- **No magic numbers in UI** — all components use `CrmTheme.spacing.*`, `.typography.*`,
  `.shapes.*`, `.colors.*`. Never hardcode `dp`, `sp`, `Color(0x...)` or shapes;
  define new tokens in `CrmTheme.kt` if needed.

## Domain boundaries to respect

- Subscription administration is an **external system**: the paywall enforces access
  and drives conversion; it only ingests entitlement changes (`EntitlementGranted/Revoked`).
- Access decisions are server-side (AC-01): premium content never appears in any
  response to an unentitled request — also not in test fixtures for API responses.

## Version policy — check on every dependency change

- **Latest *stable* versions only.** No alpha, beta, RC, dev, or SNAPSHOT versions in
  `gradle/libs.versions.toml` or the Gradle wrapper — ever. The version catalog is the
  single source of truth; this file deliberately pins no version matrix.
- When upgrading or adding a dependency, look up the latest version on Maven Central /
  services.gradle.org first and take the newest **stable**, skipping prereleases even
  if newer.
- The root task `verifyStableVersions` enforces this and runs with `check`; it must stay green.

## Test Commands

```bash
./gradlew :shared:jvmTest :backend:test      # must pass before every commit
# Full suite incl. Postgres integration + speed tests (start a local PG 16 first):
PAYWALL_TEST_PG_URL=jdbc:postgresql://127.0.0.1:5433/paywall_test \
PAYWALL_TEST_PG_USER=paywall ./gradlew :backend:test
# Upgrade verification:
./gradlew verifyStableVersions :shared:jvmTest :backend:test :shared:compileKotlinWasmJs \
  :composeApp:wasmJsBrowserDistribution :composeApp:compileKotlinJvm
```

- The NFR-12 decision matrix test (`DecisionMatrixTest`) must stay exhaustive
  (4 paywall types × 4 visitor states × 2 tiers = 32 cases).
- Speed tests assert the API-05 budget; keep their assertions, adjust fixtures only
  with good reason.

## Git Rules

- **NEVER push directly to main.** Always work on a feature branch with a functional name.
- Branch naming: `feature/<short-description>` (e.g., `feature/wall-events`,
  `feature/postgres-projections`) — unless the session prescribes a branch.
- Commit, push to the feature branch, then create a PR **only when asked** — do not
  open PRs unprompted.

## Working Agreements

Default rules of engagement unless the user says otherwise: reviewable diffs beat
big-bang commits.

### Small incremental steps
- Break each bounded context / feature into **~7–10 small commits**. Canonical shape:
  1. Prerequisites / cross-cutting changes
  2. Value objects + events + commands (domain types only)
  3. Decision model + command handler + unit tests
  4. Projection + read models + query handler + projection tests
  5. REST controller + DI wiring + cross-BC glue
  6. Infrastructure adapters
  7. BDD tests + requirement tick-off
- **Compile and run the test suite after each step** — green before every commit.
- **Push after every commit** so the remote branch always reflects shippable state.
- If a test file exceeds ~20 new tests, split it across commits.

### Commit messages
- First line under 80 chars, imperative mood; body explains the **why** and
  non-obvious decisions (scope cuts, deferred work). Cite requirement IDs.
- Append the Claude Code session trailer on every commit.

### Requirements drive implementation
- `requirements/` Docs 1–7 define what to build; implement in document order
  (Doc 2 is the core engine). Cite requirement IDs in code and commits.
- Open questions are resolved by asking the user (`AskUserQuestion` with concrete
  options, recommended one marked) — never invent product decisions.

### Pull requests
- **Open a PR when a completable, mergeable slice is done**: tests green, no
  half-finished work, shippable on its own merits. One PR per slice; don't batch
  unrelated features. (Creation still happens on request — see Git Rules.)
- Don't force-push to an open PR unless asked. New work on a shipped feature is a
  new branch + new PR.
- PR body: summary, what's in each commit, test plan, deliberately-out-of-scope list.
  Include the Claude Code trailer.

### Scope discipline
- **Don't add features that weren't requested.** Bug fixes don't need refactors; no
  speculative extension points. Three similar lines beat a premature abstraction.
- **Validate at user-facing edges** (REST controllers); trust domain-internal invariants.
- Out-of-scope work is deferred explicitly in the commit message / PR body.

### When stuck or ambiguous
- Ask with concrete options (recommended one marked). For spec questions, ask before
  touching code; for implementation choices, ask when the code requires the decision.

## Before Committing

Always run `./gradlew :shared:jvmTest :backend:test` and fix failures before committing.

## Dev Server

```bash
./gradlew :backend:run                              # decide API on :8080 (DATABASE_URL selects Postgres)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # admin console (web) with hot reload
./gradlew :composeApp:run                           # admin console (desktop JVM)
```
