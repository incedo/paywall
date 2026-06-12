# Testing Strategy

**Status**: AGREED
**Last Updated**: 2026-06-12
**Depends On**: architecture/module-structure.md, architecture/tech-stack.md, architecture/design-system.md

---

## 1. Overview

Every spec is verified through five layers of testing, following the **test pyramid** with an emphasis on fast, deterministic tests at the base and slower integration/UI tests at the top. A **Component Workbench** (Storybook) adds visual testing and documentation across all layers.

```
          ┌───────────┐
          │  UI Tests  │  Playwright / Compose UI test
          │  (E2E)     │  Main user flows end-to-end
          ├───────────┤
          │  Component │  Storybook / Component Workbench
          │  Workbench │  Visual testing, responsive, docs
          ├───────────┤
          │  BDD Tests │  Cucumber / Gherkin feature files
          │  (Behavior)│  Business scenarios in plain English
          ├───────────┤
          │  Contract  │  Pact / OpenAPI-driven
          │  Tests     │  API producer-consumer contracts
          ├───────────┤
          │  Unit      │  JUnit5 / kotlin.test
          │  Tests     │  Domain logic, handlers, projections
          └───────────┘
              FAST ←──────────────────────── SLOW
```

The Component Workbench sits between BDD and E2E — it tests UI components in isolation (faster than full E2E) while providing visual documentation and responsive verification that unit tests cannot cover.

---

## 2. Unit Tests

**Scope**: Domain models, value objects, decision models, command handlers, query handlers, projections, validation.

**Runs in**: `shared` module (commonTest) + `backend` module (test)

**Framework**: `kotlin.test` (multiplatform) + JUnit5 (JVM-specific backend tests)

### What Unit Tests Cover

| Layer | Tests | Location |
|-------|-------|----------|
| Value Objects | Validation, construction, equality | `shared/commonTest/.../domain/model/` |
| Domain Events | Serialization roundtrip, correct tags | `shared/commonTest/.../domain/event/` |
| Decision Models | Event folding, invariant enforcement per BR | `shared/commonTest/.../domain/decision/` |
| Validation | Field-level rejection/acceptance per BR | `shared/commonTest/.../domain/validation/` |
| Command Handlers | Events emitted, decision model checked, errors returned | `shared/commonTest/.../application/command/` |
| Query Handlers | Correct read model returned, pagination | `shared/commonTest/.../application/query/` |
| Projections | Event → read model transformation | `shared/commonTest/.../application/projection/` |

### Conventions

- **One test class per production class**: `MeterLimitDecision` → `MeterLimitDecisionTest`
- **Test method naming**: `fun testBR1_rejectsBlankWallName()`, `fun testPublishWall_emitsWallPublishedEvent()`
- **Use InMemoryEventStore** for command handler tests — no real DB
- **Use InMemoryReadModelStore** for query handler tests
- **Arrange-Act-Assert** pattern, no mocking frameworks — prefer simple fakes
- **Each business rule (BR-N)** gets at least one dedicated test
- **The decision matrix (NFR-12)** is exercised at the unit layer first: `MeterLimitDecision` and the access-decision logic each get one test per (paywall type × visitor state × content tier) cell — minimum 32 cases. `MeterIncremented` is the canonical concurrency example: two concurrent increments at `limit - 1` must result in exactly one accepted command (DCB append condition rejects the second).

### Gradle Command
```bash
./gradlew :shared:allTests              # Run shared (commonTest) on all targets
./gradlew :backend:test --tests '*Unit*' # Backend unit tests only
```

---

## 3. Contract Tests

**Scope**: Verify that the REST API (producer) and frontend (consumer) agree on request/response shapes, status codes, and error formats.

**Runs in**: `backend` module (test) + `frontend` module (test)

**Framework**: Pact (JVM) or OpenAPI-driven contract testing

### Approach: Consumer-Driven Contracts

```
┌─────────────────┐         contract (pact file)        ┌─────────────────┐
│  Frontend        │ ──── defines expectations ────────→ │  Backend         │
│  (consumer)      │                                     │  (producer)      │
│                  │ ← verified against real endpoints ──│                  │
└─────────────────┘                                     └─────────────────┘
```

