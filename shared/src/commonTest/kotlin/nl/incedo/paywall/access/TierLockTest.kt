package nl.incedo.paywall.access

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.GrantId
import nl.incedo.paywall.core.PlanId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.SubscriptionId
import nl.incedo.paywall.core.UserId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.grants.GrantIssued
import nl.incedo.paywall.entitlements.EntitlementDecision
import nl.incedo.paywall.entitlements.EntitlementGranted
import nl.incedo.paywall.grants.GrantDecision
import nl.incedo.paywall.metering.MeterDecision
import nl.incedo.paywall.metering.MeterPeriod

/**
 * UP-12: complete-tier content requires a rank-2 (complete) plan.
 * Basic subscribers (rank=1) are gated with tierLocked=true so the
 * client can show a tier-upgrade offer instead of the base subscribe wall.
 */
class TierLockTest {

    private val now = 1_750_000_000_000L
    private val period = MeterPeriod("2026-06")
    private val subject = Subject(VisitorId("v-1"), UserId("u-1"))
    private val article = Article(ArticleId("a-complete"), ContentTier.COMPLETE)
    private val strategy = StrategyConfig.Hard

    private fun entitlementWith(planId: PlanId): EntitlementDecision {
        val decision = EntitlementDecision()
        decision.apply(
            EntitlementGranted(
                subjectId = subject.subjectId,
                planId = planId,
                subscriptionRef = SubscriptionId("sub-1"),
                validUntilEpochMs = now + 86_400_000, // valid for 24 h
            ),
        )
        return decision
    }

    private fun decide(entitlement: EntitlementDecision) = AccessDecisionEngine.decide(
        AccessRequest(
            article = article,
            subject = subject,
            strategy = strategy,
            entitlement = entitlement,
            grant = GrantDecision(ArticleId("a-complete")),
            meter = MeterDecision(period),
            nowEpochMs = now,
        ),
    )

    @Test
    fun completeSubscriberAccessesCompleteTierContent() {
        val decision = decide(entitlementWith(PlanId("complete-monthly")))
        assertEquals(
            AccessDecision.Full(AccessReason.ENTITLED),
            decision,
            "UP-12: complete plan (rank=2) must grant access to complete-tier content",
        )
    }

    @Test
    fun basicSubscriberIsGatedWithTierLock() {
        val decision = decide(entitlementWith(PlanId("basic-monthly")))
        val gated = assertIs<AccessDecision.Gated>(decision,
            "UP-12: basic plan (rank=1) must be gated on complete-tier content")
        assertTrue(gated.tierLocked,
            "UP-12: tierLocked must be true so the client can show a tier-upgrade offer")
    }

    @Test
    fun anonymousVisitorIsGatedWithoutTierLock() {
        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = article,
                subject = Subject(VisitorId("v-anon")),
                strategy = strategy,
                entitlement = EntitlementDecision(),
                grant = GrantDecision(ArticleId("a-complete")),
                meter = MeterDecision(period),
                nowEpochMs = now,
            ),
        )
        val gated = assertIs<AccessDecision.Gated>(decision, "Anonymous must be gated")
        assertFalse(gated.tierLocked,
            "UP-12: tierLocked must be false for anonymous — no subscription at all")
    }

    @Test
    fun unknownPlanRankGrantsFullAccessForBackwardCompat() {
        // Legacy plan IDs not in DefaultPlans get rank=0 → any-tier access
        val decision = decide(entitlementWith(PlanId("legacy-pro")))
        assertEquals(
            AccessDecision.Full(AccessReason.ENTITLED),
            decision,
            "UP-12: unknown plan (rank=0) must receive full access on all tiers (backward compat)",
        )
    }

    @Test
    fun liveGrantOverridesTierLockForBasicSubscriber() {
        val entitlement = entitlementWith(PlanId("basic-monthly"))
        val grant = GrantDecision(ArticleId("a-complete"))
        grant.apply(
            GrantIssued(
                grantId = GrantId("g-1"),
                subjectId = subject.subjectId,
                articleId = ArticleId("a-complete"),
                grantedBy = "support",
                expiresAtEpochMs = now + 3_600_000, // 1 h
            ),
        )
        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = article,
                subject = subject,
                strategy = strategy,
                entitlement = entitlement,
                grant = grant,
                meter = MeterDecision(period),
                nowEpochMs = now,
            ),
        )
        assertEquals(
            AccessDecision.Full(AccessReason.GRANT),
            decision,
            "UP-12: a live grant must override tier-lock for basic subscribers",
        )
    }
}
