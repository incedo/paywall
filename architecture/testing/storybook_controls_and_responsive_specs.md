# Storybook Controls + Responsive Specifications

**Status**: AGREED  
**Last Updated**: 2026-04-16  
**Depends On**: testing.md, ../design-system.md, ../forms-pattern.md, specs/architecture/testing/compose_storybook_requirements.md, specs/architecture/testing/storybook_story_and_scenario_specs.md

---

# Storybook Controls

**Status**: AGREED  
**Last Updated**: 2026-04-16  
**Depends On**: specs/architecture/testing/storybook_story_and_scenario_specs.md

---

## 1. Overview

A **Control** defines a runtime-adjustable input for a Story or Scenario in the CRM Storybook / Component Workbench. Controls exist so that engineers, QA, and design stakeholders can interactively vary parameters without changing source code or rebuilding the application.

Controls turn static examples into exploratory UI contracts. They are used to inspect state transitions, parameter ranges, layout edge cases, and design system compliance under changing inputs.

---

## 2. Value Objects

| Value Object | Type | Validation | Notes |
|-------------|------|------------|-------|
| ControlSchemaId | `@JvmInline value class` | Non-blank UUID or stable slug | Typed identifier |
| ControlId | `@JvmInline value class` | Non-blank, unique within schema | Typed control identifier |
| ControlKey | `@JvmInline value class` | Non-blank, normalized | Stable key used in bindings |
| ControlLabel | `@JvmInline value class` | 1..120 chars | Human-readable label |
| ControlDescription | `@JvmInline value class` | max 1000 chars | Optional help text |
| ScenarioRef | `@JvmInline value class` | Existing ScenarioId | Parent scenario reference |
| ControlType | `enum class` | BOOLEAN, TEXT, ENUM, INTEGER, DECIMAL, COLOR_TOKEN, SEGMENTED, SLIDER | Supported control types |
| ControlDefaultValue | `@JvmInline value class` | Type-compatible serialized value | Default input value |
| ControlOptionsRef | `@JvmInline value class` | Non-blank | Optional enum/options reference |
| ControlBindingRef | `@JvmInline value class` | Non-blank | Binding to render contract param |
| ControlValidationRule | `@JvmInline value class` | Non-blank | Rule identifier or inline rule |
| ControlGroupId | `@JvmInline value class` | Non-blank | Optional visual grouping |
| ControlLifecycle | `enum class` | ACTIVE, EXPERIMENTAL, DEPRECATED, ARCHIVED | Lifecycle state |

---

## 3. Domain Events

> Events are the source of truth. Each event is `@Serializable` and carries `tags: Set<String>` for DCB queries.

| Event | Tags | Payload | Trigger |
|-------|------|---------|---------|
| ControlSchemaRegistered | `["control-schema:{schemaId}", "scenario:{scenarioId}"]` | Full schema registration snapshot | RegisterControlSchema command |
| ControlAdded | `["control-schema:{schemaId}", "control:{controlId}", "scenario:{scenarioId}"]` | Control metadata + default value + binding | AddControl command |
| ControlUpdated | `["control-schema:{schemaId}", "control:{controlId}"]` | Changed label, description, type metadata, validation | UpdateControl command |
| ControlDefaultChanged | `["control-schema:{schemaId}", "control:{controlId}"]` | Old/new default value | ChangeControlDefault command |
| ControlOptionsLinked | `["control-schema:{schemaId}", "control:{controlId}"]` | Options reference | LinkControlOptions command |
| ControlValidationAttached | `["control-schema:{schemaId}", "control:{controlId}"]` | Validation rule reference | AttachControlValidation command |
| ControlGrouped | `["control-schema:{schemaId}", "control:{controlId}", "control-group:{groupId}"]` | Group assignment | GroupControl command |
| ControlRemoved | `["control-schema:{schemaId}", "control:{controlId}"]` | Removal marker | RemoveControl command |
| ControlSchemaArchived | `["control-schema:{schemaId}", "scenario:{scenarioId}", "lifecycle:archived"]` | Archive marker | ArchiveControlSchema command |

---

## 4. Commands

> Commands express intent to change state. Each declares required tags for its decision model query.