1. **Consumer side** (frontend tests): Define expectations — "when I POST to /api/v1/walls with this body, I expect 201 with this shape"
2. **Contract artifact**: Generated pact file or OpenAPI spec
3. **Producer side** (backend tests): Verify the real API matches the contract

### What Contract Tests Cover

| Contract | Consumer Expects | Producer Verifies |
|----------|-----------------|-------------------|
| Publish Wall | POST /api/v1/walls → 201 `{ wallId }` | Endpoint returns correct shape |
| Publish Wall (invalid) | POST /api/v1/walls → 400 `{ error }` | Error shape matches |
| List Walls | GET /api/v1/walls → 200 `{ data, pagination }` | Pagination shape matches |
| Get Wall | GET /api/v1/walls/{id} → 200 `WallView` | All fields present |
| Get Wall (missing) | GET /api/v1/walls/{id} → 404 `{ error }` | 404 error shape matches |
| Access Decision | POST /api/v1/decide → 200 `{ decision, variant, meter }` | Decision shape matches (API-05) |
| (repeat for each entity and endpoint) | | |

### Additional Contract Checks
- **Error response shape** is consistent across all endpoints: `{ error: { code, message, details[] } }`
- **Pagination response shape** is consistent: `{ data[], pagination: { page, size, total } }`
- **Auth error shape**: 401 and 403 responses follow the same error format

### File Locations

| Type | Location |
|------|----------|
| Consumer contract definitions | `frontend/src/wasmJsTest/.../contract/` |
| Producer contract verification | `backend/src/test/.../contract/` |
| Pact files / OpenAPI spec | `contracts/` (root directory) |

### Gradle Command
```bash
./gradlew :backend:test --tests '*Contract*'
./gradlew :frontend:wasmJsTest --tests '*Contract*'
```

---

## 4. BDD Tests (Behavior-Driven Development)

**Scope**: Business scenarios written in Gherkin (Given-When-Then) that test complete use cases end-to-end through the backend API. These are the **executable specifications** — human-readable scenarios that double as integration tests.

**Runs in**: `backend` module (test) or dedicated `bdd` module

**Framework**: Cucumber-JVM with Kotlin step definitions

### Feature File Structure

```
specs/features/                          # Gherkin .feature files (co-located with specs)
  walls.feature
  metering.feature
  experiments.feature
  entitlements.feature                   # Entitlement ingest from the external subscription administration + enforcement
  auth.feature
  decision-matrix.feature                # Cross-entity flow: paywall type × visitor state × content tier (NFR-12)

backend/src/test/kotlin/nl/incedo/paywall/backend/
  bdd/
    steps/
      WallSteps.kt                      # Step definitions for wall scenarios
      MeteringSteps.kt
      ExperimentSteps.kt
      EntitlementSteps.kt
      AuthSteps.kt
      CommonSteps.kt                    # Shared steps (authentication, assertions)
    CucumberRunner.kt                   # JUnit5 Cucumber runner
    TestWorld.kt                        # Shared state across steps (Ktor test client, tokens)
```

### Example Feature File

```gherkin
# specs/features/metering.feature

Feature: Article Metering
  As a media company
  I want to meter premium article reads per visitor
  So that the metered wall gates at the configured free limit

  Background:
    Given the metered wall is configured with a monthly limit of 5

  Scenario: Reading a premium article increments the meter
    Given an anonymous visitor with 2 articles read this month
    When the visitor opens a premium article
    Then the response status is 200
    And a "MeterIncremented" event is recorded
    And the visitor's meter shows 3 of 5

  Scenario: Cannot read past the monthly free limit
    Given an anonymous visitor with 5 articles read this month
    When the visitor opens a premium article
    Then the response status is 402
    And the error code is "METER_LIMIT_REACHED"
    And a "WallShown" event is recorded

  Scenario: Re-reading an article does not double-count
    Given an anonymous visitor who already read article "long-read-42" this month
    When the visitor opens article "long-read-42" again
    Then the response status is 200
    And the visitor's meter is unchanged

  Scenario: Meter reset restores access
    Given an anonymous visitor with 5 articles read this month
    When an admin resets the visitor's meter
    Then a "MeterReset" event is recorded
    And the visitor can open a premium article
    But the audit log records the reset with actor and reason
```

