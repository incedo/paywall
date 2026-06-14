package nl.incedo.paywall.backend.contract

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import nl.incedo.paywall.api.AddControlRequest
import nl.incedo.paywall.api.ChangeControlDefaultRequest
import nl.incedo.paywall.api.ControlSchemaResponse
import nl.incedo.paywall.api.RegisterControlSchemaRequest
import nl.incedo.paywall.api.RegisterScenarioRequest
import nl.incedo.paywall.api.RegisterStoryRequest
import nl.incedo.paywall.api.ScenarioResponse
import nl.incedo.paywall.api.StoryResponse
import nl.incedo.paywall.backend.AccessService
import nl.incedo.paywall.backend.defaultExperiment
import nl.incedo.paywall.backend.module
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * Producer-side contract tests for the Storybook API (SB-01 – SB-15).
 * Verifies response shapes, status codes, and business rule enforcement.
 * Mirrors testing.md §10b.
 */
class StorybookContractTest {

    private fun contractTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { 1_750_000_000_000L },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    private fun storyReq(id: String = "s1", key: String = "button") = RegisterStoryRequest(
        storyId = id, storyKey = key, title = "Button", type = "COMPONENT",
        groupId = "inputs", owner = "design", renderContractRef = "contracts/button.md",
    )

    private fun scenarioReq(id: String = "sc1", key: String = "default") = RegisterScenarioRequest(
        scenarioId = id, scenarioKey = key, title = "Default state", type = "STATE",
    )

    private fun controlReq(id: String = "c1", key: String = "variant") = AddControlRequest(
        controlId = id, key = key, label = "Variant", type = "ENUM",
        defaultValue = "primary", bindingRef = "props.variant",
    )

    // ── Story endpoints ───────────────────────────────────────────────────────

