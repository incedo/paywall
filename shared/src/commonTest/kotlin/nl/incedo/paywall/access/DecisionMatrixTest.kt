package nl.incedo.paywall.access

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.PlanId
import nl.incedo.paywall.core.SubscriptionId
import nl.incedo.paywall.core.UserId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.entitlements.EntitlementDecision
import nl.incedo.paywall.entitlements.EntitlementGranted
import nl.incedo.paywall.grants.GrantDecision
import nl.incedo.paywall.metering.MeterDecision
import nl.incedo.paywall.metering.MeterIncremented
import nl.incedo.paywall.metering.MeterPeriod

/**
 * NFR-12: the decision matrix — each paywall type × (anonymous, registered,
 * subscriber, expired) × (free, premium). 4 × 4 × 2 = 32 cases, exercised
 * exhaustively below. Gating states are chosen deterministically: the meter
 * is exhausted and the dynamic score is at the threshold, so every
 * non-entitled premium case must gate.
 */
class DecisionMatrixTest {

    private enum class VisitorState { ANONYMOUS, REGISTERED, SUBSCRIBER, EXPIRED }

    private val now = 1_750_000_000_000L
    private val period = MeterPeriod("2026-06")
    private val article = { tier: ContentTier -> Article(ArticleId("a-1"), tier) }

    private val strategies = mapOf(
        "hard" to StrategyConfig.Hard,
        "metered" to StrategyConfig.Metered(limit = 3),
        "freemium" to StrategyConfig.Freemium,
        "dynamic" to StrategyConfig.Dynamic(threshold = 0.5, floorLimit = 10),
    )

    private fun subject(state: VisitorState) = when (state) {
        VisitorState.ANONYMOUS -> Subject(VisitorId("v-1"))
        else -> Subject(VisitorId("v-1"), UserId("u-1"))
    }

    private fun entitlement(state: VisitorState): EntitlementDecision {
        val decision = EntitlementDecision()
        when (state) {
            VisitorState.SUBSCRIBER -> decision.apply(
                EntitlementGranted(
                    subjectId = subject(state).subjectId,
                    planId = PlanId("pro"),
                    subscriptionRef = SubscriptionId("sub-1"),
                    validUntilEpochMs = now + 86_400_000,
                ),
            )
            VisitorState.EXPIRED -> decision.apply(
                EntitlementGranted(
                    subjectId = subject(state).subjectId,
                    planId = PlanId("pro"),
                    subscriptionRef = SubscriptionId("sub-1"),
                    validUntilEpochMs = now - 1, // paid period over (AC-02)
                ),
            )
            else -> {}
        }
        return decision
    }

    /** Meter exhausted: 3 other articles already counted this period. */
    private fun exhaustedMeter(state: VisitorState): MeterDecision {
        val meter = MeterDecision(period)
        repeat(3) { i ->
            meter.apply(MeterIncremented(subject(state).subjectId, ArticleId("other-$i"), period))
        }
        return meter
    }

    private fun decide(strategy: StrategyConfig, state: VisitorState, tier: ContentTier): AccessDecision =
        AccessDecisionEngine.decide(
            AccessRequest(
                article = article(tier),
                subject = subject(state),
                strategy = strategy,
                entitlement = entitlement(state),
                grant = GrantDecision(ArticleId("a-1")),
                meter = exhaustedMeter(state),
                propensityScore = 0.5, // at threshold → dynamic gates
                nowEpochMs = now,
            ),
        )