### Main Flow Scenarios (Required)

Each entity must have BDD scenarios for these main flows:

| Flow | Scenarios |
|------|-----------|
| **CRUD** | Create, read (single + list), update, delete |
| **Validation** | Reject invalid input per each business rule |
| **Limits** | Detect and reject meter overruns (DCB decision model `MeterLimitDecision`, MT-*) |
| **Cross-entity** | Publish wall with experiment references, cascade behavior on archive |
| **Auth** | Protected endpoints require token, role-based access (ADM-05) |
| **Decision Matrix** (Walls) | Each paywall type × (anonymous, registered, subscriber, expired) × (free, premium) — minimum 32 cases (NFR-12) |
| **Timeline** (WallEvents) | `WallShown` / `VariantAssigned` events recorded per decision, subject inspector data (ADM-04) |

### Gradle Command
```bash
./gradlew :backend:test --tests '*Cucumber*'
# Or if separate module:
./gradlew :bdd:test
```

---

## 5. Component Workbench (Storybook)

**Scope**: Visual testing and documentation of all design system components and paywall console screens in isolation, across themes, viewports, and interaction states.

**Runs in**: `:docs` module (Compose Desktop) + future `:storybook` module

**Framework**: Custom Compose-based workbench (no React/JS Storybook — pure Kotlin Multiplatform)

### What the Workbench Provides

| Capability | Purpose | Layer |
|-----------|---------|-------|
| **Component Catalog** | Browse all atoms, molecules, organisms by category | Documentation |
| **Story Rendering** | Each component has named stories (variants, states) | Visual testing |
| **Scenarios** | Predefined states: loading, error, empty, long content | Edge case verification |
| **Interactive Controls** | Tweak props at runtime (boolean, text, enum, number) | Exploratory testing |
| **Responsive Preview** | View at phone / tablet / desktop / web breakpoints | Responsive verification |
| **Theme Switching** | Light / Dark / Custom themes side by side | Theme regression |
| **Decorator Wrapping** | Scaffold, padding, fake data providers | Isolation |
| **Visual Regression** | Screenshot comparison (future — Phase 4) | Automated regression |

### Story Structure

Each component has one or more **Stories** — named, discoverable render configurations:

```kotlin
// stories/ButtonStories.kt
val buttonStories = storyGroup("Buttons") {
    story("Primary") { CrmPrimaryButton(text = "Upgrade to Pro", onClick = {}) }
    story("Secondary") { CrmSecondaryButton(text = "Compare plans", onClick = {}) }
    story("Text") { CrmTextButton(text = "Maybe later", onClick = {}) }
    story("Toggle Chip") { CrmToggleChip(label = "Web", selected = true, onClick = {}) }
    story("Segmented") { CrmSegmentedToggle(options = listOf("Monthly", "Annual"), selectedIndex = 0, onSelect = {}) }
}
```

### Scenarios (State Variations)

Each story can have **Scenarios** — predefined data/state configurations:

| Scenario | Purpose | Example |
|----------|---------|---------|
| **Default** | Happy path | Wall designer with a valid wall definition |
| **Loading** | Async in-progress | Spinner + skeleton in walls overview table |
| **Error** | Validation/network failure | Designer with invalid variant weights message |
| **Empty** | No data | Empty walls overview with EmptyState |
| **Long Content** | Overflow/truncation | Wall name with 200 characters |
| **Disabled** | Non-interactive | All inputs disabled |
| **RTL** | Right-to-left layout | Arabic text direction (future) |

### Controls (Runtime Interaction)

```kotlin
story("UsageMeter") {
    val label = textControl("Label", default = "Free articles this month")
    val used = intControl("Used", default = 3)
    val limit = intControl("Limit", default = 5)

    CrmUsageMeter(
        label = label.value,
        used = used.value,
        limit = limit.value,
    )
}
```

### Responsive Preview

Stories render at multiple breakpoints simultaneously:

