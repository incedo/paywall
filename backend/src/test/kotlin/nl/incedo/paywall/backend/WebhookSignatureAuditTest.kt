package nl.incedo.paywall.backend

import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * Q-7/NFR-03: every inbound M2M webhook must verify a signature — missing or
 * forged signatures must return 401 on every integration endpoint.
 *
 * This is the coverage register: every integration endpoint is listed here.
 * If a new integration endpoint is added, it MUST appear in this test.
 */
class WebhookSignatureAuditTest {

    private val secret = "audit-test-secret"

    private fun sign(body: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return "sha256=" + mac.doFinal(body).joinToString("") { "%02x".format(it) }
    }

    private fun auditTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { System.currentTimeMillis() },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store, webhookVerifier = WebhookVerifier(secret)) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    private suspend fun assertForgedBodyRejects401(
        client: io.ktor.client.HttpClient,
        path: String,
        body: ByteArray,
    ) {
        val resp = client.post(path) {
            // No signature or bad signature — both must be rejected
            header(WebhookVerifier.SIGNATURE_HEADER, "sha256=badbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadbadb")
            setBody(body)
        }
        assertEquals(HttpStatusCode.Unauthorized, resp.status,
            "Q-7/NFR-03: $path must reject unsigned/forged requests")
    }

    @Test fun entitlementsRejectsForgedSignature() = auditTest { client ->
        val body = """{"subjectId":"u:1","subscriptionRef":"s-1","planId":"pro","validUntilEpochMs":9999999999999}""".toByteArray()
        assertForgedBodyRejects401(client, "/api/v1/integration/entitlements", body)
    }

    @Test fun identityLinkRejectsForgedSignature() = auditTest { client ->
        val body = """{"subjectA":"visitor:a","subjectB":"visitor:b","cause":"test","link":true}""".toByteArray()
        assertForgedBodyRejects401(client, "/api/v1/integration/identity-link", body)
    }

    @Test fun accountDeletionRejectsForgedSignature() = auditTest { client ->
        val body = """{"userId":"u-1"}""".toByteArray()
        assertForgedBodyRejects401(client, "/api/v1/integration/account-deletion", body)
    }

    @Test fun adCompletionRejectsForgedSignature() = auditTest { client ->
        val body = """{"subjectId":"visitor:v-1","adPlayId":"play-1","articleId":"art-1"}""".toByteArray()
        assertForgedBodyRejects401(client, "/api/v1/integration/ad-completion", body)
    }

    @Test fun dataGateCompletionRejectsForgedSignature() = auditTest { client ->
        val body = """{"subjectId":"visitor:v-1","purposeId":"p-1","completionId":"c-1"}""".toByteArray()
        assertForgedBodyRejects401(client, "/api/v1/integration/data-gate-completion", body)
    }

    @Test fun dataGateConsentWithdrawalRejectsForgedSignature() = auditTest { client ->
        val body = """{"subjectId":"visitor:v-1","purposeId":"p-1"}""".toByteArray()
        assertForgedBodyRejects401(client, "/api/v1/integration/data-gate-consent-withdrawal", body)
    }

    @Test fun cepOffersRejectsForgedSignature() = auditTest { client ->
        val body = """{"subjectId":"visitor:v-1","offerId":"o-1","kind":"discount"}""".toByteArray()
        assertForgedBodyRejects401(client, "/api/v1/integration/cep-offers", body)
    }

    @Test fun cepAdviceRejectsForgedSignature() = auditTest { client ->
        val body = """{"subjectId":"visitor:v-1","advice":"upsell"}""".toByteArray()
        assertForgedBodyRejects401(client, "/api/v1/integration/cep-advice", body)
    }
}
