package nl.incedo.paywall.entitlements

import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubscriptionId

/**
 * Decision model answering "does this subject hold a valid entitlement?"
 * (AC-02). Entitlement claims inside tokens are advisory only (AC-08);
 * this model, fed from the local store, is authoritative.
 */
class EntitlementDecision {

    private val active = mutableMapOf<SubscriptionId, Long?>() // ref → validUntil
    private val paused = mutableSetOf<SubscriptionId>() // SUB-07: billing-paused refs → access off

    fun apply(event: DomainEvent) {
        when (event) {
            is EntitlementGranted -> active[event.subscriptionRef] = event.validUntilEpochMs
            is EntitlementRevoked -> {
                active.remove(event.subscriptionRef)
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
}
