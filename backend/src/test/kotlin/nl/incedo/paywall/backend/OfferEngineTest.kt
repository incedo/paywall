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
import kotlin.test.assertNull
import nl.incedo.paywall.cep.MockCepClient
import nl.incedo.paywall.cep.Offer
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * UP-01/02/04/05/07/09/PAY-01: offer engine integration tests.
 */
class OfferEngineTest {

    private val now = 1_750_000_000_000L

    private val validOffer = Offer(
        offerId = "offer-test-1",
        kind = "downsell",
        fromPlanId = "complete-monthly",
        toPlanId = "basic-monthly",
        discountPercent = 20,
        trigger = "cancel_intent",
        channels = setOf("web"),
        source = "campaign-42",
        cta = "Stay with Basic",
    )

    private fun apiTest(
        offer: Offer? = validOffer,
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
            cepTimeoutMs = 5_000L, // generous timeout for tests
        )
        application { module(service, store, cepClient = cepClient, offerService = offerService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    private suspend fun io.ktor.client.HttpClient.requestOffer(
        visitorId: String = "vis-1",
        trigger: String = "gate_shown",
        channel: String = "web",
    ): OfferResponse = post("/api/v1/offers/request?trigger=$trigger") {
        contentType(ContentType.Application.Json)
        setBody(DecideRequest(visitorId = visitorId, articleId = "a-1", tier = "premium", channel = channel))
    }.body()

    // ── PAY-01: plan catalogue ────────────────────────────────────────────────

    @Test
    fun planCatalogueReturnsAllFourPlans() = apiTest { client ->
        val body = client.get("/api/v1/plans").body<List<PlanResponse>>()
        assertEquals(4, body.size)
        val planIds = body.map { it.planId }
        assert("basic-monthly" in planIds)
        assert("basic-annual" in planIds)
        assert("complete-monthly" in planIds)
        assert("complete-annual" in planIds)
    }

    @Test
    fun planCatalogueHasCorrectRanks() = apiTest { client ->
        val body = client.get("/api/v1/plans").body<List<PlanResponse>>()
        val ranks = body.associateBy({ it.planId }, { it.rank })
        assertEquals(1, ranks["basic-monthly"])
        assertEquals(1, ranks["basic-annual"])
        assertEquals(2, ranks["complete-monthly"])
        assertEquals(2, ranks["complete-annual"])
    }

    // ── UP-01/01a: decide_offer via CEP ───────────────────────────────────────

    @Test
    fun validOfferIsTriggeredOnRequest() = apiTest(offer = validOffer) { client ->
        val body = client.requestOffer(channel = "web")
        assertEquals("offer-test-1", body.offerId)
        assertEquals("downsell", body.kind)
    }

    @Test
    fun noCepClientYieldsNullOffer() = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store, experiment = defaultExperiment,
            clock = { now }, currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) } // no cepClient → no offerService
        val client = createClient { install(ContentNegotiation) { json() } }
        val body = client.post("/api/v1/offers/request?trigger=gate_shown") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "v", articleId = "a", tier = "premium"))
        }.body<OfferResponse>()
        assertNull(body.offerId)
    }

    // ── UP-02a: channel filter ─────────────────────────────────────────────────

    @Test
    fun offerSuppressedOnChannelMismatch() = apiTest(offer = validOffer) { client ->
        // validOffer has channels={"web"} but request is from "app"
        val body = client.requestOffer(channel = "app")
        assertNull(body.offerId, "offer with channel mismatch must be suppressed (UP-02a)")
    }

    @Test
    fun emptyChannelSetAllowsAllChannels() = apiTest(
        offer = validOffer.copy(channels = emptySet()),
    ) { client ->
        val body = client.requestOffer(channel = "app")
        assertEquals("offer-test-1", body.offerId, "empty channels = all channels allowed")
    }

    // ── UP-05: frequency capping ───────────────────────────────────────────────

    @Test
    fun declinedOfferIsSuppressedWithinCooldown() = apiTest(offer = validOffer) { client ->
        // First request: offer is returned
        val first = client.requestOffer()
        assertEquals("offer-test-1", first.offerId, "first request must return the offer")

        // Visitor declines
        val decline = client.post("/api/v1/offers/decline?visitorId=vis-1&offerId=offer-test-1&channel=web")
        assertEquals(HttpStatusCode.Accepted, decline.status)

        // Second request: offer suppressed by frequency cap (UP-05)
        val second = client.requestOffer()
        assertNull(second.offerId, "declined offer must be suppressed within the cooldown (UP-05)")
    }

    // ── UP-09: local guardrails ────────────────────────────────────────────────

    @Test
    fun offerWithUnknownPlanRejectedByGuardrail() = apiTest(
        offer = validOffer.copy(toPlanId = "nonexistent-plan"),
    ) { client ->
        val body = client.requestOffer()
        assertNull(body.offerId, "offer with unknown toPlanId must be rejected (UP-09)")
    }

    @Test
    fun upsellWithLowerRankRejected() = apiTest(
        offer = validOffer.copy(
            kind = "upsell",
            fromPlanId = "complete-monthly",
            toPlanId = "basic-monthly", // lower rank → incoherent upsell
        ),
    ) { client ->
        val body = client.requestOffer()
        assertNull(body.offerId, "upsell with lower-rank toPlan must be rejected (UP-09/PAY-01a)")
    }

    @Test
    fun discountExceedingMaxRejected() = apiTest(
        offer = validOffer.copy(discountPercent = 99),
    ) { client ->
        val body = client.requestOffer()
        assertNull(body.offerId, "offer exceeding max discount must be rejected (UP-09)")
    }
}
