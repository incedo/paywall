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
import kotlin.test.assertTrue
import nl.incedo.paywall.cep.MockCepClient
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * UP-08: CEP-initiated offer push for asynchronous channels (email, chat).
 * The CEP pushes offers via webhook; the paywall validates, frequency-caps,
 * and stores them as pending offers the async delivery system can fetch.
 */
class CepAsyncOfferTest {

    private val now = 1_750_000_000_000L

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        val offerService = OfferService(
            eventStore = store,
            cepClient = MockCepClient(),
            clock = { now },
        )
        application { module(service, store, offerService = offerService) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    // Use visitor: prefix so the accept endpoint can match the same subjectId via ?visitorId=
    private val emailOffer = CepOfferPushRequest(
        subjectId = "visitor:up08-vis",
        channel = "email",
        offerId = "offer-up08-annual",
        kind = "upsell",
        fromPlanId = "basic-monthly",
        toPlanId = "basic-annual",
        source = "campaign-up08",
    )

    @Test
    fun validPushIsAcceptedAndStored() = apiTest { client ->
        val resp = client.post("/api/v1/integration/cep-offers") {
            contentType(ContentType.Application.Json)
            setBody(emailOffer)
        }
        assertEquals(HttpStatusCode.Accepted, resp.status, "UP-08: valid offer push must return 202")
    }

    @Test
    fun pushedOfferAppearsInPendingQuery() = apiTest { client ->
        client.post("/api/v1/integration/cep-offers") {
            contentType(ContentType.Application.Json)
            setBody(emailOffer)
        }
        val pending = client.get("/api/v1/subjects/${emailOffer.subjectId}/offers/pending")
            .body<List<PendingOfferResponse>>()
        assertEquals(1, pending.size, "UP-08: stored offer must appear in pending list")
        assertEquals(emailOffer.offerId, pending[0].offerId)
        assertEquals("email", pending[0].channel)
    }

    @Test
    fun channelFilterNarrowsPendingList() = apiTest { client ->
        // push email offer
        client.post("/api/v1/integration/cep-offers") {
            contentType(ContentType.Application.Json)
            setBody(emailOffer)
        }
        // push chat offer with same subject
        client.post("/api/v1/integration/cep-offers") {
            contentType(ContentType.Application.Json)
            setBody(emailOffer.copy(offerId = "offer-up08-chat", channel = "chat"))
        }
        // query for email only
        val emailPending = client.get(
            "/api/v1/subjects/${emailOffer.subjectId}/offers/pending?channel=email",
        ).body<List<PendingOfferResponse>>()
        assertEquals(1, emailPending.size, "UP-08: channel filter must return only email offers")
        assertEquals("email", emailPending[0].channel)
    }

    @Test
    fun acceptedOfferRemovedFromPending() = apiTest { client ->
        client.post("/api/v1/integration/cep-offers") {
            contentType(ContentType.Application.Json)
            setBody(emailOffer)
        }
        // accept the offer using the same visitorId (resolves to "visitor:up08-vis")
        client.post(
            "/api/v1/offers/accept?visitorId=up08-vis&offerId=${emailOffer.offerId}&kind=upsell&channel=email",
        )
        val pending = client.get("/api/v1/subjects/visitor:up08-vis/offers/pending")
            .body<List<PendingOfferResponse>>()
        assertTrue(pending.isEmpty(), "UP-08: accepted offer must not appear in pending list")
    }

    @Test
    fun guardrailRejectedOfferReturnsUnprocessable() = apiTest { client ->
        val badOffer = emailOffer.copy(
            offerId = "offer-guardrail-bad",
            kind = "upsell",
            fromPlanId = "complete-monthly",
            toPlanId = "basic-monthly", // rank_incoherent_upsell
        )
        val resp = client.post("/api/v1/integration/cep-offers") {
            contentType(ContentType.Application.Json)
            setBody(badOffer)
        }
        assertEquals(
            HttpStatusCode.UnprocessableEntity,
            resp.status,
            "UP-08: guardrail-rejected offer must return 422",
        )
    }
}
