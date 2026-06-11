# paywall

Paywall experiment for a media company — see [`requirements/`](requirements/index.html) for the full specification (Docs 1–7).

## Can we use Kotlin Multiplatform Compose with Wasm for web?

**Yes — but scoped by the requirements:**

| Surface | Technology | Why |
|---|---|---|
| Admin console & visual wall editor (Doc 7, `ADM-*`) | **Compose Multiplatform → Wasm** (this repo, `composeApp/`) | Internal tool, staff-only (ADM-05), no SEO needs. One Compose source tree serves web (Wasm), desktop (JVM), and later Android/iOS. The design system in the design package is itself a port of a Compose `CrmTheme`, so tokens map 1:1. |
| Public article pages & gates | Server-rendered HTML from the Kotlin native origin (TS-01) behind Cloudflare Workers (INF-01) | AC-01 requires the access decision server-side and forbids shipping premium content to unentitled clients; AC-05 requires server-generated teasers; SEO-* requires crawlable HTML. A canvas-rendered Wasm app cannot satisfy these. |
| Wall definitions | Structured JSON (ADM-11), produced by this editor, rendered by the gate component | The gate's appearance is content, not code (GAP-10). |

The whole stack stays Kotlin: Compose Multiplatform for the console, Kotlin/GraalVM native for the backend (TS-01), shared models (e.g. `WallDefinition`) publishable as a KMP library consumed by both.

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
