package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.api.VariantStatsResponse
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.ExperimentId
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.metering.MeterPeriod

/**
 * AN-11: reach cost — change in page views and article reads vs. the EX-04
 * control variant, so conversion gains can be weighed against lost traffic.
 * Null when no control variant is configured in the experiment (EX-04 optional).
 */
class ReachCostTest {

    private val now = 1_750_000_000_000L

    // Experiment with an explicit EX-04 control/holdout variant.
    private val experimentWithControl = ExperimentDefinition(
        id = ExperimentId("reach-cost-test"),
        name = "Reach cost test",
        variants = listOf(
            Variant("control", StrategyConfig.Metered(limit = Int.MAX_VALUE), weight = 50, isControl = true),
            Variant("hard", StrategyConfig.Hard, weight = 50),
        ),
    )

    private fun visitorIn(variant: String, experiment: ExperimentDefinition = experimentWithControl): String =
        (0 until 100_000).asSequence()
            .map { "rc-visitor-$it" }
            .first { VariantAssigner.assign(VisitorId(it), experiment).name == variant }

    private fun apiTest(
        experiment: ExperimentDefinition = experimentWithControl,
        block: suspend (io.ktor.client.HttpClient) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = experiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2025-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    private suspend fun io.ktor.client.HttpClient.decide(visitorId: String, articleId: String = "art-1") =
        post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = visitorId, articleId = articleId, tier = "premium"))
        }

    @Test
    fun reachCostIsNullWhenNoControlVariantConfigured() = apiTest(experiment = defaultExperiment) { client ->
        client.decide(visitorIn("hard", defaultExperiment))
        val stats: List<VariantStatsResponse> = client.get("/api/v1/stats").body()
        val hardStats = stats.firstOrNull { it.variant == "hard" }
        assertNull(hardStats?.pageViewsDeltaVsControl, "AN-11: delta must be null when no control variant exists")
        assertNull(hardStats?.articleReadsDeltaVsControl, "AN-11: delta must be null when no control variant exists")
    }

    @Test
    fun controlVariantDeltaIsZero() = apiTest { client ->
        // Control variant: article reads count but there are no walls (metered limit = max).
        val controlVisitor = visitorIn("control")
        client.decide(controlVisitor) // article_read (metered, limit = max → free pass)
        val stats: List<VariantStatsResponse> = client.get("/api/v1/stats").body()
        val controlStats = stats.firstOrNull { it.variant == "control" }
        assertEquals(0, controlStats?.pageViewsDeltaVsControl, "AN-11: control vs itself = 0")
        assertEquals(0, controlStats?.articleReadsDeltaVsControl, "AN-11: control vs itself = 0")
    }

    @Test
    fun reachCostNegativeWhenHardGateReducesReads() = apiTest { client ->
        // Give the control variant 3 article reads.
        repeat(3) { i -> client.decide(visitorIn("control"), "art-control-$i") }
        // Hard gate only yields wall_shown (no article read); give it 1 read via a free article.
        client.decide(visitorIn("hard"), "art-hard-free")

        val stats: List<VariantStatsResponse> = client.get("/api/v1/stats").body()
        val hardStats = stats.first { it.variant == "hard" }
        val controlStats = stats.first { it.variant == "control" }

        // Control has 3 reads; hard has 1 → delta = 1 - 3 = -2 (reach cost = 2 lost reads).
        assertEquals(
            hardStats.articleReads - controlStats.articleReads,
            hardStats.articleReadsDeltaVsControl,
            "AN-11: delta must equal hard.reads - control.reads",
        )
    }

    @Test
    fun pageViewsDeltaComputedCorrectly() = apiTest { client ->
        // 2 page_view events for control, 1 for hard.
        repeat(2) { i ->
            client.post("/api/v1/events") {
                contentType(ContentType.Application.Json)
                setBody(ClientEventRequest(type = "page_view", visitorId = visitorIn("control"), articleId = "a-$i"))
            }
        }
        client.post("/api/v1/events") {
            contentType(ContentType.Application.Json)
            setBody(ClientEventRequest(type = "page_view", visitorId = visitorIn("hard"), articleId = "a-0"))
        }

        val stats: List<VariantStatsResponse> = client.get("/api/v1/stats").body()
        val hardStats = stats.first { it.variant == "hard" }
        // Control has 2 page views; hard has 1 → delta = 1 - 2 = -1.
        assertEquals(-1, hardStats.pageViewsDeltaVsControl, "AN-11: pageViewsDelta = hard.pageViews - control.pageViews")
    }
}
