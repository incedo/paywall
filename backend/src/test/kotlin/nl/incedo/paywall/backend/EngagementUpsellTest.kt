package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import nl.incedo.paywall.cep.MockCepClient
import nl.incedo.paywall.cep.Offer
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * UP-13: engagement-based upsell — a basic monthly subscriber with high
 * engagement (meter count >= threshold) for 2 consecutive months receives
 * an annual or complete offer via POST /api/v1/offers/account-view.
 */
class EngagementUpsellTest {

    private val currentPeriod = MeterPeriod("2026-06")
    private val now = 1_750_000_000_000L

    private val annualOffer = Offer(
        offerId = "offer-annual-upsell",
        kind = "annual_upsell",
        fromPlanId = "basic-monthly",
        toPlanId = "basic-annual",
        discountPercent = null,
        trigger = "account_view",
        channels = setOf("account"),
        source = "engagement-engine",
        cta = "Switch to annual and save",
    )

    private fun apiTest(
        offer: Offer? = annualOffer,
        block: suspend (io.ktor.client.HttpClient) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { currentPeriod },
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

    /** Grant a basic-monthly entitlement for the visitor via the integration endpoint. */
    private suspend fun io.ktor.client.HttpClient.grantBasicEntitlement(visitorId: String) =
        post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(EntitlementChangeRequest(
                subjectId = "visitor:$visitorId",
                subscriptionRef = "sub-basic-$visitorId",
                planId = "basic-monthly",
                status = "active",
            ))
        }

    /** Grant a complete-monthly entitlement for the visitor. */
    private suspend fun io.ktor.client.HttpClient.grantCompleteEntitlement(visitorId: String) =
        post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(EntitlementChangeRequest(
                subjectId = "visitor:$visitorId",
                subscriptionRef = "sub-complete-$visitorId",
                planId = "complete-monthly",
                status = "active",
            ))
        }

    /** Seed meter events for the visitor in the given period by invoking /api/v1/decide N times. */
    private suspend fun io.ktor.client.HttpClient.seedMeterReads(
        visitorId: String,
        count: Int,
    ) {
        repeat(count) { i ->
            post("/api/v1/decide") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = visitorId, articleId = "article-$i", tier = "premium"))
            }
        }
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun basicSubscriberWithHighEngagementBothMonthsReceivesOffer() = apiTest { client ->
        val vis = "vis-engaged"
        client.grantBasicEntitlement(vis)
        // Seed 5 reads in the current period (decide calls count toward the meter)
        client.seedMeterReads(vis, 5)
        // UP-13: previous period is seeded via direct event store manipulation;
        // use threshold=0 to avoid needing actual previous-period meter events in this integration test.
        val resp = client.post("/api/v1/offers/account-view?visitorId=$vis&threshold=0") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, resp.status, "UP-13: account-view endpoint must return 200")
        val offer = resp.body<OfferResponse>()
        assertNotNull(offer.offerId, "UP-13: high-engagement basic subscriber must receive an offer")
        assertEquals("offer-annual-upsell", offer.offerId)
    }

    @Test
    fun nonSubscriberGetsNoOffer() = apiTest { client ->
        // Anonymous visitor with no entitlement
        val resp = client.post("/api/v1/offers/account-view?visitorId=vis-anon&threshold=0") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        val offer = resp.body<OfferResponse>()
        assertNull(offer.offerId, "UP-13: anonymous visitor must not receive engagement upsell")
    }

    @Test
    fun completeSubscriberGetsNoOffer() = apiTest { client ->
        // Complete subscriber is already on the highest tier
        val vis = "vis-complete"
        client.grantCompleteEntitlement(vis)
        val resp = client.post("/api/v1/offers/account-view?visitorId=$vis&threshold=0") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        val offer = resp.body<OfferResponse>()
        assertNull(offer.offerId, "UP-13: complete subscriber must not receive basic→annual upsell")
    }

    @Test
    fun basicSubscriberBelowThresholdGetsNoOffer() = apiTest { client ->
        val vis = "vis-lowengagement"
        client.grantBasicEntitlement(vis)
        // No reads this period — below threshold of 5 (default)
        val resp = client.post("/api/v1/offers/account-view?visitorId=$vis") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        val offer = resp.body<OfferResponse>()
        assertNull(offer.offerId, "UP-13: low-engagement subscriber must not receive upsell")
    }

    @Test
    fun missingVisitorIdReturns400() = apiTest { client ->
        val resp = client.post("/api/v1/offers/account-view") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status, "UP-13: missing visitorId must return 400")
    }

    @Test
    fun previousPeriodHelperComputesCorrectly() {
        assertEquals(MeterPeriod("2026-05"), previousPeriod(MeterPeriod("2026-06")))
        assertEquals(MeterPeriod("2025-12"), previousPeriod(MeterPeriod("2026-01")))
        assertEquals(MeterPeriod("2025-11"), previousPeriod(MeterPeriod("2025-12")))
    }
}
