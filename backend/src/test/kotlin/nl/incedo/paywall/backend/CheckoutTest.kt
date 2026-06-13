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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.analytics.wallEventShardTags
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.entitlements.EntitlementGranted
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.metering.MeterPeriod
import nl.incedo.paywall.notifications.MailSent

/**
 * PAY-03/PAY-05: checkout endpoint creates a session via the PaymentProvider interface;
 * mock confirm endpoint completes the purchase without a real payment provider.
 */
class CheckoutTest {

    private val now = 1_750_000_000_000L

    private fun hardVariantVisitor(seed: String): String =
        (0 until 10_000).asSequence()
            .map { "co-$seed-$it" }
            .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == "hard" }

    private val mockProvider = MockPaymentProvider()

    private fun apiTest(block: suspend (io.ktor.client.HttpClient, InMemoryEventStore) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store, paymentProvider = mockProvider) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client, store)
    }

    @Test
    fun checkoutCreatesSessionAndLogsCheckoutStart() = apiTest { client, store ->
        val vis = hardVariantVisitor("session")
        val resp = client.post("/api/v1/checkout") {
            contentType(ContentType.Application.Json)
            setBody(CheckoutRequest(subjectId = "visitor:$vis", planId = "pro"))
        }
        assertEquals(HttpStatusCode.Created, resp.status, "PAY-03: checkout must return 201")
        val body = resp.body<JsonObject>()
        val sessionId = body["sessionId"]?.jsonPrimitive?.contentOrNull
        assertNotNull(sessionId, "PAY-05: response must include sessionId")
        assertTrue(sessionId.startsWith("mock-"), "mock provider must return a mock session ID")

        // AN-02: CHECKOUT_START must be logged for funnel analytics.
        val events = store.query(EventQuery(wallEventShardTags())).events
            .filterIsInstance<WallEventRecorded>()
        assertTrue(events.any { it.eventType == WallEventType.CHECKOUT_START },
            "PAY-03: CHECKOUT_START event must be logged")
    }

    @Test
    fun confirmGrantsEntitlementAndLogsMail() = apiTest { client, store ->
        val vis = hardVariantVisitor("confirm")
        val checkoutBody = client.post("/api/v1/checkout") {
            contentType(ContentType.Application.Json)
            setBody(CheckoutRequest(subjectId = "visitor:$vis", planId = "basic"))
        }.body<JsonObject>()
        val sessionId = checkoutBody["sessionId"]!!.jsonPrimitive.content

        val confirmResp = client.post("/api/v1/checkout/$sessionId/confirm") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Accepted, confirmResp.status, "PAY-05: confirm must return 202")

        // Entitlement granted.
        val grants = store.query(EventQuery(setOf("subject:visitor:$vis"))).events
            .filterIsInstance<EntitlementGranted>()
        assertEquals(1, grants.size, "PAY-03: confirm must record EntitlementGranted")

        // Purchase confirmation mail logged (US-10).
        val mails = store.query(EventQuery(setOf("mail_event"))).events.filterIsInstance<MailSent>()
            .filter { it.kind == "purchase_confirmation" }
        assertEquals(1, mails.size, "US-10: confirm must log purchase_confirmation mail")

        // CHECKOUT_COMPLETE event logged.
        val wallEvents = store.query(EventQuery(wallEventShardTags())).events
            .filterIsInstance<WallEventRecorded>()
        assertTrue(wallEvents.any { it.eventType == WallEventType.CHECKOUT_COMPLETE },
            "PAY-03: confirm must log CHECKOUT_COMPLETE event")
    }

    @Test
    fun confirmIdempotentOnRepeatCall() = apiTest { client, store ->
        val vis = hardVariantVisitor("idem")
        val sessionId = client.post("/api/v1/checkout") {
            contentType(ContentType.Application.Json)
            setBody(CheckoutRequest(subjectId = "visitor:$vis", planId = "pro"))
        }.body<JsonObject>()["sessionId"]!!.jsonPrimitive.content

        client.post("/api/v1/checkout/$sessionId/confirm") { contentType(ContentType.Application.Json) }
        val second = client.post("/api/v1/checkout/$sessionId/confirm") { contentType(ContentType.Application.Json) }
        assertEquals(HttpStatusCode.NotFound, second.status, "confirm must return 404 on repeat (session consumed)")
    }

    @Test
    fun returnUrlRoundTripsFromCheckoutToConfirm() = apiTest { client, _ ->
        // AC-12: returnUrl supplied at checkout must survive through to the confirm response
        // so the frontend can navigate the subscriber back to the gated article after payment.
        val vis = hardVariantVisitor("returnurl")
        val articleUrl = "https://example.com/articles/premium-story-42"
        val checkoutBody = client.post("/api/v1/checkout") {
            contentType(ContentType.Application.Json)
            setBody(CheckoutRequest(subjectId = "visitor:$vis", planId = "basic-monthly", returnUrl = articleUrl))
        }.body<JsonObject>()
        assertEquals(articleUrl, checkoutBody["returnUrl"]?.jsonPrimitive?.contentOrNull,
            "AC-12: checkout response must echo back returnUrl")

        val sessionId = checkoutBody["sessionId"]!!.jsonPrimitive.content
        val confirmBody = client.post("/api/v1/checkout/$sessionId/confirm") {
            contentType(ContentType.Application.Json)
        }.body<JsonObject>()
        assertEquals(articleUrl, confirmBody["returnUrl"]?.jsonPrimitive?.contentOrNull,
            "AC-12: confirm response must include returnUrl so frontend can redirect to gated article")
    }

    @Test
    fun checkoutWithoutReturnUrlOmitsReturnUrlFromResponse() = apiTest { client, _ ->
        // AC-12: when no returnUrl is provided, responses must not include the field at all.
        val vis = hardVariantVisitor("noreturnurl")
        val checkoutBody = client.post("/api/v1/checkout") {
            contentType(ContentType.Application.Json)
            setBody(CheckoutRequest(subjectId = "visitor:$vis", planId = "basic-monthly"))
        }.body<JsonObject>()
        assertEquals(null, checkoutBody["returnUrl"]?.jsonPrimitive?.contentOrNull,
            "AC-12: checkout response must omit returnUrl when none was supplied")
    }

    // ── PAY-06: iDEAL + credit card payment method selection ─────────────────────────

    @Test
    fun availablePaymentMethodsIncludedInCheckoutResponse() = apiTest { client, _ ->
        val vis = hardVariantVisitor("methods")
        val body = client.post("/api/v1/checkout") {
            contentType(ContentType.Application.Json)
            setBody(CheckoutRequest(subjectId = "visitor:$vis", planId = "pro"))
        }.body<JsonObject>()
        val methods = body["availablePaymentMethods"]
        assertNotNull(methods, "PAY-06: checkout response must include availablePaymentMethods")
        val methodList = (methods as JsonArray).map {
            it.jsonPrimitive.content
        }
        assertTrue("ideal" in methodList, "PAY-06: iDEAL must be in available methods (Dutch market)")
        assertTrue("credit_card" in methodList, "PAY-06: credit_card must be in available methods")
    }

    @Test
    fun selectedPaymentMethodRoundTripsToConfirm() = apiTest { client, _ ->
        val vis = hardVariantVisitor("ideal")
        val checkoutBody = client.post("/api/v1/checkout") {
            contentType(ContentType.Application.Json)
            setBody(CheckoutRequest(subjectId = "visitor:$vis", planId = "basic", paymentMethod = "ideal"))
        }.body<JsonObject>()
        assertEquals("ideal", checkoutBody["paymentMethod"]?.jsonPrimitive?.contentOrNull,
            "PAY-06: selected paymentMethod must be echoed in checkout response")
        val sessionId = checkoutBody["sessionId"]!!.jsonPrimitive.content
        val confirmBody = client.post("/api/v1/checkout/$sessionId/confirm") {
            contentType(ContentType.Application.Json)
        }.body<JsonObject>()
        assertEquals("ideal", confirmBody["paymentMethod"]?.jsonPrimitive?.contentOrNull,
            "PAY-06: paymentMethod must survive to confirm response")
    }

    // ── PAY-07: failed payment retry path ────────────────────────────────────────────

    @Test
    fun simulatedFailureReturns402WithRetryInfo() = apiTest { client, store ->
        val vis = hardVariantVisitor("fail")
        val sessionId = client.post("/api/v1/checkout") {
            contentType(ContentType.Application.Json)
            setBody(CheckoutRequest(subjectId = "visitor:$vis", planId = "pro"))
        }.body<JsonObject>()["sessionId"]!!.jsonPrimitive.content

        val failResp = client.post("/api/v1/checkout/$sessionId/confirm?simulate_failure=true") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.PaymentRequired, failResp.status,
            "PAY-07: simulated failure must return 402 Payment Required")
        val body = failResp.body<JsonObject>()
        assertEquals("payment_declined", body["error"]?.jsonPrimitive?.contentOrNull,
            "PAY-07: error field must be payment_declined")
        assertEquals(true, body["retryAllowed"]?.jsonPrimitive?.content?.toBoolean(),
            "PAY-07: retryAllowed must be true so client knows it can retry")
        assertNotNull(body["retryUrl"], "PAY-07: retryUrl must be present for the retry path")
    }

    @Test
    fun sessionSurvivesFailureAndCanBeRetried() = apiTest { client, store ->
        val vis = hardVariantVisitor("retry")
        val sessionId = client.post("/api/v1/checkout") {
            contentType(ContentType.Application.Json)
            setBody(CheckoutRequest(subjectId = "visitor:$vis", planId = "basic"))
        }.body<JsonObject>()["sessionId"]!!.jsonPrimitive.content

        // Simulate failure — session must still be alive.
        client.post("/api/v1/checkout/$sessionId/confirm?simulate_failure=true") {
            contentType(ContentType.Application.Json)
        }
        // Retry with the same sessionId — must succeed (PAY-07: no data re-entry).
        val retryResp = client.post("/api/v1/checkout/$sessionId/confirm") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Accepted, retryResp.status,
            "PAY-07: retry after simulated failure must complete the purchase (same sessionId, no data loss)")
        // Entitlement must be granted on the successful retry.
        val grants = store.query(EventQuery(setOf("subject:visitor:$vis"))).events
            .filterIsInstance<EntitlementGranted>()
        assertEquals(1, grants.size, "PAY-07: exactly one EntitlementGranted after successful retry")
    }

    @Test
    fun checkoutCompletedSubscriberGetsFullAccess() = apiTest { client, store ->
        val vis = hardVariantVisitor("access")
        val sessionId = client.post("/api/v1/checkout") {
            contentType(ContentType.Application.Json)
            setBody(CheckoutRequest(subjectId = "visitor:$vis", planId = "pro"))
        }.body<JsonObject>()["sessionId"]!!.jsonPrimitive.content
        client.post("/api/v1/checkout/$sessionId/confirm") { contentType(ContentType.Application.Json) }

        val decide = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = vis, articleId = "article-premium", tier = "premium"))
        }.body<DecideResponse>()
        assertEquals("full", decide.access, "PAY-03: subscriber after checkout must get full access (AC-01)")
    }
}
