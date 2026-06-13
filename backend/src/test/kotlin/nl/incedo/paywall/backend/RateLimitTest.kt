package nl.incedo.paywall.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import nl.incedo.paywall.backend.auth.RequestRateLimiter
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * NFR-04: per-IP rate limiting on auth/checkout/integration endpoints.
 * Covers the unit-level sliding-window logic and the HTTP-level 429 integration.
 */
class RateLimitTest {

    // ── RequestRateLimiter unit tests ─────────────────────────────────────────

    @Test
    fun requestsBelowLimitAreAllowed() {
        val limiter = RequestRateLimiter(maxRequests = 5, windowMs = 60_000)
        repeat(5) { assertFalse(limiter.isExceeded("10.0.0.1:/offers"), "NFR-04: under limit must not be exceeded") }
        assertEquals(5, limiter.countFor("10.0.0.1:/offers"))
    }

    @Test
    fun requestAboveLimitIsExceeded() {
        val limiter = RequestRateLimiter(maxRequests = 3, windowMs = 60_000)
        repeat(3) { limiter.isExceeded("10.0.0.2:/offers") }
        assertTrue(limiter.isExceeded("10.0.0.2:/offers"), "NFR-04: request over limit must be exceeded")
    }

    @Test
    fun windowExpiryResetsCount() {
        var fakeNow = 1_000L
        val limiter = RequestRateLimiter(clock = { fakeNow }, maxRequests = 3, windowMs = 1_000)
        repeat(3) { limiter.isExceeded("10.0.0.3:/offers") }
        fakeNow = 3_000L
        assertEquals(0, limiter.countFor("10.0.0.3:/offers"), "NFR-04: window expired → count reset")
        assertFalse(limiter.isExceeded("10.0.0.3:/offers"), "NFR-04: fresh window allows first request")
    }

    @Test
    fun differentKeysTrackedIndependently() {
        val limiter = RequestRateLimiter(maxRequests = 1, windowMs = 60_000)
        limiter.isExceeded("10.0.0.10:/offers")                   // count = 1 → not exceeded
        assertTrue(limiter.isExceeded("10.0.0.10:/offers"), "NFR-04: ip A must hit limit at second call")
        assertFalse(limiter.isExceeded("10.0.0.11:/offers"), "NFR-04: different IP starts fresh")
    }

    @Test
    fun unknownKeyCountIsZero() {
        val limiter = RequestRateLimiter()
        assertEquals(0, limiter.countFor("127.0.0.1:/offers"))
    }

    // ── HTTP integration: 429 on sensitive endpoints ──────────────────────────

    /**
     * A test-only module wired with a tight rate limiter (1 req / 60 s per key)
     * so the second request always hits the limit.
     */
    private fun tightLimitTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { 1_750_000_000_000L },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        application {
            module(
                service,
                store,
                rateLimiter = nl.incedo.paywall.backend.auth.RequestRateLimiter(maxRequests = 1, windowMs = 60_000),
            )
        }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    @Test
    fun offerAcceptRateLimitedAfterThreshold() = tightLimitTest { client ->
        // First accept goes through
        val first = client.post("/api/v1/offers/accept?visitorId=rl-vis&offerId=o1&kind=upsell&channel=web")
        assertFalse(first.status == HttpStatusCode.TooManyRequests, "NFR-04: first request must not be rate-limited")

        // Second accept from same (virtual) IP hits the limit
        val second = client.post("/api/v1/offers/accept?visitorId=rl-vis&offerId=o2&kind=upsell&channel=web")
        assertEquals(HttpStatusCode.TooManyRequests, second.status, "NFR-04: second request must be rate-limited")
    }

    @Test
    fun rateLimitedResponseCarriesRetryAfterHeader() = tightLimitTest { client ->
        client.post("/api/v1/offers/accept?visitorId=rl-hdr&offerId=o1&kind=upsell&channel=web")
        val limited = client.post("/api/v1/offers/accept?visitorId=rl-hdr&offerId=o2&kind=upsell&channel=web")
        assertEquals(HttpStatusCode.TooManyRequests, limited.status)
        assertNotNull(limited.headers["Retry-After"], "NFR-04: 429 response must carry Retry-After header")
    }

    @Test
    fun integrationEntitlementEndpointIsRateLimited() = tightLimitTest { client ->
        val payload = EntitlementChangeRequest(
            subjectId = "user:rl-sub",
            subscriptionRef = "sub-rl",
            planId = "basic-monthly",
            validUntilEpochMs = 1_750_086_400_000L,
        )
        val first = client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        assertFalse(first.status == HttpStatusCode.TooManyRequests, "NFR-04: first entitlement update must not be rate-limited")

        val second = client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        assertEquals(HttpStatusCode.TooManyRequests, second.status, "NFR-04: rapid second call must be rate-limited")
    }

    @Test
    fun csrfProtectionViaContentTypeRequirement() = tightLimitTest { client ->
        // NFR-04: state-changing endpoints require Content-Type: application/json.
        // A cross-origin form POST with application/x-www-form-urlencoded will
        // not be accepted as a valid JSON payload — the request does not succeed
        // (Ktor ContentNegotiation rejects it with a non-2xx response).
        val withFormEncoded = client.post("/api/v1/integration/entitlements") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("subjectId=x&subscriptionRef=y&planId=basic-monthly")
        }
        assertFalse(
            withFormEncoded.status.value in 200..299,
            "NFR-04: form-encoded CSRF attempt must not succeed (got ${withFormEncoded.status})",
        )
    }
}