    @Test fun `POST stories returns 201 with story fields`() = contractTest { client ->
        val resp = client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json)
            setBody(storyReq())
        }
        assertEquals(HttpStatusCode.Created, resp.status)
        val body = resp.body<StoryResponse>()
        assertEquals("s1", body.storyId)
        assertEquals("button", body.storyKey)
        assertEquals("COMPONENT", body.type)
        assertEquals("ACTIVE", body.lifecycle)
    }

    @Test fun `POST stories with duplicate storyKey returns 409`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        val resp = client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json)
            setBody(storyReq(id = "s2", key = "button")) // same key, different id
        }
        assertEquals(HttpStatusCode.Conflict, resp.status)
    }

    @Test fun `POST stories with duplicate storyId returns 409`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        val resp = client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json)
            setBody(storyReq(id = "s1", key = "card")) // same id, different key
        }
        assertEquals(HttpStatusCode.Conflict, resp.status)
    }

    @Test fun `POST stories with missing fields returns 400`() = contractTest { client ->
        val resp = client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json)
            setBody(storyReq().copy(title = ""))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test fun `GET stories lists registered stories`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        val resp = client.get("/api/v1/storybook/stories")
        assertEquals(HttpStatusCode.OK, resp.status)
        val list = resp.body<List<StoryResponse>>()
        assertEquals(1, list.size)
        assertEquals("s1", list.first().storyId)
    }

    @Test fun `GET stories by id returns story`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        val resp = client.get("/api/v1/storybook/stories/s1")
        assertEquals(HttpStatusCode.OK, resp.status)
        assertEquals("s1", resp.body<StoryResponse>().storyId)
    }

    @Test fun `GET stories by unknown id returns 404`() = contractTest { client ->
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/storybook/stories/unknown").status)
    }

    // ── Scenario endpoints ────────────────────────────────────────────────────

    @Test fun `POST scenarios under story returns 201`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        val resp = client.post("/api/v1/storybook/stories/s1/scenarios") {
            contentType(ContentType.Application.Json); setBody(scenarioReq())
        }
        assertEquals(HttpStatusCode.Created, resp.status)
        val body = resp.body<ScenarioResponse>()
        assertEquals("sc1", body.scenarioId)
        assertEquals("s1", body.storyId)
        assertEquals("STATE", body.type)
    }

    @Test fun `POST scenarios with duplicate key in same story returns 409`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        client.post("/api/v1/storybook/stories/s1/scenarios") {
            contentType(ContentType.Application.Json); setBody(scenarioReq())
        }
        val resp = client.post("/api/v1/storybook/stories/s1/scenarios") {
            contentType(ContentType.Application.Json)
            setBody(scenarioReq(id = "sc2", key = "default")) // same key
        }
        assertEquals(HttpStatusCode.Conflict, resp.status)
    }

    @Test fun `POST scenarios under unknown story returns 404`() = contractTest { client ->
        val resp = client.post("/api/v1/storybook/stories/unknown/scenarios") {
            contentType(ContentType.Application.Json); setBody(scenarioReq())
        }
        assertEquals(HttpStatusCode.NotFound, resp.status)
    }

    @Test fun `GET scenarios lists active scenarios for story`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        client.post("/api/v1/storybook/stories/s1/scenarios") {
            contentType(ContentType.Application.Json); setBody(scenarioReq())
        }
        val resp = client.get("/api/v1/storybook/stories/s1/scenarios")
        assertEquals(HttpStatusCode.OK, resp.status)
        val list = resp.body<List<ScenarioResponse>>()
        assertEquals(1, list.size)
        assertEquals("sc1", list.first().scenarioId)
    }

    // ── ControlSchema endpoints ───────────────────────────────────────────────

    private suspend fun io.ktor.client.HttpClient.seedStoryAndScenario() {
        post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        post("/api/v1/storybook/stories/s1/scenarios") {
            contentType(ContentType.Application.Json); setBody(scenarioReq())
        }
    }

    @Test fun `POST control-schema under scenario returns 201`() = contractTest { client ->
        client.seedStoryAndScenario()
        val resp = client.post("/api/v1/storybook/scenarios/sc1/control-schema") {
            contentType(ContentType.Application.Json)
            setBody(RegisterControlSchemaRequest(schemaId = "cs1"))
        }
        assertEquals(HttpStatusCode.Created, resp.status)
        val body = resp.body<ControlSchemaResponse>()
        assertEquals("cs1", body.schemaId)
        assertEquals("sc1", body.scenarioId)
        assertTrue(body.controls.isEmpty())
    }

    @Test fun `POST control-schema under unknown scenario returns 404 (BR-2)`() = contractTest { client ->
        val resp = client.post("/api/v1/storybook/scenarios/unknown/control-schema") {
            contentType(ContentType.Application.Json)
            setBody(RegisterControlSchemaRequest(schemaId = "cs1"))
        }
        assertEquals(HttpStatusCode.NotFound, resp.status)
    }

    @Test fun `POST control adds control to schema (BR-3)`() = contractTest { client ->
        client.seedStoryAndScenario()
        client.post("/api/v1/storybook/scenarios/sc1/control-schema") {
            contentType(ContentType.Application.Json)
            setBody(RegisterControlSchemaRequest(schemaId = "cs1"))
        }
        val resp = client.post("/api/v1/storybook/control-schemas/cs1/controls") {
            contentType(ContentType.Application.Json); setBody(controlReq())
        }
        assertEquals(HttpStatusCode.Created, resp.status)
    }

    @Test fun `POST control with duplicate key returns 409 (BR-4)`() = contractTest { client ->
        client.seedStoryAndScenario()
        client.post("/api/v1/storybook/scenarios/sc1/control-schema") {
            contentType(ContentType.Application.Json)
            setBody(RegisterControlSchemaRequest(schemaId = "cs1"))
        }
        client.post("/api/v1/storybook/control-schemas/cs1/controls") {
            contentType(ContentType.Application.Json); setBody(controlReq())
        }
        val resp = client.post("/api/v1/storybook/control-schemas/cs1/controls") {
            contentType(ContentType.Application.Json)
            setBody(controlReq(id = "c2", key = "variant")) // same key
        }
        assertEquals(HttpStatusCode.Conflict, resp.status)
    }

    @Test fun `GET controls returns schema with active controls`() = contractTest { client ->
        client.seedStoryAndScenario()
        client.post("/api/v1/storybook/scenarios/sc1/control-schema") {
            contentType(ContentType.Application.Json)
            setBody(RegisterControlSchemaRequest(schemaId = "cs1"))
        }
        client.post("/api/v1/storybook/control-schemas/cs1/controls") {
            contentType(ContentType.Application.Json); setBody(controlReq())
        }
        val resp = client.get("/api/v1/storybook/scenarios/sc1/controls")
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = resp.body<ControlSchemaResponse>()
        assertEquals(1, body.controls.size)
        assertEquals("variant", body.controls.first().key)
    }

    @Test fun `PUT default changes control default value`() = contractTest { client ->
        client.seedStoryAndScenario()
        client.post("/api/v1/storybook/scenarios/sc1/control-schema") {
            contentType(ContentType.Application.Json)
            setBody(RegisterControlSchemaRequest(schemaId = "cs1"))
        }
        client.post("/api/v1/storybook/control-schemas/cs1/controls") {
            contentType(ContentType.Application.Json); setBody(controlReq())
        }
        val resp = client.put("/api/v1/storybook/control-schemas/cs1/controls/c1/default") {
            contentType(ContentType.Application.Json)
            setBody(ChangeControlDefaultRequest("secondary"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val controls = client.get("/api/v1/storybook/scenarios/sc1/controls").body<ControlSchemaResponse>()
        assertEquals("secondary", controls.controls.first().defaultValue)
    }

    @Test fun `DELETE control removes it from active view (BR-10)`() = contractTest { client ->
        client.seedStoryAndScenario()
        client.post("/api/v1/storybook/scenarios/sc1/control-schema") {
            contentType(ContentType.Application.Json)
            setBody(RegisterControlSchemaRequest(schemaId = "cs1"))
        }
        client.post("/api/v1/storybook/control-schemas/cs1/controls") {
            contentType(ContentType.Application.Json); setBody(controlReq())
        }
        val resp = client.delete("/api/v1/storybook/control-schemas/cs1/controls/c1")
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val controls = client.get("/api/v1/storybook/scenarios/sc1/controls").body<ControlSchemaResponse>()
        assertTrue(controls.controls.isEmpty())
    }

    @Test fun `PUT default on removed control returns 404`() = contractTest { client ->
        client.seedStoryAndScenario()
        client.post("/api/v1/storybook/scenarios/sc1/control-schema") {
            contentType(ContentType.Application.Json)
            setBody(RegisterControlSchemaRequest(schemaId = "cs1"))
        }
        client.post("/api/v1/storybook/control-schemas/cs1/controls") {
            contentType(ContentType.Application.Json); setBody(controlReq())
        }
        client.delete("/api/v1/storybook/control-schemas/cs1/controls/c1")
        val resp = client.put("/api/v1/storybook/control-schemas/cs1/controls/c1/default") {
            contentType(ContentType.Application.Json)
            setBody(ChangeControlDefaultRequest("tertiary"))
        }
        assertEquals(HttpStatusCode.NotFound, resp.status)
    }
}
