package nl.incedo.paywall.access

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.cep.CepAdviceDecision
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.entitlements.EntitlementDecision
import nl.incedo.paywall.grants.GrantDecision
import nl.incedo.paywall.metering.MeterDecision
import nl.incedo.paywall.metering.MeterIncremented
import nl.incedo.paywall.metering.MeterPeriod

/**
 * DY-01/02/04: heuristic propensity scorer + threshold mapping in the engine.
 */
class PropensityScorerTest {

    private val now = 1_750_000_000_000L
    private val period = MeterPeriod("2026-06")
    private val subjectId = SubjectId.of(VisitorId("v-dy"))

    private fun wallEvent(type: WallEventType, ageMs: Long = 0L) = WallEventRecorded(
        eventType = type,
        subjectId = subjectId,
        variant = "dynamic",
        channel = "web",
        occurredAtEpochMs = now - ageMs,
    )

    private fun meterIncrement() = MeterIncremented(subjectId, ArticleId("a-1"), period)

    @Test
    fun scorerReturnsZeroForFreshVisitorWithNoHistory() {
        val score = HeuristicPropensityScorer.score(emptyList(), meterUsed = 0, nowEpochMs = now)
        assertEquals(0, score)
    }

    @Test
    fun scorerIsInBoundsForHeavyUser() {
        // DY-01: score must stay in [0, 100]
        val events = List(20) { wallEvent(WallEventType.ARTICLE_READ) } +
            List(30) { meterIncrement() }
        val score = HeuristicPropensityScorer.score(events, meterUsed = 30, nowEpochMs = now)
        assertTrue(score in 0..100, "score $score out of [0, 100]")
        assertEquals(100, score, "heavy user should hit the cap")
    }

    @Test
    fun eventsOlderThan30DaysDoNotCountAsRecentActivity() {
        val thirtyOneDaysAgo = 31L * 24 * 3600 * 1000
        val events = List(10) { wallEvent(WallEventType.PAGE_VIEW, ageMs = thirtyOneDaysAgo) }
        val score = HeuristicPropensityScorer.score(events, meterUsed = 0, nowEpochMs = now)
        // Recent activity = 0; tenure contributes (days since first ≈ 31, capped at 60)
        val tenureScore = minOf(31, 60)
        assertEquals(tenureScore, score, "only tenure contributes for events older than 30 days")
    }

    @Test
    fun dynamicStrategyGatesWhenScoreExceedsTSoft() {
        // DY-02: score >= tSoft (40) → gate. Default tSoft = 40.
        val strategy = StrategyConfig.Dynamic(floorLimit = 100, tSoft = 40)
        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = Article(ArticleId("a-1"), ContentTier.PREMIUM),
                subject = Subject(VisitorId("v-1")),
                strategy = strategy,
                entitlement = EntitlementDecision(),
                grant = GrantDecision(ArticleId("a-1")),
                meter = MeterDecision(period),
                propensityScore = 40, // exactly at threshold
                nowEpochMs = now,
            ),
        )
        assertIs<AccessDecision.Gated>(decision, "score=40 at tSoft=40 should gate")
    }

    @Test
    fun dynamicStrategyServesFullWhenScoreBelowTSoft() {
        // DY-02: score < tSoft → open
        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = Article(ArticleId("a-1"), ContentTier.PREMIUM),
                subject = Subject(VisitorId("v-1")),
                strategy = StrategyConfig.Dynamic(floorLimit = 100),
                entitlement = EntitlementDecision(),
                grant = GrantDecision(ArticleId("a-1")),
                meter = MeterDecision(period),
                propensityScore = 39,
                nowEpochMs = now,
            ),
        )
        assertIs<AccessDecision.Full>(decision, "score=39 below tSoft=40 should open")
    }

    @Test
    fun nullScoreFallsBackToCepAndFloorOnly() {
        // DY-04: when propensityScore is null (no scorer configured), the engine
        // falls back to CEP advice + floor rule only
        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = Article(ArticleId("a-1"), ContentTier.PREMIUM),
                subject = Subject(VisitorId("v-1")),
                strategy = StrategyConfig.Dynamic(floorLimit = 100, tSoft = 1), // very low threshold
                entitlement = EntitlementDecision(),
                grant = GrantDecision(ArticleId("a-1")),
                meter = MeterDecision(period),
                propensityScore = null, // no scorer — ignore threshold
                nowEpochMs = now,
            ),
        )
        assertIs<AccessDecision.Full>(decision, "null propensityScore must not trigger score-based gate")
    }
}
