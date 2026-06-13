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
import kotlin.test.assertTrue
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.metering.MeterPeriod

/**
 * DY-05: counterfactuals — what each other variant would have decided on the
 * same page view. Returned in [DecideResponse.counterfactuals] and persisted
 * in the [nl.incedo.paywall.analytics.WallEventRecorded] context (keys: "cf_{name}").
 */
class CounterfactualTest {

    private val now = 1_750_000_000_000L

    // Two-variant experiment so counterfactuals are predictable.
    private val twoVariant = ExperimentDefinition(
        id = nl.incedo.paywall.core.ExperimentId("cf-test"),
        name = "Counterfactual test",
        variants = listOf(
            Variant("hard", StrategyConfig.Hard, weight = 50),
            Variant("freemium", StrategyConfig.Freemium, weight = 50),
        ),
    )

    private fun apiTest(
        block: suspend (io.ktor.client.HttpClient, InMemoryEventStore) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = twoVariant,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client, store)
    }

    // ── Response includes counterfactuals ─────────────────────────────────────

    @Test
    fun decideResponseContainsCounterfactualsForOtherVariants() = apiTest { client, _ ->
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-cf-1", articleId = "art-premium", tier = "premium"))
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = resp.body<DecideResponse>()
        // Exactly one counterfactual for the other variant
        assertEquals(1, body.counterfactuals.size, "DY-05: one counterfactual expected (two-variant experiment)")
        val otherVariant = twoVariant.variants.first { it.name != body.variant }
        assertTrue(otherVariant.name in body.counterfactuals, "DY-05: other variant must be present")
    }

    @Test
    fun hardVariantGatesWhileFreemiumCounterfactualIsAlsoGate() = apiTest { client, _ ->
        // For "hard" strategy: premium = gate. Freemium is also gate for premium (freemium gates
        // everything without subscription). Both should be "gate" for premium anonymous visitor.
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-cf-hard", articleId = "art-p", tier = "premium"))
        }.body<DecideResponse>()
        // Either variant gates a premium anonymous visitor — counterfactual value must be "gate"
        val cf = resp.counterfactuals.values.first()
        assertEquals("gate", cf, "DY-05: freemium also gates premium for anonymous visitor")
    }

    @Test
    fun entitledVisitorReceivesFullWithCounterfactualsAlsoFull() = apiTest { client, _ ->
        // Grant entitlement to visitor — both hard and freemium strategies should be "full"
        client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(EntitlementChangeRequest(
                subjectId = "visitor:vis-cf-ent",
                subscriptionRef = "sub-cf-1",
                planId = "basic-monthly",
                status = "active",
                validUntilEpochMs = now + 30 * 24 * 3600 * 1000L,
            ))
        }
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-cf-ent", articleId = "art-p2", tier = "premium"))
        }.body<DecideResponse>()
        assertEquals("full", resp.access, "DY-05: entitled visitor gets full access")
        val cf = resp.counterfactuals.values.first()
        assertEquals("full", cf, "DY-05: other strategy would also be full for an entitled visitor")
    }

    // ── Counterfactuals persisted in WallEventRecorded context ────────────────

    @Test
    fun wallShownEventContainsCfContextKeys() = apiTest { client, store ->
        // Ensure we get a gated variant by probing a premium article
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-cf-ev", articleId = "art-cf-3", tier = "premium"))
        }.body<DecideResponse>()
        if (resp.access == "gate") {
            val events = store.query(EventQuery(setOf("subject:visitor:vis-cf-ev"))).events
                .filterIsInstance<WallEventRecorded>()
            val wallShown = events.firstOrNull { it.context.keys.any { k -> k.startsWith("cf_") } }
            // At least one cf_ key must be present in the context
            val cfKeys = wallShown?.context?.keys?.filter { it.startsWith("cf_") } ?: emptyList()
            assertEquals(1, cfKeys.size, "DY-05: one cf_ key expected for two-variant experiment")
        }
        // If access is "full" (entitled/free path) we skip this check; full reads that don't
        // count toward the meter don't produce a WallEventRecorded at all.
    }

    // ── Debug override (EX-05) suppresses counterfactuals ─────────────────────

    @Test
    fun forceVariantProducesEmptyCounterfactuals() = apiTest { client, _ ->
        val resp = client.post("/api/v1/decide?forceVariant=hard") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-cf-debug", articleId = "art-cf-4", tier = "premium"))
        }.body<DecideResponse>()
        assertTrue(resp.counterfactuals.isEmpty(), "DY-05: forceVariant must suppress counterfactuals (debug mode, EX-05)")
    }
}
