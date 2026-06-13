package nl.incedo.paywall.entitlements

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import nl.incedo.paywall.core.PlanId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.SubscriptionId
import nl.incedo.paywall.core.UserId
import nl.incedo.paywall.entitlements.SubscriptionPaused
import nl.incedo.paywall.entitlements.SubscriptionResumed

class EntitlementDecisionTest {

    private val subject = SubjectId.of(UserId("u-1"))
    private val now = 1_750_000_000_000L

    private fun granted(ref: String, validUntil: Long? = null) = EntitlementGranted(
        subjectId = subject,
        planId = PlanId("pro"),
        subscriptionRef = SubscriptionId(ref),
        validUntilEpochMs = validUntil,
    )

    @Test
    fun noEventsMeansNoEntitlement() {
        assertFalse(EntitlementDecision().hasValidEntitlement(now))
    }

    @Test
    fun grantedEntitlementIsValid() {
        val decision = EntitlementDecision()
        decision.apply(granted("sub-1", validUntil = now + 1))
        assertTrue(decision.hasValidEntitlement(now))
    }

    @Test
    fun openEndedEntitlementIsValid() {
        val decision = EntitlementDecision()
        decision.apply(granted("sub-1", validUntil = null))
        assertTrue(decision.hasValidEntitlement(now))
    }

    @Test
    fun accessEndsAtEndOfPaidPeriod() {
        // AC-02: an expired subscription loses access at the end of its paid period
        val decision = EntitlementDecision()
        decision.apply(granted("sub-1", validUntil = now))
        assertFalse(decision.hasValidEntitlement(now), "validUntil is exclusive")
    }

    @Test
    fun revokedEntitlementIsInvalid() {
        val decision = EntitlementDecision()
        decision.apply(granted("sub-1", validUntil = now + 86_400_000))
        decision.apply(EntitlementRevoked(subject, SubscriptionId("sub-1")))
        assertFalse(decision.hasValidEntitlement(now))
    }

    @Test
    fun revokeOnlyAffectsTheReferencedSubscription() {
        val decision = EntitlementDecision()
        decision.apply(granted("sub-1"))
        decision.apply(granted("sub-2"))
        decision.apply(EntitlementRevoked(subject, SubscriptionId("sub-1")))
        assertTrue(decision.hasValidEntitlement(now))
    }

    // SUB-07 — paused status has no access even though grant is still in the active map

    @Test
    fun pausedSubscriptionHasNoAccess() {
        val decision = EntitlementDecision()
        decision.apply(granted("sub-1"))
        decision.apply(SubscriptionPaused(subject, SubscriptionId("sub-1"), PlanId("pro"), now))
        assertFalse(decision.hasValidEntitlement(now), "paused subscription must yield no access (SUB-07)")
    }

    @Test
    fun resumedSubscriptionRestoresAccess() {
        val decision = EntitlementDecision()
        decision.apply(granted("sub-1"))
        decision.apply(SubscriptionPaused(subject, SubscriptionId("sub-1"), PlanId("pro"), now))
        decision.apply(SubscriptionResumed(subject, SubscriptionId("sub-1"), resumedAtEpochMs = now + 1))
        assertTrue(decision.hasValidEntitlement(now), "resumed subscription must restore access (SUB-07)")
    }

    @Test
    fun pauseOnlyAffectsTargetSubscription() {
        val decision = EntitlementDecision()
        decision.apply(granted("sub-1"))
        decision.apply(granted("sub-2"))
        decision.apply(SubscriptionPaused(subject, SubscriptionId("sub-1"), PlanId("pro"), now))
        assertTrue(decision.hasValidEntitlement(now), "a second active sub must still grant access")
    }

    @Test
    fun hardRevokeAfterPauseCleansUpPausedSet() {
        val decision = EntitlementDecision()
        decision.apply(granted("sub-1"))
        decision.apply(SubscriptionPaused(subject, SubscriptionId("sub-1"), PlanId("pro"), now))
        decision.apply(EntitlementRevoked(subject, SubscriptionId("sub-1")))
        // re-grant the same ref → must work as if never paused
        decision.apply(granted("sub-1"))
        assertTrue(decision.hasValidEntitlement(now), "revoked-then-regranted sub must not stay paused")
    }
}
