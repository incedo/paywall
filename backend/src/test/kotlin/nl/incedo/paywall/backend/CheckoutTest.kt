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
