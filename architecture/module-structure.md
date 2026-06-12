# Module Structure — DDD Hexagonal + CQRS + DCB

**Status**: AGREED
**Last Updated**: 2026-04-09
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

This module contains the **domain** (events, commands, queries, value objects, decision models, ports) and the **application layer** (command handlers, query handlers, projections, read model shapes).

### Directory Layout

```
shared/
  build.gradle.kts
  src/
    commonMain/kotlin/crm/

      ── DOMAIN LAYER ──────────────────────────────────────────

      domain/
        event/                             # DOMAIN EVENTS — source of truth
          DomainEvent.kt                   # sealed interface
          EventMetadata.kt                 # id, timestamp, tags, position
          contact/
            ContactCreated.kt
            ContactUpdated.kt
            ContactDeleted.kt
          company/
            CompanyCreated.kt
            CompanyUpdated.kt
            CompanyDeleted.kt
          deal/
            DealCreated.kt
            DealStageChanged.kt
            DealClosed.kt
          activity/
            ActivityCreated.kt
            ActivityCompleted.kt
          user/
            UserCreated.kt
            UserAuthenticated.kt
            UserDeactivated.kt
            PasswordChanged.kt

        command/                            # COMMANDS — intent to change state
          contact/
            CreateContact.kt
            UpdateContact.kt
            DeleteContact.kt
          company/
            CreateCompany.kt
            UpdateCompany.kt
            DeleteCompany.kt
          deal/
            CreateDeal.kt
            UpdateDeal.kt
            ChangeDealStage.kt
            CloseDeal.kt
          activity/
            CreateActivity.kt
            CompleteActivity.kt
          user/
            CreateUser.kt
            Authenticate.kt
            ChangePassword.kt
            DeactivateUser.kt

        query/                             # QUERIES — intent to read state
          contact/
            GetContact.kt
            ListContacts.kt
          company/
            GetCompany.kt
            ListCompanies.kt
          deal/
            GetDeal.kt
            ListDeals.kt
            GetPipeline.kt
          activity/
            GetActivity.kt
            ListActivities.kt
          user/
            GetCurrentUser.kt
            ListUsers.kt

        model/                             # VALUE OBJECTS
          ContactId.kt                     # @JvmInline value class
          CompanyId.kt
          DealId.kt
          ActivityId.kt
          UserId.kt
          Email.kt                         # Validated email format
          PhoneNumber.kt
          Money.kt                         # amount: BigDecimal + currency: String
          DealStage.kt                     # enum with transition rules
          ActivityType.kt                  # enum: CALL, EMAIL, MEETING, TASK, NOTE
          UserRole.kt                      # enum: ADMIN, MANAGER, USER
          Tag.kt                           # Lowercase, deduplicated

        decision/                          # DECISION MODELS — ephemeral, built per command
          ContactDecisionModel.kt          # Does contact exist? Current state?
          CompanyDecisionModel.kt          # Does company exist? Has dependents?
          DealDecisionModel.kt             # Current stage? Valid transition?
          ActivityDecisionModel.kt         # Exists? Type check?
          UserDecisionModel.kt             # Active? Current role?
          EmailUniquenessDecision.kt       # Is this email already taken? (cross-entity)
          CompanyNameUniquenessDecision.kt # Is this company name taken? (cross-entity)

        port/                              # PORTS — interfaces to the outside world
          EventStore.kt                    # query(tags, since), append(events, condition)
          ReadModelStore.kt                # Generic read model persistence
          EventSubscription.kt             # Subscribe to event stream
          PasswordHasher.kt               # Hash/verify passwords (infra concern)

        validation/                        # PURE VALIDATION FUNCTIONS
          ContactValidation.kt
          CompanyValidation.kt
          DealValidation.kt
          UserValidation.kt
          ValidationResult.kt             # sealed: Valid | Invalid(errors: List<ValidationError>)

      ── APPLICATION LAYER ─────────────────────────────────────

      application/
        command/                            # COMMAND HANDLERS — orchestrate writes
          ContactCommandHandler.kt
          CompanyCommandHandler.kt
          DealCommandHandler.kt
          ActivityCommandHandler.kt
          UserCommandHandler.kt

        query/                             # QUERY HANDLERS — orchestrate reads
          ContactQueryHandler.kt
          CompanyQueryHandler.kt
          DealQueryHandler.kt
          ActivityQueryHandler.kt
          UserQueryHandler.kt

        projection/                        # EVENT → READ MODEL transformations
          ContactProjection.kt
          CompanyProjection.kt
          DealProjection.kt
          ActivityProjection.kt
          UserProjection.kt

        readmodel/                         # READ MODEL SHAPES (query-optimized)
          ContactView.kt                   # Denormalized: includes company name
          CompanyView.kt                   # Includes: contact count, deal count
          DealView.kt                      # Includes: company name, contacts, stage
          DealPipelineView.kt              # Deals grouped by stage (kanban data)
          ActivityView.kt                  # Includes: related entity names
          UserView.kt                      # Excludes: password hash
          PaginatedResult.kt              # Generic pagination wrapper

    commonTest/kotlin/crm/
      domain/
        decision/
          ContactDecisionModelTest.kt
          CompanyDecisionModelTest.kt
          DealDecisionModelTest.kt
          EmailUniquenessDecisionTest.kt
          CompanyNameUniquenessDecisionTest.kt
        model/
          EmailTest.kt
          MoneyTest.kt
          DealStageTest.kt
          TagTest.kt
        validation/
          ContactValidationTest.kt
          CompanyValidationTest.kt
          DealValidationTest.kt
      application/
        command/
          ContactCommandHandlerTest.kt
          CompanyCommandHandlerTest.kt
          DealCommandHandlerTest.kt
          ActivityCommandHandlerTest.kt
          UserCommandHandlerTest.kt
        projection/
          ContactProjectionTest.kt
          CompanyProjectionTest.kt
          DealProjectionTest.kt
```

