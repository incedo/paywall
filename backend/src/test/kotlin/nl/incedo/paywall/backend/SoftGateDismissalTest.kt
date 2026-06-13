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
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.analytics.SoftGateDismissed
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.metering.MeterPeriod

/**
 * AC-13: soft gates (Dynamic variant) are dismissible once per session.
 * Dismissal is logged as a SoftGateDismissed event and honoured within the
 * 30-minute session window. Hard gates (score ≥ T_hard) are not dismissible.
 */
class SoftGateDismissalTest {

    // Fix clock in the past; advance time by using fixed offsets in service.clock.
    private val baseNow = 1_750_000_000_000L

    // All-Dynamic experiment: any visitor is in the Dynamic variant.
    // tSoft=40, tHard=70. A fresh visitor (score=0) gets "full"; we use
    // externalScore to force soft-gate or hard-gate scenarios.
    private val dynamicExperiment = ExperimentDefinition(
        id = nl.incedo.paywall.core.ExperimentId("ac13-test"),
        name = "AC-13 dismissal test",
        variants = listOf(Variant("dynamic", StrategyConfig.Dynamic(tSoft = 40, tHard = 70), weight = 1)),
    )

    private fun apiTest(
        clockMs: Long = baseNow,
        block: suspend (io.ktor.client.HttpClient) -> Unit,
    ) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = dynamicExperiment,
            clock = { clockMs },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    @Test
    fun softGateBeforeDismissalGatesVisitor() = apiTest { client ->
        // external score = 55 → soft gate (tSoft=40..tHard=70)
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(
                visitorId = "vis-ac13-1", articleId = "a-1", tier = "premium",
                externalScore = 55,
            ))
        }.body<DecideResponse>()
        assertEquals("gate", resp.access, "AC-13: soft-gate range should gate before dismissal")
    }

    @Test
    fun dismissalAllowsAccessWithinSession() = apiTest { client ->
        // Dismiss the soft gate.
        val dismissResp = client.post("/api/v1/decide/dismiss-soft-gate?visitorId=vis-ac13-2")
        assertEquals(HttpStatusCode.Accepted, dismissResp.status)

        // Next decide call with score in soft-gate range → full (gate dismissed).
        val after = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(
                visitorId = "vis-ac13-2", articleId = "a-1", tier = "premium",
                externalScore = 55,
            ))
        }.body<DecideResponse>()
        assertEquals("full", after.access, "AC-13: dismissed soft gate must grant access")
    }

    @Test
    fun hardGateIsNotDismissible() = apiTest { client ->
        // Dismiss the gate.
        client.post("/api/v1/decide/dismiss-soft-gate?visitorId=vis-ac13-3")

        // Hard gate: external score = 80 ≥ tHard (70) → still gated.
        val resp = client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(
                visitorId = "vis-ac13-3", articleId = "a-1", tier = "premium",
                externalScore = 80,
            ))
        }.body<DecideResponse>()
        assertEquals("gate", resp.access, "AC-13: hard gate must not be bypassed by dismissal")
    }

    @Test
    fun dismissalLogsWallDismissedEvent() = apiTest { client ->
        val dismissResp = client.post("/api/v1/decide/dismiss-soft-gate?visitorId=vis-ac13-log")
        assertEquals(HttpStatusCode.Accepted, dismissResp.status)
    }

    @Test
    fun expiredDismissalRestoresSoftGate() {
        // Use a two-test-application approach: store is pre-populated with an
        // expired dismissal (older than SESSION_WINDOW_MS=30 min).
        val expiredAt = baseNow - SoftGateDismissed.SESSION_WINDOW_MS - 1
        val storeWithExpiredDismissal = InMemoryEventStore()
        kotlinx.coroutines.runBlocking {
            storeWithExpiredDismissal.append(
                listOf(
                    SoftGateDismissed(
                        subjectId = nl.incedo.paywall.core.SubjectId("visitor:vis-ac13-expired"),
                        dismissedAtEpochMs = expiredAt,
                    ),
                ),
                condition = null,
            )
        }
        testApplication {
            val service = AccessService(
                eventStore = storeWithExpiredDismissal,
                experiment = dynamicExperiment,
                clock = { baseNow },
                currentPeriod = { MeterPeriod("2026-06") },
            )
            application { module(service, storeWithExpiredDismissal) }
            val client = createClient { install(ContentNegotiation) { json() } }
            val resp = client.post("/api/v1/decide") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(
                    visitorId = "vis-ac13-expired", articleId = "a-1", tier = "premium",
                    externalScore = 55,
                ))
            }.body<DecideResponse>()
            assertEquals("gate", resp.access, "AC-13: expired dismissal must not grant access")
        }
    }
}