| Command | Fields | Required Decision Tags | Decision Model | Business Rules |
|---------|--------|----------------------|----------------|----------------|
| RegisterControlSchema | `schemaId, scenarioId, lifecycle` | `["control-schema:{schemaId}", "scenario:{scenarioId}"]` | ControlSchemaDecisionModel + ScenarioControlsPolicyDecision | BR-1, BR-2 |
| AddControl | `schemaId, controlId, key, label, type, defaultValue, bindingRef, optionsRef?, validationRules?` | `["control-schema:{schemaId}", "control:{controlId}", "scenario:{scenarioId}"]` | ControlSchemaDecisionModel + ControlKeyUniquenessDecision | BR-1, BR-3, BR-4, BR-5 |
| UpdateControl | `schemaId, controlId, label?, description?, typeMetadata?` | `["control-schema:{schemaId}", "control:{controlId}"]` | ControlSchemaDecisionModel | BR-1, BR-6 |
| ChangeControlDefault | `schemaId, controlId, defaultValue` | `["control-schema:{schemaId}", "control:{controlId}"]` | ControlSchemaDecisionModel | BR-1, BR-7 |
| LinkControlOptions | `schemaId, controlId, optionsRef` | `["control-schema:{schemaId}", "control:{controlId}"]` | ControlSchemaDecisionModel | BR-1, BR-8 |
| AttachControlValidation | `schemaId, controlId, validationRule` | `["control-schema:{schemaId}", "control:{controlId}"]` | ControlSchemaDecisionModel | BR-1, BR-9 |
| GroupControl | `schemaId, controlId, groupId` | `["control-schema:{schemaId}", "control:{controlId}"]` | ControlSchemaDecisionModel | BR-1 |
| RemoveControl | `schemaId, controlId` | `["control-schema:{schemaId}", "control:{controlId}"]` | ControlSchemaDecisionModel | BR-1, BR-10 |
| ArchiveControlSchema | `schemaId` | `["control-schema:{schemaId}"]` | ControlSchemaDecisionModel | BR-1, BR-11 |

---

## 5. Decision Models

> Decision models are ephemeral — built per-command from queried events. They enforce domain invariants (business rules).

| Decision Model | Queried Tags | State Built | Invariants Enforced |
|---------------|-------------|-------------|---------------------|
| ControlSchemaDecisionModel | `["control-schema:{schemaId}"]` | exists?, scenarioRef, controls, lifecycle, bindings, defaults | BR-1 through BR-11 |
| ControlKeyUniquenessDecision | `["control-schema:{schemaId}"]` | used control keys within schema | BR-4 |
| ScenarioControlsPolicyDecision | `["scenario:{scenarioId}"]` | scenario exists?, lifecycle compatibility, schema linkage | BR-2 |

### Decision Model Behavior

```text
Command arrives
  → Query EventStore by tags
  → Fold events into DecisionModel: events.fold(initial) { state, event -> state.apply(event) }
  → Check invariants
  → If valid: create new event(s), append with AppendCondition
  → If invalid: return error
```

### Suggested Decision State

```kotlin
@Serializable
 data class ControlSchemaDecisionState(
    val exists: Boolean = false,
    val archived: Boolean = false,
    val scenarioId: String? = null,
    val lifecycle: String? = null,
    val controls: Map<String, ControlState> = emptyMap()
)

@Serializable
 data class ControlState(
    val controlId: String,
    val key: String,
    val label: String,
    val type: String,
    val defaultValue: String,
    val bindingRef: String,
    val optionsRef: String? = null,
    val validationRules: Set<String> = emptySet(),
    val groupId: String? = null,
    val removed: Boolean = false
)
```

---

## 6. Read Models (Projections)

> Read models are denormalized, query-optimized views built from events. They are disposable — can be rebuilt at any time.

