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
import kotlin.test.assertTrue
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.grants.DataGateConsentGiven
import nl.incedo.paywall.grants.DataGateConsentWithdrawn
import nl.incedo.paywall.grants.GrantIssued
import nl.incedo.paywall.metering.MeterPeriod

/** DG-02/03: data-gate completion webhook — consent record + 7-day grant. */
class DataGateTest {

    private val webhookSecret = "dg-test-webhook-secret"

    private fun sign(body: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(webhookSecret.toByteArray(), "HmacSHA256"))
        return "sha256=" + mac.doFinal(body).joinToString("") { "%02x".format(it) }
    }

    private fun apiTest(block: suspend (io.ktor.client.HttpClient, InMemoryEventStore) -> Unit) =
        testApplication {
            val store = InMemoryEventStore()
            val service = AccessService(
                eventStore = store,
                experiment = defaultExperiment,
                clock = { System.currentTimeMillis() },
                currentPeriod = { MeterPeriod("2026-06") },
            )
            application {
                module(service, store, webhookVerifier = WebhookVerifier(webhookSecret))
            }
            val client = createClient { install(ContentNegotiation) { json() } }
            block(client, store)
        }

    private fun makePayload(
        subjectId: String = "visitor:vis-dg",
        purposeId: String = "newsletter_article_access",
        completionId: String = "completion-abc",
        articleId: String? = "article-1",
    ): ByteArray {
        val json = buildString {
            append("{")
            append("\"subjectId\":\"$subjectId\",")
            append("\"purposeId\":\"$purposeId\",")
            append("\"completionId\":\"$completionId\"")
            if (articleId != null) append(",\"articleId\":\"$articleId\"")
            append("}")
        }
        return json.toByteArray()
    }

    @Test
    fun dataGateCompletionIssuesGrantAndConsentEvent() = apiTest { client, store ->
        val body = makePayload()
        val resp = client.post("/api/v1/integration/data-gate-completion") {
            header(WebhookVerifier.SIGNATURE_HEADER, sign(body))
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Created, resp.status)
        val result = resp.body<Map<String, String>>()
        assertTrue(result["grantId"]!!.startsWith("dg-"), "grant ID must be prefixed dg- (DG-02)")

        val events = store.query(EventQuery(setOf("subject:visitor:vis-dg"))).events
        assertTrue(events.any { it is DataGateConsentGiven }, "consent event must be recorded (DG-03)")
        val grant = events.filterIsInstance<GrantIssued>().firstOrNull { it.grantedBy == "data_gate" }
        assertTrue(grant != null, "data_gate GrantIssued must be recorded (DG-02)")
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        assertTrue(grant.expiresAtEpochMs > System.currentTimeMillis() + sevenDaysMs - 5000,
            "grant TTL must be 7 days (DG-02)")
    }

    @Test
    fun dataGateInvalidSignatureReturns401() = apiTest { client, _ ->
        val body = makePayload()
        val resp = client.post("/api/v1/integration/data-gate-completion") {
            header(WebhookVerifier.SIGNATURE_HEADER, "sha256=badbadbad")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }

    @Test
    fun dataGateMissingFieldsReturns400() = apiTest { client, _ ->
        val body = """{"subjectId":"vis-1"}""".toByteArray()
        val resp = client.post("/api/v1/integration/data-gate-completion") {
            header(WebhookVerifier.SIGNATURE_HEADER, sign(body))
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test
    fun dataGateGrantHasDataGateGrantedBy() = apiTest { client, store ->
        val body = makePayload(subjectId = "visitor:vis-dg2", completionId = "c-dg2")
        client.post("/api/v1/integration/data-gate-completion") {
            header(WebhookVerifier.SIGNATURE_HEADER, sign(body))
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val grants = store.query(EventQuery(setOf("subject:visitor:vis-dg2"))).events
            .filterIsInstance<GrantIssued>()
        assertEquals("data_gate", grants.first().grantedBy, "DG-02: grantedBy must be data_gate")
    }

    @Test
    fun dataGateBlankFieldsReturnsBadRequest() = apiTest { client, _ ->
        val body = """{"subjectId":"","purposeId":"","completionId":""}""".toByteArray()
        val resp = client.post("/api/v1/integration/data-gate-completion") {
            header(WebhookVerifier.SIGNATURE_HEADER, sign(body))
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status, "DG-02: blank required fields must be rejected")
    }

    // ── DG-03: consent withdrawal ────────────────────────────────────────────

    private fun makeWithdrawalPayload(
        subjectId: String = "visitor:vis-dg",
        purposeId: String = "newsletter_article_access",
    ): ByteArray = """{"subjectId":"$subjectId","purposeId":"$purposeId"}""".toByteArray()

    @Test
    fun consentWithdrawalIsRecordedAndBlocksFutureGrants() = apiTest { client, store ->
        val subjectId = "visitor:vis-dg-w1"
        val purposeId = "newsletter_article_access"

        // First: issue a normal data-gate grant (succeeds).
        val firstBody = makePayload(subjectId = subjectId, purposeId = purposeId, completionId = "c-w1a")
        val firstResp = client.post("/api/v1/integration/data-gate-completion") {
            header(WebhookVerifier.SIGNATURE_HEADER, sign(firstBody))
            contentType(ContentType.Application.Json)
            setBody(firstBody)
        }
        assertEquals(HttpStatusCode.Created, firstResp.status, "first data-gate grant must succeed")

        // Withdraw consent.
        val withdrawBody = makeWithdrawalPayload(subjectId = subjectId, purposeId = purposeId)
        val withdrawResp = client.post("/api/v1/integration/data-gate-consent-withdrawal") {
            header(WebhookVerifier.SIGNATURE_HEADER, sign(withdrawBody))
            contentType(ContentType.Application.Json)
            setBody(withdrawBody)
        }
        assertEquals(HttpStatusCode.Accepted, withdrawResp.status, "DG-03: withdrawal must be recorded")

        // Verify DataGateConsentWithdrawn event is in the store.
        val events = store.query(EventQuery(setOf("subject:$subjectId"))).events
        assertTrue(events.any { it is DataGateConsentWithdrawn },
            "DG-03: DataGateConsentWithdrawn must be recorded")

        // Second data-gate grant after withdrawal must be blocked.
        val secondBody = makePayload(subjectId = subjectId, purposeId = purposeId, completionId = "c-w1b")
        val secondResp = client.post("/api/v1/integration/data-gate-completion") {
            header(WebhookVerifier.SIGNATURE_HEADER, sign(secondBody))
            contentType(ContentType.Application.Json)
            setBody(secondBody)
        }
        assertEquals(HttpStatusCode.Forbidden, secondResp.status,
            "DG-03: data-gate grant must be blocked after consent withdrawal")
    }

    @Test
    fun consentWithdrawalDoesNotAffectOtherPurposes() = apiTest { client, _ ->
        val subjectId = "visitor:vis-dg-w2"
        // Withdraw one purpose.
        val withdrawBody = makeWithdrawalPayload(subjectId = subjectId, purposeId = "purpose-a")
        client.post("/api/v1/integration/data-gate-consent-withdrawal") {
            header(WebhookVerifier.SIGNATURE_HEADER, sign(withdrawBody))
            contentType(ContentType.Application.Json)
            setBody(withdrawBody)
        }
        // Grant for a different purpose must still be allowed.
        val otherBody = makePayload(subjectId = subjectId, purposeId = "purpose-b", completionId = "c-w2")
        val resp = client.post("/api/v1/integration/data-gate-completion") {
            header(WebhookVerifier.SIGNATURE_HEADER, sign(otherBody))
            contentType(ContentType.Application.Json)
            setBody(otherBody)
        }
        assertEquals(HttpStatusCode.Created, resp.status,
            "DG-03: withdrawal of one purpose must not block other purposes")
    }

    @Test
    fun withdrawalWithInvalidSignatureReturns401() = apiTest { client, _ ->
        val body = makeWithdrawalPayload()
        val resp = client.post("/api/v1/integration/data-gate-consent-withdrawal") {
            header(WebhookVerifier.SIGNATURE_HEADER, "sha256=badbad")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status)
    }
}
