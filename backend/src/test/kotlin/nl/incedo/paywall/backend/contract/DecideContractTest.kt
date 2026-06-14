package nl.incedo.paywall.backend.contract

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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import nl.incedo.paywall.backend.AccessService
import nl.incedo.paywall.backend.DecideRequest
import nl.incedo.paywall.backend.DecideResponse
import nl.incedo.paywall.backend.defaultExperiment
import nl.incedo.paywall.backend.module
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * Producer-side contract tests — verify the access-decision API response shapes.
 * (API-05, testing.md §3 "Access Decision")
 */
class DecideContractTest {

    private fun contractTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { 1_750_000_000_000L },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    // ── Contract: Access Decision (full) ────────────────────────────────────
    // Consumer expects: POST /api/v1/decide → 200 DecideResponse with access, variant, meter
    // (API-05, testing.md §3 "Access Decision")

    @Test
    fun `Contract - POST decide free article returns 200 with full access shape`() = contractTest { client ->
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "contract-vis-1", articleId = "art-free", tier = "free"))
        }
        assertEquals(HttpStatusCode.OK, resp.status, "Consumer contract: decide must return 200")
        val body: DecideResponse = resp.body()
        assertEquals("full", body.access, "free article must always return full access")
        assertNotNull(body.variant, "variant must always be present")
        assertTrue(body.variant.isNotBlank(), "variant must not be blank")
    }

    @Test
    fun `Contract - POST decide premium article returns 200 with gate shape`() = contractTest { client ->
        // Force hard variant so the response is deterministically gated
        val resp = client.post("/api/v1/decide?forceVariant=hard") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "contract-vis-2", articleId = "art-prem", tier = "premium"))
        }
        assertEquals(HttpStatusCode.OK, resp.status, "Consumer contract: decide must return 200")
        val body: DecideResponse = resp.body()
        assertEquals("gate", body.access, "hard variant must gate anonymous premium requests")
        assertNotNull(body.variant, "variant must be present on gate response")
        assertNotNull(body.wallType, "wallType must be present on gate response")
        assertTrue(body.variant.isNotBlank())
        assertTrue(body.wallType.orEmpty().isNotBlank())
    }

    @Test
    fun `Contract - POST decide metered variant returns meter fields`() = contractTest { client ->
        val resp = client.post("/api/v1/decide?forceVariant=metered") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "contract-vis-3", articleId = "art-m1", tier = "premium"))
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        val body: DecideResponse = resp.body()
        assertEquals("full", body.access, "metered variant within limit must return full")
        assertNotNull(body.meterUsed, "meterUsed must be present for metered variant")
        assertNotNull(body.meterLimit, "meterLimit must be present for metered variant")
        assertTrue(body.meterUsed >= 0)
        assertTrue(body.meterLimit > 0)
    }

    // ── Contract: error shapes ──────────────────────────────────────────────

    @Test
    fun `Contract - POST decide with invalid tier returns 400 with error shape`() = contractTest { client ->
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-bad", articleId = "art-bad", tier = "invalid-tier"))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status,
            "Consumer contract: invalid tier must return 400")
        val json = Json.parseToJsonElement(resp.body<String>()).jsonObject
        assertTrue(json.containsKey("error"), "400 must include 'error' field")
    }
}