### Conventions

**Events:**
- All events implement `sealed interface DomainEvent`
- Events are `@Serializable` data classes
- Every event carries `tags: Set<String>` for DCB queries
- Tags follow format: `"{entityType}:{entityId}"` (e.g., `"contact:abc-123"`)
- Events with cross-entity relationships carry multiple tags (e.g., `DealCreated` tags: `"deal:{id}"`, `"company:{companyId}"`, `"contact:{contactId}"`)

**Commands:**
- Data classes carrying the intent and all required data
- No side effects — pure data

**Decision Models:**
- Built by folding over queried events: `events.fold(initialState) { state, event -> state.apply(event) }`
- Ephemeral — created per command, never persisted
- Enforce domain invariants (business rules)
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
    main/kotlin/crm/backend/

      adapter/
        inbound/                           # DRIVING ADAPTERS — trigger commands/queries
          rest/
            ContactController.kt           # fun Route.contactRoutes(handler, queryHandler)
            CompanyController.kt
            DealController.kt
            ActivityController.kt
            AuthController.kt
            UserController.kt
            dto/
              request/
                CreateContactRequest.kt    # REST-specific shapes
                UpdateContactRequest.kt
                CreateCompanyRequest.kt
                CreateDealRequest.kt
                CreateActivityRequest.kt
                LoginRequest.kt
                CreateUserRequest.kt
                ChangePasswordRequest.kt
              response/
                ContactResponse.kt         # From ContactView read model
                CompanyResponse.kt
                DealResponse.kt
                ActivityResponse.kt
                UserResponse.kt
                PipelineResponse.kt
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
              ContactsReadTable.kt         # Denormalized contact view
              CompaniesReadTable.kt
              DealsReadTable.kt
              ActivitiesReadTable.kt
              UsersReadTable.kt

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

    test/kotlin/crm/backend/
      adapter/
        inbound/rest/
          ContactControllerTest.kt         # Ktor testApplication integration tests
          CompanyControllerTest.kt
          DealControllerTest.kt
          ActivityControllerTest.kt
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
- Extension functions on `Route`: `fun Route.contactRoutes(commandHandler, queryHandler)`
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
  val contactCommandHandler = ContactCommandHandler(eventStore)
  val contactQueryHandler = ContactQueryHandler(readModelStore)
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
    commonMain/kotlin/crm/designsystem/
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
        CrmBadge.kt                       # Status indicators
        CrmTag.kt                         # Contact/deal tags
        CrmChip.kt                        # Filter chips
        CrmAvatar.kt                      # User/contact avatars
        CrmTab.kt                         # Tab navigation
        CrmDialog.kt                      # Foundation Dialog + CrmSurface
        CrmSnackbar.kt                    # Notification bar
        CrmTooltip.kt                     # Hover info popup
      organism/                            # ORGANISMS — CRM-specific composites
        CrmNavBar.kt                      # Top navigation bar
        CrmSideNav.kt                     # Side navigation (desktop/tablet)
        CrmBottomNav.kt                   # Bottom navigation (mobile)
        CrmDataTable.kt                   # Sortable, paginated data table
        CrmKanbanBoard.kt                 # Drag-and-drop deal pipeline
        CrmDealCard.kt                    # Single deal card for kanban
        CrmContactCard.kt                 # Contact summary card
        CrmActivityItem.kt               # Single activity in timeline
        CrmTimeline.kt                    # Activity feed
        CrmSearchBar.kt                   # Search with debounce
        CrmFormSection.kt                 # Grouped form fields
        CrmEmptyState.kt                  # Empty list placeholder
        CrmConfirmDialog.kt              # Delete confirmation
      layout/
        CrmScaffold.kt                    # Responsive scaffold (adapts nav per platform)
        CrmResponsive.kt                  # WindowSizeClass helpers

    commonTest/kotlin/crm/designsystem/
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
      commonMain/kotlin/crm/frontend/
        app/
          App.kt                           # Root @Composable (used by all platforms)
          Navigation.kt                    # Navigation graph (routes ↔ screens)
        screen/
          contacts/
            ContactListScreen.kt           # Uses CrmDataTable, CrmSearchBar
            ContactDetailScreen.kt         # Uses CrmSurface, CrmTag, CrmAvatar
            ContactFormScreen.kt           # Uses CrmInputField, CrmSelectField
          companies/
            CompanyListScreen.kt
            CompanyDetailScreen.kt
            CompanyFormScreen.kt
          deals/
            DealPipelineScreen.kt          # Uses CrmKanbanBoard, CrmDealCard
            DealDetailScreen.kt
            DealFormScreen.kt
          activities/
            ActivityTimelineScreen.kt      # Uses CrmTimeline, CrmActivityItem
            ActivityFormScreen.kt
          auth/
            LoginScreen.kt                 # OIDC redirect trigger
            CallbackScreen.kt             # OIDC callback handler
            ProfileScreen.kt
          admin/
            UserListScreen.kt
            UserFormScreen.kt
          dashboard/
            DashboardScreen.kt
        state/                             # MVI StateHolders (platform-agnostic)
          contacts/
            ContactListState.kt            # State data class
            ContactListIntent.kt           # Sealed interface of intents
            ContactListEffect.kt           # Side effects (navigation, snackbar)
            ContactListStateHolder.kt      # StateFlow + intent handling
          companies/
            CompanyListState.kt
            CompanyListStateHolder.kt
          deals/
            PipelineState.kt
            PipelineStateHolder.kt
            DealFormState.kt
            DealFormStateHolder.kt
          activities/
            ActivityTimelineState.kt
            ActivityTimelineStateHolder.kt
          auth/
            AuthState.kt
            AuthStateHolder.kt
        api/                               # Outbound adapter — HTTP client
          ApiClient.kt                     # Ktor HttpClient setup (expect/actual per platform)
          ContactApi.kt                    # suspend fun listContacts(), createContact(), etc.
          CompanyApi.kt
          DealApi.kt
          ActivityApi.kt
          AuthApi.kt

  web/                                     # WEB entry point (wasmJs)
    build.gradle.kts
    src/
      wasmJsMain/kotlin/crm/frontend/web/
        Main.kt                            # fun main() { renderComposable("root") { App() } }
      wasmJsMain/resources/
        index.html                         # WASM host page

  desktop/                                 # DESKTOP entry point (jvm)
    build.gradle.kts
    src/
      jvmMain/kotlin/crm/frontend/desktop/
        Main.kt                            # application { Window(title = "CRM") { App() } }

  android/                                 # ANDROID entry point
    build.gradle.kts                       # Android application plugin
    src/
      androidMain/kotlin/crm/frontend/android/
        MainActivity.kt                   # setContent { CrmTheme { App() } }
        CrmApplication.kt                 # Application class (DI setup)
      androidMain/AndroidManifest.xml

  ios/                                     # iOS entry point
    build.gradle.kts
    src/
      iosMain/kotlin/crm/frontend/ios/
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
rootProject.name = "crm"

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
        mainClass = "crm.frontend.desktop.MainKt"
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
    namespace = "crm.frontend.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "crm.frontend.android"
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
    iosArm64 { binaries.framework { baseName = "CrmApp" } }
    iosSimulatorArm64 { binaries.framework { baseName = "CrmApp" } }

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

