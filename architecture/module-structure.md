# Module Structure — DDD Hexagonal + CQRS + DCB

**Status**: AGREED
**Last Updated**: 2026-06-12
**Depends On**: architecture/tech-stack.md

---

## 1. Overview

This spec defines the Kotlin Multiplatform module layout using **DDD Hexagonal Architecture** with **CQRS** (Command Query Responsibility Segregation) and **Dynamic Consistency Boundaries** (DCB).

The key architectural principles:

- **The domain is the hexagon core** — pure Kotlin, zero infrastructure dependencies
- **Events are the source of truth** — not database rows
- **Commands write via events**, queries read from projections (CQRS)
- **No fixed aggregates** — decision models are built dynamically from tagged events (DCB)
- **Ports define boundaries**, adapters implement them (hexagonal)

---

## 2. Module Dependency Graph

```
                 ┌────────────────┐
                 │     shared      │  Domain + Application
                 │  (commonMain)   │  Events, Commands, Handlers, Ports
                 └───────┬────────┘
                         │
                 ┌───────┴────────┐
                 │  designsystem   │  Tokens + Components
                 │  (all targets)  │  Colors, Typography, Atoms, Molecules, Organisms
                 └───────┬────────┘
                    ╱    │    ╲       ╲
                   ╱     │     ╲       ╲
         ┌────────┴┐ ┌──┴────┐ ┌┴──────┐ ┌──────────┐
         │   web    │ │desktop│ │android│ │   ios     │
         │ (wasmJs) │ │ (jvm) │ │       │ │(iosArm64)│
         └──────────┘ └───────┘ └───────┘ └──────────┘
         ▲ All share commonMain screens, state holders, and API adapters
         
         ┌─────────────┐
         │   backend    │  Infrastructure adapters (JVM only)
         │   (jvm)      │  No designsystem dependency
         └─────────────┘
```

### Module Responsibilities

| Module | Purpose | Targets |
|--------|---------|---------|
| `shared` | Domain core: events, commands, queries, decision models, ports, handlers, projections | commonMain → all |
| `designsystem` | UI tokens + components: CrmTheme, atoms, molecules, organisms, responsive layout | commonMain → wasmJs, jvm, android, iosArm64 |
| `frontend/common` | Shared screens, state holders (MVI), navigation, API adapters | commonMain → all UI targets |
| `frontend/web` | WASM entry point, browser-specific wiring | wasmJs |
| `frontend/desktop` | JVM desktop entry point, window management | jvm |
| `frontend/android` | Android Activity entry point, lifecycle | android |
| `frontend/ios` | iOS UIViewController entry point | iosArm64, iosSimulatorArm64 |
| `backend` | Infrastructure adapters: REST, EventStore, ReadModel DB, Ory integration | jvm |

### Rules
- `shared` has **no** dependencies on any UI module or `backend`
- `designsystem` depends only on Compose Foundation (no Material) — **no** `shared` dependency
- All frontend modules depend on both `shared` (domain models) and `designsystem` (UI components)
- `backend` depends on `shared` but **not** `designsystem`
- Frontend platform modules depend on `frontend/common` for shared screens/state

---

## 3. Shared Module — The Hexagon Core

**Build target**: `commonMain` → compiles to both `jvm` and `wasmJs`

This module contains the **domain** (events, commands, queries, value objects, decision models, ports) and the **application layer** (command handlers, query handlers, projections, read model shapes), organized by bounded context: walls, entitlements (enforcement; synced from the external subscription administration), experiments, analytics, and accounts.

> **Context boundary**: Subscription administration is an **external system**. The paywall's responsibility is (1) enforcing access and (2) activating/showing paywalls to drive conversion toward a subscription. It only **consumes** entitlement changes from the external subscription administration via integration events. Conversion steps (checkout_start/complete) are recorded as analytics events (AN-02) — checkout itself is a handoff to the external system, not owned here.

### Directory Layout

