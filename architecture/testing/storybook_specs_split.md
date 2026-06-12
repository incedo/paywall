# CRM Storybook / Component Workbench — Spec Index

This document maps the storybook specification to its actual files.

---

## File Structure

```
specs/architecture/testing/
├── testing.md                                    ← Master testing strategy (includes workbench as layer 5)
├── compose_storybook_requirements.md             ← Overall workbench requirements
├── storybook_story_and_scenario_specs.md          ← Story + Scenario domain models
├── storybook_controls_and_responsive_specs.md     ← Controls + Responsive rules
├── storybook_decorators_phases_governance_specs.md ← Decorators + Phases + Governance
└── storybook_specs_split.md                       ← THIS FILE (index)
```

## Spec → Concept Mapping

| Concept | File | Status |
|---------|------|--------|
| **Workbench** (catalog, preview, env switching) | `compose_storybook_requirements.md` | DRAFT |
| **Story** (identity, grouping, metadata, lifecycle) | `storybook_story_and_scenario_specs.md` | DRAFT |
| **Scenario** (state variations, fake data) | `storybook_story_and_scenario_specs.md` | DRAFT |
| **Controls** (runtime-adjustable inputs) | `storybook_controls_and_responsive_specs.md` | DRAFT |
| **Responsive** (breakpoints, form factors) | `storybook_controls_and_responsive_specs.md` | DRAFT |
| **Decorators** (theme, scaffold, fake DI) | `storybook_decorators_phases_governance_specs.md` | DRAFT |
| **Phases** (rollout plan) | `storybook_decorators_phases_governance_specs.md` | DRAFT |
| **Governance** (coverage rules, quality gates) | `storybook_decorators_phases_governance_specs.md` | DRAFT |

## Dependency Graph

```
testing.md (master)
  └── compose_storybook_requirements.md (overall)
        ├── storybook_story_and_scenario_specs.md (core domain)
        │     └── storybook_controls_and_responsive_specs.md (interaction + layout)
        │           └── storybook_decorators_phases_governance_specs.md (wrapping + rollout)
        ├── ../design-system.md (tokens + components)
        └── ../forms-pattern.md (form layout rules)
```

## Relationship to Design System

The Component Workbench is a **derived artifact** of the design system:
- Every token category (colors, typography, spacing, etc.) gets a token showcase story
- Every component gets stories for each variant + state scenarios
- Responsive behavior is verified against the design system's breakpoint tokens
- Themes are tested against the design system's CrmDesignTheme bundles

The design system defines **what** components look like. The workbench verifies and documents **how** they look in practice.

## Current Implementation Status

| Phase | Status | Module |
|-------|--------|--------|
| Phase 1: Catalog + preview | **DONE** | `:docs` (12-page Compose Desktop app) |
| Phase 2: Stories + scenarios + controls | Not started | `:storybook` (planned) |
| Phase 3: Responsive comparison | Not started | `:storybook` |
| Phase 4: Visual regression | Not started | `:storybook` + CI |
| Phase 5: Token audit + discovery | Not started | `:storybook` |
