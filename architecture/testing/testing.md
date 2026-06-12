# Testing Strategy

**Status**: AGREED
**Last Updated**: 2026-04-16
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

- **One test class per production class**: `ContactDecisionModel` → `ContactDecisionModelTest`
- **Test method naming**: `fun testBR1_rejectsBlankFirstName()`, `fun testCreateContact_emitsContactCreatedEvent()`
- **Use InMemoryEventStore** for command handler tests — no real DB
- **Use InMemoryReadModelStore** for query handler tests
- **Arrange-Act-Assert** pattern, no mocking frameworks — prefer simple fakes
- **Each business rule (BR-N)** gets at least one dedicated test

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

1. **Consumer side** (frontend tests): Define expectations — "when I POST to /api/v1/contacts with this body, I expect 201 with this shape"
2. **Contract artifact**: Generated pact file or OpenAPI spec
3. **Producer side** (backend tests): Verify the real API matches the contract

### What Contract Tests Cover

| Contract | Consumer Expects | Producer Verifies |
|----------|-----------------|-------------------|
| Create Contact | POST /api/v1/contacts → 201 `{ contactId }` | Endpoint returns correct shape |
| Create Contact (invalid) | POST /api/v1/contacts → 400 `{ error }` | Error shape matches |
| List Contacts | GET /api/v1/contacts → 200 `{ data, pagination }` | Pagination shape matches |
| Get Contact | GET /api/v1/contacts/{id} → 200 `ContactView` | All fields present |
| Get Contact (missing) | GET /api/v1/contacts/{id} → 404 `{ error }` | 404 error shape matches |
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
  contacts.feature
  companies.feature
  deals.feature
  activities.feature
  auth.feature
  pipeline.feature                       # Cross-entity flow: deal through pipeline

backend/src/test/kotlin/crm/backend/
  bdd/
    steps/
      ContactSteps.kt                   # Step definitions for contact scenarios
      CompanySteps.kt
      DealSteps.kt
      ActivitySteps.kt
      AuthSteps.kt
      CommonSteps.kt                    # Shared steps (authentication, assertions)
    CucumberRunner.kt                   # JUnit5 Cucumber runner
    TestWorld.kt                        # Shared state across steps (Ktor test client, tokens)
```

### Example Feature File

```gherkin
# specs/features/contacts.feature

Feature: Contact Management
  As a CRM user
  I want to manage contacts
  So that I can track relationships with people

  Background:
    Given I am authenticated as an "ADMIN" user

  Scenario: Create a new contact
    When I create a contact with:
      | firstName | lastName | email              |
      | John      | Doe      | john@example.com   |
    Then the response status is 201
    And the response contains a "contactId"

  Scenario: Cannot create contact with duplicate email
    Given a contact exists with email "jane@example.com"
    When I create a contact with:
      | firstName | lastName | email              |
      | Another   | Person   | jane@example.com   |
    Then the response status is 409
    And the error code is "UNIQUENESS_VIOLATION"

  Scenario: List contacts with pagination
    Given 25 contacts exist
    When I list contacts with page 1 and size 10
    Then the response contains 10 contacts
    And the pagination total is 25

  Scenario: Delete contact removes deal associations
    Given a contact "John Doe" exists
    And a deal "Big Sale" is associated with "John Doe"
    When I delete contact "John Doe"
    Then the contact is deleted
    And the deal "Big Sale" still exists
    But "John Doe" is no longer associated with "Big Sale"
```

### Main Flow Scenarios (Required)

Each entity must have BDD scenarios for these main flows:

| Flow | Scenarios |
|------|-----------|
| **CRUD** | Create, read (single + list), update, delete |
| **Validation** | Reject invalid input per each business rule |
| **Uniqueness** | Detect and reject duplicates (DCB decision model) |
| **Cross-entity** | Create with references, cascade behavior on delete |
| **Auth** | Protected endpoints require token, role-based access |
| **Pipeline** (Deals) | Move through stages, close deal, kanban view data |
| **Timeline** (Activities) | Create activities linked to entities, complete tasks |

### Gradle Command
```bash
./gradlew :backend:test --tests '*Cucumber*'
# Or if separate module:
./gradlew :bdd:test
```

---

## 5. Component Workbench (Storybook)

**Scope**: Visual testing and documentation of all design system components and CRM screens in isolation, across themes, viewports, and interaction states.

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
val buttonStories = storyGroup("Button") {
    story("Primary") { CrmButton(text = "Save", onClick = {}) }
    story("Secondary") { CrmButton(text = "Cancel", onClick = {}, variant = SECONDARY) }
    story("Danger") { CrmButton(text = "Delete", onClick = {}, variant = DANGER) }
    story("Disabled") { CrmButton(text = "Disabled", onClick = {}, enabled = false) }
    story("With Icon") { CrmButton(text = "Add", onClick = {}, leadingIcon = { CrmIcon(glyph = "+") }) }
}
```