    @Test
    fun decisionMatrix_all32Cases() {
        var cases = 0
        for ((name, strategy) in strategies) {
            for (state in VisitorState.entries) {
                for (tier in ContentTier.entries) {
                    cases += 1
                    val decision = decide(strategy, state, tier)
                    val label = "$name × $state × $tier"
                    when {
                        // Free content is never gated, for anyone (PW-01)
                        tier == ContentTier.FREE ->
                            assertEquals(
                                AccessDecision.Full(AccessReason.FREE_CONTENT),
                                decision,
                                label,
                            )
                        // A valid entitlement always wins (PW-04/AC-02)
                        state == VisitorState.SUBSCRIBER ->
                            assertEquals(
                                AccessDecision.Full(AccessReason.ENTITLED),
                                decision,
                                label,
                            )
                        // Everyone else hits the gate in these fixtures —
                        // including EXPIRED, which must behave like anonymous (AC-02)
                        else -> assertIs<AccessDecision.Gated>(decision, label)
                    }
                }
            }
        }
        assertEquals(32, cases, "NFR-12 requires a minimum of 32 matrix cases")
    }

    @Test
    fun meteredServesFullContentWhileCreditRemains() {
        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = article(ContentTier.PREMIUM),
                subject = subject(VisitorState.ANONYMOUS),
                strategy = StrategyConfig.Metered(limit = 3),
                entitlement = EntitlementDecision(),
                grant = GrantDecision(ArticleId("a-1")),
                meter = MeterDecision(period), // fresh meter
                nowEpochMs = now,
            ),
        )
        assertEquals(AccessDecision.Full(AccessReason.METER_CREDIT, countsTowardMeter = true), decision)
    }

    @Test
    fun meteredReReadDoesNotCountAgain() {
        val meter = MeterDecision(period)
        meter.apply(MeterIncremented(subject(VisitorState.ANONYMOUS).subjectId, ArticleId("a-1"), period))
        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = article(ContentTier.PREMIUM),
                subject = subject(VisitorState.ANONYMOUS),
                strategy = StrategyConfig.Metered(limit = 3),
                entitlement = EntitlementDecision(),
                grant = GrantDecision(ArticleId("a-1")),
                meter = meter,
                nowEpochMs = now,
            ),
        )
        // PW-21: already-counted article stays readable without new credit
        assertEquals(AccessDecision.Full(AccessReason.METER_CREDIT, countsTowardMeter = false), decision)
    }

    @Test
    fun registeredMeterLimitCanExceedAnonymous() {
        // PW-25: registered visitors may get a higher limit (5 vs 3)
        val strategy = StrategyConfig.Metered(limit = 3, registeredLimit = 5)
        val state = VisitorState.REGISTERED
        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = article(ContentTier.PREMIUM),
                subject = subject(state),
                strategy = strategy,
                entitlement = EntitlementDecision(),
                grant = GrantDecision(ArticleId("a-1")),
                meter = exhaustedMeter(state), // 3 used: anonymous would gate
                nowEpochMs = now,
            ),
        )
        assertIs<AccessDecision.Full>(decision)
    }

    @Test
    fun dynamicBelowThresholdServesFullContent() {
        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = article(ContentTier.PREMIUM),
                subject = subject(VisitorState.ANONYMOUS),
                strategy = StrategyConfig.Dynamic(threshold = 0.5, floorLimit = 10),
                entitlement = EntitlementDecision(),
                grant = GrantDecision(ArticleId("a-1")),
                meter = MeterDecision(period),
                propensityScore = 0.49,
                nowEpochMs = now,
            ),
        )
        assertEquals(AccessDecision.Full(AccessReason.BELOW_THRESHOLD, countsTowardMeter = true), decision)
    }

    @Test
    fun dynamicFloorRuleGatesRegardlessOfScore() {
        // PW-42: the gate appears at or before the Nth premium article even for low scores
        val state = VisitorState.ANONYMOUS
        val meter = MeterDecision(period)
        repeat(10) { i ->
            meter.apply(MeterIncremented(subject(state).subjectId, ArticleId("other-$i"), period))
        }
        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = article(ContentTier.PREMIUM),
                subject = subject(state),
                strategy = StrategyConfig.Dynamic(threshold = 0.99, floorLimit = 10),
                entitlement = EntitlementDecision(),
                grant = GrantDecision(ArticleId("a-1")),
                meter = meter,
                propensityScore = 0.0,
                nowEpochMs = now,
            ),
        )
        assertIs<AccessDecision.Gated>(decision)
    }
}
