package nl.incedo.paywall.backend

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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import nl.incedo.paywall.api.PartnerIpRangeRequest
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * IPW-03: CIDR validation at partner IP range config load.
 * Invalid and overlapping CIDRs must be rejected; valid non-overlapping CIDRs accepted.
 */
class CidrValidationTest {

    // ── Unit tests for CidrValidator ──────────────────────────────────────────

    @Test
    fun validIpv4CidrAccepted() {
        assertNull(CidrValidator.validate("10.0.0.0/8"))
        assertNull(CidrValidator.validate("192.168.1.0/24"))
        assertNull(CidrValidator.validate("0.0.0.0/0"))
        assertNull(CidrValidator.validate("255.255.255.255/32"))
    }

    @Test
    fun validIpv6CidrAccepted() {
        assertNull(CidrValidator.validate("2001:db8::/32"))
        assertNull(CidrValidator.validate("::/0"))
    }

    @Test
    fun missingPrefixLengthRejected() {
        assertNotNull(CidrValidator.validate("192.168.1.0"))
    }

    @Test
    fun invalidIpAddressRejected() {
        assertNotNull(CidrValidator.validate("999.0.0.0/8"))
        assertNotNull(CidrValidator.validate("not-an-ip/24"))
    }

    @Test
    fun prefixLengthOutOfRangeRejected() {
        assertNotNull(CidrValidator.validate("10.0.0.0/33"))
        assertNotNull(CidrValidator.validate("10.0.0.0/-1"))
    }

    @Test
    fun hostBitsSetRejected() {
        // 10.1.2.3/8 has host bits set (should be 10.0.0.0/8)
        assertNotNull(CidrValidator.validate("10.1.2.3/8"))
    }

    @Test
    fun overlapWithSubnetDetected() {
        // 10.1.0.0/16 is a subnet of 10.0.0.0/8 — they overlap
        assertNotNull(CidrValidator.overlaps("10.1.0.0/16", listOf("10.0.0.0/8")))
    }

    @Test
    fun overlapWithSupernet() {
        // 10.0.0.0/8 is a supernet of 10.1.0.0/16
        assertNotNull(CidrValidator.overlaps("10.0.0.0/8", listOf("10.1.0.0/16")))
    }

    @Test
    fun duplicateCidrDetected() {
        assertNotNull(CidrValidator.overlaps("192.168.1.0/24", listOf("192.168.1.0/24")))
    }

    @Test
    fun nonOverlappingCidrsAccepted() {
        assertNull(CidrValidator.overlaps("10.0.0.0/8", listOf("192.168.0.0/16")))
        assertNull(CidrValidator.overlaps("172.16.0.0/12", listOf("10.0.0.0/8", "192.168.0.0/16")))
    }

    @Test
    fun adjacentCidrsDoNotOverlap() {
        // 10.0.0.0/25 and 10.0.0.128/25 are adjacent, not overlapping
        assertNull(CidrValidator.overlaps("10.0.0.0/25", listOf("10.0.0.128/25")))
    }

    // ── Integration tests for the API endpoint ────────────────────────────────

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
    fun validCidrIsAccepted() = apiTest { client ->
        val resp = client.post("/api/v1/admin/partners/partner-test/ip-ranges") {
            contentType(ContentType.Application.Json)
            setBody(PartnerIpRangeRequest(cidr = "10.0.0.0/8"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status)
    }

    @Test
    fun invalidCidrIsRejected() = apiTest { client ->
        val resp = client.post("/api/v1/admin/partners/partner-test/ip-ranges") {
            contentType(ContentType.Application.Json)
            setBody(PartnerIpRangeRequest(cidr = "10.1.2.3/8"))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status, "IPW-03: host-bits-set CIDR must be rejected")
    }

    @Test
    fun overlappingCidrIsRejected() = apiTest { client ->
        client.post("/api/v1/admin/partners/partner-test/ip-ranges") {
            contentType(ContentType.Application.Json)
            setBody(PartnerIpRangeRequest(cidr = "10.0.0.0/8"))
        }
        val resp = client.post("/api/v1/admin/partners/partner-test/ip-ranges") {
            contentType(ContentType.Application.Json)
            setBody(PartnerIpRangeRequest(cidr = "10.1.0.0/16"))
        }
        assertEquals(HttpStatusCode.Conflict, resp.status, "IPW-03: overlapping CIDR must be rejected")
    }

    @Test
    fun deactivateCidrBypassesOverlapCheck() = apiTest { client ->
        // Deactivating a range (active=false) doesn't need overlap check.
        val resp = client.post("/api/v1/admin/partners/partner-test/ip-ranges") {
            contentType(ContentType.Application.Json)
            setBody(PartnerIpRangeRequest(cidr = "10.1.0.0/16", active = false))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status, "deactivation should not trigger overlap check")
    }
}