| Breakpoint | Width | Label |
|-----------|-------|-------|
| Phone | 360dp | Compact |
| Tablet Portrait | 768dp | Medium |
| Tablet Landscape | 1024dp | Expanded |
| Desktop | 1280dp | Large |

Side-by-side comparison mode shows the same story at all 4 widths.

### Decorators

Wrappers that provide the rendering environment without the full app runtime:

| Decorator | Purpose |
|-----------|---------|
| **ThemeDecorator** | Wraps in `CrmTheme(darkTheme = ...)` |
| **ScaffoldDecorator** | Wraps in the console scaffold with nav |
| **PaddingDecorator** | Adds consistent padding |
| **FakeDataDecorator** | Provides fake read models / API responses (fake `WallDefinition`s, meter states) |

### Relationship to Other Test Layers

```
Unit tests     → verify logic (events, handlers, projections)
Contract tests → verify API shapes
BDD tests      → verify business scenarios
─────────────────────────────────────────────────
Workbench      → verify visual rendering, responsive behavior,
                  theme consistency, interaction states, documentation
─────────────────────────────────────────────────
E2E tests      → verify full user flows in real browser
```

The Workbench does NOT replace unit or E2E tests. It fills the gap between "logic works" and "it looks right" — catching visual regressions, layout breaks, and theme inconsistencies that code-level tests miss.

### Phased Rollout

| Phase | Capabilities | Module |
|-------|-------------|--------|
| 1 | Catalog + preview (current `:docs` module) | `:docs` |
| 2 | Stories + scenarios + controls | `:storybook` |
| 3 | Responsive comparison + theme side-by-side | `:storybook` |
| 4 | Visual regression (screenshot diff in CI) | `:storybook` + CI |
| 5 | Design token audit + coverage report | `:storybook` |

### Commands
```bash
./gradlew :docs:run                # Phase 1: catalog app (current)
./gradlew :storybook:run           # Phase 2+: full workbench
./gradlew :storybook:screenshotTest  # Phase 4: visual regression
```

### Detailed Specifications

See the split spec files for full DDD-level detail:
- `compose_storybook_requirements.md` — full requirements
- `storybook_story_and_scenario_specs.md` — Story + Scenario domain model
- `storybook_controls_and_responsive_specs.md` — Controls + responsive rules
- `storybook_decorators_phases_governance_specs.md` — Decorators + phases + governance
- `storybook_specs_split.md` — spec decomposition overview

---

## 5a. UI Tests (End-to-End)

**Scope**: Test the main user flows through the actual browser UI (Compose WASM running in a real browser). These are the slowest tests but verify the full stack.

**Runs in**: Separate `e2e` module or test directory

**Framework**: Playwright (browser automation) — works with any frontend, including WASM

### Why Playwright (not Compose test framework)?
- Compose WASM renders to a canvas — Compose's test framework doesn't support wasmJs well yet
- Playwright works with any browser-rendered content, including canvas
- Cross-browser testing (Chrome, Firefox, Safari)
- Mature, well-documented, great debugging tools

### Test Structure

```
e2e/
  playwright.config.ts               # Playwright configuration
  package.json                       # Node.js deps (playwright)
  tests/
    auth.spec.ts                     # Login/logout flow
    walls.spec.ts                    # Wall designer + publish flows
    metering.spec.ts                 # Metered gate flows
    experiments.spec.ts              # A/B variant assignment flows
    conversion.spec.ts               # Checkout handoff to the external subscription administration + entitlement flows
    dashboard.spec.ts                # Console dashboard rendering
  fixtures/
    auth.fixture.ts                  # Login helper, authenticated context
  helpers/
    api.helper.ts                    # Direct API calls for test setup
```

### Main Flow UI Tests (Required)

