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
        assertTrue((grant.expiresAtEpochMs ?: 0) > System.currentTimeMillis() + sevenDaysMs - 5000,
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
}
