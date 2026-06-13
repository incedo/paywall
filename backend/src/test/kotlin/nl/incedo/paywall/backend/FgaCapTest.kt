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
import nl.incedo.paywall.metering.MeterPeriod

/** FGA-03: max 10 active grants per subject per source (grantedBy). */
class FgaCapTest {

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { System.currentTimeMillis() },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    @Test
    fun tenGrantsAllowedEleventhRejected() = apiTest { client ->
        val subjectId = "visitor:vis-fga-cap"
        val now = System.currentTimeMillis()
        val expiresAt = now + 30L * 24 * 3600 * 1000 // 30 days — well within FGA-02 max

        for (i in 1..10) {
            val resp = client.post("/api/v1/grants") {
                contentType(ContentType.Application.Json)
                setBody(GrantChangeRequest(
                    grantId = "grant-cap-$i",
                    subjectId = subjectId,
                    articleId = "article-$i",
                    grantedBy = "support",
                    expiresAtEpochMs = expiresAt,
                    active = true,
                ))
            }
            assertEquals(HttpStatusCode.Accepted, resp.status, "grant $i of 10 must be accepted (FGA-03)")
        }

        // Eleventh grant from same source must be rejected
        val eleventh = client.post("/api/v1/grants") {
            contentType(ContentType.Application.Json)
            setBody(GrantChangeRequest(
                grantId = "grant-cap-11",
                subjectId = subjectId,
                articleId = "article-11",
                grantedBy = "support",
                expiresAtEpochMs = expiresAt,
                active = true,
            ))
        }
        assertEquals(HttpStatusCode.TooManyRequests, eleventh.status,
            "eleventh grant from same source must be rejected (FGA-03)")
    }

    @Test
    fun differentSourceDoesNotCountAgainstCap() = apiTest { client ->
        val subjectId = "visitor:vis-fga-multi"
        val expiresAt = System.currentTimeMillis() + 30L * 24 * 3600 * 1000

        // Fill up "support" source
        for (i in 1..10) {
            client.post("/api/v1/grants") {
                contentType(ContentType.Application.Json)
                setBody(GrantChangeRequest(grantId = "grant-src-$i", subjectId = subjectId, articleId = "art-$i", grantedBy = "support", expiresAtEpochMs = expiresAt))
            }
        }

        // Different source: "day_pass" — should still be allowed
        val dayPass = client.post("/api/v1/grants") {
            contentType(ContentType.Application.Json)
            setBody(GrantChangeRequest(grantId = "grant-dp-1", subjectId = subjectId, articleId = "art-dp", grantedBy = "day_pass", expiresAtEpochMs = expiresAt))
        }
        assertEquals(HttpStatusCode.Accepted, dayPass.status,
            "grants from a different source must not count against other source's cap (FGA-03)")
    }
}
