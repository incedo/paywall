# paywall

Paywall experiment for a media company — see [`requirements/`](requirements/index.html) for the full specification (Docs 1–7).

## Can we use Kotlin Multiplatform Compose with Wasm for web?

**Yes — for both the public web channel and the admin console.** AC-01/AC-05 require server-side *decision and enforcement*, not server-side *rendering*: a Wasm SPA that fetches all content from entitlement-enforcing origin APIs satisfies them, because the premium body is never present in any response to an unentitled request (BP-01).

| Surface | Technology | Why |
|---|---|---|
| Public web channel (TS-07) | **Compose Multiplatform → Wasm SPA**, served as static assets from the edge | The SPA holds no content and makes no access decisions; teaser and full body come from origin APIs enforcing AC-01..05. |
| SEO / crawlers (SEO-01..03) | Dynamic rendering at the edge: the Worker (INF-01) routes *verified* crawlers to a server-rendered HTML rendering with paywalled-content JSON-LD | A canvas-rendered SPA is not crawlable; verified crawlers bypass it entirely. Unverified UA claimants get the normal gated experience (BP-02). |
| Admin console & visual wall editor (Doc 7, `ADM-*`) | Same Compose source tree (this repo, `composeApp/`) | Internal, staff-only (ADM-05), no SEO needs. |
| Wall definitions | Structured JSON (ADM-11), produced by this editor, rendered by the gate component | The gate's appearance is content, not code (GAP-10). |

The whole stack stays Kotlin: Compose Multiplatform on the client, Kotlin/GraalVM native for the backend (TS-01), shared models (e.g. `WallDefinition`) publishable as a KMP library consumed by both.

## What's in `shared/`

The domain core (pure Kotlin, KMP: `jvm` + `wasmJs`), per `architecture/`:

- `core/` — `DomainEvent` (tagged, DCB), `EventStore` port with `EventQuery`/`AppendCondition`, in-memory adapter
- `access/AccessDecisionEngine.kt` — the Doc 2 §1 decision flow as a pure function: free → entitled → grant → strategy (hard/metered/freemium/dynamic incl. PW-42 floor rule)
- `metering/` — `MeterDecision` (PW-20/21), `RecordArticleReadHandler` (DCB append condition, 3 retries per Q-7)
- `entitlements/`, `grants/` — decision models fed by integration events (external subscription administration; Keto-backed FGA)
- `experiments/VariantAssigner.kt` — deterministic FNV-1a assignment (EX-01)

Tests: `./gradlew :shared:jvmTest` — includes the **NFR-12 decision matrix** (4 types × 4 visitor states × 2 tiers = 32 cases) plus unit suites per decision model, concurrency-retry and event-store contract tests.

## What's in `composeApp/`

A working Compose Multiplatform scaffold (Kotlin 2.4.0, Compose Multiplatform 1.11.1) with targets `wasmJs` (web) and `jvm` (desktop):

- `theme/CrmTheme.kt` — 1:1 Compose port of the design-system tokens (colors light/dark, typography, spacing, shapes, borders). Components read tokens only; hard-coded hex/dp is banned per the design system.
- `ui/Components.kt` — atoms: buttons, card, tag, divider, segmented toggle, usage meter, text field, avatar.
- `model/WallDefinition.kt` — the structured wall definition (ADM-11).
- `designer/WallsOverviewScreen.kt` — walls overview table (design variant B1): status, channels, A/B, conversion; click a row to edit.
- `designer/WallDesignerScreen.kt` — visual wall editor, workspace variant (design variant A): configuration left, live preview centre (web/mobile, light/dark), targeting & publishing (audience, channels, A/B, version history) right.
- `screens/` — gate renderers used as the live preview, driven by `WallDefinition`: hard wall (pricing table) and the content gate (blurred list + gate card) covering the metered, freemium and dynamic strategies (PW-*).
- `wasmJsMain/resources/index.html` — SPA shell including the styled no-JS/no-Wasm fallback page (TS-08).

## Run

```sh
# Web (Wasm) — dev server with hot reload
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Web — production bundle (output in composeApp/build/dist/wasmJs/productionExecutable)
./gradlew :composeApp:wasmJsBrowserDistribution

# Desktop (JVM)
./gradlew :composeApp:run
```

## Adding mobile targets later

`commonMain` has no web- or desktop-specific code. Adding `androidTarget()` and `iosArm64()/iosSimulatorArm64()` to `composeApp/build.gradle.kts` plus thin entrypoints is enough to ship the same console to mobile.
