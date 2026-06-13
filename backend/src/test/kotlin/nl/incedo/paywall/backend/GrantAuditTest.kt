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
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import nl.incedo.paywall.api.GrantAuditEntry
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * FGA-08: grants per subject visible via admin/debug endpoint with full audit
 * trail (grantedBy, expiry, live status). Covers active grants, revoked grants,
 * and expired grants.
 */
class GrantAuditTest {

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

    private suspend fun io.ktor.client.HttpClient.issueGrant(
        subjectId: String = "visitor:fga08-vis",
        articleId: String = "a-fga08",
        grantId: String = "g-fga08",
        grantedBy: String = "support",
        reason: String = "",
        expiresAtEpochMs: Long? = null,
        active: Boolean = true,
    ) = post("/api/v1/grants") {
        contentType(ContentType.Application.Json)
        setBody(
            GrantChangeRequest(
                grantId = grantId,
                subjectId = subjectId,
                articleId = articleId,
                grantedBy = grantedBy,
                reason = reason,
                expiresAtEpochMs = expiresAtEpochMs,
                active = active,
            ),
        )
    }

    private suspend fun io.ktor.client.HttpClient.grantsFor(subjectId: String) =
        get("/api/v1/admin/subjects/$subjectId/grants").body<List<GrantAuditEntry>>()

    @Test
    fun emptyWhenNoGrantsIssued() = apiTest { client ->
        val entries = client.grantsFor("visitor:fga08-nobody")
        assertTrue(entries.isEmpty(), "FGA-08: no grants issued → empty audit list")
    }

    @Test
    fun activeGrantAppearsAsLive() = apiTest { client ->
        // null expiry = no expiration; grant is live as long as it isn't revoked
        client.issueGrant(
            subjectId = "visitor:fga08-a",
            grantId = "g-fga08-a",
            grantedBy = "support",
            expiresAtEpochMs = null,
        )
        val entries = client.grantsFor("visitor:fga08-a")
        assertEquals(1, entries.size, "FGA-08: one grant must appear")
        val entry = entries[0]
        assertEquals("g-fga08-a", entry.grantId)
        assertEquals("support", entry.grantedBy)
        assertTrue(entry.isLive, "FGA-08: unexpired, unrevoked grant must be live")
    }

    @Test
    fun revokedGrantShownAsNotLive() = apiTest { client ->
        client.issueGrant(subjectId = "visitor:fga08-b", grantId = "g-fga08-b", active = true)
        // revoke
        client.issueGrant(subjectId = "visitor:fga08-b", grantId = "g-fga08-b", active = false)
        val entries = client.grantsFor("visitor:fga08-b")
        assertEquals(1, entries.size)
        assertFalse(entries[0].isLive, "FGA-08: revoked grant must not be live")
    }

    @Test
    fun expiredGrantShownAsNotLive() = apiTest { client ->
        client.issueGrant(
            subjectId = "visitor:fga08-c",
            grantId = "g-fga08-c",
            // epoch 1 = long in the past — definitely expired
            expiresAtEpochMs = 1L,
        )
        val entries = client.grantsFor("visitor:fga08-c")
        assertEquals(1, entries.size)
        assertFalse(entries[0].isLive, "FGA-08: expired grant must not be live")
    }

    @Test
    fun auditTrailShowsGrantedByAndArticle() = apiTest { client ->
        client.issueGrant(
            subjectId = "visitor:fga08-d",
            grantId = "g-fga08-d",
            articleId = "a-premium-42",
            grantedBy = "ad_gated",
        )
        val entries = client.grantsFor("visitor:fga08-d")
        val entry = entries.single()
        assertEquals("a-premium-42", entry.articleId, "FGA-08: article ID must be in audit trail")
        assertEquals("ad_gated", entry.grantedBy, "FGA-08: grantedBy must be in audit trail")
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/admin/subjects/visitor:fga08-d/grants").status)
    }

    @Test
    fun auditTrailIncludesReason() = apiTest { client ->
        // FGA-01: grant metadata must include reason so the audit trail answers "why"
        client.issueGrant(
            subjectId = "visitor:fga08-e",
            grantId = "g-fga08-e",
            grantedBy = "support",
            reason = "support ticket 4711",
        )
        val entry = client.grantsFor("visitor:fga08-e").single()
        assertEquals("support ticket 4711", entry.reason, "FGA-01: reason must appear in grant audit entry")
    }
}
