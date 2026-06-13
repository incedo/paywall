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
import nl.incedo.paywall.entitlements.EntitlementDecision
import nl.incedo.paywall.entitlements.EntitlementGranted
import nl.incedo.paywall.grants.GrantDecision
import nl.incedo.paywall.grants.GrantIssued
import nl.incedo.paywall.metering.MeterDecision
import nl.incedo.paywall.metering.MeterPeriod

/**
 * PW-50: registration wall — anonymous visitors are gated with registrationRequired=true
 * when the variant's registrationWall flag is enabled. Logged-in visitors bypass the
 * registration check and proceed to the normal paywall strategy.
 */
class RegistrationWallTest {

    private val now = 1_750_000_000_000L
    private val period = MeterPeriod("2026-06")
    private val articleId = ArticleId("a-premium")
    private val article = Article(articleId, ContentTier.PREMIUM)
    private val strategy = StrategyConfig.Hard

    private fun decide(
        subject: Subject,
        registrationWall: Boolean = true,
        entitlement: EntitlementDecision = EntitlementDecision(),
        grant: GrantDecision = GrantDecision(articleId),
    ) = AccessDecisionEngine.decide(
        AccessRequest(
            article = article,
            subject = subject,
            strategy = strategy,
            entitlement = entitlement,
            grant = grant,
            meter = MeterDecision(period),
            registrationWall = registrationWall,
            nowEpochMs = now,
        ),
    )

    @Test
    fun anonymousVisitorGatedWithRegistrationRequired() {
        val decision = decide(Subject(VisitorId("vis-anon")))
        val gated = assertIs<AccessDecision.Gated>(decision,
            "PW-50: anonymous visitor must be gated when registrationWall=true")
        assertTrue(gated.registrationRequired,
            "PW-50: registrationRequired must be true so client shows registration wall")
    }

    @Test
    fun loggedInVisitorBypassesRegistrationWall() {
        val decision = decide(Subject(VisitorId("vis-1"), UserId("user-1")))
        val gated = assertIs<AccessDecision.Gated>(decision,
            "PW-50: logged-in visitor should proceed to normal strategy (Hard → gate)")
        assertFalse(gated.registrationRequired,
            "PW-50: registrationRequired must be false for logged-in visitor")
    }

    @Test
    fun registrationWallFalseHasNoEffect() {
        val decision = decide(Subject(VisitorId("vis-anon-2")), registrationWall = false)
        val gated = assertIs<AccessDecision.Gated>(decision, "Hard wall always gates")
        assertFalse(gated.registrationRequired,
            "PW-50: registrationWall=false must not produce registrationRequired")
    }

    @Test
    fun freeContentNotAffectedByRegistrationWall() {
        val freeArticle = Article(ArticleId("a-free"), ContentTier.FREE)
        val decision = AccessDecisionEngine.decide(
            AccessRequest(
                article = freeArticle,
                subject = Subject(VisitorId("vis-anon-3")),
                strategy = strategy,
                entitlement = EntitlementDecision(),
                grant = GrantDecision(ArticleId("a-free")),
                meter = MeterDecision(period),
                registrationWall = true,
                nowEpochMs = now,
            ),
        )
        assertEquals(
            AccessDecision.Full(AccessReason.FREE_CONTENT),
            decision,
            "PW-50: free content must never be gated regardless of registrationWall",
        )
    }

    @Test
    fun entitlementOverridesRegistrationWall() {
        val entitlement = EntitlementDecision().apply {
            apply(
                EntitlementGranted(
                    subjectId = SubjectId("visitor:vis-entitled"),
                    planId = PlanId("basic-monthly"),
                    subscriptionRef = SubscriptionId("sub-1"),
                    validUntilEpochMs = now + 86_400_000,
                ),
            )
        }
        val decision = decide(Subject(VisitorId("vis-entitled")), entitlement = entitlement)
        assertEquals(
            AccessDecision.Full(AccessReason.ENTITLED),
            decision,
            "PW-50: an active entitlement must grant access before the registration wall is checked",
        )
    }

    @Test
    fun liveGrantOverridesRegistrationWall() {
        val grant = GrantDecision(articleId).apply {
            apply(
                GrantIssued(
                    grantId = GrantId("g-pw50"),
                    subjectId = SubjectId("visitor:vis-grant"),
                    articleId = articleId,
                    grantedBy = "support",
                    expiresAtEpochMs = now + 3_600_000,
                ),
            )
        }
        val decision = decide(Subject(VisitorId("vis-grant")), grant = grant)
        assertEquals(
            AccessDecision.Full(AccessReason.GRANT),
            decision,
            "PW-50: a live grant must grant access before the registration wall is checked",
        )
    }
}
