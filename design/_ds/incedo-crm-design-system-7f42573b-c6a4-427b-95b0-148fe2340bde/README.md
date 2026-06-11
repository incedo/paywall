# Incedo CRM Design System

A design system for **Incedo CRM** — a Kotlin Multiplatform customer-relationship-management suite that runs on web (Compose-Wasm), desktop (JVM), iOS and Android from a single Compose source tree.

## Sources

All tokens, components and screens in this system were lifted from the customer's private repository:

- **Repo:** `incedo/crm` (private, default branch `main`)
- **Design system code:** `designsystem/src/commonMain/kotlin/crm/designsystem/`
  - `theme/CrmTheme.kt` — colors, typography, spacing, shapes, elevation, animation, opacity, border, icon sizes, focus
  - `component/Crm*.kt` — 26 atoms & molecules (buttons, inputs, badges, tags, avatars, checkboxes, switches, menus, …)
  - `organism/Crm*.kt` — side nav, nav bar, data table, kanban board, timeline, search bar, form section
- **Screens (reference):** `frontend/common/src/commonMain/kotlin/crm/frontend/screen/` — dashboard, login, admin
- **Bounded contexts (frontend/*):** activities, companies, contacts, deals, dunning, invoice, payment, portal, pricing, selfservice, subscription, ticket, user
- **Storybook:** `storybook/src/jvmMain/kotlin/crm/storybook/` — stories for tokens, foundation, inputs, selection, navigation, overlay, feedback, data, organisms
- **Project guidance:** `CLAUDE.md` at repo root

Do NOT assume the reader has access to the repo — but cite these paths in case they do. The assets in this project are a faithful port, not a reinterpretation.

## Product context

Incedo CRM is a back-office CRM organised around DDD bounded contexts:

- **Contacts** — people & relationships
- **Companies** — organisations
- **Deals** — sales pipeline (kanban)
- **Activities** — timeline of calls / emails / meetings / tasks
- **Invoice / Payment / Pricing / Subscription / Dunning** — billing & revenue BCs
- **Tickets** — support cases
- **Portal / Self-service** — customer-facing portal routes
- **Admin / User** — user management

The product is a token-driven, accessibility-first internal tool — dense but not crowded, closer in spirit to Google Workspace / Material 3 than to Salesforce Lightning or HubSpot. All components read from `CrmTheme.{colors|typography|spacing|shapes|…}`; hard-coded dp/sp/hex is explicitly banned in the repo's `CLAUDE.md`.

## Index — files in this system

| File | Purpose |
|---|---|
| `README.md` | this file |
| `colors_and_type.css` | full CSS-variable port of `CrmTheme.kt` + semantic element styles |
| `SKILL.md` | Agent-Skill manifest, loadable in Claude Code |
| `fonts/` | web fonts (Inter, JetBrains Mono — see *Fonts* below) |
| `assets/` | logos, icon reference, generic imagery |
| `preview/` | small HTML cards populating the Design-System tab (one token group / component per card) |
| `ui_kits/crm-web/` | React recreation of the Compose CRM workspace (sidebar, dashboard, deals kanban, contacts, activities) |

## CONTENT FUNDAMENTALS

The product copy across `frontend/**` screens and design-system components is tight, literal, and product-native — it reads like a tool built by engineers for operators, not a consumer app.

### Voice

- **Literal > clever.** Screen titles are just the noun: "CRM Dashboard", "Contacts", "Deals", "Activities". Buttons are verbs: "Sign in with SSO", "Save", "Delete". No taglines, no emoji, no marketing gloss.
- **Short imperatives.** CTAs rarely exceed 3 words ("Sign in with SSO", "Add contact", "Create deal").
- **"You" is implicit, not spoken.** Instruction labels drop the pronoun ("Sign in to continue", not "You can sign in here"). First-person is never used.
- **No exclamation marks, no sentence-case "Welcome!", no "Let's…" copy.**

### Casing

- **Titles / headings:** Title Case or Sentence case — both appear. Screen-level `h1` uses sentence case ("CRM Dashboard"), while component-card titles are sentence case ("Pipeline & revenue").
- **Labels (form fields, table headers, tabs):** ALL CAPS is NOT used. Labels are regular sentence case at 12px Medium (`CrmTypography.label`).
- **Buttons:** Sentence case ("Sign in with SSO", "Save changes"). No UPPERCASE buttons.
- **Nav items:** Title Case single words ("Contacts", "Companies", "Deals").

### Examples (straight from the codebase)

| Context | Copy |
|---|---|
| Login screen | "CRM" / "Sign in to continue" / "Sign in with SSO" |
| Dashboard card | "Contacts — Manage people and relationships" |
| Dashboard card | "Deals — Pipeline & revenue" |
| Dashboard card | "Activities — Timeline & tasks" |
| Empty state | "No data" |
| Loading button | "Redirecting..." |

### Do / Don't

- ✅ "Add contact"  ❌ "➕ Add a new contact"
- ✅ "No data"  ❌ "🦗 It's quiet here..."
- ✅ "Pipeline & revenue"  ❌ "Track every dollar of your pipeline!"
- ✅ "Sign in with SSO"  ❌ "Log in to unlock your CRM"

### Emoji & punctuation

- **Emoji: never.** Not in UI, not in copy, not in docs.
- **Ampersand (&) is used** in category labels ("Pipeline & revenue", "Timeline & tasks") — a light house style.
- **Ellipses (`...`) for in-progress states** ("Redirecting...").
- **Unicode glyphs are used for icons** (see *ICONOGRAPHY*) — checkmarks `✓`, close `✕`, etc.

## VISUAL FOUNDATIONS

### Color

- **Single-hue primary** — Google-esque blue `#1A73E8` (light) / `#8AB4F8` (dark). One accent, no gradients.
- **Neutral background scale** — `#F8F9FA` background, `#FFFFFF` surface, `#F1F3F4` surface-variant. Three greys total; the surface-variant is the table-header / chip / kanban-column back.
- **Text** — `#202124` on-surface, `#5F6368` on-surface-variant. Only two text colors, ever.
- **Semantic colors** — error `#D93025`, success `#1E8E3E`, warning `#F9AB00`, info = primary. Each has a matching tinted *container*: `#FCE8E6`, `#E6F4EA`, `#FEF7E0`, `#E8F0FE`. Semantic colors NEVER appear outside their semantic role.
- **Imagery treatment** — imagery in this system is product UI, not lifestyle photography. Cool, neutral, high-contrast. Black-and-white or product-native screenshots only.

### Type

- **Family:** sans-serif, defaults to system (`Inter` / `Google Sans` / `Roboto` / system-ui). **No display or serif fonts anywhere.**
- **Scale (from `DefaultCrmTypography`):**
  - h1 — 24 / 32 / 700
  - h2 — 20 / 28 / 600
  - h3 — 16 / 24 / 600
  - body — 14 / 20 / 400
  - body-sm — 12 / 16 / 400
  - label — 12 / 16 / 500
  - caption — 11 / 14 / 400
  - button — 14 / 20 / 500
- **No italics. No uppercase styling. No letter-spacing tricks.** Weight is the only emphasis tool.

### Spacing

- **8-point grid, extended low** — `2, 4, 8, 12, 16, 24, 32`. The `12px` `md` slot is unusual (not a pure doubling) and is the most-used value — it's the default row / card padding.
- **Component padding:** buttons `px=16 py=12` (`lg` / `md`), inputs `px=md` all around, table cells `px=lg py=md`, sidenav items `px=md py=md`.
- **Column gap on kanban:** `md` (12px). Stack gap on forms: `sm` (8px) between label and field, `lg` between fields.

### Shapes & corners

- **Four shape tokens:** 4 / 8 / 12 / pill(50%).
- **Cards, inputs, buttons → `md` (8px).** Cards & modals that feel "container-sized" → `lg` (12px). Chips, tags, progress bars, avatars → `pill`. Checkboxes → `sm` (4px).
- No asymmetric corners. No cut corners.

### Elevation & shadows

- Six-step scale: none / xs(1) / sm(2) / md(4) / lg(8) / xl(16) — expressed as dp in Compose, ported here as layered soft shadows with cool-neutral tint `rgba(60,64,67,*)`.
- Cards sit at `sm`, floating menus at `md`, dialogs/sheets at `lg`, drag-preview at `xl`. The side nav and inline chips have **no** shadow — elevation is a way to say *floats above the page*, not a decorative pass.
- No colored glows. No inner shadows.

### Borders

- **3 widths:** 0.5 / 1 / 2. Default is 1. Thin (0.5) is used for table-row dividers on hi-dpi; thick (2) is the focus ring.
- Borders color = `--crm-divider` (#DADCE0) for neutral, semantic color for error-state inputs.

### Animation

- **Three durations:** fast 150 / normal 300 / slow 500, with `cubic-bezier(0.4, 0, 0.2, 1)` (Material standard).
- **Used sparingly.** Hover transitions, dialog fade-ins, snackbar slide-ups. No bounces, no spring physics, no staggered reveals, no parallax.
- **Hover state:** 8% primary overlay (`--crm-opacity-hover`).
- **Focus state:** 12% primary overlay + 2px focus ring.
- **Pressed state:** 16% primary overlay. No scale-down.
- **Disabled:** 38% text opacity (`--crm-opacity-disabled`), `--crm-disabled` surface.

### Layout rules

- **Persistent left sidenav**, 200dp wide, `surface` color. Never collapses on desktop.
- **Top app bar** (CrmNavBar) is optional — most screens are content-first with sidenav + breadcrumb.
- **Max content width** is not clamped; this is an internal tool, content fills the viewport.
- **No hero units. No full-bleed imagery. No illustrations.** Blank slates are a centred text stub ("No data") in `--crm-on-surface-variant`.

### Blur & transparency

- **No glass/blur effects.** Surfaces are opaque.
- The only transparency in the system is **token-regulated overlay** — hover/focus/pressed states use primary-at-alpha, and the modal scrim is `overlay × 0.50`.

### What cards look like

- Background `--crm-surface` (white), corner `--crm-radius-md` (8px), optional `--crm-elev-sm` shadow, **no border by default**. Borders appear only on input-fields (1px divider) and on the surface-variant table header row.

## ICONOGRAPHY

Incedo CRM's icon system is **deliberately primitive** — the `CrmIcon` foundation renders a single glyph string through `BasicText`:

```kotlin
// designsystem/src/commonMain/kotlin/crm/designsystem/foundation/CrmIcon.kt
@Composable
fun CrmIcon(glyph: String, size: Dp, tint: Color, …)
```

All icons in the codebase are passed as **Unicode glyphs** — no SVG sprites, no icon fonts, no vendor set. Observed in use:

| Glyph | Unicode | Role | Source |
|---|---|---|---|
| ✓ | U+2713 | checkbox on | `CrmCheckbox.kt` |
| ✕ | U+2715 | remove / close | `CrmTag.kt`, dialogs |
| ▶ | U+25B6 | play / expand | timeline |
| ⚠ | U+26A0 | warning | feedback |
| 🔔 | — (not used; avoid emoji) | — | — |

For this design system port — because glyph-only icons are visually flat in HTML — we substitute **[Lucide](https://lucide.dev) via CDN** at the same stroke weight (1.5px) and 20px default size. Lucide matches Material's geometric aesthetic and is the closest production-quality CDN match to the codebase's design intent. **This is a substitution** — flagged to the user at delivery. When porting these designs back to the app, continue to use `CrmIcon(glyph = …)` with Unicode symbols, or introduce a proper SVG icon set before the app grows beyond a handful of glyphs.

### Usage rules

- **Always use the token sizes** (`--crm-icon-xs … xxl`). Default is `md` (20px).
- **Always tint via `--crm-on-surface-variant`** for neutral icons, `--crm-primary` for active state, semantic color for semantic meaning.
- **Never use emoji** in product UI. (Emoji appear NOWHERE in the repo source.)
- **Unicode chars are acceptable** for trivial affordances (✓, ✕, ▶, arrows), which matches how the real app ships.

### Logos & imagery

The repo ships **no logo or brand illustration** — the product simply writes the word "CRM" at h1 / primary color on the login screen. We have recreated this wordmark in `assets/logo-wordmark.svg` as a faithful port; any richer brand asset would be an invention.

No hero imagery, no avatars (initials are rendered via `CrmAvatar`), no illustrations.

## Fonts

The codebase does not ship a webfont. The Compose theme uses the platform default sans-serif. For the HTML port, we use **Roboto** (sans) and **Roboto Mono** (mono) via Google Fonts — this is the same face Compose-Android already resolves to, and an Apache-2.0 open-source stand-in for Google Sans on the web/desktop targets.

**Rationale:** Roboto is the closest open-source match to the Compose default. Inter / DM Sans were considered; Roboto wins for one-to-one parity with the Android build. Swap in `Google Sans` (licensed) only if the customer already pays for it.
