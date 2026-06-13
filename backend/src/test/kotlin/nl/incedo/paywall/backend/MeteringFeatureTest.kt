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
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.analytics.wallEventShardTags
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.metering.MeterPeriod

/**
 * BDD scenarios for `src/test/resources/features/metering.feature` — the
 * Gherkin file and these tests mirror each other 1:1 (testing.md). The
 * Cucumber engine wedges in this CI environment, so the scenarios run as
 * plain tests against the same full HTTP stack.
 */
class MeteringFeatureTest {

    private val now = 1_750_000_000_000L

    /** Background: a visitor assigned to the metered variant with a limit of 5. */
    private val visitor: String = (0 until 10_000).asSequence()
        .map { "bdd-visitor-$it" }
        .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == "metered" }

    private fun scenario(block: suspend (io.ktor.client.HttpClient, InMemoryEventStore) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client, store)
    }

    private suspend fun decide(client: io.ktor.client.HttpClient, article: String): DecideResponse =
        client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = visitor, articleId = article, tier = "premium"))
        }.body()

    @Test
    fun `Scenario - Reading within the free limit`() = scenario { client, _ ->
        // When the visitor reads 5 distinct premium articles
        val responses = (0 until 5).map { decide(client, "article-$it") }
        // Then every article is served in full
        assertTrue(responses.all { it.access == "full" })
        // And the meter shows 5 of 5 used
        assertEquals(5, responses.last().meterUsed)
        assertEquals(5, responses.last().meterLimit)
    }

    @Test
    fun `Scenario - The gate appears at limit plus one`() = scenario { client, store ->
        // When the visitor reads 5 distinct premium articles
        repeat(5) { decide(client, "article-$it") }
        // And the visitor opens one more premium article
        val gated = decide(client, "article-beyond-limit")
        // Then the gate is shown with the metered wall
        assertEquals("gate", gated.access)
        assertEquals("metered", gated.wallType)
        // And a wall_shown event is recorded with the meter context
        val shown = store.query(EventQuery(wallEventShardTags())).events
            .filterIsInstance<WallEventRecorded>()
            .filter { it.eventType == WallEventType.WALL_SHOWN }
        assertTrue(shown.isNotEmpty(), "AN-02: wall_shown logged")
        assertEquals("5", shown.last().context["meterUsed"], "AN-03: decision context carried")
    }

    @Test
    fun `Scenario - Re-reading a counted article consumes no credit`() = scenario { client, _ ->
        // When the visitor reads 5 distinct premium articles
        repeat(5) { decide(client, "article-$it") }
        // And the visitor re-reads the first article
        val reRead = decide(client, "article-0")
        // Then the article is served in full
        assertEquals("full", reRead.access)
        // And the meter still shows 5 of 5 used
        assertEquals(5, reRead.meterUsed)
        assertEquals(5, reRead.meterLimit)
    }

    @Test
    fun `Scenario - An audited meter reset restores access`() = scenario { client, _ ->
        // When the visitor reads 5 distinct premium articles
        repeat(5) { decide(client, "article-$it") }
        // And support resets the meter citing "support ticket 4711"
        val reset = client.post("/api/v1/admin/subjects/visitor:$visitor/meter-reset") {
            contentType(ContentType.Application.Json)
            setBody(MeterResetRequest(actor = "m.visser", reason = "support ticket 4711"))
        }
        assertEquals(HttpStatusCode.Accepted, reset.status)
        // Then the visitor can read a new premium article in full
        assertEquals("full", decide(client, "article-after-reset").access)
    }
}