```
shared/
  build.gradle.kts
  src/
    commonMain/kotlin/nl/incedo/paywall/

      ── DOMAIN LAYER ──────────────────────────────────────────

      domain/
        event/                             # DOMAIN EVENTS — source of truth
          DomainEvent.kt                   # sealed interface
          EventMetadata.kt                 # id, timestamp, tags, position
          wall/
            WallCreated.kt
            WallConfigChanged.kt
            WallPublished.kt
            WallArchived.kt
          entitlement/                     # Integration events ingested from the external
                                           # subscription administration (AC-02/AC-08, EA-*)
            EntitlementGranted.kt
            EntitlementRevoked.kt
            GrantIssued.kt
            GrantRevoked.kt
          experiment/
            ExperimentDefined.kt
            VariantWeightChanged.kt
            ExperimentEnded.kt
          analytics/
            WallShown.kt
            GateCtaClicked.kt
            MeterIncremented.kt
            MeterReset.kt
          account/
            AccountLinked.kt
            AccountAuthenticated.kt
            AccountDeleted.kt
            RoleChanged.kt

        command/                            # COMMANDS — intent to change state
          wall/
            CreateWall.kt
            UpdateWallConfig.kt
            PublishWall.kt
            ArchiveWall.kt
          entitlement/
            RecordEntitlementChange.kt     # Ingestion: handled by the sync adapter receiving
                                           # webhooks/feeds from the external subscription system
            IssueGrant.kt
            RevokeGrant.kt
          experiment/
            DefineExperiment.kt
            UpdateExperiment.kt
            ChangeVariantWeights.kt
            EndExperiment.kt
          analytics/
            RecordWallShown.kt
            RecordGateCtaClick.kt
            RecordArticleRead.kt           # → MeterIncremented (MT-*)
            ResetMeter.kt
          account/
            LinkAccount.kt
            Authenticate.kt
            ChangeRole.kt
            DeleteAccount.kt

        query/                             # QUERIES — intent to read state
          wall/
            GetWall.kt
            ListWalls.kt
          entitlement/
            GetEntitlements.kt             # Per subject
            ListEntitlements.kt
          experiment/
            GetExperiment.kt
            ListExperiments.kt
            GetExperimentResults.kt
          analytics/
            GetMeterStatus.kt
            ListWallEvents.kt
          account/
            GetCurrentAccount.kt
            ListAccounts.kt

        model/                             # VALUE OBJECTS
          WallId.kt                        # @JvmInline value class
          PlanId.kt
          ExperimentId.kt
          VisitorId.kt
          UserId.kt                        # Maps to Ory `sub` claim
          SubscriptionId.kt                # Reference to the external subscription administration's id
          GrantId.kt
          ArticleId.kt
          Email.kt                         # Validated email format
          MeterLimit.kt                    # Validated positive article count
          Money.kt                         # amount: BigDecimal + currency: String
          WallStatus.kt                    # enum with transition rules (DRAFT → LIVE → ARCHIVED)
          WallType.kt                      # enum: HARD, METERED, FREEMIUM, DYNAMIC
          UserRole.kt                      # enum: ADMIN, EDITOR, ANALYST
          Tag.kt                           # Lowercase, deduplicated

        decision/                          # DECISION MODELS — ephemeral, built per command
          WallDecisionModel.kt             # Does wall exist? Current status/config?
          EntitlementDecision.kt           # valid entitlement for subject? (AC-02)
          ExperimentDecisionModel.kt       # Running? Valid weight change?
          GrantDecisionModel.kt            # Grant exists? Already revoked? (FGA-*)
          AccountDecisionModel.kt          # Linked? Current role?
          MeterLimitDecision.kt            # Has this visitor hit the meter limit? (MT-*)
          ActiveExperimentDecision.kt      # Is another experiment live on this wall? (cross-entity, EX-*)

        port/                              # PORTS — interfaces to the outside world
          EventStore.kt                    # query(tags, since), append(events, condition)
          ReadModelStore.kt                # Generic read model persistence
          EventSubscription.kt             # Subscribe to event stream
          PasswordHasher.kt               # Hash/verify passwords (infra concern)

        validation/                        # PURE VALIDATION FUNCTIONS
          WallValidation.kt
          EntitlementValidation.kt
          ExperimentValidation.kt
          AccountValidation.kt
          ValidationResult.kt             # sealed: Valid | Invalid(errors: List<ValidationError>)

      ── APPLICATION LAYER ─────────────────────────────────────

      application/
        command/                            # COMMAND HANDLERS — orchestrate writes
          WallCommandHandler.kt
          EntitlementCommandHandler.kt     # Ingests entitlement changes from the external system
          ExperimentCommandHandler.kt
          AnalyticsCommandHandler.kt
          AccountCommandHandler.kt

        query/                             # QUERY HANDLERS — orchestrate reads
          WallQueryHandler.kt
          EntitlementQueryHandler.kt
          ExperimentQueryHandler.kt
          AnalyticsQueryHandler.kt
          AccountQueryHandler.kt

        projection/                        # EVENT → READ MODEL transformations
          WallProjection.kt
          EntitlementProjection.kt
          ExperimentProjection.kt
          WallEventProjection.kt
          AccountProjection.kt

        readmodel/                         # READ MODEL SHAPES (query-optimized)
          WallView.kt                      # Denormalized: includes active experiment name
          EntitlementView.kt               # Subject, plan, validity, source
          ExperimentView.kt                # Includes: wall name, variants, weights
          ExperimentResultsView.kt         # Variants grouped with conversion metrics (EX-*)
          WallEventView.kt                 # Includes: wall + article names
          AccountView.kt                   # Excludes: password hash (Ory-managed)
          PaginatedResult.kt              # Generic pagination wrapper

    commonTest/kotlin/nl/incedo/paywall/
      domain/
        decision/
          WallDecisionModelTest.kt
          EntitlementDecisionTest.kt
          ExperimentDecisionModelTest.kt
          MeterLimitDecisionTest.kt
          ActiveExperimentDecisionTest.kt
        model/
          EmailTest.kt
          MoneyTest.kt
          WallStatusTest.kt
          TagTest.kt
        validation/
          WallValidationTest.kt
          EntitlementValidationTest.kt
          ExperimentValidationTest.kt
      application/
        command/
          WallCommandHandlerTest.kt
          EntitlementCommandHandlerTest.kt
          ExperimentCommandHandlerTest.kt
          AnalyticsCommandHandlerTest.kt
          AccountCommandHandlerTest.kt
        projection/
          WallProjectionTest.kt
          EntitlementProjectionTest.kt
          ExperimentProjectionTest.kt
```

