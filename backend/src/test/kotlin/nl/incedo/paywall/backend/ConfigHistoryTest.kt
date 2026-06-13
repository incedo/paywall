package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import nl.incedo.paywall.api.ExperimentConfigResponse
import nl.incedo.paywall.api.ExperimentConfigVersionSummary
import nl.incedo.paywall.api.PublishExperimentConfigRequest
import nl.incedo.paywall.core.ExperimentId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.metering.MeterPeriod

/**
 * ADM-06: experiment config version history and rollback.
 * History lists all past ExperimentConfigPublished events (newest-first);
 * rollback re-publishes an old version as a new event.
 */
class ConfigHistoryTest {

    private val now = 1_750_000_000_000L

    private val v1 = ExperimentDefinition(
        id = ExperimentId("hist-test"),
        name = "v1 — single control",
        variants = listOf(Variant("control", StrategyConfig.Hard, weight = 1)),
    )
    private val v2 = ExperimentDefinition(
        id = ExperimentId("hist-test"),
        name = "v2 — control + challenger",
        variants = listOf(
            Variant("control", StrategyConfig.Hard, weight = 1),
            Variant("challenger", StrategyConfig.Hard, weight = 1),
        ),
    )

    private fun configTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val configStore = ConfigStore(store, fallback = defaultExperiment, clock = { now }, cacheTtlMs = 0L)
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod(currentPeriod().value) },
            experimentLoader = configStore::experiment,
        )
        application { module(service, store, configStore = configStore) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    @Test
    fun `history is empty before any publish`() = configTest { client ->
        val history = client.get("/api/v1/admin/config/history")
            .body<List<ExperimentConfigVersionSummary>>()
        assertEquals(0, history.size)
    }

    @Test
    fun `history returns one entry after first publish`() = configTest { client ->
        client.post("/api/v1/admin/config") {
            contentType(ContentType.Application.Json)
            setBody(PublishExperimentConfigRequest(v1))
        }

        val history = client.get("/api/v1/admin/config/history")
            .body<List<ExperimentConfigVersionSummary>>()

        assertEquals(1, history.size)
        assertEquals(1, history[0].version)
        assertEquals(1, history[0].variantCount)
        assertEquals("control", history[0].variantNames)
    }

    @Test
    fun `history is newest-first after two publishes`() = configTest { client ->
        client.post("/api/v1/admin/config") {
            contentType(ContentType.Application.Json)
            setBody(PublishExperimentConfigRequest(v1))
        }
        client.post("/api/v1/admin/config") {
            contentType(ContentType.Application.Json)
            setBody(PublishExperimentConfigRequest(v2))
        }

        val history = client.get("/api/v1/admin/config/history")
            .body<List<ExperimentConfigVersionSummary>>()

        assertEquals(2, history.size)
        // newest-first: v2 at index 0, v1 at index 1
        assertEquals(2, history[0].version)
        assertEquals(2, history[0].variantCount)
        assertEquals(1, history[1].version)
        assertEquals(1, history[1].variantCount)
    }

    @Test
    fun `rollback re-publishes old config as new event`() = configTest { client ->
        // v1 published first, then v2
        client.post("/api/v1/admin/config") {
            contentType(ContentType.Application.Json)
            setBody(PublishExperimentConfigRequest(v1))
        }
        client.post("/api/v1/admin/config") {
            contentType(ContentType.Application.Json)
            setBody(PublishExperimentConfigRequest(v2))
        }

        // Roll back to version 1 (v1 = 1 variant)
        val rollbackResp = client.post("/api/v1/admin/config/rollback?version=1")
        assertEquals(HttpStatusCode.Accepted, rollbackResp.status)

        // Active config should now be v1 again
        val active = client.get("/api/v1/admin/config").body<ExperimentConfigResponse>()
        assertEquals(1, active.experiment.variants.size)
        assertEquals("control", active.experiment.variants[0].name)

        // History now has 3 entries (v1, v2, rollback-to-v1)
        val history = client.get("/api/v1/admin/config/history")
            .body<List<ExperimentConfigVersionSummary>>()
        assertEquals(3, history.size)
    }

    @Test
    fun `rollback to nonexistent version returns 404`() = configTest { client ->
        val resp = client.post("/api/v1/admin/config/rollback?version=99")
        assertEquals(HttpStatusCode.NotFound, resp.status)
    }
}
