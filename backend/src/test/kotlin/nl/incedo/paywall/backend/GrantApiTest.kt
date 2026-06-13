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
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.metering.MeterPeriod

/** Grant lifecycle over the API (FGA-03, PW-08 day-pass TTL). */
class GrantApiTest {

    private val now = 1_750_000_000_000L

    private fun visitorIn(variant: String): String =
        (0 until 10_000).asSequence()
            .map { "grant-visitor-$it" }
            .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == variant }

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    private suspend fun decide(client: io.ktor.client.HttpClient, visitor: String, article: String): DecideResponse =
        client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = visitor, articleId = article, tier = "premium"))
        }.body()

    private suspend fun grant(client: io.ktor.client.HttpClient, request: GrantChangeRequest): HttpStatusCode =
        client.post("/api/v1/grants") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.status

    @Test
    fun dayPassUnlocksOneArticleUntilItExpires() = apiTest { client ->
        val visitor = visitorIn("hard")
        val subject = "visitor:$visitor"

        // Without a grant: hard wall gates
        assertEquals("gate", decide(client, visitor, "a-1").access)

        // Day pass for a-1 (PW-08: TTL = pass duration)
        val status = grant(
            client,
            GrantChangeRequest(
                grantId = "g-1", subjectId = subject, articleId = "a-1",
                grantedBy = "day_pass", expiresAtEpochMs = now + 86_400_000,
            ),
        )
        assertEquals(HttpStatusCode.Accepted, status)
        assertEquals("full", decide(client, visitor, "a-1").access)
        assertEquals("grant", decide(client, visitor, "a-1").reason)

        // Article-scoped: a-2 stays gated (FGA grants are per article)
        assertEquals("gate", decide(client, visitor, "a-2").access)
    }

    @Test
    fun expiredPassNoLongerUnlocks() = apiTest { client ->
        val visitor = visitorIn("hard")
        grant(
            client,
            GrantChangeRequest(
                grantId = "g-2", subjectId = "visitor:$visitor", articleId = "a-1",
                grantedBy = "day_pass", expiresAtEpochMs = now - 1,
            ),
        )
        assertEquals("gate", decide(client, visitor, "a-1").access)
    }

    @Test
    fun revokedGrantNoLongerUnlocks() = apiTest { client ->
        val visitor = visitorIn("hard")
        val subject = "visitor:$visitor"
        grant(client, GrantChangeRequest(grantId = "g-3", subjectId = subject, articleId = "a-1", grantedBy = "support"))
        assertEquals("full", decide(client, visitor, "a-1").access)

        grant(client, GrantChangeRequest(grantId = "g-3", subjectId = subject, articleId = "a-1", active = false))
        assertEquals("gate", decide(client, visitor, "a-1").access)
    }

    @Test
    fun blankFieldsAreRejected() = apiTest { client ->
        val status = grant(client, GrantChangeRequest(grantId = "", subjectId = "visitor:v", articleId = "a-1"))
        assertEquals(HttpStatusCode.BadRequest, status)
    }

    // FGA-02 TTL enforcement -------------------------------------------------------

    @Test
    fun grantWithoutExpiryDefaultsToThirtyDays() = apiTest { client ->
        // FGA-02: expires_at defaults to 30 days when not supplied
        val visitor = visitorIn("hard")
        val status = grant(
            client,
            GrantChangeRequest(
                grantId = "g-fga-default", subjectId = "visitor:$visitor", articleId = "a-1",
                grantedBy = "support",
                expiresAtEpochMs = null, // intentionally omitted
            ),
        )
        assertEquals(HttpStatusCode.Accepted, status, "grant without expiry must be accepted with a default TTL")
        assertEquals("full", decide(client, visitor, "a-1").access, "default-TTL grant must unlock the article")
    }

    @Test
    fun grantExceedingMaxTtlIsRejected() = apiTest { client ->
        // FGA-02: max TTL is 90 days
        val ninetyOneDays = 91L * 24 * 3600 * 1000
        val status = grant(
            client,
            GrantChangeRequest(
                grantId = "g-fga-max", subjectId = "visitor:v-1", articleId = "a-1",
                grantedBy = "support",
                expiresAtEpochMs = System.currentTimeMillis() + ninetyOneDays,
            ),
        )
        assertEquals(HttpStatusCode.BadRequest, status, "grant TTL > 90 days must be rejected (FGA-02)")
    }
}