| Read Model | Source Events | Key Fields | Purpose |
|-----------|--------------|------------|---------|
| ControlSchemaView | ControlSchemaRegistered, ControlSchemaArchived | schemaId, scenarioId, lifecycle | Control schema overview |
| ControlListItemView | ControlAdded, ControlUpdated, ControlDefaultChanged, ControlOptionsLinked, ControlValidationAttached, ControlGrouped, ControlRemoved | controlId, key, type, label, defaultValue, groupId | Control panel rendering |
| ControlBindingView | ControlAdded, ControlUpdated, ControlRemoved | bindingRef, type, value shape | Runtime binding inspection |
| ControlValidationView | ControlAdded, ControlValidationAttached | validation rules, supported range/options | UI validation hints |

---

## 7. Business Rules

> Each rule is numbered and enforced either by validation (field-level) or decision model (invariant-level).

- **BR-1**: A control schema must exist before controls can be added, updated, grouped, or archived. — **Enforced by**: Decision Model
- **BR-2**: A control schema must reference an existing, non-archived parent scenario. — **Enforced by**: Validation / Decision Model
- **BR-3**: A control must declare a valid type, default value, and binding reference at creation time. — **Enforced by**: Validation
- **BR-4**: A `ControlKey` must be unique within a control schema. — **Enforced by**: Decision Model
- **BR-5**: A control default value must be compatible with its control type. — **Enforced by**: Validation / Decision Model
- **BR-6**: Updates may not change a control into an incompatible runtime shape without explicit migration semantics. — **Enforced by**: Decision Model
- **BR-7**: Default value changes must remain compatible with the existing control type and validation rules. — **Enforced by**: Validation / Decision Model
- **BR-8**: Options references may only be attached to control types that support enumerated or selectable options. — **Enforced by**: Validation
- **BR-9**: Validation rules must be expressible and executable by the workbench runtime. — **Enforced by**: Validation
- **BR-10**: Removed controls may not continue to participate in active runtime control rendering. — **Enforced by**: Decision Model / Projection
- **BR-11**: Archived control schemas are read-only. — **Enforced by**: Decision Model

---

## 8. API Endpoints

> POST/PUT/DELETE = **commands** (write path). GET = **queries** (read path).

### Command Endpoints

#### POST /api/v1/storybook/scenarios/{scenarioId}/controls
- **Command**: RegisterControlSchema
- **Request Body**:
  ```json
  {
    "schemaId": "controls-button-primary",
    "lifecycle": "ACTIVE"
  }
  ```
- **Success Response** (201): Created event confirmation + generated ID
- **Error Responses**: 400, 401, 404, 409

#### POST /api/v1/storybook/control-schemas/{schemaId}/controls
- **Command**: AddControl
- **Request Body**:
  ```json
  {
    "controlId": "loading",
    "key": "loading",
    "label": "Loading",
    "type": "BOOLEAN",
    "defaultValue": false,
    "bindingRef": "props.loading"
  }
  ```
- **Success Response** (201): Created event confirmation
- **Error Responses**: 400, 404, 409

#### PUT /api/v1/storybook/control-schemas/{schemaId}/controls/{controlId}
- **Command**: UpdateControl
- **Request Body**: Partial update fields
- **Success Response** (200): Updated confirmation
- **Error Responses**: 400, 404, 409

#### PUT /api/v1/storybook/control-schemas/{schemaId}/controls/{controlId}/default
- **Command**: ChangeControlDefault
- **Request Body**:
  ```json
  {
    "defaultValue": true
  }
  ```
- **Success Response** (200): Updated confirmation
- **Error Responses**: 400, 404, 409

#### PUT /api/v1/storybook/control-schemas/{schemaId}/controls/{controlId}/options
- **Command**: LinkControlOptions
- **Request Body**:
  ```json
  {
    "optionsRef": "options.button.variant"
  }
  ```
- **Success Response** (200): Linked confirmation
- **Error Responses**: 400, 404, 409

#### PUT /api/v1/storybook/control-schemas/{schemaId}/controls/{controlId}/validation
- **Command**: AttachControlValidation
- **Request Body**:
  ```json
  {
    "validationRule": "range:0..100"
  }
  ```
- **Success Response** (200): Attached confirmation
- **Error Responses**: 400, 404, 409

#### DELETE /api/v1/storybook/control-schemas/{schemaId}/controls/{controlId}
- **Command**: RemoveControl
- **Success Response** (204): No content
- **Error Responses**: 404, 409

