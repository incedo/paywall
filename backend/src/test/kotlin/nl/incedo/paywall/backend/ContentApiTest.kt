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

    private fun apiTest(
        originSecret: String? = null,
        block: suspend (io.ktor.client.HttpClient) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store, articles = articles, originSecret = originSecret) }
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

    // MT-05/SEO-01/02: verified crawler exemption ------------------------------------

    @Test
    fun verifiedCrawlerGetsPremiumBodyWithoutMetering() = apiTest(originSecret = "edge-secret") { client ->
        // MT-05/SEO-02: a request forwarded by the edge with X-Verified-Bot: true
        // and the shared secret bypasses the gate and metering entirely.
        // The hard-wall variant gates every other request — crawler is exempt.
        val visitor = visitorIn("hard")
        val response = client.get("/api/v1/articles/prem-1") {
            header("X-Visitor-Id", visitor)
            header("X-Origin-Secret", "edge-secret")  // proves edge is in front (INF-02)
            header("X-Verified-Bot", "true")           // edge's crawler signal (SEO-02)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val article: ArticleResponse = response.body()
        assertEquals("full", article.access, "MT-05: verified crawler must not be gated")
        assertTrue(article.body!!.contains(premiumSentinel), "SEO-02: verified crawler gets full body")
        assertNull(article.gate, "verified crawler must not see gate info")
        assertNull(article.meterUsed, "MT-05: meter is not incremented for verified crawlers")
        // SEO-01: JSON-LD structured data included for edge HTML rendering
        assertNotNull(article.structuredData, "SEO-01: JSON-LD must be included for premium crawler response")
        assertTrue("isAccessibleForFree" in article.structuredData!!, "SEO-01: JSON-LD must carry isAccessibleForFree")
    }

    @Test
    fun verifiedCrawlerSignalWithoutEdgeSecretIsIgnored() = apiTest(originSecret = "edge-secret") { client ->
        // BP-02: the edge verified the crawler — UA alone grants nothing.
        // If there is no shared secret in the request, X-Verified-Bot is untrusted.
        val response = client.get("/api/v1/articles/prem-1") {
            header("X-Visitor-Id", visitorIn("hard"))
            // Deliberately NOT setting X-Origin-Secret — request did not come from edge.
            header("X-Verified-Bot", "true")
        }
        // Request is blocked by origin trust check (no edge secret) — 401 (INF-02).
        // In this test originSecret is configured so the request without the
        // shared secret is rejected before the crawler signal is ever inspected.
        assertEquals(HttpStatusCode.Unauthorized, response.status, "BP-02: request without edge secret is rejected")
    }

    // BP-04: RSS feed ---------------------------------------------------------------

    @Test
    fun rssFeedExposesTeaserOnlyForPremiumArticles() = apiTest { client ->
        // BP-04: the RSS feed must not expose the full premium body;
        // free articles may expose their full body.
        val response = client.get("/api/v1/feed.rss")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()

        // Free article body is present
        assertTrue("Everyone may read this." in body, "free article body must appear in the RSS feed")

        // Premium sentinel (the last word of the full body) must NOT appear in the feed
        assertFalse(premiumSentinel in body, "BP-04: premium body must be absent from the RSS feed")

        // Some teaser text from the premium article must appear (first ~150 words)
        assertTrue("word1" in body, "BP-04: premium article teaser must appear in the RSS feed")

        // The feed is valid RSS 2.0
        assertTrue(body.contains("<rss version=\"2.0\""), "feed must be RSS 2.0")
        assertTrue(body.contains("<item>"), "feed must contain items")
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

    // INF-09: Cloudflare bot score -----------------------------------------------

    @Test
    fun cfBotScoreBelowThresholdFlagsAsBot() = apiTest(originSecret = "edge-secret") { client ->
        // INF-09: a score ≤29 forwarded by the Worker flags the request as bot.
        // The article endpoint must gate access, identical to a bot UA.
        val response = client.get("/api/v1/articles/prem-1") {
            header("X-Visitor-Id", visitorIn("hard"))
            header("X-Origin-Secret", "edge-secret")
            header("X-CF-Bot-Score", "15")
        }
        val article: ArticleResponse = response.body()
        assertEquals("gate", article.access, "INF-09: CF score ≤29 must not bypass the gate")
        assertNull(article.body, "INF-09: premium body must be absent when CF score ≤29")
    }

    @Test
    fun cfBotScoreAboveThresholdDoesNotFlagAsBot() = apiTest(originSecret = "edge-secret") { client ->
        // INF-09: a score ≥30 is "likely human" — the request is not flagged as bot.
        // A metered visitor with score 80 should still get full access on first read.
        val visitor = visitorIn("metered")
        val response = client.get("/api/v1/articles/prem-1") {
            header("X-Visitor-Id", visitor)
            header("X-Origin-Secret", "edge-secret")
            header("X-CF-Bot-Score", "80")
        }
        val article: ArticleResponse = response.body()
        assertEquals("full", article.access, "INF-09: CF score ≥30 must not flag as bot")
    }

    @Test
    fun cfBotScoreIsIgnoredWithoutOriginSecret() = apiTest { client ->
        // INF-09 / INF-02: the header is only trusted when the edge secret is present.
        // Without an originSecret configured, the header is ignored (no DoS surface).
        val visitor = visitorIn("metered")
        val withLowScore = client.get("/api/v1/articles/prem-1") {
            header("X-Visitor-Id", visitor)
            header("X-CF-Bot-Score", "5")    // low score, but no origin secret configured
        }
        assertEquals("full", withLowScore.body<ArticleResponse>().access,
            "INF-09: CF score must be ignored when no originSecret is configured")
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