## 7. Command Handler Flow (DCB Pattern)

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
- [x] All modules listed in settings.gradle.kts compile
- [x] `shared` targets: jvm, wasmJs, android, iosArm64, iosSimulatorArm64
- [x] `designsystem` targets: jvm, wasmJs, android, iosArm64, iosSimulatorArm64
- [x] `frontend/common` targets: jvm, wasmJs, android, iosArm64, iosSimulatorArm64
- [x] `frontend/web` targets: wasmJs — produces executable WASM
- [x] `frontend/desktop` targets: jvm — produces runnable desktop app
- [ ] `frontend/android` — produces installable APK
- [ ] `frontend/ios` — produces framework linkable from Xcode
- [x] `backend` targets: jvm only
- [x] Domain layer has zero infrastructure imports
- [x] Design system has zero Material imports
- [x] Port interfaces exist for EventStore, ReadModelStore, PasswordHasher

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
- [ ] `./gradlew :shared:allTests` passes on all targets
- [ ] `./gradlew :designsystem:allTests` passes on all targets
- [ ] `./gradlew :frontend:web:wasmJsBrowserDistribution` produces WASM output
- [ ] `./gradlew :frontend:desktop:run` launches desktop window
- [ ] `./gradlew :frontend:android:assembleDebug` produces APK
- [ ] `./gradlew :frontend:ios:linkDebugFrameworkIosSimulatorArm64` produces framework
- [x] `./gradlew :backend:test` passes
- [ ] HTTP client engine resolves correctly per platform (CIO/Js/OkHttp/Darwin)

---

## 10. Open Questions

- **Q-1**: Event Store implementation — PostgreSQL-based custom, Axon Server, or EventStoreDB? — **Decision**: PostgreSQL custom
- **Q-2**: Projection strategy — sync (in-process after append) or async (separate subscription)? — **Decision**: Sync in-process
- **Q-3**: Navigation library — Compose Navigation Multiplatform, Voyager, or Decompose? — **Decision**: Decompose (lifecycle-aware, supports all KMP targets)
- **Q-5**: Android min SDK — 26 (Android 8.0) or higher? — **Decision**: 26 (Android 8.0)
