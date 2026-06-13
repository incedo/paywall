# Article Metering (PW-20..24, MT-*) — mirrors the Kotlin BDD steps 1:1
Feature: Article metering
  The metered paywall lets a visitor read a limited number of premium
  articles per calendar month. Reads are counted server-side when the full
  article is served (MT-04); re-reads consume no extra credit (PW-21).

  Background:
    Given a visitor assigned to the metered variant with a limit of 5

  Scenario: Reading within the free limit
    When the visitor reads 5 distinct premium articles
    Then every article is served in full
    And the meter shows 5 of 5 used

  Scenario: The gate appears at limit plus one
    When the visitor reads 5 distinct premium articles
    And the visitor opens one more premium article
    Then the gate is shown with the metered wall
    And a wall_shown event is recorded with the meter context

  Scenario: Re-reading a counted article consumes no credit
    When the visitor reads 5 distinct premium articles
    And the visitor re-reads the first article
    Then the article is served in full
    And the meter still shows 5 of 5 used

  Scenario: An audited meter reset restores access
    When the visitor reads 5 distinct premium articles
    And support resets the meter citing "support ticket 4711"
    Then the visitor can read a new premium article in full