### Conventions

**Events:**
- All events implement `sealed interface DomainEvent`
- Events are `@Serializable` data classes
- Every event carries `tags: Set<String>` for DCB queries
- Tags follow format: `"{entityType}:{entityId}"` (e.g., `"visitor:abc-123"`)
- Events with cross-entity relationships carry multiple tags (e.g., `ExperimentDefined` tags: `"experiment:{id}"`, `"wall:{wallId}"`; `MeterIncremented` tags: `"visitor:{visitorId}"`, `"article:{articleId}"`)

**Commands:**
- Data classes carrying the intent and all required data
- No side effects — pure data

**Decision Models:**
- Built by folding over queried events: `events.fold(initialState) { state, event -> state.apply(event) }`
- Ephemeral — created per command, never persisted
- Enforce domain invariants (business rules, e.g., the meter limit MT-*)
- Each declares which tags it needs to query

**Value Objects:**
- `@JvmInline value class` for IDs (type safety, zero overhead)
- `data class` for compound values (Money, Email)
- Validation in `init {}` blocks — invalid values cannot be constructed

**Ports:**
- All ports are `interface` in the domain
- No default implementations in the domain
- EventStore port is the primary write mechanism
- ReadModelStore port is the primary read mechanism

---

## 4. Backend Module — Infrastructure Adapters

**Build target**: JVM

### Directory Layout

