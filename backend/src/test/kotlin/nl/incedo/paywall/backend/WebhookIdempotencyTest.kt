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
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.entitlements.EntitlementGranted
import nl.incedo.paywall.metering.MeterPeriod
import nl.incedo.paywall.notifications.MailSent

/** SUB-02 / NFR-03: entitlement webhook endpoint is idempotent on webhookEventId. */
class WebhookIdempotencyTest {

    private val now = 1_750_000_000_000L

    private fun apiTest(block: suspend (io.ktor.client.HttpClient, InMemoryEventStore) -> Unit) = testApplication {
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
    fun duplicateWebhookSkippedWhenEventIdMatches() = apiTest { client, store ->
        val body = EntitlementChangeRequest(
            subjectId = "visitor:sub02-v",
            subscriptionRef = "sub-ref-01",
            planId = "pro",
            status = "past_due",
            webhookEventId = "evt_stripe_abc123",
        )
        val first = client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Accepted, first.status)

        // second delivery of the same event (payment provider retry)
        val second = client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Accepted, second.status, "duplicate webhook must still return 202 (SUB-02)")

        // exactly one MailSent — no duplicate email log entry (NFR-03)
        val mails = store.query(EventQuery(setOf("mail_event"))).events.filterIsInstance<MailSent>()
        assertEquals(1, mails.size, "duplicate webhook must not produce a second MailSent event (NFR-03)")
    }

    @Test
    fun webhooksWithoutEventIdAreNotDeduped() = apiTest { client, store ->
        // Without a webhookEventId field, backward-compat: no dedup, both appended.
        repeat(2) {
            client.post("/api/v1/integration/entitlements") {
                contentType(ContentType.Application.Json)
                setBody(EntitlementChangeRequest(
                    subjectId = "visitor:sub02-noid",
                    subscriptionRef = "sub-ref-02",
                    planId = "pro",
                    status = "canceled",
                    validUntilEpochMs = now + 86_400_000,
                ))
            }
        }
        val grants = store.query(EventQuery(setOf("subject:visitor:sub02-noid"))).events
            .filterIsInstance<EntitlementGranted>()
        assertEquals(2, grants.size, "without webhookEventId, no dedup — backward-compat preserved")
    }
}
