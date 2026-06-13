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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import nl.incedo.paywall.cep.Offer
import nl.incedo.paywall.cep.MockCepClient
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod
import nl.incedo.paywall.plans.DefaultPlans

/**
 * PAY-02: introductory pricing — the plan catalogue exposes introPriceMinorUnits
 * and introPeriodsCount for plans that have an intro offer; null otherwise.
 * The gate must display the intro price so it matches checkout exactly.
 *
 * UP-02: the OfferResponse returned by POST /api/v1/offers/request includes all
 * required offer-object fields: fromPlanId, toPlanId, pauseMonths, source, trigger,
 * channels. Clients need these to render the gate copy and pre-fill checkout.
 */
class IntroductoryPriceTest {

    private val now = 1_750_000_000_000L

    private fun apiTest(
        offer: Offer? = null,
        block: suspend (io.ktor.client.HttpClient) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val cepClient = MockCepClient(offer)
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2025-06") },
        )
        application { module(service, store, cepClient = cepClient) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    // ── PAY-02 ──────────────────────────────────────────────────────────────

    @Test
    fun basicMonthlyPlanExposesIntroPriceFields() = apiTest { client ->
        val plans = client.get("/api/v1/plans").body<List<PlanResponse>>()
        val basic = plans.first { it.planId == "basic-monthly" }
        assertNotNull(basic.introPriceMinorUnits, "PAY-02: basic-monthly must expose introPriceMinorUnits")
        assertNotNull(basic.introPeriodsCount, "PAY-02: basic-monthly must expose introPeriodsCount")
        assertEquals(100L, basic.introPriceMinorUnits, "PAY-02: intro price should be €1.00 (100 minor units)")
        assertEquals(1, basic.introPeriodsCount, "PAY-02: intro applies to 1 billing period")
    }

    @Test
    fun plansWithoutIntroOfferHaveNullIntroFields() = apiTest { client ->
        val plans = client.get("/api/v1/plans").body<List<PlanResponse>>()
        val annualPlans = plans.filter { it.billingPeriod == "annual" }
        assertTrue(annualPlans.isNotEmpty(), "PAY-02: annual plans must exist")
        for (p in annualPlans) {
            assertNull(p.introPriceMinorUnits, "PAY-02: ${p.planId} must have null intro price")
            assertNull(p.introPeriodsCount, "PAY-02: ${p.planId} must have null intro periods")
        }
    }

    @Test
    fun introPriceLowerThanRegularPrice() = apiTest { client ->
        val plans = client.get("/api/v1/plans").body<List<PlanResponse>>()
        val plansWithIntro = plans.filter { it.introPriceMinorUnits != null }
        for (p in plansWithIntro) {
            assertTrue(
                p.introPriceMinorUnits!! < p.priceMinorUnits,
                "PAY-02: intro price (${p.introPriceMinorUnits}) must be lower than regular price (${p.priceMinorUnits}) for ${p.planId}",
            )
        }
    }

    // ── UP-02 OfferResponse completeness ────────────────────────────────────

    @Test
    fun offerResponseIncludesAllUp02Fields() = apiTest(
        offer = Offer(
            offerId = "offer-up02",
            kind = "downsell",
            fromPlanId = "complete-monthly",
            toPlanId = "basic-monthly",
            discountPercent = null,
            validForSeconds = 3600L,
            pauseMonths = null,
            trigger = "cancel_intent",
            channels = setOf("web", "app"),
            source = "cep-campaign-dn-01",
            cta = "Switch to Basic",
        ),
    ) { client ->
        val resp = client.post("/api/v1/offers/request?trigger=cancel_intent") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-up02", articleId = "art-1", tier = "premium", channel = "web"))
        }.body<OfferResponse>()
        assertEquals("offer-up02", resp.offerId, "UP-02: offerId must be present")
        assertEquals("downsell", resp.kind, "UP-02: kind must be present")
        assertEquals("complete-monthly", resp.fromPlanId, "UP-02: fromPlanId must be present")
        assertEquals("basic-monthly", resp.toPlanId, "UP-02: toPlanId must be present")
        assertEquals(3600L, resp.validForSeconds, "UP-02: validForSeconds must be present")
        assertEquals("cancel_intent", resp.trigger, "UP-02: trigger must be present")
        assertEquals(setOf("web", "app"), resp.channels, "UP-02: channels must be present")
        assertEquals("cep-campaign-dn-01", resp.source, "UP-02: source must be present")
        assertEquals("Switch to Basic", resp.cta, "UP-02: cta must be present")
    }

    @Test
    fun pauseOfferExposesPauseMonths() = apiTest(
        offer = Offer(
            offerId = "offer-pause",
            kind = "downsell",
            pauseMonths = 3,
            trigger = "cancel_intent",
            channels = setOf("web"),
        ),
    ) { client ->
        val resp = client.post("/api/v1/offers/request?trigger=cancel_intent") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-pause", articleId = "art-1", tier = "premium", channel = "web"))
        }.body<OfferResponse>()
        assertEquals(3, resp.pauseMonths, "UP-02: pauseMonths must be exposed for pause offers")
    }

    @Test
    fun noOfferReturnsNullFields() = apiTest(offer = null) { client ->
        val resp = client.post("/api/v1/offers/request") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-none", articleId = "art-1", tier = "premium", channel = "web"))
        }.body<OfferResponse>()
        assertNull(resp.offerId, "UP-02: no-offer response must have null offerId")
        assertNull(resp.kind, "UP-02: no-offer response must have null kind")
        assertNull(resp.fromPlanId, "UP-02: no-offer response must have null fromPlanId")
        assertNull(resp.toPlanId, "UP-02: no-offer response must have null toPlanId")
        assertNull(resp.trigger, "UP-02: no-offer response must have null trigger")
        assertTrue(resp.channels.isEmpty(), "UP-02: no-offer response must have empty channels")
    }
}
