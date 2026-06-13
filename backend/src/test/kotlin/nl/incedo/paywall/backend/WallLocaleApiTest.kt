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
import nl.incedo.paywall.walls.WallCopy

/**
 * ADM-15: per-brand locale support — admin API persists and returns
 * per-locale copy overrides alongside the wall design.
 */
class WallLocaleApiTest {

    private val now = 1_750_000_000_000L

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

    @Test
    fun wallDesignWithTranslationsRoundTrips() = apiTest { client ->
        val saved = client.post("/api/v1/walls/wall-adm15") {
            contentType(ContentType.Application.Json)
            setBody(
                SaveWallRequest(
                    name = "NL wall",
                    wallType = "hard",
                    title = "Default title",
                    body = "Default body",
                    primaryCta = "Subscribe",
                    secondaryCta = "Maybe later",
                    translations = mapOf(
                        "nl-NL" to WallCopy(
                            title = "Abonneer nu",
                            primaryCta = "Ja, ik wil",
                        ),
                    ),
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, saved.status, "ADM-15: wall save with translations must succeed")
        val wall: WallResponse = saved.body()
        // translations are preserved
        assertEquals(1, wall.translations.size, "ADM-15: one locale in translations")
        val nlCopy = wall.translations["nl-NL"]!!
        assertEquals("Abonneer nu", nlCopy.title, "ADM-15: nl-NL title override preserved")
        assertEquals("Ja, ik wil", nlCopy.primaryCta, "ADM-15: nl-NL primaryCta override preserved")
        // Unset translation fields remain null
        assertEquals(null, nlCopy.body)
    }

    @Test
    fun wallDesignWithoutTranslationsRoundTripsAsEmpty() = apiTest { client ->
        val saved = client.post("/api/v1/walls/wall-adm15b") {
            contentType(ContentType.Application.Json)
            setBody(
                SaveWallRequest(
                    name = "Plain wall",
                    wallType = "metered",
                    title = "T",
                    body = "B",
                    primaryCta = "Go",
                    secondaryCta = "No",
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, saved.status)
        val wall: WallResponse = saved.body()
        assertEquals(emptyMap(), wall.translations, "ADM-15: no translations → empty map")
    }

    @Test
    fun updatedTranslationsArePersistedOnSave() = apiTest { client ->
        // Create without translations
        client.post("/api/v1/walls/wall-adm15c") {
            contentType(ContentType.Application.Json)
            setBody(SaveWallRequest(name = "W", wallType = "hard", title = "T", body = "B", primaryCta = "Go", secondaryCta = "No"))
        }
        // Update with translations
        val updated = client.post("/api/v1/walls/wall-adm15c") {
            contentType(ContentType.Application.Json)
            setBody(
                SaveWallRequest(
                    name = "W",
                    wallType = "hard",
                    title = "T",
                    body = "B",
                    primaryCta = "Go",
                    secondaryCta = "No",
                    translations = mapOf("nl-NL" to WallCopy(title = "Vertaling")),
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, updated.status)
        val wall: WallResponse = updated.body()
        assertEquals("Vertaling", wall.translations["nl-NL"]?.title, "ADM-15: update with translations persists correctly")

        // GET returns the same
        val fetched = client.get("/api/v1/walls/wall-adm15c").body<WallResponse>()
        assertEquals("Vertaling", fetched.translations["nl-NL"]?.title, "ADM-15: GET returns translations")
    }
}
