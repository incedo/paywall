# Wall designer lifecycle (ADM-10..14, PW-*) — mirrors WallsFeatureTest 1:1
Feature: Wall designer lifecycle
  The admin console lets staff create, edit, publish and roll back
  paywall/registration wall definitions. Walls start as drafts, are
  published explicitly, and every save bumps the version counter so
  concurrent editors get a conflict rather than a lost update.

  Background:
    Given a clean event store

  Scenario: Creating a draft wall
    When staff saves a metered wall "Monthly limit" for the first time
    Then the wall is returned with status "draft" and version 1
    And the wall appears in the wall list

  Scenario: Editing bumps the version and keeps the draft
    Given a saved wall "Monthly limit" at version 1
    When staff edits the wall with an updated title
    Then the wall is returned with status "draft" and version 2

  Scenario: Publishing changes the status to published
    Given a saved wall "Monthly limit" at version 1
    When staff publishes the wall
    Then the wall status is "published"

  Scenario: Editing a published wall returns it to draft
    Given a published wall "Monthly limit" at version 1
    When staff edits the wall
    Then the wall status is "draft"
    And the version is 2

  Scenario: Concurrent editor gets a conflict
    Given a saved wall "Monthly limit" at version 1
    And another editor also holds version 1
    When the second editor tries to save with expected version 1
    Then the save is rejected with a 409 Conflict

  Scenario: Unknown wall returns 404
    When staff requests a wall that does not exist
    Then the response is 404 Not Found

  Scenario: Rollback restores an earlier version
    Given wall "RB wall" saved at version 1 with title "Original"
    And the wall edited to version 2 with title "Changed"
    When staff rolls back to version 1
    Then the active title is "Original"
    And the wall is at version 3 as a new draft
