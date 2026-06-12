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
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/** Wall designer API (ADM-01/02/06/11). */
class WallApiTest {

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

    private fun draft(name: String = "Metered limit — invoices") = SaveWallRequest(
        name = name,
        wallType = "metered",
        title = "You've reached this month's free limit",
        body = "Pro includes unlimited documents.",
        primaryCta = "Upgrade to Pro",
        secondaryCta = "Compare plans",
        channels = setOf("web", "email"),
        actor = "m.visser",
    )

    private suspend fun save(client: io.ktor.client.HttpClient, id: String, request: SaveWallRequest) =
        client.post("/api/v1/walls/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

    @Test
    fun createListUpdatePublishFlow() = apiTest { client ->
        // Create draft
        val created: WallResponse = save(client, "wall-1", draft()).body()
        assertEquals("draft", created.status)
        assertEquals(1, created.version)

        // Appears in the overview
        val list: List<WallResponse> = client.get("/api/v1/walls").body()
        assertEquals(listOf("wall-1"), list.map { it.id })

        // Edit bumps the version, stays draft (ADM-06)
        val updated: WallResponse = save(client, "wall-1", draft().copy(title = "New title", expectedVersion = 1)).body()
        assertEquals(2, updated.version)
        assertEquals("draft", updated.status)
        assertEquals("New title", updated.title)

        // Publish
        val published: WallResponse = client.post("/api/v1/walls/wall-1/publish") {
            contentType(ContentType.Application.Json)
        }.body()
        assertEquals("published", published.status)

        // Editing a published wall returns it to draft
        val reEdited: WallResponse = save(client, "wall-1", draft().copy(title = "Another title")).body()
        assertEquals("draft", reEdited.status)
        assertEquals(3, reEdited.version)
    }

    @Test
    fun staleEditorGetsVersionConflict() = apiTest { client ->
        save(client, "wall-2", draft())
        save(client, "wall-2", draft().copy(title = "Editor A's change", expectedVersion = 1))

        // Editor B still looks at version 1: the save must not silently win
        val response = save(client, "wall-2", draft().copy(title = "Editor B's change", expectedVersion = 1))
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun invalidWallTypeIsRejected() = apiTest { client ->
        val response = save(client, "wall-3", draft().copy(wallType = "registration"))
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun unknownWallIs404() = apiTest { client ->
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/walls/nope").status)
        assertEquals(
            HttpStatusCode.NotFound,
            client.post("/api/v1/walls/nope/publish") { contentType(ContentType.Application.Json) }.status,
        )
    }
}
