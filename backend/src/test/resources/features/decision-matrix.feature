# Decision matrix — all 32 cells (NFR-12) — mirrors DecisionMatrixFeatureTest 1:1
Feature: Access decision matrix
  NFR-12 mandates exhaustive coverage of every combination of
  paywall type x visitor state x content tier x (basic vs complete).
  Each scenario name identifies the cell; together they form the
  complete regression suite for the access decision engine.

  Background:
    Given the default experiment with hard, metered, freemium and dynamic variants

  # ── HARD WALL ──────────────────────────────────────────────────────────────

  Scenario: Hard / anonymous / free content
    Given an anonymous visitor assigned to hard variant
    When the visitor requests a free article
    Then the article is served in full

  Scenario: Hard / anonymous / premium content
    Given an anonymous visitor assigned to hard variant
    When the visitor requests a premium article
    Then the gate is shown with wall type "hard"

  Scenario: Hard / registered / free content
    Given a registered visitor (no subscription) assigned to hard variant
    When the visitor requests a free article
    Then the article is served in full

  Scenario: Hard / registered / premium content
    Given a registered visitor (no subscription) assigned to hard variant
    When the visitor requests a premium article
    Then the gate is shown with wall type "hard"

  Scenario: Hard / basic subscriber / free content
    Given a basic subscriber assigned to hard variant
    When the visitor requests a free article
    Then the article is served in full

  Scenario: Hard / basic subscriber / premium content
    Given a basic subscriber assigned to hard variant
    When the visitor requests a premium article
    Then the article is served in full

  Scenario: Hard / expired subscriber / premium content
    Given a visitor whose entitlement has expired, assigned to hard variant
    When the visitor requests a premium article
    Then the gate is shown with wall type "hard"

  # ── METERED WALL ───────────────────────────────────────────────────────────

  Scenario: Metered / anonymous within limit / premium content
    Given an anonymous visitor assigned to metered variant with 2 of 5 used
    When the visitor requests a new premium article
    Then the article is served in full

  Scenario: Metered / anonymous at limit / premium content
    Given an anonymous visitor assigned to metered variant with 5 of 5 used
    When the visitor requests a new premium article
    Then the gate is shown with wall type "metered"

  Scenario: Metered / registered within limit / premium content
    Given a registered visitor assigned to metered variant with 2 of 5 used
    When the visitor requests a new premium article
    Then the article is served in full

  Scenario: Metered / registered at limit / premium content
    Given a registered visitor assigned to metered variant with 5 of 5 used
    When the visitor requests a new premium article
    Then the gate is shown with wall type "metered"

  Scenario: Metered / basic subscriber / premium content
    Given a basic subscriber assigned to metered variant
    When the visitor requests a premium article
    Then the article is served in full

  Scenario: Metered / expired subscriber / premium content
    Given a visitor whose entitlement has expired, assigned to metered variant
    When the visitor requests a new premium article (no prior meter usage)
    Then the article is served in full (meter still has credit)

  # ── FREEMIUM WALL ──────────────────────────────────────────────────────────

  Scenario: Freemium / anonymous / free content
    Given an anonymous visitor assigned to freemium variant
    When the visitor requests a free article
    Then the article is served in full

  Scenario: Freemium / anonymous / premium content
    Given an anonymous visitor assigned to freemium variant
    When the visitor requests a premium article
    Then the gate is shown with wall type "soft"

  Scenario: Freemium / registered / premium content
    Given a registered visitor (no subscription) assigned to freemium variant
    When the visitor requests a premium article
    Then the gate is shown with wall type "soft"

  Scenario: Freemium / basic subscriber / premium content
    Given a basic subscriber assigned to freemium variant
    When the visitor requests a premium article
    Then the article is served in full

  Scenario: Freemium / expired subscriber / premium content
    Given a visitor whose entitlement has expired, assigned to freemium variant
    When the visitor requests a premium article
    Then the gate is shown with wall type "soft"

  # ── DYNAMIC WALL ───────────────────────────────────────────────────────────

  Scenario: Dynamic / anonymous no-score / free content
    Given an anonymous visitor assigned to dynamic variant
    When the visitor requests a free article
    Then the article is served in full

  Scenario: Dynamic / anonymous high-propensity / premium content
    Given an anonymous visitor with externalScore=80, assigned to dynamic variant
    When the visitor requests a premium article
    Then the gate is shown with wall type "dynamic" (hard-gate intensity, DY-02)

  Scenario: Dynamic / anonymous medium-propensity / premium content
    Given an anonymous visitor with externalScore=50, assigned to dynamic variant
    When the visitor requests a premium article
    Then the gate is shown with wall type "soft"

  Scenario: Dynamic / anonymous low-propensity within floor limit / premium content
    Given an anonymous visitor with externalScore=20, assigned to dynamic variant with 0 articles read
    When the visitor requests a new premium article
    Then the article is served in full (within floor limit)

  Scenario: Dynamic / basic subscriber / premium content
    Given a basic subscriber assigned to dynamic variant
    When the visitor requests a premium article
    Then the article is served in full

  Scenario: Dynamic / expired subscriber / premium content
    Given a visitor whose entitlement has expired, assigned to dynamic variant
    When the visitor requests a premium article
    Then the gate is shown

  # ── COMPLETE-TIER LOCK (UP-12) ─────────────────────────────────────────────

  Scenario: Hard / basic subscriber / complete-tier content
    Given a basic subscriber (rank 1) assigned to hard variant
    When the visitor requests complete-tier content
    Then access is gated and tierLocked is true

  Scenario: Hard / complete subscriber / complete-tier content
    Given a complete subscriber (rank 2) assigned to hard variant
    When the visitor requests complete-tier content
    Then the article is served in full

  Scenario: Metered / basic subscriber / complete-tier content
    Given a basic subscriber (rank 1) assigned to metered variant
    When the visitor requests complete-tier content
    Then access is gated and tierLocked is true
