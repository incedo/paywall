package nl.incedo.paywall.offers

import nl.incedo.paywall.core.DomainEvent

/**
 * AN-14: offer performance view — per offer_id funnel counts and acceptance rate,
 * broken down by channel. Rebuilt from the offer-event stream; disposable (DM-04).
 */
class OfferStatsProjection {

    data class ChannelStats(
        val triggered: Int = 0,
        val accepted: Int = 0,
        val declined: Int = 0,
        val suppressed: Int = 0,
    )

    data class OfferStats(
        val offerId: String,
        val triggered: Int,
        val accepted: Int,
        val declined: Int,
        val suppressed: Int,
        val acceptanceRate: Double,
        val channels: Map<String, ChannelStats>,
    )

    private class Accumulator {
        var triggered = 0
        var accepted = 0
        var declined = 0
        var suppressed = 0
        val channels = mutableMapOf<String, MutableChannelAcc>()
    }

    private class MutableChannelAcc {
        var triggered = 0
        var accepted = 0
        var declined = 0
        var suppressed = 0
    }

    private val byOffer = mutableMapOf<String, Accumulator>()

    private fun accFor(offerId: String) = byOffer.getOrPut(offerId) { Accumulator() }
    private fun chanFor(acc: Accumulator, channel: String) =
        acc.channels.getOrPut(channel) { MutableChannelAcc() }

    fun apply(event: DomainEvent) {
        when (event) {
            is OfferTriggered -> {
                val acc = accFor(event.offerId)
                acc.triggered++
                chanFor(acc, event.channel).triggered++
            }
            is OfferAccepted -> {
                val acc = accFor(event.offerId)
                acc.accepted++
                chanFor(acc, event.channel).accepted++
            }
            is OfferDeclined -> {
                val acc = accFor(event.offerId)
                acc.declined++
                chanFor(acc, event.channel).declined++
            }
            is OfferSuppressed -> {
                val offerId = event.offerId ?: return
                val acc = accFor(offerId)
                acc.suppressed++
                chanFor(acc, event.channel).suppressed++
            }
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)

    fun stats(): List<OfferStats> = byOffer.map { (offerId, acc) ->
        OfferStats(
            offerId = offerId,
            triggered = acc.triggered,
            accepted = acc.accepted,
            declined = acc.declined,
            suppressed = acc.suppressed,
            acceptanceRate = if (acc.triggered == 0) 0.0 else acc.accepted.toDouble() / acc.triggered,
            channels = acc.channels.mapValues { (_, ch) ->
                ChannelStats(
                    triggered = ch.triggered,
                    accepted = ch.accepted,
                    declined = ch.declined,
                    suppressed = ch.suppressed,
                )
            },
        )
    }.sortedBy { it.offerId }
}
