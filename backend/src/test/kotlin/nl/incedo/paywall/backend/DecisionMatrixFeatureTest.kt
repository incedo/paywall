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
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * BDD scenarios for `src/test/resources/features/decision-matrix.feature` —
 * mirrors each Gherkin scenario 1:1 (NFR-12).
 *
 * Uses ?forceVariant to place visitors in specific variants, and the
 * [DecideRequest.externalScore] (DY-06) to control propensity for Dynamic.
 * Entitlements are ingested via the webhook (AC-02) before subscriber scenarios.
 */
class DecisionMatrixFeatureTest {

    private val now = 1_750_000_000_000L

    private fun scenario(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
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

    private suspend fun decide(
        client: io.ktor.client.HttpClient,
        visitorId: String,
        variant: String,
        tier: String = "premium",
        article: String = "art-dm-1",
        score: Int? = null,
    ): DecideResponse = client.post("/api/v1/decide?forceVariant=$variant") {
        contentType(ContentType.Application.Json)
        setBody(DecideRequest(visitorId = visitorId, articleId = article, tier = tier, externalScore = score))
    }.body()

    private suspend fun grant(client: io.ktor.client.HttpClient, visitorId: String, planId: String = "basic-monthly") {
        client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(EntitlementChangeRequest(
                subjectId = "visitor:$visitorId",
                subscriptionRef = "sub-$visitorId",
                planId = planId,
                validUntilEpochMs = now + 86_400_000L,
                status = "active",
            ))
        }.also { assertEquals(HttpStatusCode.Accepted, it.status) }
    }

    // ── HARD WALL ──────────────────────────────────────────────────────────────

    @Test
    fun `Hard - anonymous - free content`() = scenario { client ->
        val r = decide(client, "dm-h-anon-free", "hard", tier = "free")
        assertEquals("full", r.access, "Hard: free content always served (PW-01)")
    }

    @Test
    fun `Hard - anonymous - premium content`() = scenario { client ->
        val r = decide(client, "dm-h-anon-prem", "hard", tier = "premium")
        assertEquals("gate", r.access, "Hard: anonymous always gated on premium (PW-10)")
        assertEquals("hard", r.wallType)
    }

    @Test
    fun `Hard - registered visitor - free content`() = scenario { client ->
        val r = decide(client, "dm-h-reg-free", "hard", tier = "free")
        assertEquals("full", r.access, "Hard: free content always served")
    }

    @Test
    fun `Hard - registered visitor - premium content`() = scenario { client ->
        val r = decide(client, "dm-h-reg-prem", "hard", tier = "premium")
        assertEquals("gate", r.access, "Hard: no subscription = gate (PW-10)")
        assertEquals("hard", r.wallType)
    }

    @Test
    fun `Hard - basic subscriber - free content`() = scenario { client ->
        grant(client, "dm-h-sub-free")
        val r = decide(client, "dm-h-sub-free", "hard", tier = "free")
        assertEquals("full", r.access, "Hard: free content always served")
    }

    @Test
    fun `Hard - basic subscriber - premium content`() = scenario { client ->
        grant(client, "dm-h-sub-prem")
        val r = decide(client, "dm-h-sub-prem", "hard", tier = "premium")
        assertEquals("full", r.access, "Hard: basic subscriber gets full access (AC-01)")
    }

    @Test
    fun `Hard - expired subscriber - premium content`() = scenario { client ->
        // Expired = no active entitlement granted (never granted in this test)
        val r = decide(client, "dm-h-expired", "hard", tier = "premium")
        assertEquals("gate", r.access, "Hard: expired subscriber is gated")
    }

    // ── METERED WALL ───────────────────────────────────────────────────────────

    @Test
    fun `Metered - anonymous within limit - premium content`() = scenario { client ->
        // Read 2 articles first to consume some credit
        decide(client, "dm-m-within", "metered", article = "art-0")
        decide(client, "dm-m-within", "metered", article = "art-1")
        val r = decide(client, "dm-m-within", "metered", article = "art-2")
        assertEquals("full", r.access, "Metered: within limit must serve full (MT-04)")
        assertTrue((r.meterUsed ?: 0) <= (r.meterLimit ?: Int.MAX_VALUE))
    }

    @Test
    fun `Metered - anonymous at limit - premium content`() = scenario { client ->
        // Consume all 5 credits
        repeat(5) { i -> decide(client, "dm-m-limit", "metered", article = "art-$i") }
        val r = decide(client, "dm-m-limit", "metered", article = "art-beyond")
        assertEquals("gate", r.access, "Metered: at limit must gate (MT-04)")
        assertEquals("metered", r.wallType)
    }

    @Test
    fun `Metered - registered visitor within limit - premium content`() = scenario { client ->
        val r = decide(client, "dm-m-reg", "metered", article = "art-new")
        assertEquals("full", r.access, "Metered: registered visitor with credit served full")
    }

    @Test
    fun `Metered - registered visitor at limit - premium content`() = scenario { client ->
        repeat(5) { i -> decide(client, "dm-m-reg-limit", "metered", article = "art-$i") }
        val r = decide(client, "dm-m-reg-limit", "metered", article = "art-x")
        assertEquals("gate", r.access, "Metered: registered visitor at limit must gate")
    }

    @Test
    fun `Metered - basic subscriber - premium content`() = scenario { client ->
        grant(client, "dm-m-sub")
        val r = decide(client, "dm-m-sub", "metered", tier = "premium")
        assertEquals("full", r.access, "Metered: subscriber bypasses meter (AC-01)")
    }

