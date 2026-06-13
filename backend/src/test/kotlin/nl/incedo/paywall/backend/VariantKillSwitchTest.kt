package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * NFR-15: variant kill switch — an operator can disable any variant instantly
 * so visitors assigned to it receive full access without a deployment. Restore
 * reverts to the normal paywall strategy.
 */
class VariantKillSwitchTest {

    private val now = 1_750_000_000_000L

    // Operator JWT stub — jwtValidator=null means every caller is staff in test mode.
    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) =
        testApplication {
            val store = InMemoryEventStore()
            val service = AccessService(
                eventStore = store,
                experiment = defaultExperiment,
                clock = { now },
                currentPeriod = { MeterPeriod("2026-06") },
            )
            application { module(service, store) }
            val client = createClient { install(ContentNegotiation) { json() } }
            block(client)
        }

    // ── GET /api/v1/admin/variant-kill-switches ───────────────────────────────

    @Test
    fun listKilledVariantsIsEmptyByDefault() = apiTest { client ->
        val resp = client.get("/api/v1/admin/variant-kill-switches")
        assertEquals(HttpStatusCode.OK, resp.status, "NFR-15: list endpoint must return 200")
        val body = resp.body<Map<String, List<String>>>()
        assertTrue(body["killed"]!!.isEmpty(), "NFR-15: no variants killed by default")
    }

    // ── kill → visitors get full access ──────────────────────────────────────

    @Test
    fun killedVariantGrantsFullAccess() = apiTest { client ->
        // First verify the 'hard' variant gates normally
        val before = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-kv-1", articleId = "art-1", tier = "premium"))
        }.body<DecideResponse>()

        // Kill the variant (jwtValidator=null → all requests are staff in tests)
        val kill = client.post("/api/v1/admin/variants/${before.variant}/kill")
        assertEquals(HttpStatusCode.Accepted, kill.status, "NFR-15: kill must return 202")

        // Decide again with the same visitor — must now get full access
        val after = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-kv-1", articleId = "art-1", tier = "premium"))
        }.body<DecideResponse>()
        assertEquals("full", after.access, "NFR-15: killed variant must yield full access")
        assertEquals("variant_killed", after.reason, "NFR-15: reason must be variant_killed")
    }

    // ── restore reverts to normal paywall behaviour ───────────────────────────

    @Test
    fun restoreVariantRevertsToNormalBehaviour() = apiTest { client ->
        val variant = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-kv-2", articleId = "art-2", tier = "premium"))
        }.body<DecideResponse>().variant

        client.post("/api/v1/admin/variants/$variant/kill")

        val killed = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-kv-2", articleId = "art-2", tier = "premium"))
        }.body<DecideResponse>()
        assertEquals("full", killed.access, "NFR-15: killed must be full")

        val restore = client.post("/api/v1/admin/variants/$variant/restore")
        assertEquals(HttpStatusCode.Accepted, restore.status, "NFR-15: restore must return 202")

        val restored = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-kv-2", articleId = "art-2", tier = "premium"))
        }.body<DecideResponse>()
        assertEquals(variant, restored.variant, "NFR-15: same visitor must be re-assigned to their variant after restore")
    }

    // ── list shows killed variants ────────────────────────────────────────────

    @Test
    fun killedVariantsAppearInList() = apiTest { client ->
        client.post("/api/v1/admin/variants/hard/kill")
        client.post("/api/v1/admin/variants/metered/kill")
        val body = client.get("/api/v1/admin/variant-kill-switches")
            .body<Map<String, List<String>>>()
        val killed = body["killed"]!!
        assertTrue("hard" in killed, "NFR-15: hard must be in killed list")
        assertTrue("metered" in killed, "NFR-15: metered must be in killed list")
        assertEquals(2, killed.size, "NFR-15: exactly 2 variants killed")
    }

    // ── free content unaffected by kill switch ────────────────────────────────

    @Test
    fun freeContentAlwaysOpenRegardlessOfKillSwitch() = apiTest { client ->
        // Kill all variants
        defaultExperiment.variants.forEach { v ->
            client.post("/api/v1/admin/variants/${v.name}/kill")
        }
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "vis-kv-free", articleId = "art-free", tier = "free"))
        }.body<DecideResponse>()
        assertEquals("full", resp.access, "NFR-15: free content must be open even when variant is killed")
    }
}