```
backend/
  build.gradle.kts
  src/
    main/kotlin/nl/incedo/paywall/backend/

      adapter/
        inbound/                           # DRIVING ADAPTERS — trigger commands/queries
          rest/
            WallController.kt              # fun Route.wallRoutes(handler, queryHandler)
            EntitlementController.kt       # Read-only lookups + inbound sync/webhook endpoint
                                           # from the external subscription administration
            ExperimentController.kt
            AnalyticsController.kt
            AuthController.kt
            AccountController.kt
            dto/
              request/
                CreateWallRequest.kt       # REST-specific shapes
                UpdateWallConfigRequest.kt
                EntitlementChangeRequest.kt # Inbound integration payload
                DefineExperimentRequest.kt
                RecordArticleReadRequest.kt
                LoginRequest.kt
                LinkAccountRequest.kt
                ChangeRoleRequest.kt
              response/
                WallResponse.kt            # From WallView read model
                EntitlementResponse.kt
                ExperimentResponse.kt
                WallEventResponse.kt
                AccountResponse.kt
                ExperimentResultsResponse.kt
                PaginatedResponse.kt
                ErrorResponse.kt
            mapper/
              RequestMapper.kt             # REST DTO → domain Command
              ResponseMapper.kt            # Read Model → REST Response

        outbound/                          # DRIVEN ADAPTERS — implement domain ports
          eventstore/
            InMemoryEventStore.kt          # implements EventStore (dev/testing)
            PostgresEventStore.kt          # implements EventStore (production)
            table/
              EventsTable.kt              # id, type, data (JSONB), tags, position, timestamp
              EventTagsTable.kt           # event_id, tag — indexed for DCB queries
            EventSerializer.kt            # DomainEvent ↔ JSON

          readmodel/
            PostgresReadModelStore.kt      # implements ReadModelStore
            table/
              WallsReadTable.kt            # Denormalized wall view
              EntitlementsReadTable.kt
              ExperimentsReadTable.kt
              WallEventsReadTable.kt
              AccountsReadTable.kt

          projection/
            ProjectionRunner.kt            # Subscribes to events, dispatches to projections
            ProjectionPosition.kt          # Tracks last processed event per projection

          security/
            BcryptPasswordHasher.kt        # implements PasswordHasher port

      config/
        Application.kt                     # fun main(), embeddedServer(Ktor)
        DependencyInjection.kt             # Wire ports → adapters, construct handlers
        DatabaseConfig.kt
        EventStoreConfig.kt
        AuthConfig.kt
        CorsConfig.kt

    test/kotlin/nl/incedo/paywall/backend/
      adapter/
        inbound/rest/
          WallControllerTest.kt            # Ktor testApplication integration tests
          EntitlementControllerTest.kt
          ExperimentControllerTest.kt
          AnalyticsControllerTest.kt
          AuthControllerTest.kt
        outbound/eventstore/
          InMemoryEventStoreTest.kt
          PostgresEventStoreTest.kt
        outbound/projection/
          ProjectionRunnerTest.kt
      config/
        DependencyInjectionTest.kt         # Verify wiring

    resources/
      application.conf
    test/resources/
      application-test.conf

  Dockerfile
```

### Conventions

**REST Controllers:**
- Extension functions on `Route`: `fun Route.wallRoutes(commandHandler, queryHandler)`
- POST/PUT/DELETE endpoints dispatch **commands** to command handlers
- GET endpoints dispatch **queries** to query handlers
- Map REST DTOs to/from domain commands/read models via mappers

**Event Store Adapter:**
- `PostgresEventStore` stores events in a single `events` table with JSONB data
- `event_tags` join table enables tag-based queries (DCB)
- Append condition: `SELECT MAX(position) FROM events WHERE id IN (SELECT event_id FROM event_tags WHERE tag IN (?))` — if position > expected, reject (optimistic concurrency)
- `InMemoryEventStore` for tests — same interface, in-memory list

**Projection Runner:**
- Polls or subscribes to new events from the event store
- Dispatches each event to registered projections
- Tracks last processed position per projection (restart safety)

