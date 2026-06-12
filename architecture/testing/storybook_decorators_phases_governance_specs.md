# Storybook Decorators + Phases + Governance Specifications

**Status**: AGREED  
**Last Updated**: 2026-06-12  
**Depends On**: testing.md, ../design-system.md, specs/architecture/testing/compose_storybook_requirements.md, specs/architecture/testing/storybook_story_and_scenario_specs.md, specs/architecture/testing/storybook_controls_and_responsive_specs.md

---

# Storybook Decorators

**Status**: AGREED  
**Last Updated**: 2026-06-12  
**Depends On**: specs/architecture/testing/storybook_story_and_scenario_specs.md, specs/architecture/testing/storybook_controls_and_responsive_specs.md, ../design-system.md

---

## 1. Overview

A **Decorator** defines a reusable runtime wrapper around a Story or Scenario preview in the paywall platform's Storybook / Component Workbench. Decorators provide the surrounding environment needed to render UI correctly without depending on the full production runtime.

Decorators exist to make preview environments explicit and reusable. They allow stories and scenarios to be rendered with a controlled theme, locale, surface, navigation shell, dependency scope, density profile, or preview frame so that teams can validate UI behavior under consistent conditions — e.g. the same light/dark `CrmTheme` pair the wall designer's live preview toggles between (ADM-12), or a phone device frame matching the designer's web/mobile preview.

---

## 2. Value Objects

| Value Object | Type | Validation | Notes |
|-------------|------|------------|-------|
| DecoratorId | `@JvmInline value class` | Non-blank UUID or stable slug | Typed identifier |
| DecoratorKey | `@JvmInline value class` | Non-blank, unique | Stable key, e.g. `theme/default-dark` |
| DecoratorTitle | `@JvmInline value class` | 1..120 chars | Human-readable title |
| DecoratorDescription | `@JvmInline value class` | max 2000 chars | Optional description |
| DecoratorType | `enum class` | THEME, SURFACE, LOCALE, VIEWPORT, NAVIGATION_SHELL, DEPENDENCY_SCOPE, DENSITY, ACCESSIBILITY, DEVICE_FRAME, CUSTOM | Classification |
| StoryRef | `@JvmInline value class` | Existing StoryId | Optional story link |
| ScenarioRef | `@JvmInline value class` | Existing ScenarioId | Optional scenario link |
| DecoratorRenderRef | `@JvmInline value class` | Non-blank | Composable/decorator registry reference |
| DecoratorPriority | `@JvmInline value class` | Positive integer | Order of application |
| DecoratorLifecycle | `enum class` | ACTIVE, EXPERIMENTAL, DEPRECATED, ARCHIVED | Lifecycle state |
| DecoratorScope | `enum class` | GLOBAL, STORY, SCENARIO | Application scope |
| DecoratorConfigurationRef | `@JvmInline value class` | Non-blank | Optional config reference |

---

## 3. Domain Events

> Events are the source of truth. Each event is `@Serializable` and carries `tags: Set<String>` for DCB queries.

| Event | Tags | Payload | Trigger |
|-------|------|---------|---------|
| DecoratorRegistered | `["decorator:{decoratorId}", "decorator-key:{decoratorKey}"]` | Full decorator registration snapshot | RegisterDecorator command |
| DecoratorMetadataUpdated | `["decorator:{decoratorId}"]` | Changed title, description, type, config refs | UpdateDecoratorMetadata command |
| DecoratorPriorityChanged | `["decorator:{decoratorId}"]` | Old/new priority | ChangeDecoratorPriority command |
| DecoratorScopeAssigned | `["decorator:{decoratorId}", "scope:{scope}"]` | Scope assignment | AssignDecoratorScope command |
| DecoratorLinkedToStory | `["decorator:{decoratorId}", "story:{storyId}"]` | Story association | LinkDecoratorToStory command |
| DecoratorLinkedToScenario | `["decorator:{decoratorId}", "scenario:{scenarioId}"]` | Scenario association | LinkDecoratorToScenario command |
| DecoratorLifecycleChanged | `["decorator:{decoratorId}", "lifecycle:{state}"]` | Lifecycle transition | ChangeDecoratorLifecycle command |
| DecoratorArchived | `["decorator:{decoratorId}", "lifecycle:archived"]` | Archive marker | ArchiveDecorator command |

