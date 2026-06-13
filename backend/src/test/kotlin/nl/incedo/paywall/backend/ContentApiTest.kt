package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import nl.incedo.paywall.access.ContentTier
import nl.incedo.paywall.backend.content.ArticleRepository
import nl.incedo.paywall.backend.content.StoredArticle
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.metering.MeterPeriod

/** The consumer article endpoint: AC-01/04/05, PW-02, BP-01. */
class ContentApiTest {

    private val now = 1_750_000_000_000L

    /** A sentinel that must NEVER leave the server on a gated request (BP-01). */
    private val premiumSentinel = "SECRET-PREMIUM-SENTINEL"

    private val longBody = (1..400).joinToString(" ") { "word$it" } + " $premiumSentinel"

    private val articles = ArticleRepository(
        listOf(
            StoredArticle(ArticleId("free-1"), "Open news", ContentTier.FREE, "Everyone may read this."),
            StoredArticle(ArticleId("prem-1"), "Premium analysis", ContentTier.PREMIUM, longBody),
        ),
    )

    private fun visitorIn(variant: String): String =
        (0 until 10_000).asSequence()
            .map { "content-visitor-$it" }
            .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == variant }

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store, articles = articles) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    @Test
    fun freeArticleServesFullBody() = apiTest { client ->
        val response = client.get("/api/v1/articles/free-1") { header("X-Visitor-Id", visitorIn("hard")) }
        assertEquals(HttpStatusCode.OK, response.status)
        val article: ArticleResponse = response.body()
        assertEquals("full", article.access)
        assertEquals("Everyone may read this.", article.body)
    }

    @Test
    fun gatedResponseCarriesTeaserNeverTheBody() = apiTest { client ->
        val response = client.get("/api/v1/articles/prem-1") { header("X-Visitor-Id", visitorIn("hard")) }
        val raw = response.bodyAsText()
        assertFalse(premiumSentinel in raw, "AC-01/BP-01: the premium body must be absent, not hidden")

        val article: ArticleResponse = response.body()
        assertEquals("gate", article.access)
        assertNull(article.body)
        val teaserWords = article.teaser!!.split(" ").size
        assertTrue(teaserWords <= 152, "PW-02: teaser cut at ~150 words, got $teaserWords")
        assertEquals("hard", article.gate?.wallType)
    }

    @Test
    fun meteredVisitorReadsFullUntilTheLimit() = apiTest { client ->
        val visitor = visitorIn("metered")
        // Limit 5: this premium article reads fully and counts (MT-04)
        val first: ArticleResponse = client.get("/api/v1/articles/prem-1") { header("X-Visitor-Id", visitor) }.body()
        assertEquals("full", first.access)
        assertEquals(1, first.meterUsed, "PW-22: meter indicator after the counted read")
        assertTrue(first.body!!.contains(premiumSentinel), "a metered read is a real full read")
    }

    @Test
    fun unknownArticleIs404AndMissingVisitorIs400() = apiTest { client ->
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/articles/nope") { header("X-Visitor-Id", "v") }.status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/v1/articles/prem-1").status)
    }
}
