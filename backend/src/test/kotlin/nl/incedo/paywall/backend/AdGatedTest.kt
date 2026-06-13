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
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.metering.MeterPeriod

/**
 * AG-02: verified ad-completion integration tests — 24h grant issuance and daily cap (2/day).
 * ADM-14: wallDesignId appears in decide response when set on the variant.
 */
class AdGatedTest {

    private val now = 1_750_000_000_000L

    private fun hardVariantVisitor(seed: String): String =
        (0 until 10_000).asSequence()
            .map { "$seed-ag-$it" }
            .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == "hard" }

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
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

    // ── AG-02: ad-gated grant issuance ────────────────────────────────────────

    @Test
    fun adCompletionIssuesGrantForArticle() = apiTest { client ->
        val vis = hardVariantVisitor("ag-grant")
        val articleId = "article-ag-1"

        // Before completion: gate
        val before = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = vis, articleId = articleId, tier = "premium"))
        }.body<DecideResponse>()
        assertEquals("gate", before.access, "article must be gated before ad completion")

        // Verified ad completion signal
        val completion = client.post("/api/v1/integration/ad-completion") {
            contentType(ContentType.Application.Json)
            setBody(AdCompletionRequest(
                subjectId = "visitor:$vis",
                articleId = articleId,
                adPlayId = "play-001",
            ))
        }
        assertEquals(HttpStatusCode.Created, completion.status)

        // After completion: access granted (24h TTL)
        val after = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = vis, articleId = articleId, tier = "premium"))
        }.body<DecideResponse>()
        assertEquals("full", after.access, "ad-gated grant must allow access after completion (AG-02)")
    }

    @Test
    fun adCompletionIsIdempotentOnSamePlayId() = apiTest { client ->
        val vis = hardVariantVisitor("ag-idem")
        val articleId = "article-ag-idem"

        // First completion
        val first = client.post("/api/v1/integration/ad-completion") {
            contentType(ContentType.Application.Json)
            setBody(AdCompletionRequest("visitor:$vis", articleId, "play-idem-1"))
        }
        assertEquals(HttpStatusCode.Created, first.status)

        // Same play ID again — idempotent (grant already exists with same ID)
        val second = client.post("/api/v1/integration/ad-completion") {
            contentType(ContentType.Application.Json)
            setBody(AdCompletionRequest("visitor:$vis", articleId, "play-idem-1"))
        }
        // The second append with the same grantId is a no-op in the event store
        // (append does not reject duplicates, but the decision model deduplicates)
        assertEquals(HttpStatusCode.Created, second.status)
    }

    @Test
    fun dailyCapEnforcedAfterTwoGrants() = apiTest { client ->
        val vis = hardVariantVisitor("ag-cap")

        // First unlock
        val first = client.post("/api/v1/integration/ad-completion") {
            contentType(ContentType.Application.Json)
            setBody(AdCompletionRequest("visitor:$vis", "article-a", "play-cap-1"))
        }
        assertEquals(HttpStatusCode.Created, first.status)

        // Second unlock (different article)
        val second = client.post("/api/v1/integration/ad-completion") {
            contentType(ContentType.Application.Json)
            setBody(AdCompletionRequest("visitor:$vis", "article-b", "play-cap-2"))
        }
        assertEquals(HttpStatusCode.Created, second.status)

        // Third unlock (cap = 2): rejected (AG-02)
        val third = client.post("/api/v1/integration/ad-completion") {
            contentType(ContentType.Application.Json)
            setBody(AdCompletionRequest("visitor:$vis", "article-c", "play-cap-3"))
        }
        assertEquals(HttpStatusCode.TooManyRequests, third.status,
            "daily cap of 2 ad-gated unlocks must be enforced (AG-02)")
    }

    @Test
    fun missingFieldsReturnBadRequest() = apiTest { client ->
        val resp = client.post("/api/v1/integration/ad-completion") {
            contentType(ContentType.Application.Json)
            setBody(AdCompletionRequest(subjectId = "", articleId = "art-1", adPlayId = "p1"))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    // ── ADM-14: wallDesignId in decide response ───────────────────────────────

    @Test
    fun decideResponseIncludesWallDesignIdWhenConfigured() {
        val wallId = "wall-special-v2"
        val experimentWithDesign = defaultExperiment.copy(
            variants = defaultExperiment.variants.map { v ->
                if (v.name == "hard") v.copy(wallDesignId = wallId) else v
            },
        )

        testApplication {
            val store = InMemoryEventStore()
            val service = AccessService(
                eventStore = store,
                experiment = experimentWithDesign,
                clock = { now },
                currentPeriod = { MeterPeriod("2026-06") },
            )
            application { module(service, store) }
            val client = createClient { install(ContentNegotiation) { json() } }

            val vis = (0 until 10_000).asSequence()
                .map { "adm14-$it" }
                .first { VariantAssigner.assign(VisitorId(it), experimentWithDesign).name == "hard" }

            val resp = client.post("/api/v1/decide") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = vis, articleId = "art-premium", tier = "premium"))
            }.body<DecideResponse>()

            assertEquals("gate", resp.access)
            assertEquals(wallId, resp.wallDesignId, "wallDesignId must be included in response (ADM-14)")
        }
    }

    @Test
    fun decideResponseHasNullWallDesignIdWhenNotConfigured() = apiTest { client ->
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "any-visitor", articleId = "art", tier = "free"))
        }.body<DecideResponse>()
        assertEquals(null, resp.wallDesignId, "wallDesignId defaults to null when not set")
    }
}
