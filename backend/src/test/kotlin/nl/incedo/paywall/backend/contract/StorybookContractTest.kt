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
import nl.incedo.paywall.api.DecoratorResponse
import nl.incedo.paywall.api.RegisterDecoratorRequest
import nl.incedo.paywall.api.UpdateDecoratorPriorityRequest
import nl.incedo.paywall.api.UpdateScenarioRequest
import nl.incedo.paywall.api.UpdateStoryRequest
import nl.incedo.paywall.api.ChangeControlDefaultRequest
import nl.incedo.paywall.api.ControlSchemaResponse
import nl.incedo.paywall.api.RegisterControlSchemaRequest
import nl.incedo.paywall.api.RegisterScenarioRequest
import nl.incedo.paywall.api.RegisterStoryRequest
import nl.incedo.paywall.api.ScenarioResponse
import nl.incedo.paywall.api.StoryResponse
import nl.incedo.paywall.api.RegisterResponsiveProfileRequest
import nl.incedo.paywall.api.AddFormFactorRequest
import nl.incedo.paywall.api.DefineWidthClassesRequest
import nl.incedo.paywall.api.SetNavigationPatternRequest
import nl.incedo.paywall.api.SetDensityProfileRequest
import nl.incedo.paywall.api.LinkExpectationRequest
import nl.incedo.paywall.api.AddLayoutRuleRequest
import nl.incedo.paywall.api.ResponsiveProfileResponse
import nl.incedo.paywall.api.RegisterPhaseRequest
import nl.incedo.paywall.api.AddCapabilityRequest
import nl.incedo.paywall.api.PhaseResponse
import nl.incedo.paywall.api.RegisterGovernancePolicyRequest
import nl.incedo.paywall.api.AttachQualityGateRequest
import nl.incedo.paywall.api.AssignOwnerRequest
import nl.incedo.paywall.api.LinkEvidenceRequest
import nl.incedo.paywall.api.RecordGovernanceDecisionRequest
import nl.incedo.paywall.api.GovernLifecycleRequest
import nl.incedo.paywall.api.GovernancePolicyResponse
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

    // ── Story update/archive ──────────────────────────────────────────────────

    @Test fun `PUT story updates metadata`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        val resp = client.put("/api/v1/storybook/stories/s1") {
            contentType(ContentType.Application.Json)
            setBody(UpdateStoryRequest(title = "Button v2"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        assertEquals("Button v2", client.get("/api/v1/storybook/stories/s1").body<StoryResponse>().title)
    }

    @Test fun `PUT story on unknown story returns 404`() = contractTest { client ->
        val resp = client.put("/api/v1/storybook/stories/unknown") {
            contentType(ContentType.Application.Json); setBody(UpdateStoryRequest(title = "X"))
        }
        assertEquals(HttpStatusCode.NotFound, resp.status)
    }

    @Test fun `DELETE story archives it`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        assertEquals(HttpStatusCode.Accepted, client.delete("/api/v1/storybook/stories/s1").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/storybook/stories/s1").status)
    }

    @Test fun `DELETE already archived story returns 409`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        client.delete("/api/v1/storybook/stories/s1")
        assertEquals(HttpStatusCode.Conflict, client.delete("/api/v1/storybook/stories/s1").status)
    }

    // ── Scenario GET, update, archive ─────────────────────────────────────────

    @Test fun `GET scenario by id returns scenario`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        client.post("/api/v1/storybook/stories/s1/scenarios") {
            contentType(ContentType.Application.Json); setBody(scenarioReq())
        }
        val resp = client.get("/api/v1/storybook/scenarios/sc1")
        assertEquals(HttpStatusCode.OK, resp.status)
        assertEquals("sc1", resp.body<ScenarioResponse>().scenarioId)
    }

    @Test fun `GET scenario with unknown id returns 404`() = contractTest { client ->
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/storybook/scenarios/unknown").status)
    }

    @Test fun `PUT scenario updates metadata`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        client.post("/api/v1/storybook/stories/s1/scenarios") {
            contentType(ContentType.Application.Json); setBody(scenarioReq())
        }
        val resp = client.put("/api/v1/storybook/scenarios/sc1") {
            contentType(ContentType.Application.Json)
            setBody(UpdateScenarioRequest(title = "Default state v2"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        assertEquals("Default state v2", client.get("/api/v1/storybook/scenarios/sc1").body<ScenarioResponse>().title)
    }

    @Test fun `DELETE scenario archives it`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        client.post("/api/v1/storybook/stories/s1/scenarios") {
            contentType(ContentType.Application.Json); setBody(scenarioReq())
        }
        assertEquals(HttpStatusCode.Accepted, client.delete("/api/v1/storybook/scenarios/sc1").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/storybook/scenarios/sc1").status)
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

    // ── Decorator contract tests ───────────────────────────────────────────────

    private fun decoratorReq(id: String = "d1", key: String = "theme/default-dark") = RegisterDecoratorRequest(
        decoratorId = id, decoratorKey = key, title = "Default Dark",
        type = "THEME", renderRef = "decorators.theme.defaultDark", priority = 10, scope = "GLOBAL",
    )

    @Test fun `POST decorator returns 201 with fields`() = contractTest { client ->
        val resp = client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json); setBody(decoratorReq())
        }
        assertEquals(HttpStatusCode.Created, resp.status)
        val body = resp.body<DecoratorResponse>()
        assertEquals("d1", body.decoratorId)
        assertEquals("theme/default-dark", body.decoratorKey)
        assertEquals("THEME", body.type)
        assertEquals("GLOBAL", body.scope)
        assertEquals(10, body.priority)
        assertEquals("ACTIVE", body.lifecycle)
    }

    @Test fun `POST decorator with duplicate key returns 409 (BR-3)`() = contractTest { client ->
        client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json); setBody(decoratorReq())
        }
        val resp = client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json)
            setBody(decoratorReq(id = "d2", key = "theme/default-dark"))
        }
        assertEquals(HttpStatusCode.Conflict, resp.status)
    }

    @Test fun `POST decorator with missing required fields returns 400`() = contractTest { client ->
        val resp = client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json)
            setBody(decoratorReq().copy(renderRef = ""))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test fun `POST decorator with non-positive priority returns 400 (BR-5)`() = contractTest { client ->
        val resp = client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json)
            setBody(decoratorReq().copy(priority = 0))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test fun `GET decorators lists active decorators`() = contractTest { client ->
        client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json); setBody(decoratorReq())
        }
        val list = client.get("/api/v1/storybook/decorators").body<List<DecoratorResponse>>()
        assertEquals(1, list.size)
    }

    @Test fun `GET decorator by id returns decorator`() = contractTest { client ->
        client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json); setBody(decoratorReq())
        }
        val resp = client.get("/api/v1/storybook/decorators/d1")
        assertEquals(HttpStatusCode.OK, resp.status)
        assertEquals("d1", resp.body<DecoratorResponse>().decoratorId)
    }

    @Test fun `GET decorator with unknown id returns 404`() = contractTest { client ->
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/storybook/decorators/unknown").status)
    }

    @Test fun `PUT priority updates decorator ordering`() = contractTest { client ->
        client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json); setBody(decoratorReq())
        }
        val resp = client.put("/api/v1/storybook/decorators/d1/priority") {
            contentType(ContentType.Application.Json)
            setBody(UpdateDecoratorPriorityRequest(priority = 20))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        assertEquals(20, client.get("/api/v1/storybook/decorators/d1").body<DecoratorResponse>().priority)
    }

    @Test fun `PUT priority with zero returns 400 (BR-5)`() = contractTest { client ->
        client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json); setBody(decoratorReq())
        }
        val resp = client.put("/api/v1/storybook/decorators/d1/priority") {
            contentType(ContentType.Application.Json)
            setBody(UpdateDecoratorPriorityRequest(priority = 0))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test fun `POST link decorator to story succeeds for STORY-scope`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json)
            setBody(storyReq())
        }
        client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json)
            setBody(decoratorReq().copy(scope = "STORY"))
        }
        val resp = client.post("/api/v1/storybook/decorators/d1/stories/s1")
        assertEquals(HttpStatusCode.Created, resp.status)
    }

    @Test fun `POST link GLOBAL decorator to story returns 409 (BR-7)`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") {
            contentType(ContentType.Application.Json); setBody(storyReq())
        }
        client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json); setBody(decoratorReq()) // GLOBAL scope
        }
        val resp = client.post("/api/v1/storybook/decorators/d1/stories/s1")
        assertEquals(HttpStatusCode.Conflict, resp.status)
    }

    @Test fun `POST link decorator to scenario requires existing scenario (BR-8)`() = contractTest { client ->
        client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json)
            setBody(decoratorReq().copy(scope = "SCENARIO"))
        }
        val resp = client.post("/api/v1/storybook/decorators/d1/scenarios/unknown")
        assertEquals(HttpStatusCode.NotFound, resp.status)
    }

    @Test fun `DELETE decorator archives it`() = contractTest { client ->
        client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json); setBody(decoratorReq())
        }
        assertEquals(HttpStatusCode.Accepted, client.delete("/api/v1/storybook/decorators/d1").status)
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/storybook/decorators/d1").status)
    }

    @Test fun `DELETE already archived decorator returns 409`() = contractTest { client ->
        client.post("/api/v1/storybook/decorators") {
            contentType(ContentType.Application.Json); setBody(decoratorReq())
        }
        client.delete("/api/v1/storybook/decorators/d1")
        assertEquals(HttpStatusCode.Conflict, client.delete("/api/v1/storybook/decorators/d1").status)
    }

    // ── Responsive BC ─────────────────────────────────────────────────────────

    private fun responsiveReq() = RegisterResponsiveProfileRequest(profileKey = "mobile/phone-first")

    private suspend fun setupStoryAndResponsive(client: io.ktor.client.HttpClient): String {
        client.post("/api/v1/storybook/stories") { contentType(ContentType.Application.Json); setBody(storyReq()) }
        val resp = client.post("/api/v1/storybook/stories/s1/responsive") {
            contentType(ContentType.Application.Json); setBody(responsiveReq())
        }
        return resp.body<Map<String, String>>()["profileId"]!!
    }

    @Test fun `POST responsive for story returns 201 with profileId`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") { contentType(ContentType.Application.Json); setBody(storyReq()) }
        val resp = client.post("/api/v1/storybook/stories/s1/responsive") {
            contentType(ContentType.Application.Json); setBody(responsiveReq())
        }
        assertEquals(HttpStatusCode.Created, resp.status)
        assertNotNull(resp.body<Map<String, String>>()["profileId"])
    }

    @Test fun `POST responsive for unknown story returns 404`() = contractTest { client ->
        val resp = client.post("/api/v1/storybook/stories/unknown/responsive") {
            contentType(ContentType.Application.Json); setBody(responsiveReq())
        }
        assertEquals(HttpStatusCode.NotFound, resp.status)
    }

    @Test fun `POST responsive for archived story returns 409`() = contractTest { client ->
        client.post("/api/v1/storybook/stories") { contentType(ContentType.Application.Json); setBody(storyReq()) }
        client.delete("/api/v1/storybook/stories/s1")
        val resp = client.post("/api/v1/storybook/stories/s1/responsive") {
            contentType(ContentType.Application.Json); setBody(responsiveReq())
        }
        assertEquals(HttpStatusCode.Conflict, resp.status)
    }

    @Test fun `POST form-factor adds to profile`() = contractTest { client ->
        val pid = setupStoryAndResponsive(client)
        val resp = client.post("/api/v1/storybook/responsive/$pid/form-factors") {
            contentType(ContentType.Application.Json); setBody(AddFormFactorRequest("PHONE"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val profile = client.get("/api/v1/storybook/responsive/$pid").body<ResponsiveProfileResponse>()
        assertTrue("PHONE" in profile.formFactors)
    }

    @Test fun `POST invalid form-factor returns 400`() = contractTest { client ->
        val pid = setupStoryAndResponsive(client)
        val resp = client.post("/api/v1/storybook/responsive/$pid/form-factors") {
            contentType(ContentType.Application.Json); setBody(AddFormFactorRequest("INVALID"))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test fun `PUT width-classes sets classes on profile`() = contractTest { client ->
        val pid = setupStoryAndResponsive(client)
        val resp = client.put("/api/v1/storybook/responsive/$pid/width-classes") {
            contentType(ContentType.Application.Json); setBody(DefineWidthClassesRequest(listOf("COMPACT", "MEDIUM")))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val profile = client.get("/api/v1/storybook/responsive/$pid").body<ResponsiveProfileResponse>()
        assertTrue("COMPACT" in profile.widthClasses)
        assertTrue("MEDIUM" in profile.widthClasses)
    }

    @Test fun `PUT navigation sets pattern for context`() = contractTest { client ->
        val pid = setupStoryAndResponsive(client)
        val resp = client.put("/api/v1/storybook/responsive/$pid/navigation") {
            contentType(ContentType.Application.Json)
            setBody(SetNavigationPatternRequest(context = "PHONE", navigationPattern = "BOTTOM_BAR"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val profile = client.get("/api/v1/storybook/responsive/$pid").body<ResponsiveProfileResponse>()
        assertEquals("BOTTOM_BAR", profile.navigationPatterns["PHONE"])
    }

    @Test fun `PUT density sets density for context`() = contractTest { client ->
        val pid = setupStoryAndResponsive(client)
        val resp = client.put("/api/v1/storybook/responsive/$pid/density") {
            contentType(ContentType.Application.Json)
            setBody(SetDensityProfileRequest(context = "PHONE", densityProfile = "COMPACT"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val profile = client.get("/api/v1/storybook/responsive/$pid").body<ResponsiveProfileResponse>()
        assertEquals("COMPACT", profile.densityProfiles["PHONE"])
    }

    @Test fun `PUT expectation links expectation ref`() = contractTest { client ->
        val pid = setupStoryAndResponsive(client)
        val resp = client.put("/api/v1/storybook/responsive/$pid/expectation") {
            contentType(ContentType.Application.Json)
            setBody(LinkExpectationRequest(expectationRef = "specs.phone.portrait"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val profile = client.get("/api/v1/storybook/responsive/$pid").body<ResponsiveProfileResponse>()
        assertTrue("specs.phone.portrait" in profile.expectationRefs)
    }

    @Test fun `PUT expectation with blank ref returns 400`() = contractTest { client ->
        val pid = setupStoryAndResponsive(client)
        val resp = client.put("/api/v1/storybook/responsive/$pid/expectation") {
            contentType(ContentType.Application.Json)
            setBody(LinkExpectationRequest(expectationRef = ""))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test fun `POST layout-rule adds rule ref`() = contractTest { client ->
        val pid = setupStoryAndResponsive(client)
        val resp = client.post("/api/v1/storybook/responsive/$pid/layout-rules") {
            contentType(ContentType.Application.Json); setBody(AddLayoutRuleRequest("rules.adaptive-grid"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val profile = client.get("/api/v1/storybook/responsive/$pid").body<ResponsiveProfileResponse>()
        assertTrue("rules.adaptive-grid" in profile.layoutRules)
    }

    @Test fun `GET story responsive profiles returns list`() = contractTest { client ->
        setupStoryAndResponsive(client)
        val resp = client.get("/api/v1/storybook/stories/s1/responsive")
        assertEquals(HttpStatusCode.OK, resp.status)
        val list = resp.body<List<ResponsiveProfileResponse>>()
        assertEquals(1, list.size)
        assertEquals("mobile/phone-first", list.first().profileKey)
    }

    @Test fun `GET responsive profile by id returns profile`() = contractTest { client ->
        val pid = setupStoryAndResponsive(client)
        val resp = client.get("/api/v1/storybook/responsive/$pid")
        assertEquals(HttpStatusCode.OK, resp.status)
        val profile = resp.body<ResponsiveProfileResponse>()
        assertEquals("mobile/phone-first", profile.profileKey)
        assertEquals("s1", profile.storyId)
        assertEquals("ACTIVE", profile.lifecycle)
    }

    @Test fun `GET unknown responsive profile returns 404`() = contractTest { client ->
        val resp = client.get("/api/v1/storybook/responsive/unknown-id")
        assertEquals(HttpStatusCode.NotFound, resp.status)
    }

    // ── Phases BC ─────────────────────────────────────────────────────────────

    private suspend fun setupPhase(
        client: io.ktor.client.HttpClient,
        id: String = "ph1",
        key: String = "alpha",
    ): String {
        client.post("/api/v1/storybook/phases") {
            contentType(ContentType.Application.Json)
            setBody(RegisterPhaseRequest(phaseId = id, phaseKey = key, phaseName = "Alpha Phase", phaseOrder = 1))
        }
        return id
    }

    @Test fun `POST phases creates phase and returns 201`() = contractTest { client ->
        val resp = client.post("/api/v1/storybook/phases") {
            contentType(ContentType.Application.Json)
            setBody(RegisterPhaseRequest(phaseId = "ph1", phaseKey = "alpha", phaseName = "Alpha Phase", phaseOrder = 1))
        }
        assertEquals(HttpStatusCode.Created, resp.status)
        val phase = resp.body<PhaseResponse>()
        assertEquals("ph1", phase.phaseId)
        assertEquals("alpha", phase.phaseKey)
        assertEquals("PLANNED", phase.lifecycle)
    }

    @Test fun `POST phases returns 409 on duplicate id`() = contractTest { client ->
        setupPhase(client)
        val resp = client.post("/api/v1/storybook/phases") {
            contentType(ContentType.Application.Json)
            setBody(RegisterPhaseRequest(phaseId = "ph1", phaseKey = "beta", phaseName = "Beta", phaseOrder = 2))
        }
        assertEquals(HttpStatusCode.Conflict, resp.status)
    }

    @Test fun `POST phases returns 409 on duplicate key`() = contractTest { client ->
        setupPhase(client, id = "ph1", key = "alpha")
        val resp = client.post("/api/v1/storybook/phases") {
            contentType(ContentType.Application.Json)
            setBody(RegisterPhaseRequest(phaseId = "ph2", phaseKey = "alpha", phaseName = "Alpha 2", phaseOrder = 2))
        }
        assertEquals(HttpStatusCode.Conflict, resp.status)
    }

    @Test fun `POST phases returns 400 for blank fields`() = contractTest { client ->
        val resp = client.post("/api/v1/storybook/phases") {
            contentType(ContentType.Application.Json)
            setBody(RegisterPhaseRequest(phaseId = "", phaseKey = "key", phaseName = "Name", phaseOrder = 1))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test fun `POST phase capabilities adds capability (BR-3)`() = contractTest { client ->
        val pid = setupPhase(client)
        val resp = client.post("/api/v1/storybook/phases/$pid/capabilities") {
            contentType(ContentType.Application.Json)
            setBody(AddCapabilityRequest(capabilityKey = "auth", title = "Auth flow"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val phase = client.get("/api/v1/storybook/phases/$pid").body<PhaseResponse>()
        assertTrue("auth" in phase.capabilities)
    }

    @Test fun `POST activate transitions PLANNED to ACTIVE (BR-4)`() = contractTest { client ->
        val pid = setupPhase(client)
        val resp = client.post("/api/v1/storybook/phases/$pid/activate") {
            contentType(ContentType.Application.Json); setBody("{}")
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val phase = client.get("/api/v1/storybook/phases/$pid").body<PhaseResponse>()
        assertEquals("ACTIVE", phase.lifecycle)
    }

    @Test fun `POST activate on non-PLANNED phase returns 409 (BR-4)`() = contractTest { client ->
        val pid = setupPhase(client)
        client.post("/api/v1/storybook/phases/$pid/activate") {
            contentType(ContentType.Application.Json); setBody("{}")
        }
        val resp = client.post("/api/v1/storybook/phases/$pid/activate") {
            contentType(ContentType.Application.Json); setBody("{}")
        }
        assertEquals(HttpStatusCode.Conflict, resp.status)
    }

    @Test fun `POST satisfy marks phase SATISFIED (BR-5)`() = contractTest { client ->
        val pid = setupPhase(client)
        client.post("/api/v1/storybook/phases/$pid/activate") { contentType(ContentType.Application.Json); setBody("{}") }
        val resp = client.post("/api/v1/storybook/phases/$pid/satisfy") { contentType(ContentType.Application.Json); setBody("{}") }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        assertEquals("SATISFIED", client.get("/api/v1/storybook/phases/$pid").body<PhaseResponse>().lifecycle)
    }

    @Test fun `POST supersede marks phase SUPERSEDED (BR-6)`() = contractTest { client ->
        val pid = setupPhase(client)
        val resp = client.post("/api/v1/storybook/phases/$pid/supersede") { contentType(ContentType.Application.Json); setBody("{}") }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        assertEquals("SUPERSEDED", client.get("/api/v1/storybook/phases/$pid").body<PhaseResponse>().lifecycle)
    }

    @Test fun `GET phases returns all phases`() = contractTest { client ->
        setupPhase(client, id = "ph1", key = "alpha")
        setupPhase(client, id = "ph2", key = "beta")
        val phases = client.get("/api/v1/storybook/phases").body<List<PhaseResponse>>()
        assertEquals(2, phases.size)
    }

    @Test fun `GET phase by id returns phase`() = contractTest { client ->
        val pid = setupPhase(client)
        val phase = client.get("/api/v1/storybook/phases/$pid").body<PhaseResponse>()
        assertEquals("alpha", phase.phaseKey)
        assertEquals("Alpha Phase", phase.phaseName)
    }

    @Test fun `GET unknown phase returns 404`() = contractTest { client ->
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/storybook/phases/unknown").status)
    }

    // ── Governance BC ─────────────────────────────────────────────────────────

    private suspend fun setupPolicy(
        client: io.ktor.client.HttpClient,
        id: String = "gov1",
        key: String = "access-policy",
    ): String {
        client.post("/api/v1/storybook/governance/policies") {
            contentType(ContentType.Application.Json)
            setBody(RegisterGovernancePolicyRequest(policyId = id, policyKey = key, title = "Access Policy"))
        }
        return id
    }

    @Test fun `POST governance policies creates policy and returns 201`() = contractTest { client ->
        val resp = client.post("/api/v1/storybook/governance/policies") {
            contentType(ContentType.Application.Json)
            setBody(RegisterGovernancePolicyRequest(policyId = "gov1", policyKey = "access-policy", title = "Access Policy"))
        }
        assertEquals(HttpStatusCode.Created, resp.status)
        val policy = resp.body<GovernancePolicyResponse>()
        assertEquals("gov1", policy.policyId)
        assertEquals("DRAFT", policy.lifecycle)
    }

    @Test fun `POST governance policies returns 409 on duplicate id`() = contractTest { client ->
        setupPolicy(client)
        val resp = client.post("/api/v1/storybook/governance/policies") {
            contentType(ContentType.Application.Json)
            setBody(RegisterGovernancePolicyRequest(policyId = "gov1", policyKey = "content-policy", title = "Content Policy"))
        }
        assertEquals(HttpStatusCode.Conflict, resp.status)
    }

    @Test fun `POST governance policies returns 409 on duplicate key`() = contractTest { client ->
        setupPolicy(client)
        val resp = client.post("/api/v1/storybook/governance/policies") {
            contentType(ContentType.Application.Json)
            setBody(RegisterGovernancePolicyRequest(policyId = "gov2", policyKey = "access-policy", title = "Access 2"))
        }
        assertEquals(HttpStatusCode.Conflict, resp.status)
    }

    @Test fun `POST quality-gate attaches gate (BR-3)`() = contractTest { client ->
        val id = setupPolicy(client)
        val resp = client.post("/api/v1/storybook/governance/policies/$id/quality-gates") {
            contentType(ContentType.Application.Json)
            setBody(AttachQualityGateRequest(gateKey = "ux-review", description = "UX sign-off"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val policy = client.get("/api/v1/storybook/governance/policies/$id").body<GovernancePolicyResponse>()
        assertTrue("ux-review" in policy.qualityGates)
    }

    @Test fun `POST owners assigns owner (BR-4)`() = contractTest { client ->
        val id = setupPolicy(client)
        val resp = client.post("/api/v1/storybook/governance/policies/$id/owners") {
            contentType(ContentType.Application.Json)
            setBody(AssignOwnerRequest(ownerRef = "team-platform"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val policy = client.get("/api/v1/storybook/governance/policies/$id").body<GovernancePolicyResponse>()
        assertEquals("team-platform", policy.owner)
    }

    @Test fun `POST evidence links evidence (BR-5)`() = contractTest { client ->
        val id = setupPolicy(client)
        val resp = client.post("/api/v1/storybook/governance/policies/$id/evidence") {
            contentType(ContentType.Application.Json)
            setBody(LinkEvidenceRequest(evidenceRef = "report-42", evidenceType = "test-report"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val policy = client.get("/api/v1/storybook/governance/policies/$id").body<GovernancePolicyResponse>()
        assertTrue("report-42" in policy.evidenceRefs)
    }

    @Test fun `POST decisions records APPROVED decision (BR-6)`() = contractTest { client ->
        val id = setupPolicy(client)
        val resp = client.post("/api/v1/storybook/governance/policies/$id/decisions") {
            contentType(ContentType.Application.Json)
            setBody(RecordGovernanceDecisionRequest(decision = "APPROVED"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
        val policy = client.get("/api/v1/storybook/governance/policies/$id").body<GovernancePolicyResponse>()
        assertEquals("APPROVED", policy.lastDecision)
    }

    @Test fun `POST decisions REJECTED without rationale returns 400 (BR-6)`() = contractTest { client ->
        val id = setupPolicy(client)
        val resp = client.post("/api/v1/storybook/governance/policies/$id/decisions") {
            contentType(ContentType.Application.Json)
            setBody(RecordGovernanceDecisionRequest(decision = "REJECTED"))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test fun `POST lifecycle records lifecycle governance (BR-7)`() = contractTest { client ->
        val id = setupPolicy(client)
        val resp = client.post("/api/v1/storybook/governance/policies/$id/lifecycle") {
            contentType(ContentType.Application.Json)
            setBody(GovernLifecycleRequest(targetRef = "story:sb-001", targetType = "story", newLifecycle = "ACTIVE"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
    }

    @Test fun `GET governance policies returns list`() = contractTest { client ->
        setupPolicy(client, id = "gov1", key = "access-policy")
        setupPolicy(client, id = "gov2", key = "content-policy")
        val policies = client.get("/api/v1/storybook/governance/policies").body<List<GovernancePolicyResponse>>()
        assertEquals(2, policies.size)
    }

    @Test fun `GET governance policy by id returns policy`() = contractTest { client ->
        val id = setupPolicy(client)
        val policy = client.get("/api/v1/storybook/governance/policies/$id").body<GovernancePolicyResponse>()
        assertEquals("access-policy", policy.policyKey)
        assertEquals("Access Policy", policy.title)
    }

    @Test fun `GET unknown governance policy returns 404`() = contractTest { client ->
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/storybook/governance/policies/unknown").status)
    }
}