### Query Endpoints

#### GET /api/v1/storybook/scenarios/{scenarioId}/controls
- **Query**: GetControlSchemaForScenario
- **Response** (200): Full control schema + controls list
- **Error Responses**: 404

#### GET /api/v1/storybook/control-schemas/{schemaId}
- **Query**: GetControlSchema
- **Response** (200): Control schema detail view
- **Error Responses**: 404

---

## 9. UI Views

### Control Schema View — Detail View
- **Route**: `/storybook/scenarios/{scenarioId}/controls`
- **Layout**: Grouped controls panel with type indicators, default values, option refs, and validation hints
- **Interactions**:
  - add control
  - edit metadata
  - change default values
  - group controls visually
- **State**: schema metadata, controls list, form state, validation state

### Control Binding Inspector — Detail View
- **Route**: `/storybook/control-schemas/{schemaId}/bindings`
- **Layout**: Mapping between control keys and render contract bindings
- **Interactions**:
  - inspect binding targets
  - verify type compatibility
- **State**: binding mappings, compatibility warnings

### Control Authoring View — Create/Edit Form
- **Route**: `/storybook/control-schemas/{schemaId}/controls/new` or `/storybook/control-schemas/{schemaId}/controls/{controlId}/edit`
- **Layout**: Form for label, type, default value, binding ref, options ref, validation rules, grouping
- **Interactions**:
  - validation feedback
  - save, cancel, remove
- **State**: form data, validation errors, submit state

---

## 10. Completion Criteria

> These checkboxes are the Ralph Wiggum loop's exit conditions.
> Four test layers: Unit → Contract → BDD → UI (see architecture/testing.md).

### 10a. Unit Tests (shared module — fast, isolated)
- [ ] Value objects validate correctly (reject invalid, accept valid)
- [ ] Events serialize/deserialize with correct tags
- [ ] ControlSchemaDecisionModel correctly folds events and enforces BR-1 through BR-11
- [ ] ControlKey uniqueness is enforced within schema
- [ ] Validation rejects invalid control defaults, invalid options refs, and invalid validation rules
- [ ] Command handler queries decision events, validates, and appends new events
- [ ] Removed controls are excluded from active runtime view generation
- [ ] Query handler reads from read model store and returns correct control schema views
- [ ] Projection updates control panel views on each relevant event type

### 10b. Contract Tests (backend — API shape verification)
- [ ] POST schema registration request/response shape matches contract
- [ ] POST add control request/response shape matches contract
- [ ] PUT update/default/options/validation request shape matches contract
- [ ] GET control schema response shape matches contract
- [ ] GET missing schema 404 error shape matches contract
- [ ] DELETE response matches contract
- [ ] Error response shape is consistent across all endpoints

### 10c. BDD Tests (backend — Gherkin scenarios for main flows)
- [ ] Scenario: Register control schema for valid scenario → 201
- [ ] Scenario: Add boolean control with valid default → 201
- [ ] Scenario: Add duplicate control key within schema → 409
- [ ] Scenario: Change control default to incompatible type → 400 or 409
- [ ] Scenario: Link options to enum control → 200
- [ ] Scenario: Remove control from schema → 204
- [ ] Scenario: Get controls for scenario
- [ ] Scenario: Auth — protected endpoint requires token

### 10d. UI Tests (E2E — Playwright, main user flows)
- [ ] Flow: Add control to scenario → appears in controls panel
- [ ] Flow: Update control default → preview recomposes
- [ ] Flow: Group controls → panel updates grouping
- [ ] Flow: Remove control → disappears from panel
- [ ] Flow: Inspect bindings → binding map shown

### 10e. Definition of Done
- [ ] All tests in 10a-10d pass
- [ ] Code compiles for all KMP targets (JVM, WASM)
- [ ] Domain layer has zero infrastructure imports
- [ ] No regressions in previously SATISFIED specs
- [ ] Contract tests verify all API shapes
- [ ] BDD feature file exists in specs/features/ with passing scenarios

---

## 11. Open Questions

> Unresolved decisions go here. A spec CANNOT move to AGREED while questions remain.