**Dependency Injection:**
- Manual constructor injection — no DI framework
- `DependencyInjection.kt` wires everything:
  ```kotlin
  val eventStore: EventStore = PostgresEventStore(db)
  val readModelStore: ReadModelStore = PostgresReadModelStore(db)
  val wallCommandHandler = WallCommandHandler(eventStore)
  val wallQueryHandler = WallQueryHandler(readModelStore)
  ```

---

## 5. Design System Module — Tokens + Components

**Build targets**: wasmJs, jvm, android, iosArm64, iosSimulatorArm64

See `architecture/design-system.md` for full token definitions and component catalog.

### Directory Layout

```
designsystem/
  build.gradle.kts
  src/
    commonMain/kotlin/nl/incedo/paywall/designsystem/
      theme/
        CrmTheme.kt                       # Theme provider + CompositionLocal wiring
        CrmColors.kt                      # 32+ color tokens (light/dark)
        CrmTypography.kt                  # 14 text styles
        CrmSpacing.kt                     # 9-step spacing scale
        CrmShapes.kt                      # Corner radius tokens
        CrmElevation.kt                   # Shadow depth tokens
        CrmAnimation.kt                   # Motion/duration tokens
        CrmBreakpoints.kt                 # WindowSizeClass (COMPACT → LARGE)
      foundation/                          # ATOMS — built on Compose Foundation only
        CrmText.kt                        # Wraps BasicText with color resolution
        CrmSurface.kt                     # Box + shape + color + LocalContentColor
        CrmIcon.kt                        # Wraps Image
        CrmDivider.kt                     # Thin Box with divider color
        CrmImage.kt                       # Image wrapper
      component/                           # MOLECULES
        CrmButton.kt                      # Surface + Row + CrmText
        CrmIconButton.kt
        CrmInputField.kt                  # BasicTextField + decoration
        CrmSelectField.kt                 # Input + Popup dropdown
        CrmBadge.kt                       # Status indicators (draft/live/archived)
        CrmTag.kt                         # Wall/experiment tags
        CrmChip.kt                        # Filter chips
        CrmAvatar.kt                      # Account avatars
        CrmTab.kt                         # Tab navigation
        CrmDialog.kt                      # Foundation Dialog + CrmSurface
        CrmSnackbar.kt                    # Notification bar
        CrmTooltip.kt                     # Hover info popup
      organism/                            # ORGANISMS — paywall-specific composites
        CrmNavBar.kt                      # Top navigation bar
        CrmSideNav.kt                     # Side navigation (desktop/tablet)
        CrmBottomNav.kt                   # Bottom navigation (mobile)
        CrmDataTable.kt                   # Sortable, paginated data table
        CrmKanbanBoard.kt                 # Drag-and-drop experiment variant board
        CrmDealCard.kt                    # Single variant card for kanban
        CrmContactCard.kt                 # Wall summary card
        CrmActivityItem.kt               # Single wall event in timeline
        CrmTimeline.kt                    # Wall event feed
        CrmSearchBar.kt                   # Search with debounce
        CrmFormSection.kt                 # Grouped form fields (wall editor)
        CrmEmptyState.kt                  # Empty list placeholder
        CrmConfirmDialog.kt              # Archive confirmation
      layout/
        CrmScaffold.kt                    # Responsive scaffold (adapts nav per platform)
        CrmResponsive.kt                  # WindowSizeClass helpers

    commonTest/kotlin/nl/incedo/paywall/designsystem/
      theme/
        CrmColorsTest.kt
        CrmTypographyTest.kt
      component/
        CrmButtonTest.kt
        CrmInputFieldTest.kt
```

### Conventions

- **Zero Material imports** — only Compose Foundation (`compose.foundation`, `compose.ui`)
- All components use `CrmTheme.*` tokens, never hardcoded values
- Color resolution chain: explicit parameter → TextStyle color → `LocalContentColor`
- Every component is a `@Composable` function, not a class

---

## 6. Frontend Modules — Multiplatform UI

**Targets**: Web (wasmJs), Desktop (jvm), Android, iOS (iosArm64, iosSimulatorArm64)

