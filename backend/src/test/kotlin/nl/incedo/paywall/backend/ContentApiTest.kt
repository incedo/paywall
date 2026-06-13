package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

    // BP-07 bypass regression tests -----------------------------------------------

    @Test
    fun botUserAgentStillReceivesGatedDecision() = apiTest { client ->
        // AN-05/BP-07: a request with a known bot UA is not granted special access —
        // the decision is identical. Bots that reach the origin still see the gate.
        val response = client.get("/api/v1/articles/prem-1") {
            header("X-Visitor-Id", visitorIn("hard"))
            header(HttpHeaders.UserAgent, "Googlebot/2.1 (+http://www.google.com/bot.html)")
        }
        val article: ArticleResponse = response.body()
        assertEquals("gate", article.access, "bot UA must not bypass the gate")
        assertNull(article.body, "bot UA must not receive premium body")
    }

    @Test
    fun spoofedRefererHeaderDoesNotAffectDecision() = apiTest { client ->
        // BP-05/BP-07: the Referer header is never an input to access decisions;
        // a social-referrer spoof cannot soften the wall.
        val withReferer = client.get("/api/v1/articles/prem-1") {
            header("X-Visitor-Id", visitorIn("hard"))
            header(HttpHeaders.Referrer, "https://t.co/faketweet")
        }
        val withoutReferer = client.get("/api/v1/articles/prem-1") {
            header("X-Visitor-Id", visitorIn("hard"))
        }
        assertEquals(
            withoutReferer.body<ArticleResponse>().access,
            withReferer.body<ArticleResponse>().access,
            "BP-05: Referer must not influence access decision",
        )
    }

    @Test
    fun premiumPageCarriesNoarchiveRobotsTag() = apiTest { client ->
        // BP-03/BP-07: premium article responses carry X-Robots-Tag: noarchive so
        // archive crawlers (Archive.ph, Wayback Machine) can only snapshot the teaser.
        val response = client.get("/api/v1/articles/prem-1") {
            header("X-Visitor-Id", visitorIn("hard"))
        }
        val robotsTag = response.headers["X-Robots-Tag"]
        assertNotNull(robotsTag, "premium page must carry X-Robots-Tag header")
        assertTrue("noarchive" in robotsTag, "X-Robots-Tag must include noarchive, got: $robotsTag")
    }

    @Test
    fun freePageDoesNotCarryNoarchiveTag() = apiTest { client ->
        // Free content is publicly indexable; noarchive would wrongly suppress it.
        val response = client.get("/api/v1/articles/free-1") {
            header("X-Visitor-Id", visitorIn("hard"))
        }
        val robotsTag = response.headers["X-Robots-Tag"]
        assertTrue(robotsTag == null || "noarchive" !in robotsTag, "free page must not carry noarchive")
    }

    @Test
    fun fullPremiumResponseIsNotPubliclyCacheable() = apiTest { client ->
        // BP-07: a metered visitor reading a premium article must get Cache-Control:
        // private so the full body is never stored in a shared cache (CDN/proxy).
        val visitor = visitorIn("metered")
        val response = client.get("/api/v1/articles/prem-1") { header("X-Visitor-Id", visitor) }
        val article: ArticleResponse = response.body()
        assertEquals("full", article.access, "metered visitor should have full access on first read")
        val cacheControl = response.headers[HttpHeaders.CacheControl]
        assertNotNull(cacheControl, "full premium response must carry Cache-Control header")
        assertTrue("private" in cacheControl || "no-store" in cacheControl,
            "Cache-Control must prevent shared caching, got: $cacheControl")
    }
}
