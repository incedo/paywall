# Storybook BC (SB-01 – SB-15) — mirrors StorybookFeatureTest 1:1
Feature: Storybook story, scenario, and control-schema management
  The design system's interactive documentation layer is backed by three
  bounded contexts: Story (a component entry), Scenario (a named state of
  that component), and ControlSchema (the knob definitions for a scenario).
  Each is managed as an event-sourced aggregate through the storybook API.

  Background:
    Given a clean storybook event store

  # ── Story lifecycle ───────────────────────────────────────────────────────────

  Scenario: Registering a new story succeeds
    When staff registers story "button" with key "button"
    Then the story is returned with id "button" and lifecycle "ACTIVE"
    And the story appears in the story list

  Scenario: Registering a story with a duplicate key is rejected (BR-3)
    Given story "s1" with key "button" is registered
    When staff tries to register story "s2" also with key "button"
    Then the registration is rejected with 409 Conflict

  Scenario: Fetching an unknown story returns 404
    When staff requests story "nonexistent"
    Then the response is 404 Not Found

  # ── Scenario lifecycle ────────────────────────────────────────────────────────

  Scenario: Registering a scenario under an existing story succeeds
    Given story "s1" with key "button" is registered
    When staff registers scenario "sc1" under story "s1"
    Then the scenario is returned with storyId "s1"
    And the scenario appears in the story's scenario list

  Scenario: Scenario key must be unique within a story (BR-4)
    Given story "s1" with key "button" is registered
    And scenario "sc1" with key "default" is registered under story "s1"
    When staff tries to register scenario "sc2" with key "default" under story "s1"
    Then the registration is rejected with 409 Conflict

  Scenario: Registering a scenario under an unknown story returns 404
    When staff tries to register scenario "sc1" under story "unknown"
    Then the response is 404 Not Found

  # ── ControlSchema lifecycle ───────────────────────────────────────────────────

  Scenario: Creating a control schema for a scenario
    Given story "s1", scenario "sc1" are registered
    When staff creates control schema "cs1" for scenario "sc1"
    Then the schema is returned with zero controls

  Scenario: Adding a control to a schema
    Given story "s1", scenario "sc1", and control schema "cs1" are registered
    When staff adds control "variant" to schema "cs1"
    Then the controls list contains "variant"

  Scenario: Duplicate control key is rejected (BR-4)
    Given story "s1", scenario "sc1", and schema "cs1" with control "variant" registered
    When staff tries to add another control with key "variant" to schema "cs1"
    Then the registration is rejected with 409 Conflict

  Scenario: Removing a control excludes it from the active view (BR-10)
    Given story "s1", scenario "sc1", and schema "cs1" with control "variant" registered
    When staff removes control "c1" from schema "cs1"
    Then the controls list is empty

  Scenario: Changing a control's default value
    Given story "s1", scenario "sc1", and schema "cs1" with control "variant" registered
    When staff changes the default of control "c1" in schema "cs1" to "secondary"
    Then the control "variant" has default "secondary"

  Scenario: Schema from unknown scenario returns 404 (BR-2)
    When staff tries to create control schema for scenario "unknown"
    Then the response is 404 Not Found

  # ── Story update and archive ──────────────────────────────────────────────────

  Scenario: Updating story metadata
    Given story "s1" with key "button" is registered
    When staff updates story "s1" with title "Button v2"
    Then the story title is "Button v2"

  Scenario: Archiving a story removes it from the list
    Given story "s1" with key "button" is registered
    When staff archives story "s1"
    Then the story is no longer visible

  # ── Scenario update and archive ───────────────────────────────────────────────

  Scenario: Getting a scenario by id
    Given story "s1", scenario "sc1" are registered
    When staff fetches scenario "sc1"
    Then the scenario is returned with scenarioId "sc1"

  Scenario: Updating scenario metadata
    Given story "s1", scenario "sc1" are registered
    When staff updates scenario "sc1" with title "Default state v2"
    Then the scenario title is "Default state v2"

  Scenario: Archiving a scenario removes it from the scenario list
    Given story "s1", scenario "sc1" are registered
    When staff archives scenario "sc1"
    Then the scenario is no longer visible

  # ── Responsive BC ────────────────────────────────────────────────────────────

  Scenario: Registering a responsive profile for a story
    Given story "s1" is registered
    When staff registers responsive profile "mobile/phone-first" for story "s1"
    Then a responsive profile is created with key "mobile/phone-first"

  Scenario: Adding form factors to a responsive profile
    Given a responsive profile "mobile/phone-first" exists for story "s1"
    When staff adds form factor "PHONE" to the responsive profile
    Then the responsive profile has form factor "PHONE"

  Scenario: Responsive profile listing excludes archived profiles
    Given a responsive profile "mobile/phone-first" exists for story "s1"
    When staff archives the responsive profile
    Then the story has no active responsive profiles
