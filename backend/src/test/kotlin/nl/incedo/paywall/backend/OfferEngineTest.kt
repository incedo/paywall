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

    // ── UP-10: billing-period upsell at checkout (monthly → annual, same tier) ────

    @Test
    fun annualUpsellOfferedAtMonthlyCheckout() = apiTest(
        offer = Offer(
            offerId = "up10-annual-upsell",
            kind = "upsell",
            fromPlanId = "basic-monthly",
            toPlanId = "basic-annual",
            trigger = "checkout",
            channels = setOf("web"),
            source = "up10-campaign",
            cta = "Switch to Annual and save 27%",
        ),
    ) { client ->
        // UP-10: a subject checking out basic-monthly triggers an annual offer for the same tier.
        // fromPlanId=basic-monthly (rank=1), toPlanId=basic-annual (rank=1) — same-rank upsell
        // is valid (billing-period change within the same tier).
        val body = client.post("/api/v1/offers/request?trigger=checkout&currentPlanId=basic-monthly") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-up10", articleId = "a-1", tier = "premium"))
        }.body<OfferResponse>()
        assertEquals("up10-annual-upsell", body.offerId,
            "UP-10: annual upsell for same tier must pass the UP-09 rank guardrail (rank 1→1)")
        assertEquals("upsell", body.kind)
    }

    // ── UP-11: tier upsell at basic checkout (basic → complete) ─────────────────

    @Test
    fun completeTierOfferedAtBasicCheckout() = apiTest(
        offer = Offer(
            offerId = "up11-tier-upsell",
            kind = "upsell",
            fromPlanId = "basic-monthly",
            toPlanId = "complete-monthly",
            trigger = "checkout",
            channels = setOf("web"),
            source = "up11-campaign",
            cta = "Upgrade to Complete",
        ),
    ) { client ->
        // UP-11: a subject checking out basic triggers a complete offer (rank 1 → rank 2).
        val body = client.post("/api/v1/offers/request?trigger=checkout&currentPlanId=basic-monthly") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-up11", articleId = "a-1", tier = "premium"))
        }.body<OfferResponse>()
        assertEquals("up11-tier-upsell", body.offerId,
            "UP-11: complete-tier upsell at basic checkout must be accepted (rank 1→2)")
    }

    @Test
    fun sameRankUpsellNoLongerRejected() = apiTest(
        offer = Offer(
            offerId = "same-rank-billing",
            kind = "upsell",
            fromPlanId = "complete-monthly",
            toPlanId = "complete-annual", // rank 2→2: billing-period upsell
            trigger = "checkout",
            channels = setOf("web"),
            source = "up10-complete",
        ),
    ) { client ->
        // Same rank (2→2) is a valid billing-period upsell (UP-10 for complete tier).
        val body = client.requestOffer(trigger = "checkout")
        assertEquals("same-rank-billing", body.offerId,
            "UP-09/UP-10: same-rank billing-period upsell must pass the guardrail")
    }
}