- **Q-1**: Should control schemas be one-per-scenario only, or reusable across scenarios? — **Decision**: Pending
- **Q-2**: Should control values be serializable to deep links in later phases? — **Decision**: Pending
- **Q-3**: How much validation logic should live in metadata versus executable runtime code? — **Decision**: Pending

---

# Storybook Responsive

**Status**: AGREED  
**Last Updated**: 2026-04-16  
**Depends On**: specs/architecture/testing/storybook_story_and_scenario_specs.md, ../design-system.md

---

## 1. Overview

The **Responsive** specification defines how Stories and Scenarios behave across supported form factors in the CRM Storybook / Component Workbench. It establishes a deterministic, token-based responsive model for phone, tablet, desktop, and web.

Responsive behavior is not an optional enhancement. It is a core quality characteristic of the CRM design system and must be explicit, inspectable, and testable inside the workbench.

---

## 2. Value Objects

| Value Object | Type | Validation | Notes |
|-------------|------|------------|-------|
| ResponsiveProfileId | `@JvmInline value class` | Non-blank UUID or stable slug | Typed identifier |
| ResponsiveProfileKey | `@JvmInline value class` | Non-blank, unique | Stable key, e.g. `crm-default-responsive` |
| StoryRef | `@JvmInline value class` | Existing StoryId | Parent story reference |
| ScenarioRef | `@JvmInline value class` | Existing ScenarioId | Optional scenario reference |
| FormFactor | `enum class` | PHONE, TABLET, DESKTOP, WEB | Supported runtime contexts |
| WidthClass | `enum class` | COMPACT, MEDIUM, EXPANDED | Width classification |
| HeightClass | `enum class` | COMPACT, MEDIUM, EXPANDED | Optional height classification |
| Orientation | `enum class` | PORTRAIT, LANDSCAPE | Orientation state |
| InteractionMode | `enum class` | TOUCH, POINTER, MIXED, KEYBOARD | Input modality |
| NavigationPattern | `enum class` | BOTTOM_BAR, TOP_BAR, NAV_RAIL, SIDE_NAV, DRAWER, RESPONSIVE | Navigation shell model |
| DensityProfile | `enum class` | COMPACT, COMFORTABLE, SPACIOUS | Layout density profile |
| ResponsiveExpectationRef | `@JvmInline value class` | Non-blank | Linked expectation note or spec ref |
| ResponsiveLifecycle | `enum class` | ACTIVE, EXPERIMENTAL, DEPRECATED, ARCHIVED | Lifecycle state |

---

## 3. Domain Events

> Events are the source of truth. Each event is `@Serializable` and carries `tags: Set<String>` for DCB queries.

| Event | Tags | Payload | Trigger |
|-------|------|---------|---------|
| ResponsiveProfileRegistered | `["responsive:{profileId}", "story:{storyId}"]` | Full responsive profile snapshot | RegisterResponsiveProfile command |
| ResponsiveFormFactorSupported | `["responsive:{profileId}", "form-factor:{formFactor}", "story:{storyId}"]` | Added form factor support | AddSupportedFormFactor command |
| ResponsiveWidthClassesDefined | `["responsive:{profileId}", "story:{storyId}"]` | Supported width classes | DefineSupportedWidthClasses command |
| ResponsiveNavigationPatternSet | `["responsive:{profileId}", "story:{storyId}"]` | Navigation pattern per context | SetNavigationPattern command |
| ResponsiveDensityProfileSet | `["responsive:{profileId}", "story:{storyId}"]` | Density profile | SetDensityProfile command |
| ResponsiveExpectationLinked | `["responsive:{profileId}", "story:{storyId}", "scenario:{scenarioId}"]` | Expectation reference | LinkResponsiveExpectation command |
| ResponsiveLayoutRuleAdded | `["responsive:{profileId}", "story:{storyId}"]` | Layout rule identifier / reference | AddResponsiveLayoutRule command |
| ResponsiveProfileArchived | `["responsive:{profileId}", "story:{storyId}", "lifecycle:archived"]` | Archive marker | ArchiveResponsiveProfile command |

---

## 4. Commands

> Commands express intent to change state. Each declares required tags for its decision model query.