See `architecture/reactive-patterns.md` for MVI state management details.

### Directory Layout

```
frontend/
  common/                                  # SHARED UI — all screens, state, API adapters
    build.gradle.kts                       # KMP: commonMain → all UI targets
    src/
      commonMain/kotlin/nl/incedo/paywall/frontend/
        app/
          App.kt                           # Root @Composable (used by all platforms)
          Navigation.kt                    # Navigation graph (routes ↔ screens)
        screen/
          walls/
            WallsOverviewScreen.kt         # Uses CrmDataTable, CrmSearchBar (ADM-*)
            WallDesignerScreen.kt          # Uses CrmSurface, CrmTag, CrmFormSection
            WallFormScreen.kt              # Uses CrmInputField, CrmSelectField
          entitlements/
            SubjectInspectorScreen.kt      # Subject lookup in admin console (ADM-04)
            EntitlementPanel.kt            # Read-only entitlement inspection per subject
          experiments/
            ExperimentBoardScreen.kt       # Uses CrmKanbanBoard, CrmDealCard (EX-*)
            ExperimentDetailScreen.kt
            ExperimentFormScreen.kt
          analytics/
            WallEventTimelineScreen.kt     # Uses CrmTimeline, CrmActivityItem
            MeterStatusScreen.kt
          auth/
            LoginScreen.kt                 # OIDC redirect trigger
            CallbackScreen.kt             # OIDC callback handler
            ProfileScreen.kt
          admin/
            AccountListScreen.kt
            AccountFormScreen.kt
          dashboard/
            DashboardScreen.kt
        state/                             # MVI StateHolders (platform-agnostic)
          walls/
            WallsOverviewState.kt          # State data class
            WallsOverviewIntent.kt         # Sealed interface of intents
            WallsOverviewEffect.kt         # Side effects (navigation, snackbar)
            WallsOverviewStateHolder.kt    # StateFlow + intent handling
          entitlements/
            EntitlementPanelState.kt
            EntitlementPanelStateHolder.kt
          experiments/
            ExperimentBoardState.kt
            ExperimentBoardStateHolder.kt
            ExperimentFormState.kt
            ExperimentFormStateHolder.kt
          analytics/
            WallEventTimelineState.kt
            WallEventTimelineStateHolder.kt
          auth/
            AuthState.kt
            AuthStateHolder.kt
        api/                               # Outbound adapter — HTTP client
          ApiClient.kt                     # Ktor HttpClient setup (expect/actual per platform)
          WallApi.kt                       # suspend fun listWalls(), createWall(), etc.
          EntitlementApi.kt
          ExperimentApi.kt
          AnalyticsApi.kt
          AuthApi.kt

  web/                                     # WEB entry point (wasmJs)
    build.gradle.kts
    src/
      wasmJsMain/kotlin/nl/incedo/paywall/frontend/web/
        Main.kt                            # fun main() { renderComposable("root") { App() } }
      wasmJsMain/resources/
        index.html                         # WASM host page

  desktop/                                 # DESKTOP entry point (jvm)
    build.gradle.kts
    src/
      jvmMain/kotlin/nl/incedo/paywall/frontend/desktop/
        Main.kt                            # application { Window(title = "Paywall") { App() } }

  android/                                 # ANDROID entry point
    build.gradle.kts                       # Android application plugin
    src/
      androidMain/kotlin/nl/incedo/paywall/frontend/android/
        MainActivity.kt                   # setContent { CrmTheme { App() } }
        PaywallApplication.kt             # Application class (DI setup)
      androidMain/AndroidManifest.xml

  ios/                                     # iOS entry point
    build.gradle.kts
    src/
      iosMain/kotlin/nl/incedo/paywall/frontend/ios/
        MainViewController.kt             # fun MainViewController() = ComposeUIViewController { App() }
    iosApp/                                # Xcode project wrapper
      iosApp/
        ContentView.swift                  # Hosts ComposeUIViewController
        Info.plist
```

### Platform-Specific Concerns

