package nl.incedo.paywall.offers

import kotlin.test.Test
import kotlin.test.assertEquals
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.VisitorId

/**
 * Q-5: boundary tests for [OfferStatsProjection] acceptance rate (zero denominator,
 * single sample, multi-channel breakdown).
 */
class OfferStatsProjectionTest {

    private val now = 1_750_000_000_000L
    private fun subject(n: Int) = SubjectId.of(VisitorId("v-$n"))

    private fun triggered(offerId: String, channel: String = "web", n: Int = 0) =
        OfferTriggered(subject(n), offerId, "page_view", channel, "retention", "cep", now)

    private fun accepted(offerId: String, channel: String = "web", n: Int = 0) =
        OfferAccepted(subject(n), offerId, "retention", channel, now)

    private fun declined(offerId: String, channel: String = "web", n: Int = 0) =
        OfferDeclined(subject(n), offerId, channel, now)

    private fun suppressed(offerId: String, channel: String = "web", n: Int = 0) =
        OfferSuppressed(subject(n), "page_view", channel, "capped", now, offerId)

    @Test
    fun emptyProjectionYieldsNoStats() {
        assertEquals(emptyList(), OfferStatsProjection().stats())
    }

    @Test
    fun acceptanceRateIsZeroWhenNoTriggersRecorded() {
        // Q-5: zero-denominator guard — accepted event arrives but triggered=0
        val projection = OfferStatsProjection()
        projection.apply(accepted("offer-1"))
        val stats = projection.stats().first { it.offerId == "offer-1" }
        assertEquals(0, stats.triggered)
        assertEquals(1, stats.accepted)
        assertEquals(0.0, stats.acceptanceRate)
    }

    @Test
    fun acceptanceRateIsSingleSampleBoundary() {
        // Q-5: single-sample — one trigger, one accept → rate = 1.0
        val projection = OfferStatsProjection()
        projection.apply(triggered("offer-1"))
        projection.apply(accepted("offer-1"))
        val stats = projection.stats().first { it.offerId == "offer-1" }
        assertEquals(1, stats.triggered)
        assertEquals(1, stats.accepted)
        assertEquals(1.0, stats.acceptanceRate)
    }

    @Test
    fun acceptanceRateIsZeroWhenNoneAccepted() {
        // Q-5: zero conversions
        val projection = OfferStatsProjection()
        repeat(3) { i -> projection.apply(triggered("offer-2", n = i)) }
        val stats = projection.stats().first { it.offerId == "offer-2" }
        assertEquals(3, stats.triggered)
        assertEquals(0, stats.accepted)
        assertEquals(0.0, stats.acceptanceRate)
    }

    @Test
    fun acceptanceRateIsPartialFraction() {
        val projection = OfferStatsProjection()
        repeat(4) { i -> projection.apply(triggered("offer-3", n = i)) }
        repeat(2) { i -> projection.apply(accepted("offer-3", n = i)) }
        assertEquals(0.5, projection.stats().first { it.offerId == "offer-3" }.acceptanceRate)
    }

    @Test
    fun channelBreakdownTrackedSeparately() {
        val projection = OfferStatsProjection()
        projection.apply(triggered("offer-4", "web"))
        projection.apply(triggered("offer-4", "email"))
        projection.apply(accepted("offer-4", "email"))
        val stats = projection.stats().first { it.offerId == "offer-4" }
        assertEquals(2, stats.triggered)
        assertEquals(1, stats.accepted)
        assertEquals(1, stats.channels["email"]?.triggered)
        assertEquals(1, stats.channels["email"]?.accepted)
        assertEquals(1, stats.channels["web"]?.triggered)
        assertEquals(0, stats.channels["web"]?.accepted ?: 0)
    }

    @Test
    fun suppressedAndDeclinedAreCounted() {
        val projection = OfferStatsProjection()
        projection.apply(triggered("offer-5"))
        projection.apply(declined("offer-5"))
        projection.apply(suppressed("offer-5"))
        val stats = projection.stats().first { it.offerId == "offer-5" }
        assertEquals(1, stats.triggered)
        assertEquals(1, stats.declined)
        assertEquals(1, stats.suppressed)
    }

    @Test
    fun suppressedWithNullOfferIdIsIgnored() {
        // OfferSuppressed.offerId is nullable; null means no specific offer matched
        val projection = OfferStatsProjection()
        projection.apply(OfferSuppressed(subject(0), "page_view", "web", "none_matched", now, offerId = null))
        assertEquals(emptyList(), projection.stats())
    }

    @Test
    fun multipleOffersAreIndependent() {
        val projection = OfferStatsProjection()
        projection.apply(triggered("offer-A"))
        projection.apply(triggered("offer-B"))
        projection.apply(accepted("offer-B"))
        assertEquals(2, projection.stats().size)
        assertEquals(0.0, projection.stats().first { it.offerId == "offer-A" }.acceptanceRate)
        assertEquals(1.0, projection.stats().first { it.offerId == "offer-B" }.acceptanceRate)
    }

    @Test
    fun statsAreSortedByOfferId() {
        val projection = OfferStatsProjection()
        listOf("offer-Z", "offer-A", "offer-M").forEach { id ->
            projection.apply(triggered(id))
        }
        assertEquals(listOf("offer-A", "offer-M", "offer-Z"), projection.stats().map { it.offerId })
    }
}
