package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import nl.incedo.paywall.cep.MockCepClient
import nl.incedo.paywall.cep.Offer
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/** NFR-03: webhook signature verification, and API-07: CEP client interface. */
class WebhookCepTest {

    private val webhookSecret = "test-webhook-secret"
    private val now = 1_750_000_000_000L

    private fun sign(body: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(webhookSecret.toByteArray(), "HmacSHA256"))
        val hmac = mac.doFinal(body.toByteArray())
        val hex = hmac.joinToString("") { "%02x".format(it) }
        return "sha256=$hex"
    }

    private fun apiTest(
        webhookVerifier: WebhookVerifier = WebhookVerifier(null),
        cepClient: nl.incedo.paywall.cep.CepClient? = null,
        block: suspend (io.ktor.client.HttpClient) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store, webhookVerifier = webhookVerifier, cepClient = cepClient) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    // ── NFR-03: webhook signature ─────────────────────────────────────────────

    @Test
    fun entitlementWebhookWithNoSecretAcceptsAll() = apiTest { client ->
        val resp = client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(EntitlementChangeRequest(subjectId = "user:u1", subscriptionRef = "sub-1", active = true))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
    }

    @Test
    fun entitlementWebhookWithSecretRejectsUnsigned() = apiTest(
        webhookVerifier = WebhookVerifier(webhookSecret),
    ) { client ->
        val resp = client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(EntitlementChangeRequest(subjectId = "user:u1", subscriptionRef = "sub-1", active = true))
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun entitlementWebhookWithValidSignatureAccepted() = apiTest(
        webhookVerifier = WebhookVerifier(webhookSecret),
    ) { client ->
        val body = """{"subjectId":"user:u1","subscriptionRef":"sub-1","active":true}"""
        val resp = client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            header(WebhookVerifier.SIGNATURE_HEADER, sign(body))
            setBody(body)
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
    }

    @Test
    fun entitlementWebhookWithWrongSignatureRejected() = apiTest(
        webhookVerifier = WebhookVerifier(webhookSecret),
    ) { client ->
        val body = """{"subjectId":"user:u1","subscriptionRef":"sub-1","active":true}"""
        val resp = client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            header(WebhookVerifier.SIGNATURE_HEADER, "sha256=deadbeef")
            setBody(body)
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun accountDeletionWebhookWithValidSignatureAccepted() = apiTest(
        webhookVerifier = WebhookVerifier(webhookSecret),
    ) { client ->
        val body = """{"userId":"user-to-delete"}"""
        val resp = client.post("/api/v1/integration/account-deletion") {
            contentType(ContentType.Application.Json)
            header(WebhookVerifier.SIGNATURE_HEADER, sign(body))
            setBody(body)
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
    }

    // ── API-07: CEP client interface ──────────────────────────────────────────

    @Test
    fun offerRequestReturnsNullWhenNoCepClient() = apiTest(cepClient = null) { client ->
        val resp = client.post("/api/v1/offers/request") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "visitor-1", articleId = "a", tier = "premium"))
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = resp.body<OfferResponse>()
        assertNull(body.offerId)
        assertNull(body.kind)
    }

    @Test
    fun offerRequestReturnsMockOffer() = apiTest(
        cepClient = MockCepClient(
            fixedOffer = Offer(
                offerId = "offer-123",
                kind = "discount",
                discountPercent = 50,
                validForSeconds = 3600,
                cta = "Get 50% off",
            ),
        ),
    ) { client ->
        val resp = client.post("/api/v1/offers/request") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "visitor-1", articleId = "a", tier = "premium"))
        }
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = resp.body<OfferResponse>()
        assertEquals("offer-123", body.offerId)
        assertEquals("discount", body.kind)
        assertEquals(50, body.discountPercent)
        assertEquals("Get 50% off", body.cta)
    }

    @Test
    fun mockCepClientWithNullOfferReturnsNull() = apiTest(
        cepClient = MockCepClient(fixedOffer = null),
    ) { client ->
        val resp = client.post("/api/v1/offers/request") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = "visitor-1", articleId = "a", tier = "premium"))
        }
        val body = resp.body<OfferResponse>()
        assertNull(body.offerId)
    }
}