| Concern | Web (wasmJs) | Desktop (jvm) | Android | iOS |
|---------|-------------|---------------|---------|-----|
| Entry point | `renderComposable` | `application { Window }` | `setContent` | `ComposeUIViewController` |
| HTTP client engine | `Js` | `CIO` or `OkHttp` | `OkHttp` | `Darwin` |
| OIDC flow | Browser redirect | System browser / embedded | Chrome Custom Tabs | ASWebAuthenticationSession |
| File system | Browser download | File picker dialog | SAF / MediaStore | UIDocumentPicker |
| Notifications | Web Notifications API | System tray | Firebase / local | APNs / local |
| Deep linking | URL routing | CLI args | Intent filters | Universal Links |
| Window size | `window.innerWidth` | `WindowState` | `Configuration` | `UIScreen.main.bounds` |

### Conventions

- **Screens are pure composables** — they receive state and emit intents, no side effects
- **StateHolders are in `frontend/common`** — pure Kotlin, no platform imports
- **API adapters use `expect/actual`** only for HTTP client engine selection
- **Platform modules are thin** — just entry point + platform-specific wiring
- **All UI uses design system components** — never raw Foundation composables in screens

---

## 7. Gradle Configuration

### `settings.gradle.kts`
```kotlin
rootProject.name = "paywall"

// Domain + Application
include(":shared")

// UI
include(":designsystem")
include(":frontend:common")
include(":frontend:web")
include(":frontend:desktop")
include(":frontend:android")
include(":frontend:ios")

// Backend
include(":backend")
```

### Shared module (domain core — all targets)
```kotlin
// shared/build.gradle.kts
kotlin {
    jvm()
    wasmJs { browser() }
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
```

### Design system module (UI tokens + components — all UI targets)
```kotlin
// designsystem/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()
    wasmJs { browser() }
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            // NO compose.material3 — custom design system
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
```

### Frontend common module (shared screens + state — all UI targets)
```kotlin
// frontend/common/build.gradle.kts
kotlin {
    jvm()
    wasmJs { browser() }
    androidTarget()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":designsystem"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }
        // Platform-specific HTTP client engines
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
```

### Frontend web (wasmJs entry point)
```kotlin
// frontend/web/build.gradle.kts
kotlin {
    wasmJs { browser(); binaries.executable() }
    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":frontend:common"))
        }
    }
}
```

### Frontend desktop (jvm entry point)
```kotlin
// frontend/desktop/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(project(":frontend:common"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "nl.incedo.paywall.frontend.desktop.MainKt"
    }
}
```

### Frontend android
```kotlin
// frontend/android/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "nl.incedo.paywall.frontend.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "nl.incedo.paywall.frontend.android"
        minSdk = 26
        targetSdk = 35
    }
}

dependencies {
    implementation(project(":frontend:common"))
    implementation(libs.activity.compose)
}
```

### Frontend iOS
```kotlin
// frontend/ios/build.gradle.kts
kotlin {
    iosArm64 { binaries.framework { baseName = "PaywallApp" } }
    iosSimulatorArm64 { binaries.framework { baseName = "PaywallApp" } }

    sourceSets {
        iosMain.dependencies {
            implementation(project(":frontend:common"))
        }
    }
}
```

### Backend module (JVM only — no UI dependencies)
```kotlin
// backend/build.gradle.kts
dependencies {
    implementation(project(":shared"))
    // NO dependency on :designsystem or :frontend
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.cucumber.java)
    testImplementation(libs.cucumber.junit5)
}
```

---

## 8. Command Handler Flow (DCB Pattern)

This shows how a command flows through the system:

