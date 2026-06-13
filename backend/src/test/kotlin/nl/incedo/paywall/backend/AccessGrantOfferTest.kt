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
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * FGA-07: CEP offer of kind access_grant accepted → grant automatically issued
 * for the specified article. Covers the happy path, missing articleId (no grant),
 * non-access_grant kinds (no grant), and granted_by=ai_engine short TTL.
 */
class AccessGrantOfferTest {

    private val now = 1_750_000_000_000L

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

    @Test
    fun accessGrantOfferAcceptedIssuersGrant() = apiTest { client ->
        // Accept an access_grant offer with articleId → expect a grant to appear
        val resp = client.post(
            "/api/v1/offers/accept?visitorId=vis-fga07&offerId=offer-fga07&kind=access_grant" +
                "&channel=web&articleId=a-premium-99&grantTtlSeconds=259200",
        )
        assertEquals(HttpStatusCode.Accepted, resp.status)

        // Verify the grant is now live for the subject
        val grants = client.get("/api/v1/admin/subjects/visitor:vis-fga07/grants")
            .body<List<GrantAuditEntry>>()
        assertEquals(1, grants.size, "FGA-07: one grant must be issued")
        assertEquals("a-premium-99", grants[0].articleId, "FGA-07: grant must be for the specified article")
        assertEquals("cep_offer", grants[0].grantedBy, "FGA-07: grantedBy defaults to cep_offer")
        assertEquals(true, grants[0].isLive, "FGA-07: grant must be live after accept")
    }

    @Test
    fun accessGrantWithoutArticleIdDoesNotIssueGrant() = apiTest { client ->
        // No articleId → no grant issued (just OfferAccepted logged)
        client.post(
            "/api/v1/offers/accept?visitorId=vis-fga07b&offerId=offer-fga07b&kind=access_grant&channel=web",
        )
        val grants = client.get("/api/v1/admin/subjects/visitor:vis-fga07b/grants")
            .body<List<GrantAuditEntry>>()
        assertEquals(0, grants.size, "FGA-07: no articleId → no grant issued")
    }

    @Test
    fun nonAccessGrantKindDoesNotIssueGrant() = apiTest { client ->
        // kind=downsell → no auto-grant
        client.post(
            "/api/v1/offers/accept?visitorId=vis-fga07c&offerId=offer-fga07c&kind=downsell&channel=web",
        )
        val grants = client.get("/api/v1/admin/subjects/visitor:vis-fga07c/grants")
            .body<List<GrantAuditEntry>>()
        assertEquals(0, grants.size, "FGA-07: non-access_grant kind must not issue grant")
    }

    @Test
    fun aiEngineGrantedByParam() = apiTest { client ->
        // AI engine recommendation — grantedBy=ai_engine, short TTL 72h
        client.post(
            "/api/v1/offers/accept?visitorId=vis-fga07d&offerId=offer-fga07d&kind=access_grant" +
                "&channel=web&articleId=a-ai-rec&grantedBy=ai_engine&grantTtlSeconds=259200",
        )
        val grants = client.get("/api/v1/admin/subjects/visitor:vis-fga07d/grants")
            .body<List<GrantAuditEntry>>()
        val grant = grants.single()
        assertEquals("ai_engine", grant.grantedBy, "FGA-07: ai_engine grantedBy must be passed through")
        assertEquals("a-ai-rec", grant.articleId)
    }
}
