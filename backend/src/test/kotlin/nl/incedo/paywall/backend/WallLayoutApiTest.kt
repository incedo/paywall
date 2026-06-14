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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import nl.incedo.paywall.api.SaveWallRequest
import nl.incedo.paywall.api.WallResponse
import nl.incedo.paywall.api.WallVersionSummary
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod
import nl.incedo.paywall.walls.BodyCopy
import nl.incedo.paywall.walls.CtaButton
import nl.incedo.paywall.walls.CtaRole
import nl.incedo.paywall.walls.Headline
import nl.incedo.paywall.walls.LegalText
import nl.incedo.paywall.walls.WallLayout

/**
 * VWE-03: server-side block-layout save and validation (VWE-02 enforcement).
 */
class WallLayoutApiTest {

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
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

    private fun flatDraft() = SaveWallRequest(
        name = "Wall 1",
        wallType = "metered",
        title = "Title",
        body = "Body",
        primaryCta = "Subscribe",
        secondaryCta = "Log in",
    )

    private fun validLayout() = WallLayout(listOf(
        Headline(id = "h", text = "Subscribe"),
        BodyCopy(id = "b", text = "Full access."),
        CtaButton(id = "cta", label = "Subscribe now", role = CtaRole.PRIMARY),
        LegalText(id = "l", text = "Cancel anytime."),
    ))

    private suspend fun createWall(client: io.ktor.client.HttpClient): WallResponse {
        val resp = client.post("/api/v1/walls/w-1") {
            contentType(ContentType.Application.Json)
            setBody(flatDraft())
        }
        return resp.body()
    }

    @Test
    fun saveValidLayoutReturns200WithLayout() = apiTest { client ->
        createWall(client)
        val resp = client.post("/api/v1/walls/w-1") {
            contentType(ContentType.Application.Json)
            setBody(SaveWallRequest(
                name = "Wall 1", wallType = "metered", title = "Title",
                body = "Body", primaryCta = "Subscribe", secondaryCta = "Log in",
                layout = validLayout(),
            ))
        }
        assertEquals(HttpStatusCode.OK, resp.status, "Valid layout should be accepted")
        val wall: WallResponse = resp.body()
        assertNotNull(wall.layout, "Response should include the saved layout")
        assertEquals(4, wall.layout!!.blocks.size)
    }

    @Test
    fun saveLayoutBumpsVersion() = apiTest { client ->
        createWall(client)
        val resp = client.post("/api/v1/walls/w-1") {
            contentType(ContentType.Application.Json)
            setBody(SaveWallRequest(
                name = "Wall 1", wallType = "metered", title = "T", body = "B",
                primaryCta = "S", secondaryCta = "L", layout = validLayout(),
            ))
        }
        val wall: WallResponse = resp.body()
        assertEquals(2, wall.version, "WallLayoutChanged should increment version")
    }

    @Test
    fun saveLayoutAppearsInHistory() = apiTest { client ->
        createWall(client)
        client.post("/api/v1/walls/w-1") {
            contentType(ContentType.Application.Json)
            setBody(SaveWallRequest(
                name = "Wall 1", wallType = "metered", title = "T", body = "B",
                primaryCta = "S", secondaryCta = "L", layout = validLayout(),
            ))
        }
        val history: List<WallVersionSummary> = client.get("/api/v1/walls/w-1/history").body()
        assertEquals(2, history.size, "History should have two entries: create + layout-save")
    }

    @Test
    fun saveInvalidLayoutReturns422() = apiTest { client ->
        createWall(client)
        val badLayout = WallLayout(listOf(
            Headline(id = "h", text = "Title"),
            // Missing PRIMARY CtaButton — violates VWE-02
        ))
        val resp = client.post("/api/v1/walls/w-1") {
            contentType(ContentType.Application.Json)
            setBody(SaveWallRequest(
                name = "Wall 1", wallType = "metered", title = "T", body = "B",
                primaryCta = "S", secondaryCta = "L", layout = badLayout,
            ))
        }
        assertEquals(HttpStatusCode.UnprocessableEntity, resp.status,
            "VWE-02: invalid layout must return 422")
    }

    @Test
    fun saveLayoutForNonExistentWallReturns404() = apiTest { client ->
        val resp = client.post("/api/v1/walls/nonexistent") {
            contentType(ContentType.Application.Json)
            setBody(SaveWallRequest(
                name = "W", wallType = "metered", title = "T", body = "B",
                primaryCta = "S", secondaryCta = "L", layout = validLayout(),
            ))
        }
        assertEquals(HttpStatusCode.NotFound, resp.status,
            "Layout save requires the wall to already exist")
    }

    @Test
    fun flatSaveAfterLayoutSaveClearsLayout() = apiTest { client ->
        createWall(client)
        // First: save a block layout
        client.post("/api/v1/walls/w-1") {
            contentType(ContentType.Application.Json)
            setBody(SaveWallRequest(
                name = "Wall 1", wallType = "metered", title = "T", body = "B",
                primaryCta = "S", secondaryCta = "L", layout = validLayout(),
            ))
        }
        // Then: save flat config (no layout) — should clear the layout
        val resp = client.post("/api/v1/walls/w-1") {
            contentType(ContentType.Application.Json)
            setBody(flatDraft())
        }
        val wall: WallResponse = resp.body()
        assertNull(wall.layout, "Flat-config save should clear the block layout")
    }
}
