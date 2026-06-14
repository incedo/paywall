# Design System — Forms Pattern

**Status**: AGREED
**Last Updated**: 2026-06-12
**Depends On**: architecture/design-system.md

---

## 1. Overview

Forms are the primary interaction pattern in the admin console — the wall editor's config panel, targeting & publishing panel, experiment setup, plan/pricing configuration, and settings all use forms. This pattern defines consistent rules for layout, validation, field types, and behavior across all platforms.

Inspired by [Carbon Design System Forms Pattern](https://carbondesignsystem.com/patterns/forms-pattern/).

---

## 2. Form Anatomy

```
┌──────────────────────────────────────────────────┐
│  Form Title (h2)                                  │
│  Optional description (body, onSurfaceVariant)    │
├──────────────────────────────────────────────────┤
│                                                    │
│  ┌─ CrmFormSection ─────────────────────────┐     │
│  │  Section heading (h3)                     │     │
│  │                                           │     │
│  │  Label (label)              ← always above │    │
│  │  ┌─────────────────────────┐              │     │
│  │  │ Input field              │              │    │
│  │  └─────────────────────────┘              │     │
│  │  Helper text (caption, onSurfaceVariant)  │     │
│  │  Error text (caption, error) ← on error   │     │
│  │                                           │     │
│  │  Label                                    │     │
│  │  ┌─────────────────────────┐              │     │
│  │  │ Input field              │              │    │
│  │  └─────────────────────────┘              │     │
│  └───────────────────────────────────────────┘     │
│                                                    │
│  ┌─ CrmFormSection ─────────────────────────┐     │
│  │  Another section...                       │     │
│  └───────────────────────────────────────────┘     │
│                                                    │
├──────────────────────────────────────────────────┤
│  ┌─────────┐  ┌───────────────┐                   │
│  │ Cancel  │  │  Save (Primary) │  ← action bar   │
│  └─────────┘  └───────────────┘                   │
└──────────────────────────────────────────────────┘
```

---

## 3. Field Types → Component Mapping

| Data type | Component | Notes |
|-----------|-----------|-------|
| Short text | `CrmInputField` | Single-line, optional placeholder (wall title, CTA labels) |
| Long text | `CrmTextArea` | Multi-line, rows + maxLength (wall body copy) |
| Number | `CrmInputField` | Type filter (digits only) — meter limit, traffic split % |
| Currency | `CrmInputField` | Formatted with symbol, 2 decimals (plan price) |
| Date | `CrmDatePicker` | Calendar popup/sheet (scheduled publish, experiment end) |
| Single select | `CrmSelectField` | Dropdown (wall type, audience attribute) |
| Multi select | `CrmSelectField(multi=true)` | Chips in input |
| Boolean toggle | `CrmSwitch` | On/off with label (A/B test enabled, dark preview) |
| Yes/No choice | `CrmRadio` group | Two options |
| Multi choice | `CrmCheckbox` group / `CrmToggleChip` row | Channel selection |
| File | `CrmFileUpload` | Drop zone + browse (gate artwork, V2) |
| Rich text | `CrmTextArea` | Markdown support (V2) |

---

## 4. Layout Rules

### Field spacing
- Between fields within a section: `CrmTheme.spacing.lg` (16dp)
- Between sections: `CrmTheme.spacing.xl` (24dp)
- Label to input: `CrmTheme.spacing.xs` (4dp)
- Input to helper/error text: `CrmTheme.spacing.xs` (4dp)

### Responsive columns

| WindowSizeClass | Columns | Field width |
|----------------|---------|-------------|
| COMPACT (phone) | 1 | Full width |
| MEDIUM (tablet) | 2 | Half width, full for TextArea/FileUpload |
| EXPANDED (desktop) | 2-3 | Configured per form (the designer's config panel is a fixed single column) |
| LARGE (wide desktop) | 3 | Max 400dp per field |

### Required fields
- Label shows `*` suffix in `error` color
- Optional fields show "(optional)" suffix in `onSurfaceVariant`
- Default: all fields required unless marked optional

### Read-only mode
- Inputs replaced with `CrmText` showing the value
- No edit affordance (no border, no hover)
- Background matches surface (no surfaceVariant)
- Used for published wall versions in the version history panel

---

## 5. Validation Rules

### When to validate
| Trigger | Behavior |
|---------|----------|
| On blur | Validate single field when focus leaves |
| On submit | Validate all fields, scroll to first error |
| On change (after error) | Re-validate field on every keystroke once an error was shown |
| On change (before error) | No validation — don't nag while typing |

### Error display
- Red border on input (`CrmTheme.colors.error`)
- Error message below input in `caption` style, `error` color
- Error icon inside input (right side, `CrmTheme.iconSize.sm`)
- Field stays in error state until valid

### Error summary (for long forms)
- Toast/snackbar: "Please fix N errors before publishing" (CrmSnackbar, ERROR variant)
- Scroll to first error field and focus it

### Example rules (wall editor)
- Title: required, non-blank — "Title is required"
- Primary CTA: required — "Primary CTA label is required"
- Meter limit: integer ≥ 1 (metered walls only, MT-*) — "Meter limit must be at least 1"
- Channels: at least one `Channel` selected — "Select at least one channel"
- A/B traffic split: 0–100, variants must sum to 100%

---

## 6. Form Actions

### Button placement
- **Desktop/Tablet**: Right-aligned in action bar. Primary action rightmost.
- **Mobile**: Full-width stacked. Primary action on top.

### Standard actions

| Action | Variant | Position | Behavior |
|--------|---------|----------|----------|
| Publish / Save | PRIMARY | Right | Validates, submits if valid |
| Cancel | GHOST | Left of primary | Confirms if dirty ("Unsaved changes") |
| Delete / Unpublish | DANGER | Far left, separated | Opens CrmDialog confirm |
| Save Draft | SECONDARY | Left of primary | Saves without validation |

### Dirty state
- Track changes via state holder (compare against the last saved `WallDefinition`)
- Show dot indicator on tab/nav if form has unsaved changes
- Confirm dialog on navigation away: "You have unsaved changes. Discard?"

---

## 7. CrmFormSection Component

```kotlin
@Composable
fun CrmFormSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
)
```

Groups related fields with a heading. Adds consistent spacing between fields.

---

## 8. Paywall-Specific Form Patterns

### Wall editor — config panel (ADM-11/12)
```
Section: Wall Type
  - Type (Radio/segmented: Hard / Metered / Freemium / Dynamic — WallType)
  - Meter limit (required for Metered, InputField, integer ≥ 1, MT-*)

Section: Copy
  - Title (required, InputField)
  - Body (required, TextArea, rows=4)
  - Primary CTA (required, InputField)
  - Secondary CTA (optional, InputField)
```

The panel binds directly to `WallDefinition` — every field change calls
`onDefinitionChange(definition.copy(title = newValue))` and the live preview
re-renders from the new definition.

### Wall editor — targeting & publishing panel
```
Section: Audience
  - Audience conditions (dynamic rows: attribute SelectField, operator, value)
  - [+ Add Condition] button

Section: Channels
  - Channels (required, CrmToggleChip group: Web / Mobile app / Chat /
    In-product / Email / SMS — at least one selected)

Section: Experiment
  - A/B test enabled (Switch)
  - Traffic split (InputField, 0-100%)

Section: Publishing
  - Status (read-only Badge: Live / Draft / Paused)
  - Version history (read-only timeline)
```

### Plan / pricing form (pricing wall, PW-*)
```
Section: Plan Details
  - Plan name (required, InputField)
  - Price (required, InputField, currency format)
  - Billing period (SegmentedToggle: Monthly / Annual)

Section: Features (dynamic)
  - [DataTable: Feature line | Included]
  - [+ Add Feature] button

Section: Presentation (read-only preview)
  - Rendered CrmPlanCard
```

---

## 9. Completion Criteria

- [x] `CrmFormSection` implemented — title, optional description, content slot, lg spacing between fields — 2026-06-14
- [x] `CrmInputField` implemented — label, value, placeholder, error state (red border + message), helperText, singleLine — 2026-06-14
- [x] Error takes precedence over helperText — 2026-06-14
- [x] All components compile on JVM and WasmJs targets — 2026-06-14
- [x] InputComponentsTest covers CrmInputField (7 tests) and CrmFormSection (3 tests) — 2026-06-14

---

## 10. Accessibility

- All inputs have associated labels (no floating labels — always visible above)
- Required fields announced to screen readers: "Title, required"
- Error messages associated via semantics: `error("Title is required")`
- Tab order follows visual order (top-to-bottom, left-to-right)
- Enter key submits form from any field
- Escape key in dialog forms closes without saving (with dirty confirm)
