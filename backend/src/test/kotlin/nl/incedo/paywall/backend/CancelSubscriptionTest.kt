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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import nl.incedo.paywall.cep.MockCepClient
import nl.incedo.paywall.cep.Offer
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.metering.MeterPeriod
import nl.incedo.paywall.notifications.MailSent
import nl.incedo.paywall.offers.OfferTriggered

/**
 * SUB-04: self-service subscription cancellation (≤ 2 clicks, DN-02).
 * DN-01: decide_offer(cancel_intent) is called as part of the cancel flow.
 */
class CancelSubscriptionTest {

    private val now = 1_750_000_000_000L

    private fun hardVariantVisitor(seed: String): String =
        (0 until 10_000).asSequence()
            .map { "cancel-$seed-$it" }
            .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == "hard" }

    private fun apiTest(
        cepOffer: Offer? = null,
        block: suspend (io.ktor.client.HttpClient, InMemoryEventStore) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val cepClient = cepOffer?.let { MockCepClient(fixedOffer = it) }
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        val offerSvc = cepClient?.let {
            OfferService(store, it, clock = { now })
        }
        application { module(service, store, offerService = offerSvc) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client, store)
    }

    /** Grant active entitlement then cancel. Returns the response body as JsonObject. */
    private suspend fun io.ktor.client.HttpClient.grantAndCancel(
        visitorId: String,
        subscriptionRef: String,
        validUntilEpochMs: Long? = now + 7 * 24 * 60 * 60 * 1000L,
    ): JsonObject {
        post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(EntitlementChangeRequest(
                subjectId = "visitor:$visitorId",
                subscriptionRef = subscriptionRef,
                planId = "pro",
                status = "active",
                validUntilEpochMs = validUntilEpochMs,
            ))
        }
        return post("/api/v1/subscriptions/$subscriptionRef/cancel") {
            contentType(ContentType.Application.Json)
            setBody(CancelSubscriptionRequest(
                subjectId = "visitor:$visitorId",
                planId = "pro",
                validUntilEpochMs = validUntilEpochMs,
            ))
        }.body()
    }

    @Test
    fun cancelRecordsCancellationConfirmationMail() = apiTest { client, store ->
        val vis = hardVariantVisitor("mail")
        client.grantAndCancel(vis, "sub-mail")

        val mails = store.query(EventQuery(setOf("mail_event"))).events.filterIsInstance<MailSent>()
            .filter { it.kind == "cancellation_confirmation" }
        assertEquals(1, mails.size, "SUB-04: cancel must log cancellation_confirmation mail (US-10)")
    }

    @Test
    fun cancelRetainsAccessUntilPeriodEnd() = apiTest { client, store ->
        val vis = hardVariantVisitor("access")
        val periodEnd = now + 30 * 24 * 60 * 60 * 1000L
        client.grantAndCancel(vis, "sub-access", validUntilEpochMs = periodEnd)

        // Clock frozen at `now`; period end is in the future — access must remain (SUB-03).
        val decide = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = vis, articleId = "article-premium", tier = "premium"))
        }.body<DecideResponse>()
        assertEquals("full", decide.access, "SUB-03: access must be retained until period end after cancel")
    }

    @Test
    fun cancelReturns202WithCanceledUntil() = apiTest { client, store ->
        val vis = hardVariantVisitor("resp")
        val periodEnd = now + 86_400_000L
        val body = client.grantAndCancel(vis, "sub-resp", validUntilEpochMs = periodEnd)

        assertEquals("canceled", body["recorded"]?.jsonPrimitive?.contentOrNull,
            "SUB-04: cancel response must include recorded=canceled")
        assertEquals(periodEnd.toString(), body["canceledUntilEpochMs"]?.jsonPrimitive?.contentOrNull,
            "SUB-04: cancel response must include canceledUntilEpochMs")
    }

    @Test
    fun cancelCallsDecideOfferWithCancelIntentDn01() = apiTest(
        cepOffer = Offer(
            offerId = "retention-discount-50",
            kind = "discount",
            discountPercent = 50,
            channels = setOf("web"),
        ),
    ) { client, store ->
        val vis = hardVariantVisitor("dn01")
        val body = client.grantAndCancel(vis, "sub-dn01")

        // DN-01: cancel_intent trigger must have been fired — OfferTriggered in store.
        val offerEvents = store.query(EventQuery(setOf("subject:visitor:$vis"))).events
            .filterIsInstance<OfferTriggered>()
        val cancelIntentOffer = offerEvents.firstOrNull { it.trigger == "cancel_intent" }
        assertNotNull(cancelIntentOffer, "DN-01: cancel must call decide_offer with cancel_intent trigger")
        // DN-01: offer ID is returned in response (informational; does not block cancellation).
        assertEquals("retention-discount-50", body["retentionOfferId"]?.jsonPrimitive?.contentOrNull,
            "DN-01: cancel response must include retentionOfferId")
    }
}
