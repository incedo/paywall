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

## What's in `composeApp/`

A working Compose Multiplatform scaffold (Kotlin 2.2.21, Compose Multiplatform 1.9.3) with targets `wasmJs` (web) and `jvm` (desktop):

- `theme/CrmTheme.kt` — 1:1 Compose port of the design-system tokens (colors light/dark, typography, spacing, shapes, borders). Components read tokens only; hard-coded hex/dp is banned per the design system.
- `ui/Components.kt` — atoms: buttons, card, tag, divider, segmented toggle, usage meter, text field, avatar.
- `model/WallDefinition.kt` — the structured wall definition (ADM-11).
- `designer/WallDesignerScreen.kt` — visual wall editor, workspace variant (design "Wall Designer", variant A): config panel left, live preview right.
- `screens/` — gate renderers used as the live preview: hard wall (pricing table) and metered wall (blurred list + gate with usage meter), both driven by `WallDefinition`, with light/dark preview theming.

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