| Flow | Test |
|------|------|
| **Login** | Navigate to console → redirected to Ory login → enter credentials → redirected back → see dashboard |
| **Create Wall** | Click "New Wall" → configure in designer → save → see walls overview row with correct data |
| **Search & Filter** | Type in walls overview search bar → table filters → results match |
| **Edit Wall** | Open wall → change gate copy in designer → save → changes persisted, live preview updates |
| **Publish Wall** | Open wall → click publish → confirm diff → status badge flips to Published |
| **Metered Gate** | Visit articles as anonymous reader → usage meter counts up → gate appears at the limit with "You've reached this month's free limit" |
| **Convert** | Hit gate → click "Upgrade to Pro" → checkout handoff to the external subscription administration → entitlement ingested → premium article fully visible |
| **Variant Assignment** | Same visitor revisits → same variant rendered (EX-01, no flicker) |
| **Role-Based Access** | Login as EDITOR → publish action hidden. Login as ADMIN → publish action visible |
| **OIDC Flow** | Full redirect → Kratos login → Hydra token → console authenticated |

### Test Environment
- Tests run against the full K8s stack (Rancher Desktop) or Docker Compose
- A `beforeAll` hook ensures the stack is up and seeds test data
- Tests use API helpers to create data directly (faster than UI for setup)
- Each test cleans up after itself or uses isolated test data

### Commands
```bash
cd e2e && npx playwright test                    # Run all UI tests
cd e2e && npx playwright test --headed           # Run with browser visible
cd e2e && npx playwright test walls.spec.ts      # Run single spec
cd e2e && npx playwright show-report             # View HTML report
```

---

## 6. Test Data & Fixtures

### InMemoryEventStore (Unit/Command Handler Tests)
Pre-loaded with events for the test scenario. No real DB needed.

### Ktor TestApplication (Contract/BDD Tests)
Uses Ktor's `testApplication` with InMemoryEventStore + InMemoryReadModelStore. Fast, deterministic, no Docker needed.

### Full Stack (UI/E2E Tests)
Runs against real K8s or Docker Compose. Uses API helpers to seed data:

```typescript
// e2e/helpers/api.helper.ts
async function publishWall(data: WallInput): Promise<string> {
  const response = await request.post('/api/v1/walls', { data });
  return response.json().wallId;
}
```

---

## 7. Test Pyramid Targets

| Layer | Count (per entity) | Speed | Isolation |
|-------|-------------------|-------|-----------|
| Unit | 15-30 tests | < 1s | Full (in-memory) |
| Contract | 5-10 tests | < 5s | Ktor testApplication |
| BDD | 10-20 scenarios | < 30s | Ktor testApplication |
| Workbench | 3-10 stories | < 10s | Compose Desktop (isolated) |
| UI/E2E | 5-10 tests | < 2min | Full stack (K8s/Docker) |

### Quality Gates (CI)

```
Unit tests      ── must pass ── ─→ ✓ merge to feature branch
Contract tests  ── must pass ── ─→ ✓ merge to feature branch
BDD tests       ── must pass ── ─→ ✓ merge to main
Workbench       ── must compile ─→ ✓ merge to main (screenshot diff Phase 4)
UI tests        ── must pass ── ─→ ✓ release
```

---

## 8. Spec-to-Test Mapping

The Ralph Wiggum loop uses this mapping to know which tests to write and run:

| Spec Section | Test Layer | Test Location |
|-------------|-----------|---------------|
| Value Objects | Unit | `shared/commonTest/.../domain/model/{VO}Test.kt` |
| Domain Events | Unit | `shared/commonTest/.../domain/event/{Entity}EventTest.kt` |
| Decision Models | Unit | `shared/commonTest/.../domain/decision/{Entity}DecisionModelTest.kt` |
| Validation | Unit | `shared/commonTest/.../domain/validation/{Entity}ValidationTest.kt` |
| Command Handlers | Unit | `shared/commonTest/.../application/command/{Entity}CommandHandlerTest.kt` |
| Query Handlers | Unit | `shared/commonTest/.../application/query/{Entity}QueryHandlerTest.kt` |
| Projections | Unit | `shared/commonTest/.../application/projection/{Entity}ProjectionTest.kt` |
| API Endpoints (shapes) | Contract | `backend/test/.../contract/{Entity}ContractTest.kt` |
| API Endpoints (behavior) | BDD | `specs/features/{entity}.feature` + `backend/test/.../bdd/steps/{Entity}Steps.kt` |
| Business Rules (scenarios) | BDD | `specs/features/{entity}.feature` |
| Decision matrix (NFR-12) | Unit + BDD | `shared/commonTest/.../domain/decision/` + `specs/features/decision-matrix.feature` |
| **Design system tokens** | **Workbench** | `storybook/stories/tokens/{Token}Stories.kt` |
| **Design system components** | **Workbench** | `storybook/stories/component/{Component}Stories.kt` |
| **Screen layouts** | **Workbench** | `storybook/stories/screen/{Screen}Stories.kt` |
| **Responsive behavior** | **Workbench** | Side-by-side breakpoint comparison per story |
| **Theme consistency** | **Workbench** | Light/dark/custom theme toggle per story |
| **Interaction states** | **Workbench** | Scenarios: default, hover, focus, disabled, error, loading |
| UI Views (flows) | UI/E2E | `e2e/tests/{entity}.spec.ts` |
| Auth flows | UI/E2E | `e2e/tests/auth.spec.ts` |
| Cross-entity | BDD + UI | Feature files + E2E specs |

