# Tech Stack

**Status**: AGREED
**Last Updated**: 2026-04-09
**Depends On**: None

---

## 1. Overview

This spec documents the technology choices for the CRM and the rationale behind each decision. It also captures open architectural questions that need resolution before implementation begins.

---

## 2. Kotlin Multiplatform (KMP)

**Version**: Kotlin 2.1+ (latest stable)

KMP enables sharing domain logic across the backend (JVM) and frontend (WASM) using a single codebase. The shared module compiles to `commonMain` and targets both `jvm` and `wasmJs`.

### What Goes in Shared (Domain + Application)
- Domain events with `@Serializable` annotations and tags
- Commands and queries (data classes)
- Value objects (typed IDs, Email, Money, enums)
- Decision models (built from queried events)
- Port interfaces (EventStore, ReadModelStore)
- Command handlers and query handlers
- Projections and read model shapes
- Validation logic (pure functions)

### What Does NOT Go in Shared
- Event store implementations (backend adapter)
- Database access (backend adapter)
- REST controllers (backend adapter)
- UI components (frontend adapter)
- Platform-specific APIs (use `expect/actual` only when unavoidable)

---

## 3. Compose Multiplatform (Frontend)

**Target**: `wasmJs` (WebAssembly for browser)

Compose Multiplatform provides a declarative UI framework that compiles to WASM and runs in the browser. The frontend is a single-page application (SPA).

### Key Libraries
| Library | Purpose |
|---------|---------|
| Compose Multiplatform | UI framework (Material 3) |
| Ktor Client (JS) | HTTP client for API calls |
| kotlinx.serialization | JSON parsing (shared models) |
| kotlinx.coroutines | Async state management |

### Frontend Architecture
- **Screens**: One Composable per UI View defined in specs
- **State Management**: Compose `remember`/`mutableStateOf` with a simple MVI (Model-View-Intent) pattern
- **Navigation**: Compose Navigation or manual route management
- **Theming**: Material 3 with light/dark mode support

### WASM Considerations
- Initial load is larger than JS (~2-5MB for the WASM binary)
- Mitigate with: compression (Brotli/gzip), caching, lazy loading
- Browser compatibility: Chrome, Firefox, Safari, Edge (all support WASM)
- No direct DOM access — everything goes through Compose's rendering

---

## 4. Ktor (Backend)

**Version**: Ktor 3.x (latest stable)

Ktor is a Kotlin-native, coroutine-based HTTP server. It's lightweight and modular — you install only the plugins you need.

### Plugins to Use
| Plugin | Purpose |
|--------|---------|
| ContentNegotiation | JSON serialization with kotlinx.serialization |
| StatusPages | Consistent error response handling |
| Authentication | JWT or session-based (TBD — see Open Questions) |
| CORS | Allow frontend origin |
| CallLogging | Request/response logging |
| RequestValidation | Input validation at the route level |

### Backend Architecture (Hexagonal + CQRS)
- **Inbound Adapters (REST)**: Ktor routes map REST requests to domain commands/queries
- **Command Handlers**: In shared module — query events, build decision models, append new events
- **Query Handlers**: In shared module — read from read model store
- **Outbound Adapters**: Event store implementation, read model DB, projection runner
- **Configuration**: Environment-based config (application.conf / environment variables)

### API Design Conventions
- RESTful routes: `/api/v1/{resource}`
- JSON request/response bodies
- Consistent error shape:
  ```json
  {
    "error": {
      "code": "VALIDATION_ERROR",
      "message": "Email format is invalid",
      "details": [
        { "field": "email", "rule": "BR-3", "message": "Must be a valid email address" }
      ]
    }
  }
  ```
- Pagination: `?page=1&size=20` → response includes `pagination` object
- Sorting: `?sort=name:asc,createdAt:desc`
- Filtering: `?filter=status:active,company:uuid`

---

## 5. CQRS — Command Query Responsibility Segregation

The system separates writes (commands) from reads (queries) at every level.

### Command Side (Write Path)
```
REST POST/PUT/DELETE → Controller → CommandHandler → EventStore.append()
```
- Commands are data classes expressing intent (e.g., `CreateContact`)
- Command handlers validate, build decision models from events, emit new events
- The Event Store is the **only** write target — no direct database writes

### Query Side (Read Path)
```
REST GET → Controller → QueryHandler → ReadModelStore.find()
```
- Queries are data classes expressing what to read (e.g., `ListContacts(page, size)`)
- Query handlers read from denormalized read models (projections)
- Read models are **disposable** — can be rebuilt from the event store at any time

### Projections (Event → Read Model)
```
EventStore → ProjectionRunner → Projection → ReadModelStore.save()
```
- Projections subscribe to the event stream
- Each event is dispatched to relevant projections
- Projections update denormalized read model tables