---

## 4. Commands

> Commands express intent to change state. Each declares required tags for its decision model query.

| Command | Fields | Required Decision Tags | Decision Model | Business Rules |
|---------|--------|----------------------|----------------|----------------|
| RegisterDecorator | `decoratorId, decoratorKey, title, type, renderRef, priority, scope, lifecycle, configurationRef?` | `["decorator:{decoratorId}", "decorator-key:{decoratorKey}"]` | DecoratorDecisionModel + DecoratorKeyUniquenessDecision | BR-1, BR-2, BR-3 |
| UpdateDecoratorMetadata | `decoratorId, title?, description?, configurationRef?, type?` | `["decorator:{decoratorId}"]` | DecoratorDecisionModel | BR-1, BR-4 |
| ChangeDecoratorPriority | `decoratorId, priority` | `["decorator:{decoratorId}"]` | DecoratorDecisionModel | BR-1, BR-5 |
| AssignDecoratorScope | `decoratorId, scope` | `["decorator:{decoratorId}"]` | DecoratorDecisionModel | BR-1, BR-6 |
| LinkDecoratorToStory | `decoratorId, storyId` | `["decorator:{decoratorId}", "story:{storyId}"]` | DecoratorDecisionModel + DecoratorScopePolicyDecision | BR-1, BR-7 |
| LinkDecoratorToScenario | `decoratorId, scenarioId` | `["decorator:{decoratorId}", "scenario:{scenarioId}"]` | DecoratorDecisionModel + DecoratorScopePolicyDecision | BR-1, BR-8 |
| ChangeDecoratorLifecycle | `decoratorId, newLifecycle` | `["decorator:{decoratorId}"]` | DecoratorDecisionModel | BR-1, BR-9 |
| ArchiveDecorator | `decoratorId` | `["decorator:{decoratorId}"]` | DecoratorDecisionModel | BR-1, BR-10 |

---

## 5. Decision Models

> Decision models are ephemeral — built per-command from queried events. They enforce domain invariants (business rules).

| Decision Model | Queried Tags | State Built | Invariants Enforced |
|---------------|-------------|-------------|---------------------|
| DecoratorDecisionModel | `["decorator:{decoratorId}"]` | exists?, metadata, scope, priority, story/scenario links, lifecycle | BR-1 through BR-10 |
| DecoratorKeyUniquenessDecision | `["decorator-key:{decoratorKey}"]` | is decorator key already registered? | BR-3 |
| DecoratorScopePolicyDecision | `["decorator:{decoratorId}"]` + linked entity refs | scope compatibility with story/scenario binding | BR-7, BR-8 |

### Decision Model Behavior

```text
Command arrives
  → Query EventStore by tags
  → Fold events into DecisionModel: events.fold(initial) { state, event -> state.apply(event) }
  → Check invariants
  → If valid: create new event(s), append with AppendCondition
  → If invalid: return error
```

---

## 6. Read Models (Projections)

> Read models are denormalized, query-optimized views built from events. They are disposable — can be rebuilt at any time.

| Read Model | Source Events | Key Fields | Purpose |
|-----------|--------------|------------|---------|
| DecoratorCatalogView | DecoratorRegistered, DecoratorMetadataUpdated, DecoratorLifecycleChanged, DecoratorArchived | id, key, title, type, scope, lifecycle | Decorator catalog and search |
| DecoratorBindingView | DecoratorRegistered, DecoratorLinkedToStory, DecoratorLinkedToScenario, DecoratorScopeAssigned | scope, linked story/scenario, priority | Preview pipeline assembly |
| DecoratorConfigurationView | DecoratorRegistered, DecoratorMetadataUpdated, DecoratorPriorityChanged | renderRef, configRef, priority | Runtime decorator resolution |

