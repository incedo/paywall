package nl.incedo.paywall.entitlements

import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubscriptionId
import nl.incedo.paywall.plans.DefaultPlans

/**
 * Decision model answering "does this subject hold a valid entitlement?"
 * (AC-02). Entitlement claims inside tokens are advisory only (AC-08);
 * this model, fed from the local store, is authoritative.
 */
class EntitlementDecision {

    private val active = mutableMapOf<SubscriptionId, Long?>() // ref → validUntil
    private val planRanks = mutableMapOf<SubscriptionId, Int>() // ref → plan rank (UP-12)
    private val paused = mutableSetOf<SubscriptionId>() // SUB-07: billing-paused refs → access off

    fun apply(event: DomainEvent) {
        when (event) {
            is EntitlementGranted -> {
                active[event.subscriptionRef] = event.validUntilEpochMs
                planRanks[event.subscriptionRef] = DefaultPlans.rankOf(event.planId)
            }
            is EntitlementRevoked -> {
                active.remove(event.subscriptionRef)
                planRanks.remove(event.subscriptionRef)
                paused.remove(event.subscriptionRef) // clean up paused set on hard revoke
            }
            is SubscriptionPaused -> paused.add(event.subscriptionRef) // SUB-07: access off
            is SubscriptionResumed -> paused.remove(event.subscriptionRef) // SUB-07: access restored
            else -> {}
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)

    /** AC-02: true only when subject holds an active, non-paused, non-expired entitlement. */
    fun hasValidEntitlement(nowEpochMs: Long): Boolean =
        active.entries.any { (ref, validUntil) ->
            ref !in paused && (validUntil == null || validUntil > nowEpochMs)
        }

    /**
     * UP-12: true when the subject holds a valid entitlement with a plan rank ≥ minRank.
     * Rank 0 (unknown or legacy plan ID) is treated as any-tier access for backward
     * compatibility — unknown plans are not downgraded to basic.
     */
    fun hasValidEntitlementForTier(nowEpochMs: Long, minRank: Int): Boolean =
        active.entries.any { (ref, validUntil) ->
            val rank = planRanks[ref] ?: 0
            ref !in paused &&
                (validUntil == null || validUntil > nowEpochMs) &&
                (rank == 0 || rank >= minRank)
        }
}
