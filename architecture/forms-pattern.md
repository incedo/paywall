# Design System — Forms Pattern

**Status**: DRAFT
**Last Updated**: 2026-04-16
**Depends On**: architecture/design-system.md

---

## 1. Overview

Forms are the primary interaction pattern in the CRM — contacts, companies, deals, invoices, subscriptions, and settings all use forms. This pattern defines consistent rules for layout, validation, field types, and behavior across all platforms.

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
| Short text | `CrmInputField` | Single-line, optional placeholder |
| Long text | `CrmTextArea` | Multi-line, rows + maxLength |
| Email | `CrmInputField` | Validation regex, error on invalid |
| Number | `CrmInputField` | Type filter (digits only) |
| Currency | `CrmInputField` | Formatted with symbol, 2 decimals |
| Date | `CrmDatePicker` | Calendar popup/sheet |
| Single select | `CrmSelectField` | Dropdown |
| Multi select | `CrmSelectField(multi=true)` | Chips in input |
| Boolean toggle | `CrmSwitch` | On/off with label |
| Yes/No choice | `CrmRadio` group | Two options |
| Multi choice | `CrmCheckbox` group | Multiple options |
| File | `CrmFileUpload` | Drop zone + browse |
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
| EXPANDED (desktop) | 2-3 | Configured per form |
| LARGE (wide desktop) | 3 | Max 400dp per field |

### Required fields
- Label shows `*` suffix in `error` color
- Optional fields show "(optional)" suffix in `onSurfaceVariant`
- Default: all fields required unless marked optional

### Read-only mode
- Inputs replaced with `CrmText` showing the value
- No edit affordance (no border, no hover)
- Background matches surface (no surfaceVariant)

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
- Toast/snackbar: "Please fix N errors before saving" (CrmSnackbar, ERROR variant)
- Scroll to first error field and focus it

---

## 6. Form Actions

### Button placement
- **Desktop/Tablet**: Right-aligned in action bar. Primary action rightmost.
- **Mobile**: Full-width stacked. Primary action on top.

### Standard actions

| Action | Variant | Position | Behavior |
|--------|---------|----------|----------|
| Save / Submit | PRIMARY | Right | Validates, submits if valid |
| Cancel | GHOST | Left of primary | Confirms if dirty ("Unsaved changes") |
| Delete | DANGER | Far left, separated | Opens CrmDialog confirm |
| Save Draft | SECONDARY | Left of primary | Saves without validation |

### Dirty state
- Track changes via state holder
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

## 8. CRM-Specific Form Patterns

### Contact form
```
Section: Basic Information
  - First name (required, InputField)
  - Last name (required, InputField)
  - Email (optional, InputField with email validation)
  - Phone (optional, InputField)

Section: Company & Role
  - Company (optional, SelectField → companies)
  - Job title (optional, InputField)

Section: Classification
  - Tags (optional, multi-select chips)
  - Customer tier (Radio: Self-serve / Business / Enterprise)

Section: Notes
  - Notes (optional, TextArea, rows=4)
```

### Deal form
```
Section: Deal Details
  - Title (required, InputField)
  - Company (required, SelectField)
  - Value (optional, InputField, currency format)
  - Close date (optional, DatePicker)

Section: Pipeline
  - Stage (SelectField: Prospecting → Closed Won/Lost)
  - Probability (InputField, 0-100%)

Section: Notes
  - Notes (optional, TextArea)
  - Tags (optional, multi-select)
```

### Invoice form
```
Section: Invoice Details
  - Contact (required, SelectField)
  - Subscription (optional, SelectField)
  - Currency (SelectField: EUR/USD/GBP)
  - Due date (required, DatePicker)

Section: Line Items (dynamic)
  - [DataTable: Description | Qty | Unit Price | Tax | Total]
  - [+ Add Line Item] button

Section: Totals (read-only)
  - Subtotal, Tax, Total
```

---

## 9. Accessibility

- All inputs have associated labels (no floating labels — always visible above)
- Required fields announced to screen readers: "First name, required"
- Error messages associated via semantics: `error("Name is required")`
- Tab order follows visual order (top-to-bottom, left-to-right)
- Enter key submits form from any field
- Escape key in dialog forms closes without saving (with dirty confirm)
