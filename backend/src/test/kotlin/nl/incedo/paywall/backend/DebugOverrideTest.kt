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
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.metering.MeterPeriod

/**
 * EX-05: staff debug override — forces a chosen variant for QA, excluded from analytics.
 *
 * Dev mode (no jwtValidator) always allows the override because [requireStaff] in dev
 * mode yields a synthetic admin. This matches the production contract: override is
 * staff-gated; tests run in dev mode so they prove the routing without mocking CIAM.
 */
class DebugOverrideTest {

    private val now = 1_750_000_000_000L

    private val multiVariantExperiment = ExperimentDefinition(
        id = nl.incedo.paywall.core.ExperimentId("exp-ex05"),
        name = "EX-05 debug test",
        variants = listOf(
            Variant("hard", StrategyConfig.Hard, weight = 1),
            Variant("metered", StrategyConfig.Metered(limit = 5), weight = 1),
        ),
    )

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = multiVariantExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    @Test
    fun forceVariantHardGatesAnonymousVisitor() = apiTest { client ->
        // A visitor who would normally get "metered" (weight-based) is forced to "hard".
        // Hard strategy always gates (PW-10), so the result must be "gated".
        val resp = client.post("/api/v1/decide?forceVariant=hard") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-ex05-1", articleId = "a-1", tier = "premium"))
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = resp.body<DecideResponse>()
        assertEquals("gate", body.access, "EX-05: forceVariant=hard must produce gate for premium content")
        assertEquals("hard", body.variant)
    }

    @Test
    fun forceVariantMeteredGivesFullWithinLimit() = apiTest { client ->
        // Force metered — anonymous visitor with 0 reads has credit → full.
        val resp = client.post("/api/v1/decide?forceVariant=metered") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-ex05-2", articleId = "a-1", tier = "premium"))
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = resp.body<DecideResponse>()
        assertEquals("full", body.access, "EX-05: forceVariant=metered must be open for fresh visitor within limit")
        assertEquals("metered", body.variant)
    }

    @Test
    fun unknownForceVariantFallsBackToNormalAssignment() = apiTest { client ->
        // An unrecognised forceVariant silently falls back to the normal assignment.
        val resp = client.post("/api/v1/decide?forceVariant=nonexistent") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-ex05-3", articleId = "a-1", tier = "premium"))
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        // Whatever variant was assigned, the request must succeed without error.
    }

    @Test
    fun forceVariantDoesNotLogWallEvent() = apiTest { client ->
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = multiVariantExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        // Use internal service directly to inspect event store after the call.
        val decision = kotlinx.coroutines.runBlocking {
            service.decide(
                subject = nl.incedo.paywall.access.Subject(nl.incedo.paywall.core.VisitorId("vis-ex05-no-analytics")),
                article = nl.incedo.paywall.access.Article(
                    nl.incedo.paywall.core.ArticleId("a-1"),
                    nl.incedo.paywall.access.ContentTier.PREMIUM,
                ),
                forceVariant = "hard",
            )
        }
        // The decision should be gated (hard strategy).
        assert(decision.decision is nl.incedo.paywall.access.AccessDecision.Gated)
        // No WallEventRecorded should be in the store (EX-05: excluded from analytics).
        val events = kotlinx.coroutines.runBlocking {
            store.query(nl.incedo.paywall.core.port.EventQuery(setOf("subject:visitor:vis-ex05-no-analytics"))).events
        }
        val wallEvents = events.filterIsInstance<nl.incedo.paywall.analytics.WallEventRecorded>()
        assertEquals(0, wallEvents.size, "EX-05: forceVariant must suppress analytics events")
    }
}