---

## 7. Business Rules

- **BR-1**: A decorator must exist before it can be changed, linked, or archived. — **Enforced by**: Decision Model
- **BR-2**: A decorator registration must include a valid type, render reference, priority, and scope. — **Enforced by**: Validation
- **BR-3**: A `DecoratorKey` must be unique across the workbench. — **Enforced by**: Decision Model
- **BR-4**: Metadata updates may not clear mandatory fields. — **Enforced by**: Validation / Decision Model
- **BR-5**: Decorator priorities must be positive and deterministic; equal priorities require a stable tie-break rule in runtime ordering. — **Enforced by**: Validation / Runtime policy
- **BR-6**: Scope must be one of GLOBAL, STORY, or SCENARIO only. — **Enforced by**: Validation
- **BR-7**: A decorator with GLOBAL scope may not be bound as if it were scenario-exclusive metadata. — **Enforced by**: Decision Model
- **BR-8**: A scenario-scoped decorator may only be linked to existing scenarios. — **Enforced by**: Validation / Decision Model
- **BR-9**: Lifecycle transitions must be valid and archived decorators become read-only. — **Enforced by**: Decision Model
- **BR-10**: Archived decorators may not participate in active preview assembly. — **Enforced by**: Decision Model / Projection

---

## 8. API Endpoints

### Command Endpoints

#### POST /api/v1/storybook/decorators
- **Command**: RegisterDecorator
- **Request Body**:
  ```json
  {
    "decoratorId": "theme-default-dark",
    "decoratorKey": "theme/default-dark",
    "title": "Theme / Default Dark",
    "type": "THEME",
    "renderRef": "decorators.theme.defaultDark",
    "priority": 10,
    "scope": "GLOBAL",
    "lifecycle": "ACTIVE"
  }
  ```
- **Success Response** (201): Created event confirmation + generated ID
- **Error Responses**: 400, 401, 409

#### PUT /api/v1/storybook/decorators/{id}/priority
- **Command**: ChangeDecoratorPriority
- **Request Body**:
  ```json
  {
    "priority": 20
  }
  ```
- **Success Response** (200): Updated confirmation
- **Error Responses**: 400, 404, 409

#### POST /api/v1/storybook/decorators/{id}/stories/{storyId}
- **Command**: LinkDecoratorToStory
- **Success Response** (201): Linked confirmation
- **Error Responses**: 404, 409

#### POST /api/v1/storybook/decorators/{id}/scenarios/{scenarioId}
- **Command**: LinkDecoratorToScenario
- **Success Response** (201): Linked confirmation
- **Error Responses**: 404, 409

#### DELETE /api/v1/storybook/decorators/{id}
- **Command**: ArchiveDecorator
- **Success Response** (204): No content
- **Error Responses**: 404, 409

### Query Endpoints

#### GET /api/v1/storybook/decorators
- **Query**: ListDecorators
- **Parameters**: `page`, `size`, `sort`, `filter`, `type`, `scope`, `lifecycle`
- **Response** (200):
  ```json
  {
    "data": [],
    "pagination": { "page": 1, "size": 20, "total": 0 }
  }
  ```

#### GET /api/v1/storybook/decorators/{id}
- **Query**: GetDecorator
- **Response** (200): Full decorator detail view
- **Error Responses**: 404

---

## 9. UI Views

### Decorator Catalog View — List View
- **Route**: `/storybook/decorators`
- **Layout**: Filterable list of decorators with type, scope, lifecycle, and priority
- **Interactions**:
  - search and filter decorators
  - open decorator detail
  - navigate to linked stories or scenarios
- **State**: filters, selection, pagination

### Decorator Detail View — Detail View
- **Route**: `/storybook/decorators/{id}`
- **Layout**: Metadata, runtime render ref, priority, scope, linked entities
- **Interactions**:
  - inspect ordering
  - inspect scope compatibility
  - archive or update metadata
