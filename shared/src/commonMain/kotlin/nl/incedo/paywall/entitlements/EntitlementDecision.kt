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

    fun apply(event: DomainEvent) {
        when (event) {
            is EntitlementGranted -> active[event.subscriptionRef] = event.validUntilEpochMs
            is EntitlementRevoked -> active.remove(event.subscriptionRef)
            else -> {}
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)

    fun hasValidEntitlement(nowEpochMs: Long): Boolean =
        active.values.any { validUntil -> validUntil == null || validUntil > nowEpochMs }
}
