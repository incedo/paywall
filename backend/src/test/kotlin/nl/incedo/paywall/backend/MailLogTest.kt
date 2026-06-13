package nl.incedo.paywall.backend

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
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.metering.MeterPeriod
import nl.incedo.paywall.notifications.MAIL_EVENT_TAG
import nl.incedo.paywall.notifications.MailSent

/**
 * US-10: transactional email log — in the experiment phase emails are not
 * dispatched to a real provider; instead [MailSent] events are appended to
 * the event store at the relevant trigger points, making them auditable and
 * queryable per subject via the AN-04 export path.
 *
 * Trigger → kind mapping:
 *   checkout_complete → purchase_confirmation
 *   past_due status   → payment_failure          (SUB-05 grace path)
 *   canceled status   → cancellation_confirmation
 */
class MailLogTest {

    private val now = 1_750_000_000_000L

    private fun apiTest(block: suspend (io.ktor.client.HttpClient, InMemoryEventStore) -> Unit) =
        testApplication {
            val store = InMemoryEventStore()
            val service = AccessService(
                eventStore = store,
                experiment = defaultExperiment,
                clock = { now },
                currentPeriod = { MeterPeriod("2026-06") },
            )
            application { module(service, store) }
            val client = createClient { install(ContentNegotiation) { json() } }
            block(client, store)
        }

    @Test
    fun checkoutCompleteLogsMailSentWithPurchaseConfirmation() = apiTest { client, store ->
        val resp = client.post("/api/v1/events") {
            contentType(ContentType.Application.Json)
            setBody(ClientEventRequest(
                visitorId = "vis-us10-checkout",
                type = "checkout_complete",
                channel = "web",
                articleId = "art-premium-1",
            ))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status, "US-10: checkout_complete must be accepted")
        val mail = store.query(EventQuery(setOf(MAIL_EVENT_TAG))).events.filterIsInstance<MailSent>()
        assertEquals(1, mail.size, "US-10: one MailSent must be appended on checkout_complete")
        assertEquals("purchase_confirmation", mail[0].kind, "US-10: kind must be purchase_confirmation")
        assertEquals("visitor:vis-us10-checkout", mail[0].subjectId.value,
            "US-10: MailSent must be tagged to the visitor subject")
    }

    @Test
    fun pastDueSubscriptionLogsPaymentFailureMail() = apiTest { client, store ->
        val resp = client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(EntitlementChangeRequest(
                subjectId = "visitor:vis-us10-pastdue",
                subscriptionRef = "sub-pastdue-1",
                planId = "basic-monthly",
                status = "past_due",
            ))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status, "US-10: past_due must be accepted")
        val mail = store.query(EventQuery(setOf(MAIL_EVENT_TAG))).events.filterIsInstance<MailSent>()
        assertEquals(1, mail.size, "US-10: one MailSent must be appended for past_due")
        assertEquals("payment_failure", mail[0].kind, "US-10: kind must be payment_failure")
        assertEquals("basic-monthly", mail[0].planId, "US-10: planId must be included for retry context")
        assertNotNull(mail[0].subscriptionRef, "US-10: subscriptionRef must be present for dunning linkage")
    }

    @Test
    fun canceledSubscriptionLogsCancellationConfirmationMail() = apiTest { client, store ->
        val resp = client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(EntitlementChangeRequest(
                subjectId = "visitor:vis-us10-cancel",
                subscriptionRef = "sub-cancel-1",
                planId = "complete-monthly",
                status = "canceled",
                validUntilEpochMs = now + 30 * 24 * 3600 * 1000L,
            ))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status, "US-10: canceled must be accepted")
        val mail = store.query(EventQuery(setOf(MAIL_EVENT_TAG))).events.filterIsInstance<MailSent>()
        assertEquals(1, mail.size, "US-10: one MailSent must be appended for canceled")
        assertEquals("cancellation_confirmation", mail[0].kind, "US-10: kind must be cancellation_confirmation")
    }

    @Test
    fun otherClientEventsDoNotLogMailSent() = apiTest { client, store ->
        // page_view, wall_dismissed etc. must NOT produce MailSent events
        client.post("/api/v1/events") {
            contentType(ContentType.Application.Json)
            setBody(ClientEventRequest(visitorId = "vis-us10-nomail", type = "page_view", channel = "web"))
        }
        client.post("/api/v1/events") {
            contentType(ContentType.Application.Json)
            setBody(ClientEventRequest(visitorId = "vis-us10-nomail", type = "wall_dismissed", channel = "web"))
        }
        val mail = store.query(EventQuery(setOf(MAIL_EVENT_TAG))).events.filterIsInstance<MailSent>()
        assertEquals(0, mail.size, "US-10: non-checkout events must not produce MailSent")
    }
}