- **State**: decorator detail, linked stories/scenarios

### Decorator Authoring View — Create/Edit Form
- **Route**: `/storybook/decorators/new` or `/storybook/decorators/{id}/edit`
- **Layout**: Form for title, type, render ref, priority, scope, config ref, lifecycle
- **Interactions**:
  - validation feedback
  - save, cancel, archive
- **State**: form data, validation errors, submit state

---

## 10. Completion Criteria

### 10a. Unit Tests
- [ ] Value objects validate correctly
- [ ] Events serialize/deserialize with correct tags
- [ ] DecoratorDecisionModel correctly folds events and enforces BR-1 through BR-10
- [ ] Decorator key uniqueness is enforced
- [ ] Archived decorators are excluded from active preview assembly

### 10b. Contract Tests
- [ ] POST decorator request/response shape matches contract
- [ ] GET list/detail response shapes match contract
- [ ] Priority update and archive endpoints match contract

### 10c. BDD Tests
- [ ] Scenario: Register decorator → 201
- [ ] Scenario: Link decorator to story → 201
- [ ] Scenario: Link scenario decorator to missing scenario → 404/validation error
- [ ] Scenario: Archive decorator → 204

### 10d. UI Tests
- [ ] Flow: Create decorator → appears in catalog
- [ ] Flow: Update priority → ordering changes in detail/runtime assembly view
- [ ] Flow: Link decorator to story → shown on story detail

### 10e. Definition of Done
- [ ] All tests in 10a-10d pass
- [ ] Code compiles for all KMP targets (JVM, WASM)
- [ ] Domain layer has zero infrastructure imports

---

## 11. Open Questions

- **Q-1**: Should decorator ordering be global per scope or resolved locally per story/scenario assembly? — **Decision**: Pending
- **Q-2**: Should decorators be reusable across repos/modules through published artifacts only? — **Decision**: Pending
- **Q-3**: Should fake dependency decorators be modeled separately from visual decorators? — **Decision**: Pending

---

# Storybook Phases

**Status**: AGREED  
**Last Updated**: 2026-06-12  
**Depends On**: specs/architecture/testing/compose_storybook_requirements.md, specs/architecture/testing/storybook_story_and_scenario_specs.md, specs/architecture/testing/storybook_controls_and_responsive_specs.md, specs/architecture/testing/storybook_decorators_phases_governance_specs.md

---

## 1. Overview

The **Phases** specification defines the intended capability rollout for the paywall platform's Storybook / Component Workbench. It converts roadmap items into explicit, governable capability increments rather than informal backlog notes.

Phases exist to give the platform a controlled maturity path. They help engineering and architecture teams track what is mandatory for initial delivery and what becomes available in later increments.

---

## 2. Value Objects

| Value Object | Type | Validation | Notes |
|-------------|------|------------|-------|
| PhaseId | `@JvmInline value class` | Non-blank UUID or stable slug | Typed identifier |
| PhaseKey | `@JvmInline value class` | Non-blank, unique | Stable key such as `phase-1-foundation` |
| PhaseName | `@JvmInline value class` | 1..120 chars | Human-readable label |
| PhaseOrder | `@JvmInline value class` | Positive integer | Sequence order |
| CapabilityKey | `@JvmInline value class` | Non-blank | Feature/capability identifier |
| PhaseLifecycle | `enum class` | PLANNED, ACTIVE, SATISFIED, SUPERSEDED | Phase status |

---

## 3. Domain Events

| Event | Tags | Payload | Trigger |
|-------|------|---------|---------|
| PhaseRegistered | `["phase:{phaseId}", "phase-key:{phaseKey}"]` | Full phase metadata | RegisterPhase command |
| CapabilityAddedToPhase | `["phase:{phaseId}", "capability:{capabilityKey}"]` | Capability metadata | AddCapabilityToPhase command |
| PhaseActivated | `["phase:{phaseId}", "phase-status:active"]` | Status transition | ActivatePhase command |
| PhaseSatisfied | `["phase:{phaseId}", "phase-status:satisfied"]` | Completion marker | MarkPhaseSatisfied command |
| PhaseSuperseded | `["phase:{phaseId}", "phase-status:superseded"]` | Superseded marker | SupersedePhase command |

