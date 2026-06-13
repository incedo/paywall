package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
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
import kotlin.test.assertTrue
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * PR-03: profile completeness — consent records per subject with AN-20
 * purpose tracking. Exposed so the CEP can segment on profile completeness.
 */
class ProfileCompletenessTest {

    private val webhookSecret = "pr03-webhook-secret"
    private val now = 1_750_000_000_000L

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
                clock = { now },
                currentPeriod = { MeterPeriod("2026-06") },
            )
            application { module(service, store, webhookVerifier = WebhookVerifier(webhookSecret)) }
            val client = createClient { install(ContentNegotiation) { json() } }
            block(client, store)
        }

    /** Trigger the data-gate-completion webhook for a subject. */
    private suspend fun io.ktor.client.HttpClient.dataGate(
        subjectId: String,
        purposeId: String,
        completionId: String,
        articleId: String = "art-pr03",
    ) {
        val body = """{"subjectId":"$subjectId","purposeId":"$purposeId","completionId":"$completionId","articleId":"$articleId"}""".toByteArray()
        post("/api/v1/integration/data-gate-completion") {
            header(WebhookVerifier.SIGNATURE_HEADER, sign(body))
            setBody(body)
        }
    }

    @Test
    fun subjectWithNoConsentsHasEmptyProfile() = apiTest { client, _ ->
        val resp = client.get("/api/v1/subjects/visitor:sub-none/profile")
        assertEquals(HttpStatusCode.OK, resp.status, "PR-03: profile endpoint must return 200")
        val profile = resp.body<ProfileCompletenessResponse>()
        assertEquals("visitor:sub-none", profile.subjectId)
        assertTrue(profile.consentedPurposes.isEmpty(), "PR-03: no consents → empty purposes map")
        assertEquals(0.0, profile.completenessScore, "PR-03: no consents → score = 0.0")
    }

    @Test
    fun singleConsentAppearsInProfile() = apiTest { client, _ ->
        client.dataGate("visitor:sub-single", "newsletter_signup_article_access", "c-1")
        val profile = client.get("/api/v1/subjects/visitor:sub-single/profile")
            .body<ProfileCompletenessResponse>()
        assertEquals(1, profile.consentedPurposes.size, "PR-03: one consent must appear")
        assertTrue("newsletter_signup_article_access" in profile.consentedPurposes,
            "PR-03: correct purposeId must be in consentedPurposes")
        assertTrue(profile.completenessScore > 0.0, "PR-03: score must be > 0 after first consent")
    }

    @Test
    fun multipleDistinctConsentsAccumulate() = apiTest { client, _ ->
        client.dataGate("visitor:sub-multi", "newsletter_signup_article_access", "c-1")
        client.dataGate("visitor:sub-multi", "survey_completion_article_access", "c-2")
        val profile = client.get("/api/v1/subjects/visitor:sub-multi/profile")
            .body<ProfileCompletenessResponse>()
        assertEquals(2, profile.consentedPurposes.size, "PR-03: two distinct consents expected")
        val score = profile.completenessScore
        assertTrue(score > 0.0 && score <= 1.0, "PR-03: completeness score must be in [0,1]")
    }

    @Test
    fun samePurposeAppearsOnce() = apiTest { client, store ->
        // Two data-gate completions for the same purposeId (different completionIds, so both go through)
        client.dataGate("visitor:sub-idem", "newsletter_signup_article_access", "c-a")
        client.dataGate("visitor:sub-idem", "newsletter_signup_article_access", "c-b")
        val profile = client.get("/api/v1/subjects/visitor:sub-idem/profile")
            .body<ProfileCompletenessResponse>()
        // Same purposeId → projection keeps only the latest consent timestamp
        assertEquals(1, profile.consentedPurposes.size, "PR-03: same purposeId should deduplicate in response")
    }

    @Test
    fun profileIncludesConsentTimestamp() = apiTest { client, _ ->
        client.dataGate("visitor:sub-ts", "newsletter_signup_article_access", "c-ts")
        val profile = client.get("/api/v1/subjects/visitor:sub-ts/profile")
            .body<ProfileCompletenessResponse>()
        val ts = profile.consentedPurposes["newsletter_signup_article_access"]
        assertTrue(ts != null && ts > 0L, "PR-03: consent timestamp must be a positive epoch ms")
    }
}
