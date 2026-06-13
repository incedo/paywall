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
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.metering.MeterPeriod

/**
 * DY-06: scoring interface accepts externally supplied scores — e.g. a
 * Likelihood-to-Churn score from the CDP. When present, the external score
 * replaces the heuristic for the Dynamic strategy.
 *
 * AG-03: ad-gated unlocks are logged distinctly (granted_by=ad_gated) so
 * their cannibalization of subscriptions is measurable.
 */
class ExternalScoreTest {

    private val now = 1_750_000_000_000L

    // All-Dynamic experiment: any visitor gets the Dynamic strategy.
    private val dynamicOnlyExperiment = ExperimentDefinition(
        id = nl.incedo.paywall.core.ExperimentId("dy06-test"),
        name = "DY-06 test",
        variants = listOf(Variant("dynamic", StrategyConfig.Dynamic(tSoft = 40, tHard = 70), weight = 1)),
    )

    // All-Hard experiment: any visitor sees the hard gate (for AG-03 ad-grant test).
    private val hardOnlyExperiment = ExperimentDefinition(
        id = nl.incedo.paywall.core.ExperimentId("ag03-test"),
        name = "AG-03 test",
        variants = listOf(Variant("hard", StrategyConfig.Hard, weight = 1)),
    )

    private fun dynamicApiTest(
        block: suspend (io.ktor.client.HttpClient) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = dynamicOnlyExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    private fun hardApiTest(
        block: suspend (io.ktor.client.HttpClient, InMemoryEventStore) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = hardOnlyExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client, store)
    }

    // ── DY-06: external score ─────────────────────────────────────────────────

    @Test
    fun externalScoreAboveHardThresholdGatesVisitor() = dynamicApiTest { client ->
        // External score = 90 ≥ tHard (70) → hard gate.
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(
                visitorId = "vis-dy06-high",
                articleId = "a-1",
                tier = "premium",
                externalScore = 90,
            ))
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        assertEquals("gate", resp.body<DecideResponse>().access,
            "DY-06: external score ≥ tHard must produce gate")
    }

    @Test
    fun externalScoreBelowSoftThresholdGrantsAccess() = dynamicApiTest { client ->
        // External score = 10 < tSoft (40) → full access.
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(
                visitorId = "vis-dy06-low",
                articleId = "a-1",
                tier = "premium",
                externalScore = 10,
            ))
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        assertEquals("full", resp.body<DecideResponse>().access,
            "DY-06: external score < tSoft must grant access")
    }

    @Test
    fun outOfRangeExternalScoreFallsBackToHeuristic() = dynamicApiTest { client ->
        // 150 is out of 0–100 range; fresh visitor heuristic = 0 → full access.
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(
                visitorId = "vis-dy06-invalid",
                articleId = "a-1",
                tier = "premium",
                externalScore = 150,
            ))
        }
        assertEquals("full", resp.body<DecideResponse>().access,
            "DY-06: out-of-range external score must fall back to heuristic")
    }

    @Test
    fun absentExternalScoreUsesHeuristic() = dynamicApiTest { client ->
        // No externalScore → heuristic for fresh visitor = 0 → below tSoft → full.
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-dy06-no-ext", articleId = "a-1", tier = "premium"))
        }
        assertEquals("full", resp.body<DecideResponse>().access,
            "DY-06: absent external score must use heuristic (fresh visitor → full)")
    }

    // ── AG-03: ad-gate analytics ──────────────────────────────────────────────

    @Test
    fun adGatedAccessLogsGrantedByAdGated() = hardApiTest { client, store ->
        val vis = "vis-ag03-1"
        val article = "article-ag03-1"

        // Issue ad grant.
        val grantResp = client.post("/api/v1/integration/ad-completion") {
            contentType(ContentType.Application.Json)
            setBody(AdCompletionRequest(
                subjectId = "visitor:$vis",
                articleId = article,
                adPlayId = "play-ag03",
            ))
        }
        assertEquals(HttpStatusCode.Created, grantResp.status)

        // Access the article via the grant.
        val decideResp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = vis, articleId = article, tier = "premium"))
        }.body<DecideResponse>()
        assertEquals("full", decideResp.access, "grant must allow access")

        // Verify analytics: WallEventRecorded must have granted_by=ad_gated (AG-03).
        val events = kotlinx.coroutines.runBlocking {
            store.query(EventQuery(setOf("subject:visitor:$vis"))).events
        }
        val wallEvents = events.filterIsInstance<WallEventRecorded>()
        val adGrantEvent = wallEvents.firstOrNull { it.context["granted_by"] == "ad_gated" }
        assertEquals("ad_gated", adGrantEvent?.context?.get("granted_by"),
            "AG-03: WallEventRecorded for ad-grant access must have granted_by=ad_gated")
    }
}
