package nl.incedo.paywall.offers

import nl.incedo.paywall.core.DomainEvent

/**
 * UP-05: tracks declined offers to enforce cross-channel frequency capping.
 * The same subject is not offered the same offer_id again within [cooldownMs]
 * after declining it — regardless of channel.
 */
class OfferFrequencyDecision(
    private val cooldownMs: Long = 30L * 24 * 60 * 60 * 1000, // 30 days default
) {
    private val declinedAt = mutableMapOf<String, Long>() // offerId → declinedAtMs

    fun apply(event: DomainEvent) {
        if (event is OfferDeclined) declinedAt[event.offerId] = event.declinedAtEpochMs
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)

    /** Returns true when the given offer is still within its cooldown window (UP-05). */
    fun isCapped(offerId: String, nowEpochMs: Long): Boolean {
        val at = declinedAt[offerId] ?: return false
        return nowEpochMs - at < cooldownMs
    }
}
