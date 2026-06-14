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
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.metering.MeterPeriod
import nl.incedo.paywall.offers.OfferSuppressed

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

    @Test
    fun cepReturningNullLogsNoneMatchedReason() = run {
        // UP-04: "none_matched" when CEP returns null (no offer applicable).
        // Must be distinct from "cep_timeout" so ops can tell failures from silence.
        testApplication {
            val store = InMemoryEventStore()
            val service = AccessService(
                eventStore = store, experiment = defaultExperiment,
                clock = { now }, currentPeriod = { MeterPeriod("2026-06") },
            )
            val offerService = OfferService(
                eventStore = store,
                cepClient = MockCepClient(fixedOffer = null),
                clock = { now },
            )
            application { module(service, store, offerService = offerService) }
            val client = createClient { install(ContentNegotiation) { json() } }
            client.post("/api/v1/offers/request") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = "visitor-nm", articleId = "a", tier = "premium"))
            }
            val suppressed = store.query(EventQuery(setOf("subject:visitor:visitor-nm")))
                .events.filterIsInstance<OfferSuppressed>()
            assertEquals(1, suppressed.size)
            assertEquals("none_matched", suppressed.single().reason, "UP-04: CEP null must log none_matched")
        }
    }

    @Test
    fun staticFallbackOfferReturnedWhenCepUnavailable() = run {
        // UP-07: when CEP is absent, decide_offer must return the static fallback
        // offer configured for the trigger, rather than suppressing silently.
        val fallback = Offer(offerId = "fallback-1", kind = "upsell", toPlanId = "complete-monthly", source = "static")
        testApplication {
            val store = nl.incedo.paywall.core.adapter.InMemoryEventStore()
            val service = AccessService(
                eventStore = store, experiment = defaultExperiment,
                clock = { now }, currentPeriod = { nl.incedo.paywall.metering.MeterPeriod("2026-06") },
            )
            val offerService = OfferService(
                eventStore = store,
                cepClient = null,
                clock = { now },
                fallbackOffers = mapOf("gate_shown" to fallback),
            )
            application { module(service, store, offerService = offerService) }
            val client = createClient { install(ContentNegotiation) { json() } }
            val resp = client.post("/api/v1/offers/request?trigger=gate_shown") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = "visitor-1", articleId = "a", tier = "premium"))
            }
            assertEquals(HttpStatusCode.OK, resp.status)
            val body = resp.body<OfferResponse>()
            assertEquals("fallback-1", body.offerId, "UP-07: static fallback offer must be returned when CEP is absent")
            assertEquals("upsell", body.kind)
        }
    }

    // ── API-08: CEP advice endpoint authentication ────────────────────────────

    @Test
    fun cepAdviceWithNoSecretAcceptsAll() = apiTest { client ->
        val resp = client.post("/api/v1/integration/cep-advice") {
            contentType(ContentType.Application.Json)
            setBody("""{"subjectId":"user:u1","gate":true}""")
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
    }

    @Test
    fun cepAdviceWithSecretRejectsUnsigned() = apiTest(
        webhookVerifier = WebhookVerifier(webhookSecret),
    ) { client ->
        val resp = client.post("/api/v1/integration/cep-advice") {
            contentType(ContentType.Application.Json)
            setBody("""{"subjectId":"user:u1","gate":true}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status, "API-08: unsigned CEP advice must be rejected")
    }

    @Test
    fun cepAdviceWithValidSignatureAccepted() = apiTest(
        webhookVerifier = WebhookVerifier(webhookSecret),
    ) { client ->
        val body = """{"subjectId":"user:u1","gate":true}"""
        val resp = client.post("/api/v1/integration/cep-advice") {
            contentType(ContentType.Application.Json)
            header(WebhookVerifier.SIGNATURE_HEADER, sign(body))
            setBody(body)
        }
        assertEquals(HttpStatusCode.Accepted, resp.status, "API-08: signed CEP advice must be accepted")
    }

    // ── UP-06: variant passed to CEP ─────────────────────────────────────────

    @Test
    fun offerRequestPassesVariantToCep() = run {
        // UP-06: verify the variant string is forwarded to the CEP; test uses a
        // capturing stub that records the last variant it received.
        var capturedVariant: String? = null
        val capturingCep = object : nl.incedo.paywall.cep.CepClient {
            override suspend fun requestOffer(
                subject: nl.incedo.paywall.access.Subject,
                trigger: String,
                currentPlanId: String?,
                variant: String?,
            ): nl.incedo.paywall.cep.Offer? {
                capturedVariant = variant
                return null
            }
        }
        testApplication {
            val store = nl.incedo.paywall.core.adapter.InMemoryEventStore()
            val service = AccessService(
                eventStore = store, experiment = defaultExperiment,
                clock = { now }, currentPeriod = { nl.incedo.paywall.metering.MeterPeriod("2026-06") },
            )
            val offerService = OfferService(eventStore = store, cepClient = capturingCep, clock = { now })
            application { module(service, store, cepClient = capturingCep, offerService = offerService) }
            val client = createClient { install(ContentNegotiation) { json() } }
            client.post("/api/v1/offers/request?trigger=gate_shown") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = "vis-up06", articleId = "a-1", tier = "premium"))
            }
        }
        val v = capturedVariant
        assert(!v.isNullOrEmpty() && v != "unknown") { "UP-06: variant must not be null/unknown but was '$v'" }
    }

    // ── NFR-03: signature validation for remaining webhook endpoints ──────────

    @Test
    fun cepAdviceWithBlankSubjectIdReturns400() = apiTest { client ->
        val resp = client.post("/api/v1/integration/cep-advice") {
            contentType(ContentType.Application.Json)
            setBody("""{"subjectId":"","gate":true}""")
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status, "API-08: blank subjectId must be rejected")
    }

    @Test
    fun cepOfferPushWithInvalidSignatureReturns401() = apiTest(
        webhookVerifier = WebhookVerifier(webhookSecret),
    ) { client ->
        val resp = client.post("/api/v1/integration/cep-offers") {
            contentType(ContentType.Application.Json)
            header(WebhookVerifier.SIGNATURE_HEADER, "sha256=badhash")
            setBody("""{"subjectId":"visitor:v1","channel":"email","offerId":"o1","kind":"upsell"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status, "API-08: invalid signature must be rejected")
    }

    @Test
    fun cepOfferPushWithBlankFieldsReturns400() = apiTest { client ->
        val resp = client.post("/api/v1/integration/cep-offers") {
            contentType(ContentType.Application.Json)
            setBody("""{"subjectId":"","channel":"email","offerId":"","kind":"upsell"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status, "API-08: blank subjectId/offerId must be rejected")
    }

    @Test
    fun cepOfferPushWithNoOfferServiceReturns503() = apiTest(cepClient = null) { client ->
        val resp = client.post("/api/v1/integration/cep-offers") {
            contentType(ContentType.Application.Json)
            setBody("""{"subjectId":"visitor:v1","channel":"email","offerId":"o1","kind":"upsell"}""")
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, resp.status, "UP-08: no offer service must return 503")
    }

    @Test
    fun accountDeletionWithInvalidSignatureReturns401() = apiTest(
        webhookVerifier = WebhookVerifier(webhookSecret),
    ) { client ->
        val resp = client.post("/api/v1/integration/account-deletion") {
            contentType(ContentType.Application.Json)
            header(WebhookVerifier.SIGNATURE_HEADER, "sha256=badhash")
            setBody("""{"userId":"user-1"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status, "NFR-03: invalid account-deletion sig must be rejected")
    }

    @Test
    fun adCompletionWithInvalidSignatureReturns401() = apiTest(
        webhookVerifier = WebhookVerifier(webhookSecret),
    ) { client ->
        val resp = client.post("/api/v1/integration/ad-completion") {
            contentType(ContentType.Application.Json)
            header(WebhookVerifier.SIGNATURE_HEADER, "sha256=badhash")
            setBody("""{"subjectId":"visitor:v1","articleId":"a1","adPlayId":"play-1"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status, "AG-01: invalid ad-completion sig must be rejected")
    }

    @Test
    fun entitlementWebhookWithBlankSubjectIdReturns400() = apiTest { client ->
        val body = """{"subjectId":"","subscriptionRef":"sub-1","active":true}"""
        val resp = client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status, "NFR-03: blank subjectId must be rejected")
    }
}