---

## 4. Commands

| Command | Fields | Required Decision Tags | Decision Model | Business Rules |
|---------|--------|----------------------|----------------|----------------|
| RegisterPhase | `phaseId, phaseKey, phaseName, phaseOrder, lifecycle` | `["phase:{phaseId}", "phase-key:{phaseKey}"]` | PhaseDecisionModel + PhaseKeyUniquenessDecision | BR-1, BR-2 |
| AddCapabilityToPhase | `phaseId, capabilityKey, title, description?` | `["phase:{phaseId}", "capability:{capabilityKey}"]` | PhaseDecisionModel | BR-1, BR-3 |
| ActivatePhase | `phaseId` | `["phase:{phaseId}"]` | PhaseDecisionModel | BR-1, BR-4 |
| MarkPhaseSatisfied | `phaseId` | `["phase:{phaseId}"]` | PhaseDecisionModel | BR-1, BR-5 |
| SupersedePhase | `phaseId` | `["phase:{phaseId}"]` | PhaseDecisionModel | BR-1, BR-6 |

---

## 5. Decision Models

| Decision Model | Queried Tags | State Built | Invariants Enforced |
|---------------|-------------|-------------|---------------------|
| PhaseDecisionModel | `["phase:{phaseId}"]` | exists?, capabilities, lifecycle, order | BR-1 through BR-6 |
| PhaseKeyUniquenessDecision | `["phase-key:{phaseKey}"]` | duplicate phase key? | BR-2 |

---

## 6. Read Models (Projections)

| Read Model | Source Events | Key Fields | Purpose |
|-----------|--------------|------------|---------|
| PhaseRoadmapView | PhaseRegistered, CapabilityAddedToPhase, PhaseActivated, PhaseSatisfied, PhaseSuperseded | name, order, lifecycle, capabilities | Roadmap and release planning |
| PhaseCapabilityView | CapabilityAddedToPhase | capability list per phase | Delivery tracking |

---

## 7. Business Rules

- **BR-1**: A phase must exist before capabilities or lifecycle transitions can be applied. — **Enforced by**: Decision Model
- **BR-2**: `PhaseKey` must be unique. — **Enforced by**: Decision Model
- **BR-3**: A capability may not be added twice to the same phase. — **Enforced by**: Decision Model
- **BR-4**: A phase may only be activated from PLANNED state. — **Enforced by**: Decision Model
- **BR-5**: A phase may only be marked SATISFIED once all mandatory completion evidence exists. — **Enforced by**: Decision Model / Governance integration
- **BR-6**: A superseded phase may not be re-activated. — **Enforced by**: Decision Model

---

## 8. Phased Capability Definition

### Phase 1 — Foundation
- story model
- scenario model
- catalog navigation
- preview host
- theme switching
- viewport switching
- base documentation panel

### Phase 2 — Interactive Exploration
- runtime controls
- scenario presets
- lifecycle tags such as `deprecated` and `experimental`
- story and scenario notes
- improved search and filtering

### Phase 3 — Responsive Validation
- side-by-side comparison
- viewport matrix mode
- token inspector
- responsive coverage display
- favorites / frequently reviewed stories

### Phase 4 — Quality and References
- accessibility checks
- screenshot export
- source code snippet panel
- Figma/design reference links
- richer quality metadata per story/scenario

### Phase 5 — Discovery and Sharing
- full-text search
- shareable deep links
- advanced tag filtering
- recently viewed / enhanced discovery patterns

---

## 9. Completion Criteria

