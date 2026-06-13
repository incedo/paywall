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
import kotlin.test.assertNotNull
import nl.incedo.paywall.api.GrantChangeRequest
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.analytics.wallEventShardTags
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.metering.MeterPeriod

/** Grant lifecycle over the API (FGA-03, PW-08 day-pass TTL). */
class GrantApiTest {

    private val now = 1_750_000_000_000L

    private fun visitorIn(variant: String): String =
        (0 until 10_000).asSequence()
            .map { "grant-visitor-$it" }
            .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == variant }

    private lateinit var testStore: InMemoryEventStore

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        testStore = InMemoryEventStore()
        val service = AccessService(
            eventStore = testStore,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, testStore) }
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

    // FGA-04 analytics logging -------------------------------------------------------

    @Test
    fun grantBasedAccessLogsArticleReadWithGrantedBy() = apiTest { client ->
        // FGA-04: a successful FGA grant check must log ARTICLE_READ with reason=fga_grant
        // and granted_by so CEP- and AI-granted reads are distinguishable in analytics.
        val visitor = visitorIn("hard")
        val subject = "visitor:$visitor"
        grant(
            client,
            GrantChangeRequest(
                grantId = "g-fga04", subjectId = subject, articleId = "a-1",
                grantedBy = "cep_offer",
            ),
        )
        assertEquals("full", decide(client, visitor, "a-1").access)

        val wallEvents = testStore.query(EventQuery(wallEventShardTags())).events
            .filterIsInstance<WallEventRecorded>()
        val grantRead = wallEvents.find {
            it.eventType == WallEventType.ARTICLE_READ && it.context["reason"] == "fga_grant"
        }
        assertNotNull(grantRead, "FGA-04: grant-based access must log ARTICLE_READ with reason=fga_grant")
        assertEquals("cep_offer", grantRead.context["granted_by"], "FGA-04: granted_by must be logged")
    }

    @Test
    fun grantExceedingMaxTtlIsRejected() = apiTest { client ->
        // FGA-02: default max TTL is 30 days (configurable via ExperimentDefinition.maxGrantTtlDays)
        val thirtyOneDays = 31L * 24 * 3600 * 1000
        val status = grant(
            client,
            GrantChangeRequest(
                grantId = "g-fga-max", subjectId = "visitor:v-1", articleId = "a-1",
                grantedBy = "support",
                expiresAtEpochMs = System.currentTimeMillis() + thirtyOneDays,
            ),
        )
        assertEquals(HttpStatusCode.BadRequest, status, "grant TTL > maxGrantTtlDays (30 days default) must be rejected (FGA-02)")
    }

    @Test
    fun maxGrantTtlIsConfigurableViaExperiment() = testApplication {
        // FGA-02: maxGrantTtlDays is configurable — 60-day limit set in experiment allows a 45-day grant.
        val store = InMemoryEventStore()
        val experiment = defaultExperiment.copy(maxGrantTtlDays = 60)
        val service = AccessService(
            eventStore = store, experiment = experiment,
            clock = { now }, currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        val fortyFiveDays = 45L * 24 * 3600 * 1000
        val status = client.post("/api/v1/grants") {
            contentType(ContentType.Application.Json)
            setBody(GrantChangeRequest(
                grantId = "g-configurable", subjectId = "visitor:v-ttl", articleId = "a-1",
                grantedBy = "support",
                expiresAtEpochMs = System.currentTimeMillis() + fortyFiveDays,
            ))
        }.status
        assertEquals(HttpStatusCode.Accepted, status,
            "FGA-02: 45-day grant must be accepted when maxGrantTtlDays=60 in experiment config")
    }
}