| Command | Fields | Required Decision Tags | Decision Model | Business Rules |
|---------|--------|----------------------|----------------|----------------|
| RegisterResponsiveProfile | `profileId, profileKey, storyId, lifecycle` | `["responsive:{profileId}", "story:{storyId}"]` | ResponsiveDecisionModel + StoryResponsivePolicyDecision | BR-1, BR-2 |
| AddSupportedFormFactor | `profileId, formFactor` | `["responsive:{profileId}"]` | ResponsiveDecisionModel | BR-1, BR-3 |
| DefineSupportedWidthClasses | `profileId, widthClasses` | `["responsive:{profileId}"]` | ResponsiveDecisionModel | BR-1, BR-4 |
| SetNavigationPattern | `profileId, context, navigationPattern` | `["responsive:{profileId}"]` | ResponsiveDecisionModel | BR-1, BR-5 |
| SetDensityProfile | `profileId, context, densityProfile` | `["responsive:{profileId}"]` | ResponsiveDecisionModel | BR-1, BR-6 |
| LinkResponsiveExpectation | `profileId, scenarioId?, expectationRef` | `["responsive:{profileId}", "scenario:{scenarioId}"]` | ResponsiveDecisionModel | BR-1, BR-7 |
| AddResponsiveLayoutRule | `profileId, ruleRef` | `["responsive:{profileId}"]` | ResponsiveDecisionModel | BR-1, BR-8 |
| ArchiveResponsiveProfile | `profileId` | `["responsive:{profileId}"]` | ResponsiveDecisionModel | BR-1, BR-9 |

---

## 5. Decision Models

> Decision models are ephemeral — built per-command from queried events. They enforce domain invariants (business rules).

| Decision Model | Queried Tags | State Built | Invariants Enforced |
|---------------|-------------|-------------|---------------------|
| ResponsiveDecisionModel | `["responsive:{profileId}"]` | exists?, storyRef, form factors, width classes, nav patterns, density rules, expectations, lifecycle | BR-1 through BR-9 |
| StoryResponsivePolicyDecision | `["story:{storyId}"]` | story exists?, story coverage intent, lifecycle compatibility | BR-2 |

### Decision Model Behavior

```text
Command arrives
  → Query EventStore by tags
  → Fold events into DecisionModel: events.fold(initial) { state, event -> state.apply(event) }
  → Check invariants
  → If valid: create new event(s), append with AppendCondition
  → If invalid: return error
```

### Suggested Decision State

```kotlin
@Serializable
 data class ResponsiveDecisionState(
    val exists: Boolean = false,
    val archived: Boolean = false,
    val storyId: String? = null,
    val profileKey: String? = null,
    val formFactors: Set<String> = emptySet(),
    val widthClasses: Set<String> = emptySet(),
    val heightClasses: Set<String> = emptySet(),
    val navigationPatterns: Map<String, String> = emptyMap(),
    val densityProfiles: Map<String, String> = emptyMap(),
    val expectationRefs: Map<String, String> = emptyMap(),
    val layoutRules: Set<String> = emptySet()
)
```

---

## 6. Read Models (Projections)

> Read models are denormalized, query-optimized views built from events. They are disposable — can be rebuilt at any time.

| Read Model | Source Events | Key Fields | Purpose |
|-----------|--------------|------------|---------|
| ResponsiveProfileView | ResponsiveProfileRegistered, ResponsiveProfileArchived | profileId, storyId, lifecycle | Responsive profile overview |
| ResponsiveSupportMatrixView | ResponsiveProfileRegistered, ResponsiveFormFactorSupported, ResponsiveWidthClassesDefined | supported form factors, width classes | Preview matrix and coverage display |
| ResponsiveNavigationView | ResponsiveNavigationPatternSet | context, navigation pattern | Shell / layout decisions |
| ResponsiveExpectationView | ResponsiveExpectationLinked, ResponsiveLayoutRuleAdded | expectations, layout rules | Scenario-level responsive documentation |
| ResponsiveDensityView | ResponsiveDensityProfileSet | context, density profile | Spacing and density review |

---

## 7. Business Rules

