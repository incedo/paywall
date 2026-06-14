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
import nl.incedo.paywall.api.SaveWallRequest
import nl.incedo.paywall.api.WallResponse
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * BDD scenarios for `src/test/resources/features/walls.feature` —
 * mirrors each Gherkin scenario 1:1 (ADM-10..14, PW-*).
 */
class WallsFeatureTest {

    private fun scenario(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
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

    private fun draft(name: String = "Monthly limit", title: String = "You've reached your limit") =
        SaveWallRequest(
            name = name,
            wallType = "metered",
            title = title,
            body = "Upgrade to read more.",
            primaryCta = "Upgrade to Pro",
            secondaryCta = "Compare plans",
            channels = setOf("web"),
            actor = "m.visser",
        )

    private suspend fun save(
        client: io.ktor.client.HttpClient,
        id: String,
        req: SaveWallRequest,
    ) = client.post("/api/v1/walls/$id") {
        contentType(ContentType.Application.Json)
        setBody(req)
    }

    @Test
    fun `Scenario - Creating a draft wall`() = scenario { client ->
        // When staff saves a metered wall "Monthly limit" for the first time
        val response: WallResponse = save(client, "w-new", draft("Monthly limit")).body()
        // Then the wall is returned with status "draft" and version 1
        assertEquals("draft", response.status, "ADM-10: new wall must start as draft")
        assertEquals(1, response.version)
        // And the wall appears in the wall list
        val list: List<WallResponse> = client.get("/api/v1/walls").body()
        assertEquals(listOf("w-new"), list.map { it.id })
    }

    @Test
    fun `Scenario - Editing bumps the version and keeps the draft`() = scenario { client ->
        // Given a saved wall "Monthly limit" at version 1
        save(client, "w-edit", draft())
        // When staff edits the wall with an updated title
        val updated: WallResponse = save(
            client, "w-edit",
            draft(title = "New headline").copy(expectedVersion = 1),
        ).body()
        // Then the wall is returned with status "draft" and version 2
        assertEquals("draft", updated.status, "ADM-12: editing must keep draft status")
        assertEquals(2, updated.version, "ADM-12: version must increment")
        assertEquals("New headline", updated.title)
    }

    @Test
    fun `Scenario - Publishing changes the status to published`() = scenario { client ->
        // Given a saved wall "Monthly limit" at version 1
        save(client, "w-pub", draft())
        // When staff publishes the wall
        val published: WallResponse = client.post("/api/v1/walls/w-pub/publish") {
            contentType(ContentType.Application.Json)
        }.body()
        // Then the wall status is "published"
        assertEquals("published", published.status, "ADM-10: publish must flip status")
    }

    @Test
    fun `Scenario - Editing a published wall returns it to draft`() = scenario { client ->
        // Given a published wall "Monthly limit" at version 1
        save(client, "w-re", draft())
        client.post("/api/v1/walls/w-re/publish") { contentType(ContentType.Application.Json) }
        // When staff edits the wall
        val reDrafted: WallResponse = save(client, "w-re", draft(title = "Updated")).body()
        // Then the wall status is "draft" and the version is 2
        assertEquals("draft", reDrafted.status, "ADM-12: editing published wall returns to draft")
        assertEquals(2, reDrafted.version)
    }

    @Test
    fun `Scenario - Concurrent editor gets a conflict`() = scenario { client ->
        // Given a saved wall "Monthly limit" at version 1
        save(client, "w-con", draft())
        save(client, "w-con", draft(title = "Editor A").copy(expectedVersion = 1))
        // When the second editor tries to save with expected version 1
        val conflict = save(client, "w-con", draft(title = "Editor B").copy(expectedVersion = 1))
        // Then the save is rejected with a 409 Conflict (ADM-12)
        assertEquals(HttpStatusCode.Conflict, conflict.status, "ADM-12: stale save must be rejected")
    }

    @Test
    fun `Scenario - Unknown wall returns 404`() = scenario { client ->
        // When staff requests a wall that does not exist
        val response = client.get("/api/v1/walls/nonexistent")
        // Then the response is 404 Not Found
        assertEquals(HttpStatusCode.NotFound, response.status, "ADM-10: unknown wall must be 404")
    }

    @Test
    fun `Scenario - Rollback restores an earlier version`() = scenario { client ->
        // Given wall "RB wall" saved at version 1 with title "Original"
        save(client, "w-rb", draft(title = "Original"))
        // And the wall edited to version 2 with title "Changed"
        save(client, "w-rb", draft(title = "Changed").copy(expectedVersion = 1))
        // When staff rolls back to version 1
        val rolledBack: WallResponse = client.post("/api/v1/walls/w-rb/rollback?version=1") {
            contentType(ContentType.Application.Json)
        }.body()
        // Then the active title is "Original"
        assertEquals("Original", rolledBack.title, "ADM-13: rollback must restore old config")
        // And the wall is at version 3 as a new draft
        assertEquals(3, rolledBack.version, "ADM-13: rollback creates a new version")
        assertEquals("draft", rolledBack.status)
    }
}
