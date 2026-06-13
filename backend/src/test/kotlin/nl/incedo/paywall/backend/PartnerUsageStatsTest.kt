package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import nl.incedo.paywall.api.AddPartnerMemberRequest
import nl.incedo.paywall.api.CreatePartnerRequest
import nl.incedo.paywall.api.PartnerUsageResponse
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * PA-04: partner usage (reads per partner, unique users) reported from the
 * wall-event stream for contract management.
 */
class PartnerUsageStatsTest {

    private val now = 1_750_000_000_000L
    private val originSecret = "test-pa04-secret"

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store, originSecret = originSecret) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    private suspend fun io.ktor.client.HttpClient.createPartner(partnerId: String) =
        post("/api/v1/admin/partners") {
            contentType(ContentType.Application.Json)
            header("X-Origin-Secret", originSecret)
            setBody(CreatePartnerRequest(partnerId = partnerId, name = "$partnerId Corp"))
        }

    private suspend fun io.ktor.client.HttpClient.partnerDecide(
        partnerId: String,
        visitorId: String,
        articleId: String = "a-premium",
    ) = post("/api/v1/decide") {
        contentType(ContentType.Application.Json)
        header("X-Origin-Secret", originSecret)
        header("X-Partner-Id", partnerId)
        setBody(DecideRequest(visitorId = visitorId, articleId = articleId, tier = "premium"))
    }

    private suspend fun io.ktor.client.HttpClient.partnerStats() =
        get("/api/v1/stats/partners") {
            // origin trust required when originSecret is configured
            header("X-Origin-Secret", originSecret)
        }.body<List<PartnerUsageResponse>>()

    @Test
    fun emptyStatsWhenNoPartnerAccess() = apiTest { client ->
        val resp = client.partnerStats()
        assertTrue(resp.isEmpty(), "PA-04: no partner reads → empty stats")
    }

    @Test
    fun partnerReadCountedInStats() = apiTest { client ->
        client.createPartner("partner-a")
        client.partnerDecide("partner-a", visitorId = "vis-1")

        val stats = client.partnerStats()
        assertEquals(1, stats.size)
        assertEquals("partner-a", stats[0].partnerId)
        assertEquals(1, stats[0].totalReads, "PA-04: one partner read must be counted")
        assertEquals(1, stats[0].uniqueUsers, "PA-04: one unique visitor")
    }

    @Test
    fun sameVisitorCountsOnceTowardUniqueUsers() = apiTest { client ->
        client.createPartner("partner-b")
        client.partnerDecide("partner-b", visitorId = "vis-repeat", articleId = "a-1")
        client.partnerDecide("partner-b", visitorId = "vis-repeat", articleId = "a-2")

        val stats = client.partnerStats()
        val partnerB = stats.first { it.partnerId == "partner-b" }
        assertEquals(2, partnerB.totalReads, "PA-04: 2 reads by same visitor")
        assertEquals(1, partnerB.uniqueUsers, "PA-04: same visitor counts as 1 unique user")
    }

    @Test
    fun multiplePartnersReportedSeparately() = apiTest { client ->
        client.createPartner("partner-c1")
        client.createPartner("partner-c2")
        client.partnerDecide("partner-c1", visitorId = "vis-c1")
        client.partnerDecide("partner-c2", visitorId = "vis-c2a")
        client.partnerDecide("partner-c2", visitorId = "vis-c2b")

        val stats = client.partnerStats().associateBy { it.partnerId }
        assertEquals(1, stats["partner-c1"]?.totalReads, "PA-04: partner-c1 has 1 read")
        assertEquals(2, stats["partner-c2"]?.totalReads, "PA-04: partner-c2 has 2 reads")
        assertEquals(2, stats["partner-c2"]?.uniqueUsers, "PA-04: partner-c2 has 2 unique users")
    }

    @Test
    fun statsEndpointAccessibleInDevMode() {
        // In dev mode (no originSecret), no X-Origin-Secret header needed.
        testApplication {
            val store = InMemoryEventStore()
            val service = AccessService(
                eventStore = store,
                experiment = defaultExperiment,
                clock = { now },
                currentPeriod = { MeterPeriod("2026-06") },
            )
            application { module(service, store) } // no originSecret → origin trust open
            val client = createClient { install(ContentNegotiation) { json() } }
            val resp = client.get("/api/v1/stats/partners")
            assertEquals(HttpStatusCode.OK, resp.status, "PA-04: stats endpoint accessible without origin secret in dev mode")
        }
    }
}
