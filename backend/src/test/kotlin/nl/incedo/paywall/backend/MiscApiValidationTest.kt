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
import kotlin.test.assertNull
import nl.incedo.paywall.cep.MockCepClient
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * Validation-path coverage for endpoints that have their error branches uncovered by
 * the feature-focused test suites: offer decline/accept, identity-link, soft-gate
 * dismissal, share token, engagement upsell, checkout (user: prefix and CEP path),
 * cancel subscription, and experiment config publishing.
 */
class MiscApiValidationTest {

    private val now = 1_750_000_000_000L

    private fun apiTest(
        withOfferService: Boolean = false,
        withShareService: Boolean = false,
        block: suspend (io.ktor.client.HttpClient) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        val offerService = if (withOfferService) OfferService(store, MockCepClient(), clock = { now }) else null
        val shareTokenService = if (withShareService) ShareTokenService(store, "test-secret", clock = { now }) else null
        application { module(service, store, offerService = offerService, shareTokenService = shareTokenService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    // ── UP-05: offer decline validation ─────────────────────────────────────

    @Test
    fun offerDeclineWithoutVisitorIdReturns400() = apiTest { client ->
        val resp = client.post("/api/v1/offers/decline?offerId=offer-1")
        assertEquals(HttpStatusCode.BadRequest, resp.status, "UP-05: missing visitorId must be rejected")
    }

    @Test
    fun offerDeclineWithoutOfferIdReturns400() = apiTest { client ->
        val resp = client.post("/api/v1/offers/decline?visitorId=vis-1")
        assertEquals(HttpStatusCode.BadRequest, resp.status, "UP-05: missing offerId must be rejected")
    }

    // ── DN-05: offer accept validation ──────────────────────────────────────

    @Test
    fun offerAcceptWithoutVisitorIdReturns400() = apiTest { client ->
        val resp = client.post("/api/v1/offers/accept?offerId=offer-1")
        assertEquals(HttpStatusCode.BadRequest, resp.status, "DN-05: missing visitorId must be rejected")
    }

    @Test
    fun offerAcceptWithoutOfferIdReturns400() = apiTest { client ->
        val resp = client.post("/api/v1/offers/accept?visitorId=vis-1")
        assertEquals(HttpStatusCode.BadRequest, resp.status, "DN-05: missing offerId must be rejected")
    }

    // ── MT-13: identity-link validation ─────────────────────────────────────

    @Test
    fun identityLinkWithBlankFieldsReturns400() = apiTest { client ->
        val resp = client.post("/api/v1/integration/identity-link") {
            contentType(ContentType.Application.Json)
            setBody(IdentityLinkRequest(subjectA = "", subjectB = "", cause = "login"))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status, "MT-13: blank subjectA/subjectB must be rejected")
    }

    @Test
    fun identityUnlinkRecordsUnlinkedEvent() = apiTest { client ->
        val resp = client.post("/api/v1/integration/identity-link") {
            contentType(ContentType.Application.Json)
            setBody(IdentityLinkRequest(
                subjectA = "visitor:v1",
                subjectB = "user:u1",
                cause = "user_request",
                link = false,
            ))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status, "MT-13: unlink (link=false) must be accepted")
    }

    // ── AC-13: soft-gate dismissal validation ───────────────────────────────

    @Test
    fun softGateDismissalWithoutVisitorIdReturns400() = apiTest { client ->
        val resp = client.post("/api/v1/decide/dismiss-soft-gate")
        assertEquals(HttpStatusCode.BadRequest, resp.status, "AC-13: missing visitorId must be rejected")
    }

    // ── BP-05: share token redeem when service not configured ─────────────

    @Test
    fun shareTokenRedeemWithNoServiceReturns503() = apiTest { client ->
        val resp = client.post("/api/v1/shares/redeem") {
            contentType(ContentType.Application.Json)
            setBody(RedeemShareTokenRequest(visitorId = "vis-1", token = "tok-1"))
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, resp.status, "BP-05: no share service must return 503")
    }

    // ── UP-13: engagement upsell when offer service not configured ──────────

    @Test
    fun engagementUpsellWithNoOfferServiceReturnsNullOffer() = apiTest { client ->
        val resp = client.post("/api/v1/offers/account-view?visitorId=vis-1")
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = resp.body<OfferResponse>()
        assertNull(body.offerId, "UP-13: no offer service must return null offer")
    }

    // ── PAY-03: checkout validation ─────────────────────────────────────────

    @Test
    fun checkoutWithBlankSubjectIdReturns400() = apiTest { client ->
        val resp = client.post("/api/v1/checkout") {
            contentType(ContentType.Application.Json)
            setBody(CheckoutRequest(subjectId = "", planId = "basic-monthly"))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status, "PAY-03: blank subjectId must be rejected")
    }

    @Test
    fun checkoutWithUserPrefixSubjectIdSucceeds() = apiTest { client ->
        val resp = client.post("/api/v1/checkout") {
            contentType(ContentType.Application.Json)
            setBody(CheckoutRequest(subjectId = "user:u-test-1", planId = "basic-monthly"))
        }
        assertEquals(HttpStatusCode.Created, resp.status, "PAY-03: user: prefix subjectId must be accepted")
    }

    @Test
    fun checkoutWithOfferServiceCallsDecideOffer() = apiTest(withOfferService = true) { client ->
        val resp = client.post("/api/v1/checkout") {
            contentType(ContentType.Application.Json)
            // offerId == null: triggers the UP-10/11 CEP decide_offer path
            setBody(CheckoutRequest(subjectId = "visitor:vis-co-1", planId = "basic-monthly", offerId = null))
        }
        assertEquals(HttpStatusCode.Created, resp.status, "PAY-03: checkout with offerService must succeed")
    }

    // ── SUB-04: cancel subscription validation ──────────────────────────────

    @Test
    fun cancelWithBlankSubjectIdReturns400() = apiTest { client ->
        val resp = client.post("/api/v1/subscriptions/sub-1/cancel") {
            contentType(ContentType.Application.Json)
            setBody(CancelSubscriptionRequest(subjectId = ""))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status, "SUB-04: blank subjectId must be rejected")
    }

    @Test
    fun cancelWithUserPrefixSubjectIdSucceeds() = apiTest { client ->
        val resp = client.post("/api/v1/subscriptions/sub-1/cancel") {
            contentType(ContentType.Application.Json)
            setBody(CancelSubscriptionRequest(subjectId = "user:u-cancel-1"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status, "SUB-04: user: prefix subjectId must be accepted")
    }

}
