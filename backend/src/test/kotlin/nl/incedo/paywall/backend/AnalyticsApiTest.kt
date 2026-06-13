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
import kotlin.test.assertTrue
import nl.incedo.paywall.api.SaveWallRequest
import nl.incedo.paywall.api.VariantStatsResponse
import nl.incedo.paywall.api.WallResponse
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.analytics.wallEventShardTags
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.core.ExperimentId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.metering.MeterPeriod

class AnalyticsApiTest {

    private val now = 1_750_000_000_000L

    private fun visitorIn(variant: String): String =
        (0 until 10_000).asSequence()
            .map { "an-visitor-$it" }
            .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == variant }

    private fun apiTest(block: suspend (io.ktor.client.HttpClient, InMemoryEventStore) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        block(client, store)
    }

    private suspend fun decide(client: io.ktor.client.HttpClient, visitor: String, article: String) {
        client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = visitor, articleId = article, tier = "premium"))
        }
    }

    @Test
    fun gateLogsWallShownWithDecisionContext() = apiTest { client, store ->
        decide(client, visitorIn("hard"), "a-1")

        val events = store.query(EventQuery(wallEventShardTags())).events.filterIsInstance<WallEventRecorded>()
        assertEquals(1, events.size)
        val shown = events.single()
        assertEquals(WallEventType.WALL_SHOWN, shown.eventType) // AN-02
        assertEquals("hard", shown.variant)
        assertEquals("web", shown.channel)
        assertEquals("hard", shown.context["wallType"]) // AN-03 decision context
    }

    @Test
    fun dynamicWallShownIncludesThresholds() {
        // AN-03: wall_shown for Dynamic must carry tSoft and tHard so decisions
        // are reproducible in offline analysis (DY-03).
        val tSoft = 35
        val tHard = 65
        val dynamicOnlyExperiment = ExperimentDefinition(
            id = ExperimentId("an03-test"),
            name = "AN-03 threshold test",
            variants = listOf(
                Variant("dynamic", StrategyConfig.Dynamic(floorLimit = 1, tSoft = tSoft, tHard = tHard), weight = 100),
            ),
        )
        testApplication {
            val store = InMemoryEventStore()
            val service = AccessService(
                eventStore = store,
                experiment = dynamicOnlyExperiment,
                clock = { now },
                currentPeriod = { MeterPeriod("2026-06") },
            )
            application { module(service, store) }
            val client = createClient { install(ContentNegotiation) { json() } }
            // floor=1: first visit (meterUsed=0) is open; second visit triggers gate.
            client.post("/api/v1/decide") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = "an03-visitor", articleId = "a-1", tier = "premium"))
            }
            client.post("/api/v1/decide") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = "an03-visitor", articleId = "a-2", tier = "premium"))
            }
            val events = store.query(EventQuery(wallEventShardTags())).events.filterIsInstance<WallEventRecorded>()
            val shown = events.firstOrNull { it.eventType == WallEventType.WALL_SHOWN }
                ?: error("expected a WALL_SHOWN event")
            assertEquals(tSoft.toString(), shown.context["tSoft"], "AN-03: tSoft must be in wall_shown context")
            assertEquals(tHard.toString(), shown.context["tHard"], "AN-03: tHard must be in wall_shown context")
        }
    }

    @Test
    fun countedReadLogsArticleRead() = apiTest { client, store ->
        decide(client, visitorIn("metered"), "a-1")

        val events = store.query(EventQuery(wallEventShardTags())).events.filterIsInstance<WallEventRecorded>()
        assertEquals(listOf(WallEventType.ARTICLE_READ), events.map { it.eventType }) // MT-04: a real read, no wall
    }

    @Test
    fun clientEventIsRecordedWithServerResolvedVariant() = apiTest { client, store ->
        val visitor = visitorIn("metered")
        val response = client.post("/api/v1/events") {
            contentType(ContentType.Application.Json)
            setBody(ClientEventRequest(type = "gate_cta_click", visitorId = visitor, articleId = "a-1"))
        }
        assertEquals(HttpStatusCode.Accepted, response.status)

        val event = store.query(EventQuery(wallEventShardTags())).events
            .filterIsInstance<WallEventRecorded>().single()
        assertEquals(WallEventType.GATE_CTA_CLICK, event.eventType)
        assertEquals("metered", event.variant, "variant resolved server-side (EX-01), not client-supplied")
    }

    @Test
    fun serverObservedTypesAreRejectedFromClients() = apiTest { client, _ ->
        // wall_shown/article_read are server truths — a client must not forge them
        val response = client.post("/api/v1/events") {
            contentType(ContentType.Application.Json)
            setBody(ClientEventRequest(type = "wall_shown", visitorId = "v-1"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun wallEventsExportAsCsvWithIncrementalPaging() = apiTest { client, _ ->
        val visitor = visitorIn("hard")
        decide(client, visitor, "a-1") // wall_shown

        val response = client.get("/api/v1/export/wall-events.csv")
        val csv = response.body<String>()
        val lines = csv.trim().lines()
        assertEquals("occurred_at_epoch_ms,type,subject_id,variant,channel,article_id,context", lines.first())
        assertTrue(lines.any { "wall_shown" in it && "visitor:$visitor" in it }, "AN-04 export carries the event")

        // Incremental export from the returned position yields nothing new
        val position = response.headers["X-Export-Position"]!!.toLong()
        val next = client.get("/api/v1/export/wall-events.csv?since=$position").body<String>()
        assertEquals(1, next.trim().lines().size, "only the header after the last position")
    }

    @Test
    fun statsAggregatePerVariantWithConversionRate() = apiTest { client, _ ->
        val hard = visitorIn("hard")
        // Funnel: gate shown -> CTA click -> checkout complete
        decide(client, hard, "a-1")
        for (type in listOf("gate_cta_click", "checkout_start", "checkout_complete")) {
            client.post("/api/v1/events") {
                contentType(ContentType.Application.Json)
                setBody(ClientEventRequest(type = type, visitorId = hard, articleId = "a-1"))
            }
        }

        val stats: List<VariantStatsResponse> = client.get("/api/v1/stats").body()
        val hardStats = stats.single { it.variant == "hard" }
        assertEquals(1, hardStats.wallsShown)
        assertEquals(1, hardStats.gateCtaClicks)
        assertEquals(1, hardStats.checkoutStarts)
        assertEquals(1, hardStats.conversions)
        assertEquals(1.0, hardStats.conversionRate) // 1 conversion / 1 wall_shown unique (AN-10)
        assertTrue(hardStats.gateCtr == 1.0)
    }
}
