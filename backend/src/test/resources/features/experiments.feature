# Experiment / variant assignment (EX-01..05) — mirrors ExperimentsFeatureTest 1:1
Feature: Experiment variant assignment
  Every visitor is deterministically assigned to a variant on first
  encounter and the same variant is served on every subsequent request
  (no flicker). A force-variant debug header overrides the assignment.
  The control variant is flagged so holdout reporting works.

  Background:
    Given the default experiment with metered and hard variants

  Scenario: Variant assignment is deterministic
    Given a visitor "visitor-A"
    When the visitor requests access for the first time
    Then a variant is assigned and recorded
    And the same variant is returned on the next request

  Scenario: Variant assignment is sticky across articles
    Given a visitor "visitor-B" assigned to the metered variant
    When the visitor accesses three different premium articles
    Then every response carries the same variant name

  Scenario: Force-variant overrides assignment (EX-05 debug)
    Given a visitor "visitor-C"
    When the visitor requests access with ?forceVariant=hard query parameter
    Then the response variant is "hard"
    And no permanent assignment is persisted for that request

  Scenario: Control variant gives unlimited access (EX-04)
    Given the experiment contains a control variant (isControl=true) with unlimited meter
    When a visitor is deterministically assigned to the control variant
    Then premium articles are always served in full
    And the variant name appears as "control" in the response