> Each rule is numbered and enforced either by validation (field-level) or decision model (invariant-level).

- **BR-1**: A responsive profile must exist before responsive rules can be added or changed. — **Enforced by**: Decision Model
- **BR-2**: A responsive profile must reference an existing parent story. — **Enforced by**: Validation / Decision Model
- **BR-3**: Supported form factors may only use the allowed set: PHONE, TABLET, DESKTOP, WEB. — **Enforced by**: Validation
- **BR-4**: Width class definitions may only use the allowed set: COMPACT, MEDIUM, EXPANDED. — **Enforced by**: Validation
- **BR-5**: Navigation patterns must be compatible with the target context and form factor. — **Enforced by**: Decision Model
- **BR-6**: Density profiles must remain token-aligned and may not introduce ad hoc spacing systems. — **Enforced by**: Decision Model / Validation
- **BR-7**: Responsive expectations must reference either a valid scenario or the story-level responsive contract. — **Enforced by**: Validation / Decision Model
- **BR-8**: Layout rules must be centrally defined and reusable; feature-local ad hoc responsive overrides are not permitted in the model. — **Enforced by**: Validation / Governance
- **BR-9**: Archived responsive profiles are read-only. — **Enforced by**: Decision Model

---

## 8. API Endpoints

> POST/PUT/DELETE = **commands** (write path). GET = **queries** (read path).

### Command Endpoints

#### POST /api/v1/storybook/stories/{storyId}/responsive
- **Command**: RegisterResponsiveProfile
- **Request Body**:
  ```json
  {
    "profileId": "responsive-crm-default",
    "profileKey": "crm-default-responsive",
    "lifecycle": "ACTIVE"
  }
  ```
- **Success Response** (201): Created event confirmation + generated ID
- **Error Responses**: 400, 401, 404, 409

#### POST /api/v1/storybook/responsive/{profileId}/form-factors
- **Command**: AddSupportedFormFactor
- **Request Body**:
  ```json
  {
    "formFactor": "PHONE"
  }
  ```
- **Success Response** (201): Added confirmation
- **Error Responses**: 400, 404, 409

#### PUT /api/v1/storybook/responsive/{profileId}/width-classes
- **Command**: DefineSupportedWidthClasses
- **Request Body**:
  ```json
  {
    "widthClasses": ["COMPACT", "MEDIUM", "EXPANDED"]
  }
  ```
- **Success Response** (200): Updated confirmation
- **Error Responses**: 400, 404, 409

#### PUT /api/v1/storybook/responsive/{profileId}/navigation
- **Command**: SetNavigationPattern
- **Request Body**:
  ```json
  {
    "context": "PHONE",
    "navigationPattern": "BOTTOM_BAR"
  }
  ```
- **Success Response** (200): Updated confirmation
- **Error Responses**: 400, 404, 409

#### PUT /api/v1/storybook/responsive/{profileId}/density
- **Command**: SetDensityProfile
- **Request Body**:
  ```json
  {
    "context": "DESKTOP",
    "densityProfile": "COMFORTABLE"
  }
  ```
- **Success Response** (200): Updated confirmation
- **Error Responses**: 400, 404, 409

#### PUT /api/v1/storybook/responsive/{profileId}/expectation
- **Command**: LinkResponsiveExpectation
- **Request Body**:
  ```json
  {
    "scenarioId": "button-primary-loading",
    "expectationRef": "responsive.button.primary.loading"
  }
  ```
- **Success Response** (200): Linked confirmation
- **Error Responses**: 400, 404, 409

#### POST /api/v1/storybook/responsive/{profileId}/layout-rules
- **Command**: AddResponsiveLayoutRule
- **Request Body**:
  ```json
  {
    "ruleRef": "layout.single-column-on-compact"
  }
  ```
- **Success Response** (201): Added confirmation
- **Error Responses**: 400, 404, 409

### Query Endpoints

#### GET /api/v1/storybook/stories/{storyId}/responsive
- **Query**: GetResponsiveProfileForStory
- **Response** (200): Full responsive profile + support matrix
- **Error Responses**: 404

