package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/** BP-05: subscriber-generated signed share tokens. */
class ShareApiTest {

    private val now = 1_750_000_000_000L
    private val testSecret = "test-share-secret"
    private val monthPeriod = "2026-06"

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod(monthPeriod) },
        )
        val shareTokenService = ShareTokenService(store, testSecret, clock = { now }, monthlyCapCount = 3)
        application { module(service, store, shareTokenService = shareTokenService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    @Test
    fun redeemValidTokenGrantsAccess() = apiTest { client ->
        val store = InMemoryEventStore()
        val shareTokenSvc = ShareTokenService(store, testSecret, clock = { now })
        val articleId = ArticleId("article-1")
        val subscriberSubjectId = SubjectId("user:subscriber-1")

        val result = shareTokenSvc.issue(subscriberSubjectId, articleId, monthPeriod)
        val issued = (result as ShareTokenService.IssueResult.Success).issued

        val resp = client.post("/api/v1/shares/redeem") {
            contentType(ContentType.Application.Json)
            setBody(RedeemShareTokenRequest(visitorId = "visitor-abc", token = issued.token))
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = resp.body<Map<String, String>>()
        assertNotNull(body["grantId"])
    }

    @Test
    fun redeemInvalidTokenRejects() = apiTest { client ->
        val resp = client.post("/api/v1/shares/redeem") {
            contentType(ContentType.Application.Json)
            setBody(RedeemShareTokenRequest(visitorId = "visitor-abc", token = "not-a-real-token"))
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun redeemExpiredTokenReturnsGone() = apiTest { client ->
        val store = InMemoryEventStore()
        val pastClock = { now - 10_000L } // token was issued 10s ago
        val shortTtlService = ShareTokenService(store, testSecret, clock = pastClock, tokenTtlMs = 5_000L)
        val issued = (shortTtlService.issue(SubjectId("user:u"), ArticleId("a"), monthPeriod) as ShareTokenService.IssueResult.Success).issued

        val resp = client.post("/api/v1/shares/redeem") {
            contentType(ContentType.Application.Json)
            setBody(RedeemShareTokenRequest(visitorId = "visitor-abc", token = issued.token))
        }
        assertEquals(HttpStatusCode.Gone, resp.status)
    }

    @Test
    fun capEnforcedAfterMaxTokensIssued() = apiTest { client ->
        // Issue endpoint needs an authenticated JWT — without jwtValidator it returns 401
        // (no CIAM configured in tests). Test cap via service layer directly.
        val store = InMemoryEventStore()
        val svc = ShareTokenService(store, testSecret, clock = { now }, monthlyCapCount = 2)
        val article = ArticleId("article-cap-test")
        val subscriber = SubjectId("user:cap-subscriber")

        val r1 = svc.issue(subscriber, article, monthPeriod)
        val r2 = svc.issue(subscriber, article, monthPeriod)
        val r3 = svc.issue(subscriber, article, monthPeriod) // should hit cap

        assert(r1 is ShareTokenService.IssueResult.Success)
        assert(r2 is ShareTokenService.IssueResult.Success)
        assertEquals(ShareTokenService.IssueResult.CapExceeded, r3)
    }

    @Test
    fun tokensFromDifferentSecretsAreRejected() = apiTest { client ->
        val otherStore = InMemoryEventStore()
        val otherSvc = ShareTokenService(otherStore, "different-secret", clock = { now })
        val issued = (otherSvc.issue(SubjectId("user:u"), ArticleId("a"), monthPeriod) as ShareTokenService.IssueResult.Success).issued

        val resp = client.post("/api/v1/shares/redeem") {
            contentType(ContentType.Application.Json)
            setBody(RedeemShareTokenRequest(visitorId = "visitor-x", token = issued.token))
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun shareEndpointRequiresAuthWithoutJwtValidator() = apiTest { client ->
        // With no jwtValidator configured, the share endpoint returns 401.
        val resp = client.post("/api/v1/articles/article-1/share") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun redeemMissingFieldsReturnsBadRequest() = apiTest { client ->
        val resp = client.post("/api/v1/shares/redeem") {
            contentType(ContentType.Application.Json)
            setBody(RedeemShareTokenRequest(visitorId = "", token = "tok"))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }
}
