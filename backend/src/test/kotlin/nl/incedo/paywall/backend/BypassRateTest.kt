package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import nl.incedo.paywall.access.ContentTier
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.api.BypassRateResponse
import nl.incedo.paywall.backend.content.ArticleRepository
import nl.incedo.paywall.backend.content.StoredArticle
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.ExperimentId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.metering.MeterPeriod

/**
 * BP-06: bypass-rate estimation — GET /api/v1/admin/bypass-rate returns
 * the ratio of gated-rendering requests with bypass markers to total
 * gated renders. DL-03: reported, not blocked.
 */
class BypassRateTest {

    private val now = 1_750_000_000_000L

    private val hardExperiment = ExperimentDefinition(
        id = ExperimentId("bp06-test"),
        name = "Hard only (all visitors hit a gate)",
        variants = listOf(Variant("hard", StrategyConfig.Hard, weight = 100)),
    )

    private val articles = ArticleRepository(
        listOf(
            StoredArticle(ArticleId("art-1"), "Premium story", ContentTier.PREMIUM, "Sensitive body"),
        ),
    )

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = hardExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store, articles = articles) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    @Test
    fun bypassRateIsZeroWithNoEvents() = apiTest { client ->
        val resp = client.get("/api/v1/admin/bypass-rate")
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = resp.body<BypassRateResponse>()
        assertEquals(0, body.gatedRenders)
        assertEquals(0, body.markedGatedRenders)
        assertEquals(0.0, body.bypassRate)
    }

    @Test
    fun cleanTrafficHasBypassRateZero() = apiTest { client ->
        // Three clean requests — no bot UA
        repeat(3) { i ->
            client.get("/api/v1/articles/art-1") {
                header("X-Visitor-Id", "visitor-clean-$i")
                header(HttpHeaders.UserAgent, "Mozilla/5.0 Chrome/120")
            }
        }
        val stats = client.get("/api/v1/admin/bypass-rate").body<BypassRateResponse>()
        assertEquals(3, stats.gatedRenders, "BP-06: each request hit the hard gate")
        assertEquals(0, stats.markedGatedRenders, "BP-06: no bypass markers in clean traffic")
        assertEquals(0.0, stats.bypassRate, "BP-06: bypass rate must be 0 for clean traffic")
    }

    @Test
    fun botUaTrafficIsCountedAsBypassMarker() = apiTest { client ->
        // One clean request + one bot-UA request both hitting the hard gate
        client.get("/api/v1/articles/art-1") {
            header("X-Visitor-Id", "visitor-clean")
            header(HttpHeaders.UserAgent, "Mozilla/5.0 Chrome/120")
        }
        client.get("/api/v1/articles/art-1") {
            header("X-Visitor-Id", "visitor-bot")
            header(HttpHeaders.UserAgent, "python-requests/2.28 (suspicious bot)")
        }
        val stats = client.get("/api/v1/admin/bypass-rate").body<BypassRateResponse>()
        assertEquals(2, stats.gatedRenders, "BP-06: both requests hit the gate")
        assertEquals(1, stats.markedGatedRenders, "BP-06: one request had a bot marker")
        assertEquals(0.5, stats.bypassRate, "BP-06: 1/2 = 0.5 bypass rate")
    }

    @Test
    fun sinceParameterPaginatesFromStorePosition() = apiTest { client ->
        // Trigger a gate event
        client.get("/api/v1/articles/art-1") {
            header("X-Visitor-Id", "visitor-p1")
        }
        // First page captures the event; record the position
        val first = client.get("/api/v1/admin/bypass-rate").body<BypassRateResponse>()
        val position = first.storePosition
        assert(position > 0L) { "BP-06: storePosition must be positive after events are written" }

        // Trigger another event AFTER the position
        client.get("/api/v1/articles/art-1") {
            header("X-Visitor-Id", "visitor-p2")
        }
        // Using the recorded position as `since` — should only see the newer event
        val second = client.get("/api/v1/admin/bypass-rate?since=$position").body<BypassRateResponse>()
        assertEquals(1, second.gatedRenders, "BP-06: since paginates — only newer events included")
    }
}