- [ ] Phases are explicitly modeled, not implied backlog buckets
- [ ] Each capability is assigned to exactly one initial phase
- [ ] Phase transitions are traceable and governed
- [ ] Roadmap view is queryable from projections

---

## 10. Open Questions

- **Q-1**: Should phase evidence be stored in the same event stream or linked from governance artifacts? — **Decision**: Pending
- **Q-2**: Should capabilities be movable between phases after registration? — **Decision**: Pending

---

# Storybook Governance

**Status**: AGREED  
**Last Updated**: 2026-06-12  
**Depends On**: specs/architecture/testing/storybook_story_and_scenario_specs.md, specs/architecture/testing/storybook_controls_and_responsive_specs.md, specs/architecture/testing/storybook_decorators_phases_governance_specs.md, testing.md

---

## 1. Overview

The **Governance** specification defines lifecycle ownership, quality gates, and acceptance policies for the paywall platform's Storybook / Component Workbench. It ensures that the workbench remains a trusted engineering asset rather than a loosely curated demo catalog.

Governance exists to enforce consistency, ownership, and quality over time. It provides the bridge between architectural intent, test evidence, lifecycle maturity, and operational maintainability.

---

## 2. Value Objects

| Value Object | Type | Validation | Notes |
|-------------|------|------------|-------|
| GovernancePolicyId | `@JvmInline value class` | Non-blank UUID or stable slug | Typed identifier |
| GovernancePolicyKey | `@JvmInline value class` | Non-blank, unique | Stable policy key |
| QualityGateKey | `@JvmInline value class` | Non-blank | Gate identifier |
| OwnerRef | `@JvmInline value class` | Non-blank | Team, module, or role owner |
| EvidenceRef | `@JvmInline value class` | Non-blank | Link to test/report/spec evidence |
| LifecycleState | `enum class` | DRAFT, ACTIVE, EXPERIMENTAL, DEPRECATED, ARCHIVED | Governed lifecycle |
| GovernanceDecision | `enum class` | APPROVED, REJECTED, WAIVED, PENDING | Review outcome |

---

## 3. Domain Events

| Event | Tags | Payload | Trigger |
|-------|------|---------|---------|
| GovernancePolicyRegistered | `["policy:{policyId}", "policy-key:{policyKey}"]` | Full policy metadata | RegisterGovernancePolicy command |
| QualityGateAttached | `["policy:{policyId}", "quality-gate:{gateKey}"]` | Gate definition | AttachQualityGate command |
| OwnerAssigned | `["policy:{policyId}", "owner:{ownerRef}"]` | Owner assignment | AssignOwner command |
| EvidenceLinked | `["policy:{policyId}", "evidence:{evidenceRef}"]` | Evidence reference | LinkEvidence command |
| GovernanceDecisionRecorded | `["policy:{policyId}", "decision:{decision}"]` | Decision outcome | RecordGovernanceDecision command |
| LifecycleGoverned | `["policy:{policyId}", "lifecycle:{state}"]` | Lifecycle transition approval | GovernLifecycleTransition command |

---

## 4. Commands

| Command | Fields | Required Decision Tags | Decision Model | Business Rules |
|---------|--------|----------------------|----------------|----------------|
| RegisterGovernancePolicy | `policyId, policyKey, title, lifecycle` | `["policy:{policyId}", "policy-key:{policyKey}"]` | GovernanceDecisionModel + PolicyKeyUniquenessDecision | BR-1, BR-2 |
| AttachQualityGate | `policyId, gateKey, description` | `["policy:{policyId}"]` | GovernanceDecisionModel | BR-1, BR-3 |
| AssignOwner | `policyId, ownerRef` | `["policy:{policyId}"]` | GovernanceDecisionModel | BR-1, BR-4 |
| LinkEvidence | `policyId, evidenceRef, evidenceType` | `["policy:{policyId}"]` | GovernanceDecisionModel | BR-1, BR-5 |
| RecordGovernanceDecision | `policyId, decision, rationale?` | `["policy:{policyId}"]` | GovernanceDecisionModel | BR-1, BR-6 |
| GovernLifecycleTransition | `policyId, targetRef, targetType, newLifecycle` | `["policy:{policyId}"]` | GovernanceDecisionModel | BR-1, BR-7 |