#### GET /api/v1/storybook/responsive/{profileId}
- **Query**: GetResponsiveProfile
- **Response** (200): Responsive profile detail view
- **Error Responses**: 404

---

## 9. UI Views

### Responsive Matrix View — Detail View
- **Route**: `/storybook/stories/{storyId}/responsive`
- **Layout**: Matrix showing supported form factors, width classes, navigation patterns, density profiles, and expectation links
- **Interactions**:
  - inspect support coverage
  - switch preview context
  - review responsive rules
- **State**: selected story, responsive profile, current preview context

### Responsive Rule Detail View — Detail View
- **Route**: `/storybook/responsive/{profileId}`
- **Layout**: Responsive metadata, layout rules, expectations, density profiles, navigation mapping
- **Interactions**:
  - inspect rule set
  - navigate to related scenarios
- **State**: profile detail, linked scenarios, rules, expectations

### Responsive Authoring View — Create/Edit Form
- **Route**: `/storybook/stories/{storyId}/responsive/edit`
- **Layout**: Form for form factors, width classes, navigation patterns, density profiles, linked expectations, layout rules
- **Interactions**:
  - validation feedback
  - save, cancel, archive
- **State**: form data, validation errors, submit state

---

## 10. Completion Criteria

> These checkboxes are the Ralph Wiggum loop's exit conditions.
> Four test layers: Unit → Contract → BDD → UI (see architecture/testing.md).

### 10a. Unit Tests (shared module — fast, isolated)
- [ ] Value objects validate correctly (reject invalid, accept valid)
- [ ] Events serialize/deserialize with correct tags
- [ ] ResponsiveDecisionModel correctly folds events and enforces BR-1 through BR-9
- [ ] Validation rejects invalid form factors, width classes, navigation patterns, and density profiles
- [ ] Command handler queries decision events, validates, and appends new events
- [ ] Layout rule refs remain centrally governed and reusable
- [ ] Query handler reads from read model store and returns correct responsive profile views
- [ ] Projection updates responsive matrix/detail views on each relevant event type

### 10b. Contract Tests (backend — API shape verification)
- [ ] POST profile registration request/response shape matches contract
- [ ] POST add form factor request/response shape matches contract
- [ ] PUT width classes/navigation/density/expectation request shape matches contract
- [ ] GET responsive profile response shape matches contract
- [ ] GET missing profile 404 error shape matches contract
- [ ] Error response shape is consistent across all endpoints

### 10c. BDD Tests (backend — Gherkin scenarios for main flows)
- [ ] Scenario: Register responsive profile for valid story → 201
- [ ] Scenario: Add supported form factor → 201
- [ ] Scenario: Define supported width classes → 200
- [ ] Scenario: Set navigation pattern for phone → 200
- [ ] Scenario: Link responsive expectation to scenario → 200
- [ ] Scenario: Add layout rule → 201
- [ ] Scenario: Get responsive profile for story
- [ ] Scenario: Auth — protected endpoint requires token

### 10d. UI Tests (E2E — Playwright, main user flows)
- [ ] Flow: Open responsive matrix → support matrix visible
- [ ] Flow: Switch preview from phone to tablet to desktop → context changes reflected
- [ ] Flow: Inspect navigation pattern by context → correct mapping shown
- [ ] Flow: Review responsive expectations linked to scenario → expectations visible
- [ ] Flow: Edit responsive profile → layout rules updated

### 10e. Definition of Done
- [ ] All tests in 10a-10d pass
- [ ] Code compiles for all KMP targets (JVM, WASM)
- [ ] Domain layer has zero infrastructure imports
- [ ] No regressions in previously SATISFIED specs
- [ ] Contract tests verify all API shapes
- [ ] BDD feature file exists in specs/features/ with passing scenarios

---

## 11. Open Questions

> Unresolved decisions go here. A spec CANNOT move to AGREED while questions remain.

- **Q-1**: Should responsive profiles be one-per-story only, or allow per-scenario overrides as first-class profiles? — **Decision**: Pending
- **Q-2**: Should width classes be globally fixed now, or extensible for future platform variants? — **Decision**: Pending
- **Q-3**: How much of responsive expectation modeling should be free-text versus structured rules? — **Decision**: Pending

