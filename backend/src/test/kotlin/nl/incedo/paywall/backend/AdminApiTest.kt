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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.metering.MeterPeriod

/** ADM-04 subject inspector + PW-22/23 meter indicator and nudge. */
class AdminApiTest {

    private val now = 1_750_000_000_000L

    private fun visitorIn(variant: String): String =
        (0 until 10_000).asSequence()
            .map { "adm-visitor-$it" }
            .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == variant }

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod(currentPeriod().value) }, // inspector uses the real period helper
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    private suspend fun decide(client: io.ktor.client.HttpClient, visitor: String, article: String): DecideResponse =
        client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = visitor, articleId = article, tier = "premium"))
        }.body()

    @Test
    fun inspectorShowsMeterStateAndRecentEvents() = apiTest { client ->
        val visitor = visitorIn("metered")
        decide(client, visitor, "a-1")
        decide(client, visitor, "a-2")

        val inspector: SubjectInspectorResponse = client.get("/api/v1/admin/subjects/visitor:$visitor").body()
        assertEquals(2, inspector.meterUsed)
        assertEquals("metered", inspector.variant)
        assertFalse(inspector.entitled)
        assertTrue(inspector.recentWallEvents.any { it.type == "article_read" })
    }

    @Test
    fun meterResetIsAuditedAndTakesEffect() = apiTest { client ->
        val visitor = visitorIn("metered")
        repeat(5) { decide(client, visitor, "a-$it") }
        assertEquals("gate", decide(client, visitor, "a-new").access)

        // Support resets the meter — actor and reason are mandatory (ADM-04)
        val unaudited = client.post("/api/v1/admin/subjects/visitor:$visitor/meter-reset") {
            contentType(ContentType.Application.Json)
            setBody(MeterResetRequest(actor = "", reason = ""))
        }
        assertEquals(HttpStatusCode.BadRequest, unaudited.status)

        val reset = client.post("/api/v1/admin/subjects/visitor:$visitor/meter-reset") {
            contentType(ContentType.Application.Json)
            setBody(MeterResetRequest(actor = "m.visser", reason = "support ticket 4711"))
        }
        assertEquals(HttpStatusCode.Accepted, reset.status)
        assertEquals("full", decide(client, visitor, "a-new").access)
    }

    @Test
    fun nudgeAppearsWhenOneCreditRemains() = apiTest { client ->
        val visitor = visitorIn("metered") // limit 5
        // Reads 1-3: no nudge yet
        for (i in 1..3) {
            assertFalse(decide(client, visitor, "a-$i").nudge, "read $i")
        }
        // Read 4 leaves exactly one credit: soft banner (PW-23)
        val fourth = decide(client, visitor, "a-4")
        assertTrue(fourth.nudge)
        assertEquals(4, fourth.meterUsed)
        assertEquals(5, fourth.meterLimit, "PW-22: indicator shows used of limit")
        // Read 5 exhausts: no nudge, next one gates
        assertFalse(decide(client, visitor, "a-5").nudge)
        assertEquals("gate", decide(client, visitor, "a-6").access)
    }
}
