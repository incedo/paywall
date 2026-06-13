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
import kotlin.test.assertTrue
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/** ADM-10: multi-brand entity with theme tokens, domain, and locale. */
class BrandApiTest {

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
    fun createBrandReturnsCreated() = apiTest { client ->
        val resp = client.post("/api/v1/admin/brands") {
            contentType(ContentType.Application.Json)
            setBody(CreateBrandRequest(brandId = "brand-nl", name = "NL Edition", domain = "magazine.nl", locale = "nl-NL"))
        }
        assertEquals(HttpStatusCode.Created, resp.status)
        val body = resp.body<Map<String, String>>()
        assertEquals("brand-nl", body["brandId"])
    }

    @Test
    fun getBrandReturnsState() = apiTest { client ->
        client.post("/api/v1/admin/brands") {
            contentType(ContentType.Application.Json)
            setBody(CreateBrandRequest(brandId = "brand-be", name = "BE Edition", domain = "magazine.be", locale = "nl-BE", themeJson = """{"primary":"#ff0000"}"""))
        }

        val resp = client.get("/api/v1/admin/brands/brand-be")
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = resp.body<BrandResponse>()
        assertEquals("brand-be", body.brandId)
        assertEquals("BE Edition", body.name)
        assertEquals("magazine.be", body.domain)
        assertEquals("nl-BE", body.locale)
        assertEquals("""{"primary":"#ff0000"}""", body.themeJson)
    }

    @Test
    fun getBrandNotFoundReturns404() = apiTest { client ->
        val resp = client.get("/api/v1/admin/brands/nonexistent")
        assertEquals(HttpStatusCode.NotFound, resp.status)
    }

    @Test
    fun updateThemeAppliesNewTokens() = apiTest { client ->
        client.post("/api/v1/admin/brands") {
            contentType(ContentType.Application.Json)
            setBody(CreateBrandRequest(brandId = "brand-x", name = "X Brand", domain = "x.nl"))
        }

        client.post("/api/v1/admin/brands/brand-x/theme") {
            contentType(ContentType.Application.Json)
            setBody(UpdateBrandThemeRequest(themeJson = """{"primary":"#blue","logo":"x.svg"}"""))
        }

        val resp = client.get("/api/v1/admin/brands/brand-x")
        val body = resp.body<BrandResponse>()
        assertEquals("""{"primary":"#blue","logo":"x.svg"}""", body.themeJson)
    }

    @Test
    fun listBrandsReturnsAll() = apiTest { client ->
        client.post("/api/v1/admin/brands") {
            contentType(ContentType.Application.Json)
            setBody(CreateBrandRequest(brandId = "b1", name = "Brand 1", domain = "b1.nl"))
        }
        client.post("/api/v1/admin/brands") {
            contentType(ContentType.Application.Json)
            setBody(CreateBrandRequest(brandId = "b2", name = "Brand 2", domain = "b2.nl"))
        }

        val resp = client.get("/api/v1/admin/brands")
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = resp.body<List<BrandResponse>>()
        assertEquals(2, body.size)
        assertTrue(body.any { it.brandId == "b1" })
        assertTrue(body.any { it.brandId == "b2" })
    }

    @Test
    fun createBrandMissingFieldsReturnsBadRequest() = apiTest { client ->
        val resp = client.post("/api/v1/admin/brands") {
            contentType(ContentType.Application.Json)
            setBody(CreateBrandRequest(brandId = "", name = "Name", domain = "x.nl"))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status)
    }

    @Test
    fun wallConfigAcceptsBrandId() = apiTest { client ->
        client.post("/api/v1/admin/brands") {
            contentType(ContentType.Application.Json)
            setBody(CreateBrandRequest(brandId = "brand-nl", name = "NL", domain = "mag.nl"))
        }
        // Create a wall associated with the brand
        val wallResp = client.post("/api/v1/walls/wall-1") {
            contentType(ContentType.Application.Json)
            setBody(nl.incedo.paywall.api.SaveWallRequest(
                name = "NL Paywall",
                wallType = "hard",
                title = "Subscribe",
                body = "Read everything",
                primaryCta = "Subscribe now",
                secondaryCta = "Log in",
                actor = "test",
                brandId = "brand-nl",
            ))
        }
        assertEquals(HttpStatusCode.OK, wallResp.status)
        val body = wallResp.body<nl.incedo.paywall.api.WallResponse>()
        assertEquals("brand-nl", body.brandId)
    }
}
