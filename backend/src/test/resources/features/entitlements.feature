# Entitlement lifecycle (AC-01..08, SUB-01..07) — mirrors EntitlementsFeatureTest 1:1
Feature: Entitlement lifecycle
  The paywall ingests entitlement changes from the external subscription
  administration via the integration webhook. A subscriber gets full access
  to premium content; revoking the entitlement immediately gates again.
  A payment-failure moves the subscription to grace period (SUB-05).

  Background:
    Given a clean event store with the default paywall config

  Scenario: Subscriber gets full access (AC-01)
    Given the subscription administration grants entitlement for subject "sub-1" plan "basic"
    When subject "sub-1" requests a premium article
    Then the article is served in full

  Scenario: Revoking entitlement gates the visitor (AC-01)
    Given subject "sub-1" holds an active basic entitlement
    When the subscription administration revokes the entitlement
    And subject "sub-1" requests a premium article
    Then the response status is 402 and the gate is shown

  Scenario: Grace period — past_due still gets access (SUB-05)
    Given subject "sub-2" holds an active basic entitlement
    When a payment_failure webhook arrives for "sub-2"
    Then subject "sub-2" still gets full access during the 7-day grace window

  Scenario: Duplicate webhook is idempotent (SUB-02/NFR-03)
    Given a grant webhook with id "wh-42" already processed for "sub-3"
    When the same webhook with id "wh-42" arrives again
    Then the response is 202 Accepted
    And only one entitlement event exists for "sub-3"
