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
import nl.incedo.paywall.api.CiamSession
import nl.incedo.paywall.api.ExperimentConfigResponse
import nl.incedo.paywall.api.MeterResetRequest
import nl.incedo.paywall.api.PublishExperimentConfigRequest
import nl.incedo.paywall.api.SubjectInspectorResponse
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.core.ExperimentId
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.experiments.VariantAssigner
import nl.incedo.paywall.metering.MeterPeriod

/** ADM-04 subject inspector + PW-22/23 meter indicator and nudge. */
class AdminApiTest {

    private val now = 1_750_000_000_000L

    private fun visitorIn(variant: String): String =
        (0 until 10_000).asSequence()
            .map { "adm-visitor-$it" }
            .first { VariantAssigner.assign(VisitorId(it), defaultExperiment).name == variant }

    private fun apiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod(currentPeriod().value) }, // inspector uses the real period helper
        )
        application { module(service, store) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    private suspend fun decide(client: io.ktor.client.HttpClient, visitor: String, article: String): DecideResponse =
        client.post("/api/v1/decide") {
            contentType(ContentType.Application.Json)
            setBody(DecideRequest(visitorId = visitor, articleId = article, tier = "premium"))
        }.body()

    @Test
    fun inspectorShowsMeterStateAndRecentEvents() = apiTest { client ->
        val visitor = visitorIn("metered")
        decide(client, visitor, "a-1")
        decide(client, visitor, "a-2")

        val inspector: SubjectInspectorResponse = client.get("/api/v1/admin/subjects/visitor:$visitor").body()
        assertEquals(2, inspector.meterUsed)
        assertEquals("metered", inspector.variant)
        assertFalse(inspector.entitled)
        assertTrue(inspector.recentWallEvents.any { it.type == "article_read" })
    }

    @Test
    fun meterResetIsAuditedAndTakesEffect() = apiTest { client ->
        val visitor = visitorIn("metered")
        repeat(5) { decide(client, visitor, "a-$it") }
        assertEquals("gate", decide(client, visitor, "a-new").access)

        // Support resets the meter — actor and reason are mandatory (ADM-04)
        val unaudited = client.post("/api/v1/admin/subjects/visitor:$visitor/meter-reset") {
            contentType(ContentType.Application.Json)
            setBody(MeterResetRequest(actor = "", reason = ""))
        }
        assertEquals(HttpStatusCode.BadRequest, unaudited.status)

        val reset = client.post("/api/v1/admin/subjects/visitor:$visitor/meter-reset") {
            contentType(ContentType.Application.Json)
            setBody(MeterResetRequest(actor = "m.visser", reason = "support ticket 4711"))
        }
        assertEquals(HttpStatusCode.Accepted, reset.status)
        assertEquals("full", decide(client, visitor, "a-new").access)
    }

    // MT-11: article list in meter inspector ------------------------------------------

    @Test
    fun inspectorIncludesMeteredArticleList() = apiTest { client ->
        // MT-11: the inspector must show count, articles, and period — not just count.
        val visitor = visitorIn("metered")
        decide(client, visitor, "a-1")
        decide(client, visitor, "a-3")

        val inspector: SubjectInspectorResponse = client.get("/api/v1/admin/subjects/visitor:$visitor").body()
        assertEquals(2, inspector.meterUsed)
        assertTrue("a-1" in inspector.meteredArticles, "MT-11: a-1 must appear in meteredArticles")
        assertTrue("a-3" in inspector.meteredArticles, "MT-11: a-3 must appear in meteredArticles")
    }

    // MT-09: cross-channel meter -------------------------------------------------------

    @Test
    fun crossChannelReadCountsTowardSameMeter() = apiTest { client ->
        // MT-09: web + app + chat reads count against the same meter per subject.
        val visitor = visitorIn("metered")
        val channels = listOf("web", "app", "chat")
        channels.forEach { channel ->
            client.post("/api/v1/decide") {
                contentType(ContentType.Application.Json)
                setBody(DecideRequest(visitorId = visitor, articleId = "a-$channel", tier = "premium", channel = channel))
            }
        }
        val inspector: SubjectInspectorResponse = client.get("/api/v1/admin/subjects/visitor:$visitor").body()
        assertEquals(3, inspector.meterUsed, "MT-09: reads from web, app, chat all count toward the same meter")
        assertEquals(setOf("a-web", "a-app", "a-chat"), inspector.meteredArticles.toSet())
    }

    // AN-21/US-07: account deletion pseudonymization -----------------------------------

    @Test
    fun accountDeletionUnlinksUserFromVisitors() = apiTest { client ->
        // Simulate: visitor reads some articles, then logs in (linking visitor to user)
        val visitor = visitorIn("metered")
        decide(client, visitor, "a-1")

        // Log in: link visitor to user (US-04 / MT-03)
        client.post("/api/v1/integration/identity-link") {
            contentType(ContentType.Application.Json)
            setBody(IdentityLinkRequest(
                subjectA = "visitor:$visitor",
                subjectB = "user:deleted-user-1",
                cause = "login",
            ))
        }

        // Verify the link exists (visitor now shows as linked)
        val before: SubjectInspectorResponse = client.get("/api/v1/admin/subjects/visitor:$visitor").body()
        assertTrue("user:deleted-user-1" in before.linkedSubjects, "link must exist before deletion")

        // GDPR deletion: user deletes their account
        val deletion = client.post("/api/v1/integration/account-deletion") {
            contentType(ContentType.Application.Json)
            setBody(AccountDeletionRequest(userId = "deleted-user-1"))
        }
        assertEquals(HttpStatusCode.Accepted, deletion.status, "account deletion must be accepted")

        // After deletion: visitor's linked subjects no longer include the user
        val after: SubjectInspectorResponse = client.get("/api/v1/admin/subjects/visitor:$visitor").body()
        assertFalse("user:deleted-user-1" in after.linkedSubjects,
            "AN-21: user subject must be unlinked after account deletion")
    }

    @Test
    fun accountDeletionRequiresUserId() = apiTest { client ->
        val status = client.post("/api/v1/integration/account-deletion") {
            contentType(ContentType.Application.Json)
            setBody(AccountDeletionRequest(userId = ""))
        }.status
        assertEquals(HttpStatusCode.BadRequest, status)
    }

    // AA-03: friction guard -----------------------------------------------------------

    @Test
    fun articleAccessNeverRequiresStepUp() = apiTest { client ->
        // AA-03: reading content must always succeed with AAL1. The articles endpoint
        // has no requireStaff call — it uses jwtValidator (CiamJwtValidator) which
        // returns null for an invalid/missing token, degrading to anonymous (AC-07).
        // This test verifies the gate is applied (not a 401) even without a token.
        val visitor = visitorIn("hard")
        val response = client.get("/api/v1/articles/a-1?visitorId=$visitor")
        // We get a 404 (no articles configured in this test) but crucially NOT a 401/403.
        // A 401 would mean step-up is required, violating AA-03.
        assertFalse(
            response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden,
            "AA-03: article access must never require step-up auth, got ${response.status}",
        )
    }

    @Test
    fun nudgeAppearsWhenOneCreditRemains() = apiTest { client ->
        val visitor = visitorIn("metered") // limit 5
        // Reads 1-3: no nudge yet
        for (i in 1..3) {
            assertFalse(decide(client, visitor, "a-$i").nudge, "read $i")
        }
        // Read 4 leaves exactly one credit: soft banner (PW-23)
        val fourth = decide(client, visitor, "a-4")
        assertTrue(fourth.nudge)
        assertEquals(4, fourth.meterUsed)
        assertEquals(5, fourth.meterLimit, "PW-22: indicator shows used of limit")
        // Read 5 exhausts: no nudge, next one gates
        assertFalse(decide(client, visitor, "a-5").nudge)
        assertEquals("gate", decide(client, visitor, "a-6").access)
    }

    // MT-10/API-03: hot-reloadable experiment config via event-sourced ConfigStore.

    private fun configApiTest(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        val store = InMemoryEventStore()
        val configStore = ConfigStore(store, fallback = defaultExperiment, clock = { now }, cacheTtlMs = 0L)
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod(currentPeriod().value) },
            experimentLoader = configStore::experiment,
        )
        application { module(service, store, configStore = configStore) }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    @Test
    fun getConfigReturnsDefaultWhenNonePublished() = configApiTest { client ->
        val resp = client.get("/api/v1/admin/config")
        assertEquals(HttpStatusCode.OK, resp.status)
        val body = resp.body<ExperimentConfigResponse>()
        assertTrue(body.isDefault, "expected isDefault=true before any publish")
        assertEquals(defaultExperiment, body.experiment)
    }

    @Test
    fun publishConfigAndGetReturnsPublished() = configApiTest { client ->
        val hardExperiment = ExperimentDefinition(
            id = ExperimentId("hard-only"),
            name = "Hard wall only",
            variants = listOf(Variant("hard", StrategyConfig.Hard, weight = 1)),
        )
        val postResp = client.post("/api/v1/admin/config") {
            contentType(ContentType.Application.Json)
            setBody(PublishExperimentConfigRequest(hardExperiment))
        }
        assertEquals(HttpStatusCode.Accepted, postResp.status)

        val getResp = client.get("/api/v1/admin/config")
        val body = getResp.body<ExperimentConfigResponse>()
        assertFalse(body.isDefault, "expected isDefault=false after publish")
        assertEquals(hardExperiment, body.experiment)
    }

    // ADM-04: CIAM session lookup ----------------------------------------------------------

    @Test
    fun inspectorIncludesCiamSessionsForUserSubject() = testApplication {
        val store = InMemoryEventStore()
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { now },
            currentPeriod = { MeterPeriod(currentPeriod().value) },
        )
        val fakeSessions = listOf(
            CiamSession(id = "sess-001", device = "Chrome/Mozilla", ipAddress = "1.2.3.4", lastActiveAtEpochMs = now),
            CiamSession(id = "sess-002", device = "Safari/Mobile", ipAddress = "5.6.7.8", lastActiveAtEpochMs = now - 3600_000),
        )
        val ciamClient = object : CiamSessionClient {
            override suspend fun activeSessions(userId: String): List<CiamSession> =
                if (userId == "user-123") fakeSessions else emptyList()
        }
        application { module(service, store, ciamSessionClient = ciamClient) }
        val client = createClient { install(ContentNegotiation) { json() } }

        val inspector: SubjectInspectorResponse = client.get("/api/v1/admin/subjects/user:user-123").body()
        assertEquals(2, inspector.sessions.size, "ADM-04: inspector must include CIAM sessions for user subjects")
        assertEquals("sess-001", inspector.sessions[0].id)
        assertEquals("Chrome/Mozilla", inspector.sessions[0].device)
        assertEquals("1.2.3.4", inspector.sessions[0].ipAddress)
    }

    @Test
    fun inspectorHasEmptySessionsForVisitorSubject() = apiTest { client ->
        // Anonymous visitor: no CIAM sessions — session list must be empty (not absent).
        val visitor = visitorIn("metered")
        val inspector: SubjectInspectorResponse = client.get("/api/v1/admin/subjects/visitor:$visitor").body()
        assertEquals(emptyList(), inspector.sessions, "ADM-04: visitor subjects have no CIAM sessions")
    }

    @Test
    fun configWithRolling30dPeriodIsRejected() = configApiTest { client ->
        // MT-06: rolling_30d is not yet implemented; publishing a config that
        // uses it must return 400 so the operator learns immediately (API-03).
        val rolling = ExperimentDefinition(
            id = ExperimentId("rolling-test"),
            name = "rolling window test",
            variants = listOf(
                Variant("metered", StrategyConfig.Metered(limit = 5, periodType = "rolling_30d"), weight = 1),
            ),
        )
        val resp = client.post("/api/v1/admin/config") {
            contentType(ContentType.Application.Json)
            setBody(PublishExperimentConfigRequest(rolling))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status,
            "MT-06: rolling_30d must be rejected at config-load")
        val body = resp.body<Map<String, String>>()
        assertTrue(body["error"]?.contains("rolling_30d") == true,
            "error message must name the unsupported period type")
    }

    @Test
    fun publishConfigAffectsDecisions() = configApiTest { client ->
        // Before publish: metered variant (from defaultExperiment) grants access
        val meteredVisitor = visitorIn("metered")
        assertEquals("full", decide(client, meteredVisitor, "art-1").access)

        // Publish hard-wall-only config
        val hardExperiment = ExperimentDefinition(
            id = ExperimentId("hard-only"),
            name = "Hard wall only",
            variants = listOf(Variant("hard", StrategyConfig.Hard, weight = 1)),
        )
        client.post("/api/v1/admin/config") {
            contentType(ContentType.Application.Json)
            setBody(PublishExperimentConfigRequest(hardExperiment))
        }

        // After publish: every visitor gets the hard variant → gated on premium
        val resp = decide(client, meteredVisitor, "art-2")
        assertEquals("gate", resp.access, "hard wall should gate after config change")
    }
}
