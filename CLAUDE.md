# CRM Project — Claude Code Instructions

## Architecture
DDD Hexagonal + CQRS + Dynamic Consistency Boundaries (DCB).
- **shared/** — Domain core (events, commands, decision models, ports) + Application (handlers, projections, read models). Pure Kotlin, zero infra deps.
- **backend/** — Ktor REST controllers (inbound adapters) + PostgresEventStore/ReadModelStore (outbound adapters). DI in `config/DependencyInjection.kt`.
- **designsystem/** — Custom Compose components (no Material). Atoms → Molecules → Organisms → Layout.
- **frontend/common/** — Compose screens + MVI state holders + API adapters. Shared across **all** frontend targets.
- **frontend/web/** — WASM entry point (ComposeViewport).
- **frontend/desktop/** — JVM desktop entry point.
- **frontend/android/** — Android app entry point.
- **frontend/ios/** — iOS framework producer (Kotlin/Native).

## Multiplatform Rule (non-negotiable)
The frontend is **Kotlin Multiplatform**: web (Wasm), desktop (JVM), Android (JVM), iOS (Kotlin/Native). Every frontend design decision must work on all four — "web only" or "JVM only" solutions are rejected. Concretely:

- **Prefer server-side solutions** when a client capability isn't portable. E.g. OAuth/OIDC PKCE, JWT validation, SHA-256 — if the primitive isn't already in commonMain/Kotlin stdlib, first check whether the backend can own it and expose a thin HTTP adapter. A BFF (Backend for Frontend) flow that works for every client beats four per-platform native integrations every time.
- **When a capability truly must live on the client**, use `expect`/`actual` with actuals in all four platform source sets: `jvmMain` (covers desktop + Android), `wasmJsMain`, `iosMain`. Add platform targets to `frontend/common/build.gradle.kts` if they're missing.
- **When asking the user to choose between approaches**, always flag the multiplatform impact. A choice between "SPA-PKCE in the browser" and "BFF on the backend" is really a choice between "write four platform implementations" and "write one backend route" — surface that.
- **Use design-system components only.** `frontend/common` screens render identically on every target because `CrmTheme` tokens are commonMain. No platform-specific UI branches.

## Key Patterns
- **Events are source of truth** — never query the DB directly, query the EventStore by tags
- **Decision models** replace aggregates — built on-the-fly from queried events
- **AppendCondition** enforces optimistic concurrency (DCB)
- **Projections** run sync in-process via `ProjectingEventStore` decorator
- **ReadModelStore** uses `KClass<T>` (not `Class<T>` — multiplatform)
- **`kotlin.time.Instant/Clock`** (not kotlinx.datetime — migrated in 0.7.x)
- **No magic numbers in UI** — all components must use `CrmTheme.spacing.*`, `CrmTheme.typography.*`, `CrmTheme.shapes.*`, `CrmTheme.colors.*`. Never hardcode `dp`, `sp`, `Color(0x...)`, or `RoundedCornerShape(...)` in components. Define new tokens in `CrmTheme.kt` if needed.

## Test Commands
```bash
./gradlew :shared:jvmTest :backend:test   # must pass before commit (742 tests green as of Dunning BC merge)
./e2e/run.sh                              # E2E (needs backend + frontend running)
./scripts/test-all.sh                     # All tiers
```

## Git Rules
- **NEVER push directly to main.** Always create a feature branch with a functional name.
- Branch naming: `feature/<short-description>` (e.g., `feature/self-service-portal`, `feature/dunning-bc`)
- Commit, push to the feature branch, then create a PR **only when asked** — do not open PRs unprompted.

## Working Agreements

These are the default rules of engagement unless the user says otherwise. They exist because tokens are expensive and reviewable diffs are more valuable than big-bang commits.

### Small incremental steps
- Break each BC / feature into **~7–10 small commits**, not one giant one. Canonical step breakdown:
  1. Prerequisites / cross-cutting changes on other BCs
  2. Value objects + events + commands (domain types only)
  3. Decision model + command handler + unit tests
  4. Projection + read models + query handler + projection tests
  5. REST controller + DI wiring + any cross-BC reactor/glue
  6. Any infrastructure adapters (guards, scheduler loops, etc.)
  7. BDD tests + spec tick-off + TODO update
- **Compile and run the full test suite after each step** — `./gradlew :shared:jvmTest :backend:test` must stay green before every commit.
- **Push after every commit** so the remote branch always reflects shippable, passing state. Never accumulate unpushed work.
- If a test file exceeds ~20 new tests, split it across commits (domain tests in one commit, handler tests in the next, BDD in a later one).

### Commit messages
- Conventional-commit prefix: `feat(<bc>): ...`, `fix(<bc>): ...`, `spec: ...`, `test(<bc>): ...`
- First line under 80 chars, imperative mood.
- Body explains the **why** and any non-obvious decisions (scope cuts, patterns borrowed from other BCs, deferred work).
- Append the Claude Code session trailer `https://claude.ai/code/session_<id>` on every commit.

### Specs drive implementation
- Before writing code, the domain spec at `specs/domain/<bc>.md` must be **AGREED** (all open questions resolved).
- Open questions are refined by asking the user for decisions via `AskUserQuestion`, not invented by Claude.
- Every commit that touches a BC should tick the matching checkboxes in the spec's completion criteria section (9a domain, 9b application, 9c contract, 9d BDD, 9e UI/E2E, 9f DoD).
- `TODO.md` tracks overall BC status — update the "Implemented" / "Not Yet Implemented" tables as BCs land.
- When a BC is complete, add a matching Gherkin file at `specs/features/<bc>.feature` documenting the scenarios — scenarios should mirror the Kotlin BDD tests one-to-one.

### Pull requests
- **Open a PR as soon as a completable, mergeable slice is done.** A "completable slice" is a feature / BC / fix that (a) leaves `./gradlew :shared:jvmTest :backend:test` green, (b) has no half-finished work, and (c) could ship to main on its own merits. Don't batch multiple unrelated features into one PR, and don't sit on a finished branch waiting for the user to ask.
- **Between commits inside a slice, still break the work into small increments** — the §"Small incremental steps" checklist above is the default shape (value objects → handler → projection → REST/DI → BDD + spec tick-off). Each commit keeps tests green; the PR is just the final "this is ready" step.
- **One PR per slice.** If a scope-cut / follow-up falls out of the work, it goes on its own branch + its own PR, not bolted onto the current one.
- **Don't force-push to an open PR** unless the user asks. New work on an already-shipped feature is a new PR against a new branch.
- PR title: `<BC name>: <short summary> (<N> tests)` — always include the passing test count.
- PR body follows the repo template: Summary, architecture diagram (if non-trivial), what's in each commit, test plan checklist, deliberately-out-of-scope list, architecture notes.
- Base the PR against `main` unless stacking on another open PR (in which case the base is the parent branch, and the description notes the stacking).
- Include the Claude Code trailer at the bottom of every PR body.

### Scope discipline
- **Don't add features that weren't requested**, even if they seem nice. Bug fixes don't need refactors, new BCs don't need speculative extension points. Three similar lines is better than a premature abstraction.
- **Don't validate inputs that can't happen** at internal boundaries. Validate at user-facing edges (REST controllers, portal write routes). Trust domain-internal invariants.
- **Out-of-scope work is deferred explicitly**, documented in the commit message and the PR body's "Deliberately out of scope" section, and (if significant) added to `TODO.md`.

### When stuck or ambiguous
- Use `AskUserQuestion` with concrete options (always mark the recommended one). Never invent product decisions.
- For open spec questions, ask before touching code. For implementation choices (library X vs Y, pattern A vs B) ask once the code requires the decision, not before.

## Before Committing
Always run `./gradlew :shared:jvmTest :backend:test` and fix any failures before committing.

## Dev Server
```bash
./gradlew :backend:run                                    # API on :8080
./gradlew :frontend:web:wasmJsBrowserDevelopmentRun       # Web UI on :8081
./gradlew :frontend:desktop:run                           # Desktop app (JVM/Compose)
./gradlew :frontend:ios:linkDebugFrameworkIosSimulatorArm64  # iOS .framework (needs macOS + Xcode)
./gradlew :frontend:android:assembleDebug -PenableAndroid=true  # Android APK (needs Android SDK)
```

### Platform targets
- `:shared`, `:designsystem`, `:frontend:common`, `:frontend:app`, and the 12 admin
  feature modules target `jvm` + `wasmJs` + `iosArm64` + `iosSimulatorArm64`.
- Android (`androidTarget()`) is opt-in behind `-PenableAndroid=true`. Without the
  flag the `com.android.library` plugin is never applied, so builds succeed on
  hosts without an Android SDK.
- iOS integrates via `ios-app/CRM/ContentView.swift` which loads the `CrmApp`
  framework produced by `:frontend:ios`.

## Specs
All specs are in `specs/`. Domain specs define completion criteria with checkboxes.
The Ralph Wiggum loop (`scripts/ralph-loop.sh`) reads these specs and implements against them.

## Version Matrix
- Kotlin 2.3.20, Compose 1.11.0-beta01, Gradle 9.4.1, Ktor 3.4.2