---

## 5. Decision Models

| Decision Model | Queried Tags | State Built | Invariants Enforced |
|---------------|-------------|-------------|---------------------|
| GovernanceDecisionModel | `["policy:{policyId}"]` | exists?, owner, gates, evidence, decisions | BR-1 through BR-7 |
| PolicyKeyUniquenessDecision | `["policy-key:{policyKey}"]` | duplicate policy key? | BR-2 |

---

## 6. Read Models (Projections)

| Read Model | Source Events | Key Fields | Purpose |
|-----------|--------------|------------|---------|
| GovernancePolicyView | GovernancePolicyRegistered, QualityGateAttached, OwnerAssigned | policy, gates, owners | Policy overview |
| GovernanceEvidenceView | EvidenceLinked | evidence refs by policy | Auditability and proof |
| GovernanceDecisionView | GovernanceDecisionRecorded, LifecycleGoverned | decisions, rationale, lifecycle actions | Approval history |

---

## 7. Business Rules

- **BR-1**: A governance policy must exist before gates, owners, evidence, or decisions can be attached. — **Enforced by**: Decision Model
- **BR-2**: `GovernancePolicyKey` must be unique. — **Enforced by**: Decision Model
- **BR-3**: Each governed artifact type must have at least one quality gate before it can move to ACTIVE. — **Enforced by**: Decision Model / Governance integration
- **BR-4**: Each governed artifact must have an explicit owner. — **Enforced by**: Decision Model / Process policy
- **BR-5**: Evidence references must point to verifiable artifacts such as tests, screenshots, docs, or review records. — **Enforced by**: Validation
- **BR-6**: A governance decision must include a valid decision state and may require rationale for REJECTED or WAIVED outcomes. — **Enforced by**: Validation / Decision Model
- **BR-7**: Lifecycle promotions to ACTIVE or SATISFIED require attached evidence and satisfied quality gates. — **Enforced by**: Decision Model / Governance integration

---

## 8. Quality Gates (Baseline)

The following baseline quality gates should be modeled:

- ownership assigned
- story/scenario documentation present
- responsive coverage declared
- controls schema present where interactive story required
- accessibility notes present for relevant artifacts
- test evidence linked (unit / contract / UI where applicable)
- design reference linked where required
- lifecycle rationale captured for deprecated or waived items

---

## 9. API Endpoints

### Command Endpoints

#### POST /api/v1/storybook/governance/policies
- **Command**: RegisterGovernancePolicy

#### POST /api/v1/storybook/governance/policies/{id}/quality-gates
- **Command**: AttachQualityGate

#### POST /api/v1/storybook/governance/policies/{id}/owners
- **Command**: AssignOwner

#### POST /api/v1/storybook/governance/policies/{id}/evidence
- **Command**: LinkEvidence

#### POST /api/v1/storybook/governance/policies/{id}/decisions
- **Command**: RecordGovernanceDecision

### Query Endpoints

#### GET /api/v1/storybook/governance/policies
- **Query**: ListGovernancePolicies

#### GET /api/v1/storybook/governance/policies/{id}
- **Query**: GetGovernancePolicy

---

## 10. Completion Criteria

- [ ] Governance policies are explicit and queryable
- [ ] Quality gates are defined for workbench artifacts
- [ ] Ownership is mandatory and auditable
- [ ] Evidence links exist for lifecycle promotions
- [ ] Decisions and waivers are historically traceable

---

## 11. Open Questions

- **Q-1**: Should governance be one global policy set or separate policies per artifact type? — **Decision**: Pending
- **Q-2**: Which quality gates are strictly blocking for Phase 1 versus later phases? — **Decision**: Pending
- **Q-3**: Should evidence refs support generated CI artifacts directly? — **Decision**: Pending

