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
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.entitlements.EntitlementGranted
import nl.incedo.paywall.metering.MeterPeriod

/**
 * BDD scenarios for `src/test/resources/features/entitlements.feature` —
 * mirrors each Gherkin scenario 1:1 (AC-01..08, SUB-01..07).
 *
 * Visitor-scoped entitlements (no JWT) keep tests simple: the webhook uses
 * subjectId="visitor:{id}" and decide uses visitorId="{id}" without a bearer token.
 */
class EntitlementsFeatureTest {

    private val now = 1_750_000_000_000L

    private fun scenario(block: suspend (io.ktor.client.HttpClient, InMemoryEventStore) -> Unit) =
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

    private suspend fun webhook(
        client: io.ktor.client.HttpClient,
        subjectId: String,
        ref: String,
        planId: String = "basic-monthly",
        status: String? = null,
        active: Boolean = true,
        webhookEventId: String? = null,
        validUntil: Long = now + 86_400_000L,
    ) = client.post("/api/v1/integration/entitlements") {
        contentType(ContentType.Application.Json)
        setBody(
            EntitlementChangeRequest(
                subjectId = subjectId,
                subscriptionRef = ref,
                planId = planId,
                validUntilEpochMs = validUntil,
                status = status,
                active = active,
                webhookEventId = webhookEventId,
            ),
        )
    }

    // Pin to "hard" variant so entitlement is the only axis — variant assignment is tested
    // separately in ExperimentsFeatureTest.
    private suspend fun decide(client: io.ktor.client.HttpClient, visitorId: String): DecideResponse =
        client.post("/api/v1/decide?forceVariant=hard") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = visitorId, articleId = "art-ent-1", tier = "premium"))
        }.body()

    @Test
    fun `Scenario - Subscriber gets full access (AC-01)`() = scenario { client, _ ->
        // Given the subscription administration grants entitlement for subject "sub-1" plan "basic"
        val grant = webhook(client, "visitor:sub-1", ref = "sub-ref-1", status = "active")
        assertEquals(HttpStatusCode.Accepted, grant.status)
        // When subject "sub-1" requests a premium article
        val decision = decide(client, "sub-1")
        // Then the article is served in full
        assertEquals("full", decision.access, "AC-01: entitled visitor must get full access")
    }

    @Test
    fun `Scenario - Revoking entitlement gates the visitor (AC-01)`() = scenario { client, _ ->
        // Given subject "sub-2" holds an active basic entitlement
        webhook(client, "visitor:sub-2", ref = "sub-ref-2", status = "active")
        assertEquals("full", decide(client, "sub-2").access)
        // When the subscription administration revokes the entitlement
        webhook(client, "visitor:sub-2", ref = "sub-ref-2", status = "expired")
        // And subject "sub-2" requests a premium article
        val after = decide(client, "sub-2")
        // Then the response status is 402 and the gate is shown
        assertEquals("gate", after.access, "AC-01: revoked entitlement must gate the visitor")
    }

    @Test
    fun `Scenario - Grace period — past_due still gets access (SUB-05)`() = scenario { client, _ ->
        // Given subject "sub-3" holds an active basic entitlement
        webhook(client, "visitor:sub-3", ref = "sub-ref-3", status = "active")
        assertEquals("full", decide(client, "sub-3").access)
        // When a payment_failure webhook arrives for "sub-3"
        // past_due grants a 7-day grace period (validUntil = realNow + 7d, which is >> service.clock())
        webhook(client, "visitor:sub-3", ref = "sub-ref-3", status = "past_due")
        // Then subject "sub-3" still gets full access during the 7-day grace window
        val inGrace = decide(client, "sub-3")
        assertEquals("full", inGrace.access, "SUB-05: past_due must retain access within grace period")
    }

    @Test
    fun `Scenario - Duplicate webhook is idempotent (SUB-02 NFR-03)`() = scenario { client, store ->
        // Given a grant webhook with id "wh-42" already processed for "sub-4"
        val first = webhook(client, "visitor:sub-4", ref = "sub-ref-4",
            status = "active", webhookEventId = "wh-42")
        assertEquals(HttpStatusCode.Accepted, first.status)
        val countAfterFirst = store.query(EventQuery(setOf("subject:visitor:sub-4"))).events
            .filterIsInstance<EntitlementGranted>().size
        // When the same webhook with id "wh-42" arrives again
        val dup = webhook(client, "visitor:sub-4", ref = "sub-ref-4",
            status = "active", webhookEventId = "wh-42")
        // Then the response is 202 Accepted
        assertEquals(HttpStatusCode.Accepted, dup.status, "SUB-02: duplicate webhook must be accepted")
        // And only one entitlement event exists for "sub-4"
        val countAfterDup = store.query(EventQuery(setOf("subject:visitor:sub-4"))).events
            .filterIsInstance<EntitlementGranted>().size
        assertEquals(countAfterFirst, countAfterDup, "NFR-03: duplicate webhook must not create a second event")
    }
}
