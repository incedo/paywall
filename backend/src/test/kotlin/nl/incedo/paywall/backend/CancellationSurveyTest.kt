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
import kotlin.test.assertTrue
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.entitlements.CANCEL_SURVEY_TAG
import nl.incedo.paywall.entitlements.CancellationSurveySubmitted
import nl.incedo.paywall.metering.MeterPeriod

/**
 * SUB-06: cancel-flow survey — one optional question logged when a subscriber
 * submits or skips the survey during the cancellation flow.
 */
class CancellationSurveyTest {

    private val now = 1_750_000_000_000L

    private fun apiTest(block: suspend (io.ktor.client.HttpClient, InMemoryEventStore) -> Unit) =
        testApplication {
            val store = InMemoryEventStore()
            val service = AccessService(
                eventStore = store,
                experiment = defaultExperiment,
                clock = { now },
                currentPeriod = { MeterPeriod("2026-06") },
            )
            application { module(service, store) }
            val client = createClient { install(ContentNegotiation) { json() } }
            block(client, store)
        }

    private suspend fun InMemoryEventStore.surveyEvents() =
        query(EventQuery(setOf(CANCEL_SURVEY_TAG))).events.filterIsInstance<CancellationSurveySubmitted>()

    @Test
    fun surveyWithReasonIsLogged() = apiTest { client, store ->
        val resp = client.post("/api/v1/subjects/cancellation-survey") {
            contentType(ContentType.Application.Json)
            setBody(CancellationSurveyRequest(
                subjectId = "visitor:vis-sub06-1",
                reason = "too_expensive",
                freeText = "Monthly price is too high for me",
            ))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status, "SUB-06: survey submission must return 202")
        val events = store.surveyEvents()
        assertEquals(1, events.size, "SUB-06: one survey event must be recorded")
        val e = events[0]
        assertEquals("visitor:vis-sub06-1", e.subjectId.value)
        assertEquals("too_expensive", e.reason)
        assertEquals("Monthly price is too high for me", e.freeText)
        assertTrue(e.submittedAtEpochMs > 0L, "SUB-06: timestamp must be set")
    }

    @Test
    fun surveySkipLogsNullReasonAndFreeText() = apiTest { client, store ->
        val resp = client.post("/api/v1/subjects/cancellation-survey") {
            contentType(ContentType.Application.Json)
            setBody(CancellationSurveyRequest(subjectId = "visitor:vis-sub06-skip"))
        }
        assertEquals(HttpStatusCode.Accepted, resp.status, "SUB-06: survey skip must return 202")
        val events = store.surveyEvents()
        assertEquals(1, events.size, "SUB-06: skip still produces a survey event")
        assertNull(events[0].reason, "SUB-06: skipped survey has null reason")
        assertNull(events[0].freeText, "SUB-06: skipped survey has null freeText")
    }

    @Test
    fun freeTextTrimmedToFiveHundredChars() = apiTest { client, store ->
        val longText = "x".repeat(600)
        client.post("/api/v1/subjects/cancellation-survey") {
            contentType(ContentType.Application.Json)
            setBody(CancellationSurveyRequest(
                subjectId = "visitor:vis-sub06-trim",
                reason = "other",
                freeText = longText,
            ))
        }
        val e = store.surveyEvents().single()
        assertNotNull(e.freeText, "SUB-06: freeText must be present")
        assertEquals(500, e.freeText!!.length, "SUB-06: freeText must be trimmed to 500 chars")
    }

    @Test
    fun missingSubjectIdReturns400() = apiTest { client, _ ->
        val resp = client.post("/api/v1/subjects/cancellation-survey") {
            contentType(ContentType.Application.Json)
            setBody(CancellationSurveyRequest(subjectId = ""))
        }
        assertEquals(HttpStatusCode.BadRequest, resp.status, "SUB-06: blank subjectId must return 400")
    }

    @Test
    fun surveyEventTaggedWithSubjectAndCancelSurveyTag() = apiTest { client, store ->
        client.post("/api/v1/subjects/cancellation-survey") {
            contentType(ContentType.Application.Json)
            setBody(CancellationSurveyRequest(subjectId = "visitor:vis-sub06-tags", reason = "temporary_break"))
        }
        val e = store.surveyEvents().single()
        assertTrue("subject:visitor:vis-sub06-tags" in e.tags, "SUB-06: subject tag required for subject queries")
        assertTrue(CANCEL_SURVEY_TAG in e.tags, "SUB-06: cancel_survey tag required for aggregate queries")
    }
}
