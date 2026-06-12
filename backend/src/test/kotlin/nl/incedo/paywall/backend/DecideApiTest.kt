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
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.metering.MeterPeriod

class DecideApiTest {

    private val now = 1_750_000_000_000L

    private fun testService() = AccessService(
        eventStore = InMemoryEventStore(),
        experiment = defaultExperiment,
        clock = { now },
        currentPeriod = { MeterPeriod("2026-06") },
    )

    /** Find a visitor that EX-01 assigns to the wanted variant, so tests are deterministic. */
    private fun visitorIn(variant: String): String =
        (0 until 10_000).asSequence()
            .map { "test-visitor-$it" }
            .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == variant }

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        application { module(testService()) }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        block(client)
    }

    @Test
    fun freeContentIsNeverGated() = apiTest { client ->
        // PW-01 — for every variant, so try visitors across all four
        for (variant in listOf("hard", "metered", "freemium", "dynamic")) {
            val response = client.post("/api/v1/decide") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = visitorIn(variant), articleId = "a-1", tier = "free"))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body: DecideResponse = response.body()
            assertEquals("full", body.access, "variant $variant")
            assertEquals("free_content", body.reason)
        }
    }

    @Test
    fun hardWallGatesPremiumImmediately() = apiTest { client ->
        val response = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = visitorIn("hard"), articleId = "a-1", tier = "premium"))
        }
        val body: DecideResponse = response.body()
        assertEquals("gate", body.access)
        assertEquals("hard", body.wallType)
    }

    @Test
    fun meteredCountsFiveReadsThenGates() = apiTest { client ->
        val visitor = visitorIn("metered")
        // PW-20: limit 5 — five distinct premium articles pass and are counted
        for (i in 1..5) {
            val body: DecideResponse = client.post("/api/v1/decide") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = visitor, articleId = "a-$i", tier = "premium"))
            }.body()
            assertEquals("full", body.access, "read $i")
            assertEquals(i, body.meterUsed, "PW-22: meter indicator after read $i")
        }
        // Article 6 hits the gate
        val gated: DecideResponse = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = visitor, articleId = "a-6", tier = "premium"))
        }.body()
        assertEquals("gate", gated.access)
        assertEquals(5, gated.meterUsed)
        assertEquals(5, gated.meterLimit)
        // PW-21: re-reading a counted article still works
        val reRead: DecideResponse = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = visitor, articleId = "a-3", tier = "premium"))
        }.body()
        assertEquals("full", reRead.access)
        assertEquals(5, reRead.meterUsed, "re-read consumes no credit")
    }

    @Test
    fun variantAssignmentIsStableAcrossRequests() = apiTest { client ->
        // EX-01: no flicker between variants
        val visitor = "stable-visitor"
        val variants = (1..10).map {
            client.post("/api/v1/decide") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = visitor, articleId = "a-$it", tier = "premium"))
            }.body<DecideResponse>().variant
        }.toSet()
        assertEquals(1, variants.size)
    }

    @Test
    fun invalidTierIsRejected() = apiTest { client ->
        val response = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "v-1", articleId = "a-1", tier = "platinum"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun gateResponseNeverContainsArticleContent() = apiTest { client ->
        // AC-01 shape check: the decide response carries decision data only
        val response = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = visitorIn("hard"), articleId = "a-1", tier = "premium"))
        }
        val raw = response.body<String>()
        assertTrue("body" !in raw && "content" !in raw, "decide response must not carry content fields")
    }
}