### Scenarios (State Variations)

Each story can have **Scenarios** — predefined data/state configurations:

| Scenario | Purpose | Example |
|----------|---------|---------|
| **Default** | Happy path | Contact form with valid data |
| **Loading** | Async in-progress | Spinner + skeleton in table |
| **Error** | Validation/network failure | Form with error messages |
| **Empty** | No data | Empty contact list with EmptyState |
| **Long Content** | Overflow/truncation | Contact name with 200 characters |
| **Disabled** | Non-interactive | All inputs disabled |
| **RTL** | Right-to-left layout | Arabic text direction (future) |

### Controls (Runtime Interaction)

```kotlin
story("InputField") {
    val label = textControl("Label", default = "Email")
    val error = textControl("Error", default = "")
    val enabled = booleanControl("Enabled", default = true)

    CrmInputField(
        value = "", onValueChange = {},
        label = label.value,
        error = error.value.ifEmpty { null },
        enabled = enabled.value,
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
| **ThemeDecorator** | Wraps in `CrmTheme(theme = ...)` |
| **ScaffoldDecorator** | Wraps in `CrmScaffold` with nav |
| **PaddingDecorator** | Adds consistent padding |
| **FakeDataDecorator** | Provides fake read models / API responses |

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
    contacts.spec.ts                 # Contact CRUD flows
    companies.spec.ts                # Company CRUD flows
    deals.spec.ts                    # Deal pipeline flows
    activities.spec.ts               # Activity timeline flows
    dashboard.spec.ts                # Dashboard rendering
  fixtures/
    auth.fixture.ts                  # Login helper, authenticated context
  helpers/
    api.helper.ts                    # Direct API calls for test setup
```

### Main Flow UI Tests (Required)

| Flow | Test |
|------|------|
| **Login** | Navigate to app → redirected to Ory login → enter credentials → redirected back → see dashboard |
| **Create Contact** | Click "New Contact" → fill form → save → see detail view with correct data |
| **Search & Filter** | Type in search bar → list filters → results match |
| **Edit Contact** | Open contact → click edit → change fields → save → changes persisted |
| **Delete Contact** | Open contact → click delete → confirm → contact removed from list |
| **Deal Pipeline** | View kanban → drag deal card to next stage → stage updated |
| **Close Deal** | Open deal → click close → select won/lost → deal moves to closed column |
| **Activity Timeline** | Create activity → see it in timeline → mark task complete |
| **Role-Based Access** | Login as USER → admin menu hidden. Login as ADMIN → admin menu visible |
| **OIDC Flow** | Full redirect → Kratos login → Hydra token → app authenticated |

### Test Environment
- Tests run against the full K8s stack (Rancher Desktop) or Docker Compose
- A `beforeAll` hook ensures the stack is up and seeds test data
- Tests use API helpers to create data directly (faster than UI for setup)
- Each test cleans up after itself or uses isolated test data

### Commands
```bash
cd e2e && npx playwright test                    # Run all UI tests
cd e2e && npx playwright test --headed           # Run with browser visible
cd e2e && npx playwright test contacts.spec.ts   # Run single spec
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
async function createContact(data: ContactInput): Promise<string> {
  const response = await request.post('/api/v1/contacts', { data });
  return response.json().contactId;
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
2. **Variant stories** — one per enum variant (e.g., Button: Primary/Secondary/Danger/Ghost)
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

---

## 10. Open Questions

- **Q-1**: Pact vs. OpenAPI-driven contract testing? — **Decision**: OpenAPI-driven (define API as OpenAPI spec, auto-generate tests)
- **Q-2**: Cucumber-JVM or Kotest BDD DSL for Gherkin? — **Decision**: Kotest BDD DSL (Kotlin-native given/when/then, scenarios in Kotlin)
- **Q-3**: Should BDD feature files live in `specs/features/` or `backend/src/test/resources/`? — **Decision**: `specs/features/` (co-located with spec docs, human-readable reference; Kotest BDD tests reference them)
- **Q-4**: Playwright vs. Selenium vs. Cypress for UI tests? — **Decision**: Playwright (recommended — best WASM/canvas support)