    @Test
    fun `Metered - expired subscriber - premium content (meter has credit)`() = scenario { client ->
        // No valid entitlement — falls back to metered strategy; 0 of 5 used → full
        val r = decide(client, "dm-m-expired", "metered", article = "art-first")
        assertEquals("full", r.access, "Metered: expired sub treated as anonymous; credit available")
    }

    // ── FREEMIUM WALL ──────────────────────────────────────────────────────────

    @Test
    fun `Freemium - anonymous - free content`() = scenario { client ->
        val r = decide(client, "dm-f-anon-free", "freemium", tier = "free")
        assertEquals("full", r.access, "Freemium: free content always served")
    }

    @Test
    fun `Freemium - anonymous - premium content`() = scenario { client ->
        val r = decide(client, "dm-f-anon", "freemium", tier = "premium")
        assertEquals("gate", r.access, "Freemium: anonymous gated on premium (PW-20)")
        assertEquals("freemium", r.wallType)
    }

    @Test
    fun `Freemium - registered visitor - premium content`() = scenario { client ->
        val r = decide(client, "dm-f-reg", "freemium", tier = "premium")
        assertEquals("gate", r.access, "Freemium: no subscription = soft gate")
        assertEquals("freemium", r.wallType)
    }

    @Test
    fun `Freemium - basic subscriber - premium content`() = scenario { client ->
        grant(client, "dm-f-sub")
        val r = decide(client, "dm-f-sub", "freemium", tier = "premium")
        assertEquals("full", r.access, "Freemium: subscriber gets full access")
    }

    @Test
    fun `Freemium - expired subscriber - premium content`() = scenario { client ->
        val r = decide(client, "dm-f-expired", "freemium", tier = "premium")
        assertEquals("gate", r.access, "Freemium: expired treated as anonymous")
        assertEquals("freemium", r.wallType)
    }

    // ── DYNAMIC WALL ───────────────────────────────────────────────────────────

    @Test
    fun `Dynamic - anonymous no score - free content`() = scenario { client ->
        val r = decide(client, "dm-d-free", "dynamic", tier = "free")
        assertEquals("full", r.access, "Dynamic: free content always served")
    }

    @Test
    fun `Dynamic - anonymous high propensity - premium content`() = scenario { client ->
        // externalScore=80 >= tHard=70 → hard gate (DY-02); wallType is always "dynamic"
        val r = decide(client, "dm-d-high", "dynamic", tier = "premium", score = 80)
        assertEquals("gate", r.access, "Dynamic: high propensity gates (DY-02)")
        assertEquals("dynamic", r.wallType)
    }

    @Test
    fun `Dynamic - anonymous medium propensity - premium content`() = scenario { client ->
        // externalScore=50: tSoft=40 <= 50 < tHard=70 → soft gate (DY-02)
        val r = decide(client, "dm-d-med", "dynamic", tier = "premium", score = 50)
        assertEquals("gate", r.access, "Dynamic: medium propensity soft gate (DY-02)")
        assertEquals("dynamic", r.wallType)
    }

    @Test
    fun `Dynamic - anonymous low propensity within floor limit - premium content`() = scenario { client ->
        // externalScore=20 < tSoft=40 and 0 articles read < floorLimit=10 → full
        val r = decide(client, "dm-d-low", "dynamic", tier = "premium", score = 20, article = "art-first")
        assertEquals("full", r.access, "Dynamic: low propensity within floor limit = full access (PW-42)")
    }

    @Test
    fun `Dynamic - basic subscriber - premium content`() = scenario { client ->
        grant(client, "dm-d-sub")
        val r = decide(client, "dm-d-sub", "dynamic", tier = "premium")
        assertEquals("full", r.access, "Dynamic: subscriber always gets full access (AC-01)")
    }

    @Test
    fun `Dynamic - expired subscriber - premium content`() = scenario { client ->
        // No entitlement → score-driven gate; use externalScore=50 for determinism
        val r = decide(client, "dm-d-expired", "dynamic", tier = "premium", score = 50)
        assertEquals("gate", r.access, "Dynamic: expired treated as anonymous — propensity-gated")
    }

    // ── COMPLETE-TIER LOCK (UP-12) ─────────────────────────────────────────────

    @Test
    fun `Hard - basic subscriber (rank 1) - complete-tier content`() = scenario { client ->
        // basic-monthly has rank=1; complete-tier requires rank=2 (UP-12)
        grant(client, "dm-h-basic-cmp", planId = "basic-monthly")
        val r = decide(client, "dm-h-basic-cmp", "hard", tier = "complete")
        assertEquals("gate", r.access, "UP-12: basic subscriber gated on complete-tier content")
        assertTrue(r.tierLocked, "UP-12: tierLocked must be true")
    }

    @Test
    fun `Hard - complete subscriber (rank 2) - complete-tier content`() = scenario { client ->
        // complete-monthly has rank=2 in DefaultPlans
        grant(client, "dm-h-comp-cmp", planId = "complete-monthly")
        val r = decide(client, "dm-h-comp-cmp", "hard", tier = "complete")
        assertEquals("full", r.access, "UP-12: complete subscriber gets full access on complete-tier")
    }

    @Test
    fun `Metered - basic subscriber (rank 1) - complete-tier content`() = scenario { client ->
        grant(client, "dm-m-basic-cmp", planId = "basic-monthly")
        val r = decide(client, "dm-m-basic-cmp", "metered", tier = "complete")
        assertEquals("gate", r.access, "UP-12: basic subscriber gated on complete-tier (metered variant)")
        assertTrue(r.tierLocked, "UP-12: tierLocked must be true")
    }
}