```
REST Request
    │
    ▼
┌──────────────┐
│  Controller   │  Maps REST DTO → domain Command
│  (inbound     │
│   adapter)    │
└──────┬───────┘
       │ command
       ▼
┌──────────────┐
│  Command      │  1. Validate command fields (validation/)
│  Handler      │  2. Query EventStore for decision events (by tags)
│  (application)│  3. Build DecisionModel from events
└──────┬───────┘  4. Enforce invariants via DecisionModel
       │          5. Create new DomainEvent(s)
       │          6. Append to EventStore with AppendCondition
       │
       ├──────────────► EventStore.append(events, condition)
       │                    │
       │                    ▼
       │               ┌──────────┐
       │               │  Event    │  Persists events + tags
       │               │  Store    │  Checks append condition
       │               │  Adapter  │  (optimistic concurrency)
       │               └──────────┘
       │
       ▼
  Return success/failure to controller
```

Meanwhile, asynchronously:

```
┌──────────────┐     subscribes     ┌──────────────┐
│  Projection   │◄──────────────────│  Event Store   │
│  Runner       │    new events     │               │
└──────┬───────┘                    └──────────────┘
       │
       ▼
┌──────────────┐
│  Projection   │  Updates read model tables
│  (application)│  (denormalized views)
└──────────────┘
```

---

## 9. Completion Criteria

### 9a. Module Structure
> **Current state (2026-06-14)**: CLAUDE.md says "grow toward the module split, don't
> pre-build it." The monolith layout is `shared` + `composeApp` + `backend`.
> Design system lives inside `composeApp/src/commonMain/…/theme` + `/ui`.
> The `designsystem`, `frontend/*` modules are the target end state.
- [x] `shared` compiles — jvm + wasmJs targets in settings.gradle.kts
- [x] `composeApp` compiles — jvm + wasmJs (houses design system + screens until module split)
- [x] `backend` compiles — jvm only
- [x] Domain layer has zero infrastructure imports
- [x] Design system has zero Material imports (Foundation only)
- [x] Port interfaces exist for EventStore, ReadModelStore, PasswordHasher
- [ ] `designsystem` as separate module — deferred (target state; grow toward it)
- [ ] `frontend/common`, `frontend/web`, `frontend/desktop` — deferred (target state)
- [ ] `frontend/android` — requires Android toolchain (deferred)
- [ ] `frontend/ios` — requires Xcode (deferred)

### 9b. CQRS/DCB Verification
- [x] A sample command handler queries events by tags, builds decision model, appends events
- [x] A sample projection consumes events and updates a read model
- [x] A sample query handler reads from the read model
- [x] AppendCondition enforces optimistic concurrency in EventStore adapter
- [x] InMemoryEventStore passes basic DCB tests

### 9c. Hexagonal Verification
- [x] REST controller only calls command/query handlers (never touches EventStore directly)
- [x] Command handler only uses domain ports (never imports backend adapter classes)
- [x] Screens only use design system components (never raw Foundation composables)

### 9d. Multiplatform Verification
- [x] `./gradlew :shared:jvmTest` passes (jvm target verified in CI)
- [x] `./gradlew :shared:compileKotlinWasmJs` succeeds
- [x] `./gradlew :composeApp:compileKotlinJvm` succeeds — desktop app compiles
- [x] `./gradlew :composeApp:compileKotlinWasmJs` succeeds — WASM SPA compiles
- [x] `./gradlew :backend:test` passes
- [x] HTTP client engine: CIO for jvmMain, Js for wasmJsMain (wired in composeApp)
- [ ] `./gradlew :shared:allTests` on all targets — deferred (no Android/iOS toolchain)
- [ ] `./gradlew :frontend:android:assembleDebug` — deferred (no android module yet)
- [ ] `./gradlew :frontend:ios:linkDebugFrameworkIosSimulatorArm64` — deferred (no ios module)

---

## 10. Open Questions

- **Q-1**: Event Store implementation — PostgreSQL-based custom, Axon Server, or EventStoreDB? — **Decision**: PostgreSQL custom
- **Q-2**: Projection strategy — sync (in-process after append) or async (separate subscription)? — **Decision**: Sync in-process
- **Q-3**: Navigation library — Compose Navigation Multiplatform, Voyager, or Decompose? — **Decision**: Decompose (lifecycle-aware, supports all KMP targets)
- **Q-5**: Android min SDK — 26 (Android 8.0) or higher? — **Decision**: 26 (Android 8.0)