### Design System ↔ Workbench Relationship

The Component Workbench is a **derived artifact** of the design system — it is generated from the same token and component definitions. Every design system component automatically gets:

1. **A default story** — renders the component with default props
2. **Variant stories** — one per enum variant (e.g., Buttons: Primary/Secondary/Text)
3. **State scenarios** — default, disabled, error, loading
4. **Responsive preview** — the same story at all 4 breakpoints
5. **Theme comparison** — light vs dark side by side

When a new component is added to `designsystem/`, a corresponding story file is expected in `storybook/stories/`. The governance rule: **no component ships without a story**.

---

## 9. Completion Criteria

### 9a. Test Frameworks
- [x] Unit test framework configured (kotlin.test + JUnit5)
- [ ] Contract test framework configured (Pact or OpenAPI-driven)
- [ ] BDD framework configured (Cucumber-JVM with Kotlin steps)
- [x] Playwright E2E configured with auth fixture
- [x] InMemoryEventStore and InMemoryReadModelStore available as test doubles
- [x] At least one entity has all 4 test layers implemented
- [ ] `./gradlew check` runs unit + contract + BDD tests
- [x] `npx playwright test` runs UI tests against local stack

### 9b. Component Workbench
- [x] Phase 1: Catalog app with live component examples (`:docs` module)
- [x] Phase 2: Story DSL + scenario system (`:storybook` module)
- [x] Phase 2: Interactive controls (boolean, text, enum, number)
- [x] Phase 3: Responsive side-by-side comparison (4 breakpoints)
- [x] Phase 3: Theme comparison (light/dark/custom side by side)
- [ ] Phase 4: Visual regression screenshots in CI
- [ ] Phase 5: Design token audit + coverage (which tokens are used/unused)
- [x] Governance: every design system component has at least one story (55 stories)
- [x] All storybook specs promoted from DRAFT to AGREED

### 9c. Coverage Targets
- [x] JaCoCo configured for shared + backend modules
- [x] 7/16 BCs at 95%+ line coverage
- [x] 14/16 BCs at 90%+ line coverage
- [ ] All BCs at 95%+ line coverage
- [x] CI runs tests on every PR (GitHub Actions)
- [ ] Decision-matrix suite (NFR-12) passes against the GraalVM native binary, not only the JVM (TS-03)

---

## 10. Open Questions

- **Q-1**: Pact vs. OpenAPI-driven contract testing? — **Decision**: OpenAPI-driven (define API as OpenAPI spec, auto-generate tests)
- **Q-2**: Cucumber-JVM or Kotest BDD DSL for Gherkin? — **Decision**: Kotest BDD DSL (Kotlin-native given/when/then, scenarios in Kotlin)
- **Q-3**: Should BDD feature files live in `specs/features/` or `backend/src/test/resources/`? — **Decision**: `specs/features/` (co-located with spec docs, human-readable reference; Kotest BDD tests reference them)
- **Q-4**: Playwright vs. Selenium vs. Cypress for UI tests? — **Decision**: Playwright (recommended — best WASM/canvas support)
