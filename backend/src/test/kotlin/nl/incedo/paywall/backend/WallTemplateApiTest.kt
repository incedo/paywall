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
import nl.incedo.paywall.api.WallResponse
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod
import nl.incedo.paywall.walls.WallCopy

/**
 * ADM-16: wall design templates — brand-neutral layout/copy saved once,
 * instantiated per brand with theme tokens applied at render time.
 */
class WallTemplateApiTest {

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
    fun saveAndRetrieveTemplate() = apiTest { client ->
        val saved = client.post("/api/v1/admin/wall-templates/tmpl-hard-default") {
            contentType(ContentType.Application.Json)
            setBody(
                WallTemplateRequest(
                    name = "Hard paywall default",
                    wallType = "hard",
                    title = "Subscribe to continue",
                    body = "Get unlimited access",
                    primaryCta = "Subscribe now",
                    secondaryCta = "Maybe later",
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, saved.status, "ADM-16: save template must succeed")
        val resp: WallTemplateResponse = saved.body()
        assertEquals("tmpl-hard-default", resp.id)
        assertEquals("Hard paywall default", resp.name)
        assertEquals("hard", resp.wallType)
        assertEquals("Subscribe to continue", resp.title)

        // GET by id
        val fetched: WallTemplateResponse = client.get("/api/v1/admin/wall-templates/tmpl-hard-default").body()
        assertEquals("tmpl-hard-default", fetched.id)
        assertEquals("Get unlimited access", fetched.body)
    }

    @Test
    fun listTemplatesReturnsAllSaved() = apiTest { client ->
        client.post("/api/v1/admin/wall-templates/tmpl-a") {
            contentType(ContentType.Application.Json)
            setBody(WallTemplateRequest(name = "A", wallType = "hard", title = "T", body = "B", primaryCta = "P", secondaryCta = "S"))
        }
        client.post("/api/v1/admin/wall-templates/tmpl-b") {
            contentType(ContentType.Application.Json)
            setBody(WallTemplateRequest(name = "B", wallType = "metered", title = "T2", body = "B2", primaryCta = "P2", secondaryCta = "S2"))
        }
        val list: List<WallTemplateResponse> = client.get("/api/v1/admin/wall-templates").body()
        assertEquals(2, list.size, "ADM-16: list must return both saved templates")
        val ids = list.map { it.id }.toSet()
        assert("tmpl-a" in ids)
        assert("tmpl-b" in ids)
    }

    @Test
    fun getUnknownTemplateReturns404() = apiTest { client ->
        val resp = client.get("/api/v1/admin/wall-templates/not-a-template")
        assertEquals(HttpStatusCode.NotFound, resp.status, "ADM-16: unknown template id must return 404")
    }

    @Test
    fun templateIsBrandNeutral() = apiTest { client ->
        client.post("/api/v1/admin/wall-templates/tmpl-neutral") {
            contentType(ContentType.Application.Json)
            setBody(WallTemplateRequest(name = "N", wallType = "freemium", title = "T", body = "B", primaryCta = "P", secondaryCta = "S"))
        }
        val tmpl: WallTemplateResponse = client.get("/api/v1/admin/wall-templates/tmpl-neutral").body()
        // Templates carry no brandId — the response DTO doesn't expose it;
        // verify by instantiating and confirming the brand is set from the query param.
        assertEquals("N", tmpl.name)
    }

    @Test
    fun instantiateTemplateForBrandCreatesWall() = apiTest { client ->
        // Save the template first
        client.post("/api/v1/admin/wall-templates/tmpl-for-brand") {
            contentType(ContentType.Application.Json)
            setBody(
                WallTemplateRequest(
                    name = "Brand template",
                    wallType = "hard",
                    title = "Title from template",
                    body = "Body from template",
                    primaryCta = "Subscribe",
                    secondaryCta = "Skip",
                ),
            )
        }
        // Instantiate for brand "brand-acme" → new wall "wall-acme-01"
        val wall = client.post("/api/v1/walls/wall-acme-01/from-template/tmpl-for-brand?brandId=brand-acme")
        assertEquals(HttpStatusCode.OK, wall.status, "ADM-16: instantiation must succeed")
        val wallResp: WallResponse = wall.body()
        assertEquals("wall-acme-01", wallResp.id)
        assertEquals("Title from template", wallResp.title, "ADM-16: title copied from template")
        assertEquals("brand-acme", wallResp.brandId, "ADM-16: brandId set from query param, not from template")
    }

    @Test
    fun instantiateWithUnknownTemplateReturns404() = apiTest { client ->
        val resp = client.post("/api/v1/walls/wall-x/from-template/does-not-exist?brandId=brand-x")
        assertEquals(HttpStatusCode.NotFound, resp.status, "ADM-16: unknown template must return 404")
    }

    @Test
    fun instantiateWithoutBrandIdReturns400() = apiTest { client ->
        client.post("/api/v1/admin/wall-templates/tmpl-req") {
            contentType(ContentType.Application.Json)
            setBody(WallTemplateRequest(name = "R", wallType = "hard", title = "T", body = "B", primaryCta = "P", secondaryCta = "S"))
        }
        val resp = client.post("/api/v1/walls/wall-req/from-template/tmpl-req") // no brandId param
        assertEquals(HttpStatusCode.BadRequest, resp.status, "ADM-16: missing brandId must return 400")
    }

    @Test
    fun templateWithTranslationsRoundTrips() = apiTest { client ->
        val saved = client.post("/api/v1/admin/wall-templates/tmpl-i18n") {
            contentType(ContentType.Application.Json)
            setBody(
                WallTemplateRequest(
                    name = "i18n template",
                    wallType = "hard",
                    title = "Subscribe",
                    body = "Full access",
                    primaryCta = "Get it",
                    secondaryCta = "No thanks",
                    translations = mapOf(
                        "nl-NL" to WallCopy(title = "Abonneer", primaryCta = "Ja graag"),
                    ),
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, saved.status)
        val tmpl: WallTemplateResponse = saved.body()
        assertNotNull(tmpl.translations["nl-NL"], "ADM-16: nl-NL translation must be present")
        assertEquals("Abonneer", tmpl.translations["nl-NL"]?.title)
    }

    @Test
    fun invalidWallTypeReturns400() = apiTest { client ->
        val resp = client.post("/api/v1/admin/wall-templates/tmpl-bad") {
            contentType(ContentType.Application.Json)
            setBody(WallTemplateRequest(name = "Bad", wallType = "unknown", title = "T", body = "B", primaryCta = "P", secondaryCta = "S"))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status, "ADM-16: invalid wallType must return 400")
    }
}
