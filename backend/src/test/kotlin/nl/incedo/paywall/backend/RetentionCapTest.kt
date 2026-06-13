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
import kotlin.test.Test
import kotlin.test.assertEquals
import nl.incedo.paywall.access.Subject
import nl.incedo.paywall.cep.CepClient
import nl.incedo.paywall.cep.Offer
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod
import nl.incedo.paywall.offers.OfferAccepted

/**
 * DN-05: at most one accepted retention (downsell/discount/pause) offer per rolling 12 months.
 * After an acceptance is recorded, the next decide_offer call must suppress with "retention_cap".
 */
class RetentionCapTest {

    private val now = System.currentTimeMillis()
    private val subjectId = SubjectId("visitor:vis-dn05")
    private val visitor = Subject(VisitorId("vis-dn05"), userId = null)

    private fun stubCep(kind: String = "downsell") = object : CepClient {
        override suspend fun requestOffer(subject: Subject, trigger: String, currentPlanId: String?, variant: String?) = Offer(
            offerId = "offer-retention",
            kind = kind,
            source = "cep",
            trigger = trigger,
            channels = setOf("web"),
        )
    }

    @Test
    fun retentionOfferSuppressedAfterAcceptance() {
        val store = InMemoryEventStore()
        // Pre-load: subject has already accepted a downsell offer 6 months ago (within 12-month window)
        kotlinx.coroutines.runBlocking {
            store.append(listOf(
                OfferAccepted(
                    subjectId = subjectId,
                    offerId = "offer-retention-prev",
                    kind = "downsell",
                    channel = "web",
                    acceptedAtEpochMs = now - 180L * 24 * 3600 * 1000, // 6 months ago
                )
            ), condition = null)
        }

        val offerService = OfferService(
            eventStore = store,
            cepClient = stubCep(kind = "downsell"),
            clock = { now },
        )

        val decision = kotlinx.coroutines.runBlocking {
            offerService.decideOffer(visitor, OfferService.TriggerContext("cancel_intent", "web"))
        }
        assertEquals(
            OfferService.OfferDecision.Suppressed("retention_cap"),
            decision,
            "downsell after recent acceptance must be suppressed by DN-05 retention cap"
        )
    }

    @Test
    fun retentionOfferAllowedAfterCapWindowExpires() {
        val store = InMemoryEventStore()
        // Pre-load: acceptance 13 months ago (beyond the 12-month window)
        kotlinx.coroutines.runBlocking {
            store.append(listOf(
                OfferAccepted(
                    subjectId = subjectId,
                    offerId = "offer-old",
                    kind = "downsell",
                    channel = "web",
                    acceptedAtEpochMs = now - 395L * 24 * 3600 * 1000, // 13 months ago
                )
            ), condition = null)
        }

        val offerService = OfferService(
            eventStore = store,
            cepClient = stubCep(kind = "downsell"),
            clock = { now },
        )

        val decision = kotlinx.coroutines.runBlocking {
            offerService.decideOffer(visitor, OfferService.TriggerContext("cancel_intent", "web"))
        }
        assertEquals(
            OfferService.OfferDecision.Triggered::class,
            decision::class,
            "downsell beyond 12-month window must be allowed (DN-05 cap expired)"
        )
    }

    @Test
    fun nonRetentionOfferNotAffectedByCap() {
        val store = InMemoryEventStore()
        // Pre-load: recent retention acceptance
        kotlinx.coroutines.runBlocking {
            store.append(listOf(
                OfferAccepted(
                    subjectId = subjectId,
                    offerId = "offer-downsell",
                    kind = "downsell",
                    channel = "web",
                    acceptedAtEpochMs = now - 30L * 24 * 3600 * 1000, // 1 month ago
                )
            ), condition = null)
        }

        // access_grant is NOT a retention kind — must not be capped by DN-05.
        // (upsell/downsell require coherent from/to plans; access_grant has no rank guardrail.)
        val offerService = OfferService(
            eventStore = store,
            cepClient = stubCep(kind = "access_grant"),
            clock = { now },
        )

        val decision = kotlinx.coroutines.runBlocking {
            offerService.decideOffer(visitor, OfferService.TriggerContext("gate_shown", "web"))
        }
        assertEquals(
            OfferService.OfferDecision.Triggered::class,
            decision::class,
            "access_grant must not be affected by the retention cap (DN-05 only covers downsell/discount/pause)"
        )
    }

    @Test
    fun acceptEndpointRecordsOfferAcceptedEvent() = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }

        val resp = client.post("/api/v1/offers/accept?visitorId=vis-dn05&offerId=offer-1&kind=downsell&channel=web") {
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val body = resp.body<Map<String, String>>()
        assertEquals("OfferAccepted", body["recorded"])

        val events = store.query(nl.incedo.paywall.core.port.EventQuery(setOf("subject:visitor:vis-dn05"))).events
        val accepted = events.filterIsInstance<OfferAccepted>()
        assertEquals(1, accepted.size, "OfferAccepted event must be stored")
        assertEquals("downsell", accepted.first().kind)
        assertEquals("offer-1", accepted.first().offerId)
    }
}