---

## 6. DCB — Dynamic Consistency Boundaries

**Reference**: [dcb.events](https://dcb.events/) — Sara Pellegrini & Milan Savic

DCB replaces traditional aggregates with **decision models** built dynamically from tagged events.

### Core Concepts

**Events are tagged:**
```kotlin
@Serializable
data class DealCreated(
    val dealId: DealId,
    val companyId: CompanyId,
    val contactIds: List<ContactId>,
    val title: String,
    val value: Money?,
    override val tags: Set<String> = setOf(
        "deal:${dealId.value}",
        "company:${companyId.value}"
    ) + contactIds.map { "contact:${it.value}" }.toSet()
) : DomainEvent
```

**Decision models are ephemeral:**
```kotlin
class EmailUniquenessDecision {
    private var emailTaken = false

    fun apply(event: DomainEvent) {
        when (event) {
            is ContactCreated -> if (event.email != null) emailTaken = true
            is ContactDeleted -> emailTaken = false
            else -> {}
        }
    }

    fun isEmailAvailable(): Boolean = !emailTaken
}
```

**Append conditions enforce consistency:**
```kotlin
data class AppendCondition(
    val query: EventQuery,         // Same tags used to build the decision model
    val expectedPosition: Long     // Last event position seen during query
)

// EventStore checks: no new events matching query since expectedPosition
// If new events exist → ConcurrencyException → command handler retries or fails
```

**Command handler flow:**
```kotlin
class ContactCommandHandler(private val eventStore: EventStore) {
    suspend fun handle(cmd: CreateContact): ContactCreated {
        // 1. Validate fields
        ContactValidation.validate(cmd).throwIfInvalid()

        // 2. Query decision events (DCB)
        val emailQuery = EventQuery(tags = setOf("email:${cmd.email}"))
        val (events, position) = eventStore.query(emailQuery)

        // 3. Build decision model
        val decision = EmailUniquenessDecision()
        events.forEach { decision.apply(it) }

        // 4. Enforce invariant
        require(decision.isEmailAvailable()) { "Email already in use" }

        // 5. Create event
        val event = ContactCreated(
            contactId = ContactId.generate(),
            firstName = cmd.firstName,
            // ...
        )

        // 6. Append with condition (optimistic concurrency)
        eventStore.append(listOf(event), AppendCondition(emailQuery, position))
        return event
    }
}
```

### Why DCB over Traditional Aggregates?

| Aspect | Traditional Aggregates | DCB |
|--------|----------------------|-----|
| Consistency boundary | Fixed per aggregate type | Dynamic per command |
| Cross-entity rules | Sagas, process managers | Just another decision model with different tags |
| Event streams | One per aggregate instance | One global stream (or per bounded context) |
| Concurrency | Stream revision number | Tag-based query position |
| Flexibility | Rigid — must fit into aggregate | Fluid — query whatever events matter |

### DCB Kotlin Conventions

- Decision model classes have `fun apply(event: DomainEvent)` and query methods
- Each command handler declares its required `EventQuery` (tags)
- `AppendCondition` is always passed to `EventStore.append()`
- On `ConcurrencyException`: retry the command (re-query, re-decide, re-append)
- Decision models live in `shared/domain/decision/` — pure Kotlin, no infra deps

---

## 7. Event Store

**Decision**: PostgreSQL custom (JSONB events + tag index table)

The Event Store is the single source of truth. All state changes are recorded as immutable events.

### Event Store Port (in shared/domain/port/)
```kotlin
interface EventStore {
    suspend fun query(query: EventQuery): EventQueryResult
    suspend fun append(events: List<DomainEvent>, condition: AppendCondition)
    suspend fun subscribe(fromPosition: Long, handler: (DomainEvent) -> Unit)
}

data class EventQuery(
    val tags: Set<String>,       // Events must match ALL tags
    val since: Long = 0          // Position to query from
)

data class EventQueryResult(
    val events: List<DomainEvent>,
    val position: Long           // Latest position in result set
)

data class AppendCondition(
    val query: EventQuery,       // Must match the query used for the decision
    val expectedPosition: Long   // If position has advanced, reject (optimistic lock)
)
```

### Implementation Options

| Option | Pros | Cons | Maturity |
|--------|------|------|----------|
| **PostgreSQL-based** | Full control, reuse existing PG, JSONB for event data, GIN index on tags | Must implement tag queries and append conditions ourselves | Custom but straightforward |
| **Axon Server 2025.1+** | Native DCB support, built-in projections, Axon Framework 5 integration | External dependency, heavier infrastructure | Production-ready |
| **EventStoreDB** | Purpose-built event store, Kotlin client available | Less native DCB support (need custom tag layer) | Mature for ES, less so for DCB |
| **In-Memory** | Perfect for tests, zero setup | Not for production | N/A (test only) |

### PostgreSQL Event Store Schema (if chosen)
```sql
CREATE TABLE events (
    position    BIGSERIAL PRIMARY KEY,
    event_id    UUID NOT NULL UNIQUE,
    event_type  VARCHAR(255) NOT NULL,
    data        JSONB NOT NULL,
    timestamp   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE event_tags (
    event_position  BIGINT REFERENCES events(position),
    tag             VARCHAR(255) NOT NULL,
    PRIMARY KEY (event_position, tag)
);

CREATE INDEX idx_event_tags_tag ON event_tags(tag);

-- DCB query: find events matching tags
-- SELECT e.* FROM events e
-- JOIN event_tags t ON e.position = t.event_position
-- WHERE t.tag IN ('contact:abc-123', 'email:john@example.com')
-- AND e.position > :since
-- ORDER BY e.position;

-- Append condition: check no new matching events since position
-- SELECT MAX(e.position) FROM events e
-- JOIN event_tags t ON e.position = t.event_position
-- WHERE t.tag IN (:queryTags) AND e.position > :expectedPosition;
-- If result > 0, reject (ConcurrencyException)
```

---

## 8. Database (Read Models)

PostgreSQL is used for read model tables (projections). These are denormalized, query-optimized views.

### Read Model Tables
| Table | Projected From | Purpose |
|-------|---------------|---------|
| `contacts_view` | ContactCreated, ContactUpdated, ContactDeleted | Contact list + detail queries |
| `companies_view` | CompanyCreated, CompanyUpdated, CompanyDeleted | Company list + detail queries |
| `deals_view` | DealCreated, DealStageChanged, DealClosed | Deal list + pipeline queries |
| `activities_view` | ActivityCreated, ActivityCompleted | Activity timeline queries |
| `users_view` | UserCreated, UserDeactivated | User management queries |

Read model tables can be **dropped and rebuilt** from the event store at any time. They are not the source of truth.

---

## 9. Kubernetes Deployment

Two deployment models are under consideration. Both will be fully specified and the decision will be made during spec elaboration.

### Option A: Conventional (JVM Backend + Static WASM Frontend)

```
┌─────────────────────────────────────────┐
│              Kubernetes Cluster           │
│                                          │
│  ┌──────────────────┐                    │
│  │ Frontend Pod      │                   │
│  │ Nginx + WASM/JS   │ ← static assets  │
│  │ static files      │                   │
│  └────────┬─────────┘                    │
│           │                              │
│  ┌────────┴─────────┐                    │
│  │  Ingress          │                   │
│  │  /* → frontend    │                   │
│  │  /api/* → backend │                   │
│  └────────┬─────────┘                    │
│           │                              │
│  ┌────────┴─────────┐  ┌──────────────┐ │
│  │ Backend Pod       │  │  PostgreSQL   │ │
│  │ JVM + Ktor        │──│  (events +   │ │
│  │                   │  │  read models) │ │
│  └───────────────────┘  └──────────────┘ │
└─────────────────────────────────────────┘
```

### Option B: WASM Runtime on K8s (Experimental)

Same as before — see original description.

### Hybrid Approach
Start with Option A, architect for Option B.

---

## 10. Build System

**Gradle with Kotlin DSL** and a version catalog.

```
gradle/
  libs.versions.toml    # Central version definitions
build.gradle.kts        # Root build file (plugins, allprojects config)
settings.gradle.kts     # Module declarations
gradle.properties       # Kotlin/Compose compiler flags
```

### Version Catalog Structure (`libs.versions.toml`)
```toml
[versions]
kotlin = "2.1.x"
ktor = "3.x.x"
compose = "1.7.x"
serialization = "1.7.x"
coroutines = "1.9.x"
datetime = "0.6.x"
exposed = "0.57.x"

[libraries]
# Defined per-dependency

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose" }
ktor = { id = "io.ktor.plugin", version.ref = "ktor" }
```

---

## 11. Open Questions

- **Q-1**: Event Store implementation — PostgreSQL-based custom, Axon Server, or EventStoreDB? — **Decision**: PostgreSQL custom (JSONB events + tag index table, schema already in the spec)
- **Q-2**: Projection strategy — sync (in-process) or async (separate subscription)? — **Decision**: Sync in-process (projections run in same process after append)
- **Q-3**: ~~JWT vs. session-based authentication?~~ — **Decision**: RESOLVED — OIDC (JWT) via Ory Kratos + Hydra. See `architecture/auth.md`.
- **Q-4**: ~~Option A vs. Option B vs. Hybrid for deployment?~~ — **Decision**: RESOLVED — Hybrid. Both Option A (JVM, default) and Option B (WASM, experimental/scale-to-0) deployed via Kustomize on Rancher Desktop with WASM enabled. See `architecture/deployment.md`.
- **Q-5**: Specific Kotlin, Ktor, and Compose versions to pin? — **Decision**: Use latest stable at implementation time
- **Q-6**: Database access library for read models — Exposed vs. ktorm? — **Decision**: Exposed (JetBrains Kotlin SQL DSL)
- **Q-7**: Should commands support retry on ConcurrencyException, and if so, how many retries? — **Decision**: Yes, 3 retries with exponential backoff

---

## 6. Kubernetes Deployment

Two deployment models are under consideration. Both will be fully specified and the decision will be made during spec elaboration.

### Option A: Conventional (JVM Backend + Static WASM Frontend)

```
┌─────────────────────────────────────────┐
│              Kubernetes Cluster           │
│                                          │
│  ┌──────────────────┐                    │
│  │ Frontend Pod      │                   │
│  │ Nginx + WASM/JS   │ ← static assets  │
│  │ static files      │                   │
│  └────────┬─────────┘                    │
│           │                              │
│  ┌────────┴─────────┐                    │
│  │  Ingress          │                   │
│  │  /* → frontend    │                   │
│  │  /api/* → backend │                   │
│  └────────┬─────────┘                    │
│           │                              │
│  ┌────────┴─────────┐  ┌──────────────┐ │
│  │ Backend Pod       │  │  PostgreSQL   │ │
│  │ JVM + Ktor        │──│  (StatefulSet │ │
│  │                   │  │  or managed)  │ │
│  └───────────────────┘  └──────────────┘ │
└─────────────────────────────────────────┘
```

**Pros**: Proven, well-understood, rich JVM ecosystem for backend
**Cons**: JVM container is heavier (~200-500MB), slower cold start

### Option B: WASM Runtime on K8s (Experimental)

```
┌─────────────────────────────────────────┐
│              Kubernetes Cluster           │
│         (with WASM runtime support)      │
│                                          │
│  ┌──────────────────┐                    │
│  │ Frontend Pod      │                   │
│  │ WASM runtime      │ ← same as A      │
│  └──────────────────┘                    │
│                                          │
│  ┌──────────────────┐  ┌──────────────┐ │
│  │ Backend Pod       │  │  PostgreSQL   │ │
│  │ WasmEdge / Spin   │──│              │ │
│  │ Kotlin → WASM     │  │              │ │
│  │ (~1-10MB)         │  └──────────────┘ │
│  └──────────────────┘                    │
└─────────────────────────────────────────┘
```

**Pros**: Tiny containers (~1-10MB), near-instant cold start, lower resource usage
**Cons**: Experimental, limited library support, Kotlin/WASM server-side is bleeding edge

### Hybrid Approach
Start with **Option A** for initial development (proven, debuggable), architect for eventual migration to **Option B** by keeping backend logic clean and portable.

---

## 7. Build System

**Gradle with Kotlin DSL** and a version catalog.

```
gradle/
  libs.versions.toml    # Central version definitions
build.gradle.kts        # Root build file (plugins, allprojects config)
settings.gradle.kts     # Module declarations
gradle.properties       # Kotlin/Compose compiler flags
```

### Version Catalog Structure (`libs.versions.toml`)
```toml
[versions]
kotlin = "2.1.x"
ktor = "3.x.x"
compose = "1.7.x"
serialization = "1.7.x"
coroutines = "1.9.x"
exposed = "0.57.x"  # if chosen

[libraries]
# Defined per-dependency

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose" }
ktor = { id = "io.ktor.plugin", version.ref = "ktor" }
```

---

## 8. Authentication & Identity — Ory Stack

**Decision**: OIDC via Ory (Kratos + Hydra). See `architecture/auth.md` for full details.

| Component | Role | Local Dev Port |
|-----------|------|---------------|
| Ory Kratos | Identity management (users, passwords, recovery) | 4433 (public), 4434 (admin) |
| Ory Hydra | OAuth2/OIDC token issuance | 4444 (public), 4445 (admin) |
| Kratos Self-Service UI | Login/register/recovery pages | 4455 |
| Mailslurper | Fake SMTP for dev emails | 4436 |

### CRM Backend Integration
- Validates JWT access tokens via Hydra's JWKS endpoint
- Reads user claims (sub, email, name, role) from token
- Manages roles via Kratos Admin API
- **Never stores passwords**

### CRM Frontend Integration
- OIDC Authorization Code Flow with PKCE
- Redirects to Hydra for login → Kratos handles credentials
- Stores access token in memory (not localStorage)

### Local Development
```bash
cd docker && docker compose up -d
./setup-oidc-client.sh   # Register OIDC client + seed admin user
```
