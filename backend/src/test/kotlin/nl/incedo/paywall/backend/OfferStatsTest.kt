package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import nl.incedo.paywall.api.OfferStatsResponse
import nl.incedo.paywall.cep.MockCepClient
import nl.incedo.paywall.cep.Offer
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

private const val OFFER_ID = "offer-an14"

/**
 * AN-14: offer performance view — triggered/accepted/declined/suppressed counts,
 * acceptance rate, and channel breakdown per offer_id.
 */
class OfferStatsTest {

    private val now = 1_750_000_000_000L

    private val baseOffer = Offer(
        offerId = OFFER_ID,
        kind = "downsell",
        trigger = "cancel_intent",
        channels = setOf("web"),
        source = "campaign-1",
        cta = "Stay with Basic",
    )

    private fun apiTest(
        offer: Offer? = baseOffer,
        block: suspend (io.ktor.client.HttpClient) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        val cepClient = MockCepClient(fixedOffer = offer)
        val offerService = OfferService(
            eventStore = store,
            cepClient = cepClient,
            clock = { now },
            cepTimeoutMs = 5_000L,
        )
        application { module(service, store, cepClient = cepClient, offerService = offerService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    @Test
    fun emptyStoreReturnsEmptyList() = apiTest { client ->
        val resp = client.get("/api/v1/stats/offers")
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = resp.body<List<OfferStatsResponse>>()
        assertEquals(0, body.size)
    }

    @Test
    fun triggeredCountIncreasesPerRequest() = apiTest { client ->
        repeat(3) {
            client.post("/api/v1/offers/request?trigger=cancel_intent") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = "vis-an14-$it", articleId = "a-1", tier = "premium"))
            }
        }
        val stats = client.get("/api/v1/stats/offers").body<List<OfferStatsResponse>>()
        val s = stats.single { it.offerId == OFFER_ID }
        assertEquals(3, s.triggered)
    }

    @Test
    fun acceptedCountUpdatedOnOfferAccept() = apiTest { client ->
        // Trigger then accept the offer.
        client.post("/api/v1/offers/request?trigger=cancel_intent") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-an14-accept", articleId = "a-1", tier = "premium"))
        }
        client.post("/api/v1/offers/accept?visitorId=vis-an14-accept&offerId=$OFFER_ID&channel=web")
        val stats = client.get("/api/v1/stats/offers").body<List<OfferStatsResponse>>()
        val s = stats.single { it.offerId == OFFER_ID }
        assertEquals(1, s.triggered)
        assertEquals(1, s.accepted)
        assertEquals(0, s.declined)
    }

    @Test
    fun declinedCountUpdatedOnOfferDecline() = apiTest { client ->
        client.post("/api/v1/offers/request?trigger=cancel_intent") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-an14-dec", articleId = "a-1", tier = "premium"))
        }
        client.post("/api/v1/offers/decline?visitorId=vis-an14-dec&offerId=$OFFER_ID&channel=web")
        val stats = client.get("/api/v1/stats/offers").body<List<OfferStatsResponse>>()
        val s = stats.single { it.offerId == OFFER_ID }
        assertEquals(1, s.triggered)
        assertEquals(0, s.accepted)
        assertEquals(1, s.declined)
    }

    @Test
    fun acceptanceRateIsAcceptedOverTriggered() = apiTest { client ->
        // 3 triggers, 1 accept → rate = 1/3
        repeat(3) { i ->
            client.post("/api/v1/offers/request?trigger=cancel_intent") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = "vis-rate-$i", articleId = "a-1", tier = "premium"))
            }
        }
        client.post("/api/v1/offers/accept?visitorId=vis-rate-0&offerId=$OFFER_ID&channel=web")
        val stats = client.get("/api/v1/stats/offers").body<List<OfferStatsResponse>>()
        val s = stats.single { it.offerId == OFFER_ID }
        assertEquals(3, s.triggered)
        assertEquals(1, s.accepted)
        assertEquals(1.0 / 3.0, s.acceptanceRate, 0.001)
    }

    @Test
    fun channelBreakdownIsPresentInResponse() = apiTest { client ->
        client.post("/api/v1/offers/request?trigger=cancel_intent") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-chan-1", articleId = "a-1", tier = "premium"))
        }
        val stats = client.get("/api/v1/stats/offers").body<List<OfferStatsResponse>>()
        val s = stats.single { it.offerId == OFFER_ID }
        val webChan = s.channels["web"]
        assertEquals(1, webChan?.triggered, "channel breakdown must include triggered count")
    }

    @Test
    fun zeroTriggeredYieldsZeroAcceptanceRate() = apiTest(offer = null) { client ->
        // When no CEP offer is returned, OfferSuppressed is logged without an offerId
        // so no offer_id bucket is created — result is empty.
        client.post("/api/v1/offers/request?trigger=cancel_intent") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-no-offer", articleId = "a-1", tier = "premium"))
        }
        val stats = client.get("/api/v1/stats/offers").body<List<OfferStatsResponse>>()
        assertEquals(0, stats.size, "no offer bucket should exist when CEP returns null")
    }
}
