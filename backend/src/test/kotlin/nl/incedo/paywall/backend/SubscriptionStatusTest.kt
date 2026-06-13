package nl.incedo.paywall.backend

import io.ktor.client.call.body
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
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * SUB-03/05/07 subscription status state machine:
 *   active    → access granted
 *   canceled  → access until period end (SUB-03)
 *   past_due  → 7-day grace access (SUB-05)
 *   paused    → access off (SUB-07)
 *   expired   → revoke
 *   active after pause → access restored (SUB-07 resume)
 *
 * Entitlements are stored for "visitor:{id}" subjects so no JWT is needed in tests.
 * Visitors that must gate when non-entitled are pre-assigned to the "hard" variant.
 */
class SubscriptionStatusTest {

    private val now = 1_750_000_000_000L

    /** Returns a unique visitor ID whose hash maps to the "hard" variant. */
    private fun hardVariantVisitor(seed: String): String =
        (0 until 10_000).asSequence()
            .map { "$seed-sv-$it" }
            .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == "hard" }

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

    private suspend fun io.ktor.client.HttpClient.decide(visitorId: String): DecideResponse =
        post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = visitorId, articleId = "article-premium", tier = "premium"))
        }.body()

    /** Entitlement subjectId must match "visitor:{visitorId}" — the tag the decide query uses. */
    private suspend fun io.ktor.client.HttpClient.postEntitlement(
        visitorId: String,
        subscriptionRef: String,
        status: String? = null,
        active: Boolean = true,
        validUntilEpochMs: Long? = null,
        planId: String = "pro",
    ) = post("/api/v1/integration/entitlements") {
        contentType(ContentType.Application.Json)
        setBody(EntitlementChangeRequest(
            subjectId = "visitor:$visitorId",
            subscriptionRef = subscriptionRef,
            planId = planId,
            status = status,
            active = active,
            validUntilEpochMs = validUntilEpochMs,
        ))
    }

    @Test
    fun activeStatusGrantsAccess() = apiTest { client ->
        val vis = hardVariantVisitor("active")
        client.postEntitlement(vis, "sub-active", status = "active")
        assertEquals("full", client.decide(vis).access)
    }

    @Test
    fun canceledStatusRetainsAccessUntilPeriodEnd() = apiTest { client ->
        // period end is in the future → access is retained (SUB-03)
        val vis = hardVariantVisitor("cancel")
        client.postEntitlement(vis, "sub-cancel", status = "canceled", validUntilEpochMs = now + 86_400_000)
        assertEquals("full", client.decide(vis).access, "canceled subscription retains access until period end (SUB-03)")
    }

    @Test
    fun expiredStatusRevokesAccess() = apiTest { client ->
        val vis = hardVariantVisitor("expired")
        client.postEntitlement(vis, "sub-exp", status = "active")
        client.postEntitlement(vis, "sub-exp", status = "expired")
        assertEquals("gate", client.decide(vis).access, "expired subscription loses access (SUB-01)")
    }

    @Test
    fun pausedStatusRevokesAccessImmediately() = apiTest { client ->
        val vis = hardVariantVisitor("paused")
        client.postEntitlement(vis, "sub-pause", status = "active")
        assertEquals("full", client.decide(vis).access, "active grant must allow access")

        client.postEntitlement(vis, "sub-pause", status = "paused")
        assertEquals("gate", client.decide(vis).access, "paused subscription must revoke access immediately (SUB-07)")
    }

    @Test
    fun resumedSubscriptionRestoresAccess() = apiTest { client ->
        val vis = hardVariantVisitor("resume")
        // grant → pause → resume (status=active again)
        client.postEntitlement(vis, "sub-resume", status = "active")
        client.postEntitlement(vis, "sub-resume", status = "paused")
        assertEquals("gate", client.decide(vis).access, "paused must lose access")

        client.postEntitlement(vis, "sub-resume", status = "active")
        assertEquals("full", client.decide(vis).access, "resumed subscription must restore access (SUB-07)")
    }

    @Test
    fun pastDueGrantsSevenDayGraceAccess() = apiTest { client ->
        val vis = hardVariantVisitor("pastdue")
        // past_due → backend sets validUntilEpochMs = now + 7 days (SUB-05)
        val resp = client.postEntitlement(vis, "sub-pd", status = "past_due")
        assertEquals(HttpStatusCode.Accepted, resp.status)
        // clock is frozen at `now`; grace window extends 7 days so still valid
        assertEquals("full", client.decide(vis).access, "past_due within grace period must retain access (SUB-05)")
    }

    @Test
    fun legacyActiveBooleanStillWorks() = apiTest { client ->
        val vis = hardVariantVisitor("legacy")
        client.postEntitlement(vis, "sub-leg", active = true, status = null)
        assertEquals("full", client.decide(vis).access, "legacy active=true path must still grant access")
    }

    @Test
    fun legacyRevokeBooleanStillWorks() = apiTest { client ->
        val vis = hardVariantVisitor("legrev")
        client.postEntitlement(vis, "sub-lr", active = true, status = null)
        client.postEntitlement(vis, "sub-lr", active = false, status = null)
        assertEquals("gate", client.decide(vis).access, "legacy active=false path must revoke access")
    }
}
