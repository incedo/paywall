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
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * AN-13: cohort view — conversion and 30-day retention by ISO week of first visit.
 * Each visitor is assigned to the cohort of the week they first appeared in the
 * wall-event stream.
 */
class CohortStatsTest {

    private val now = 1_750_000_000_000L  // ~ 2025-06-13

    // Monday 2025-06-02 = start of ISO W23 2025 (June 1 is Sunday, June 2 is Mon)
    private val weekW23Ms = 1_748_822_400_000L  // 2025-06-02 00:00:00 UTC (Mon W23)
    private val weekW24Ms = weekW23Ms + 7 * 24 * 3600 * 1000L // 2025-06-09 (Mon W24)

    private fun apiTest(block: suspend (io.ktor.client.HttpClient, InMemoryEventStore) -> Unit) =
        testApplication {
            val store = InMemoryEventStore()
            val service = AccessService(
                eventStore = store,
                experiment = defaultExperiment,
                clock = { now },
                currentPeriod = { MeterPeriod("2025-06") },
            )
            application { module(service, store) }
            val client = createClient { install(ContentNegotiation) { json() } }
            block(client, store)
        }

    private suspend fun InMemoryEventStore.seedWallShown(subjectId: String, atMs: Long) {
        append(
            listOf(
                WallEventRecorded(
                    eventType = WallEventType.WALL_SHOWN,
                    subjectId = SubjectId(subjectId),
                    variant = "hard",
                    channel = "web",
                    occurredAtEpochMs = atMs,
                    articleId = ArticleId("art-cohort"),
                )
            ),
            condition = null,
        )
    }

    private suspend fun InMemoryEventStore.seedCheckout(subjectId: String, atMs: Long) {
        append(
            listOf(
                WallEventRecorded(
                    eventType = WallEventType.CHECKOUT_COMPLETE,
                    subjectId = SubjectId(subjectId),
                    variant = "hard",
                    channel = "web",
                    occurredAtEpochMs = atMs,
                    articleId = ArticleId("art-cohort"),
                )
            ),
            condition = null,
        )
    }

    @Test
    fun emptyCohortStatsReturnsEmptyList() = apiTest { client, _ ->
        val resp = client.get("/api/v1/stats/cohorts")
        assertEquals(HttpStatusCode.OK, resp.status, "AN-13: cohort endpoint must return 200")
        val cohorts = resp.body<List<CohortStatsResponse>>()
        assertTrue(cohorts.isEmpty(), "AN-13: no events → empty cohort list")
    }

    @Test
    fun visitorsAreGroupedByIsoWeek() = apiTest { client, store ->
        // 2 visitors in W23, 1 visitor in W24
        store.seedWallShown("visitor:v1", weekW23Ms)
        store.seedWallShown("visitor:v2", weekW23Ms + 3600_000L)
        store.seedWallShown("visitor:v3", weekW24Ms)

        val cohorts = client.get("/api/v1/stats/cohorts")
            .body<List<CohortStatsResponse>>()
        assertEquals(2, cohorts.size, "AN-13: two cohort weeks expected")
        val (larger, smaller) = cohorts.sortedByDescending { it.visitors }
        assertEquals(2, larger.visitors, "AN-13: first week must have 2 visitors")
        assertEquals(1, smaller.visitors, "AN-13: second week must have 1 visitor")
        // Weeks must be distinct and in chronological order
        assertTrue(cohorts[0].cohortWeek < cohorts[1].cohortWeek, "AN-13: cohort weeks must be sorted chronologically")
    }

    @Test
    fun conversionCountedWhenCheckoutCompleteEventPresent() = apiTest { client, store ->
        store.seedWallShown("visitor:c1", weekW23Ms)
        store.seedCheckout("visitor:c1", weekW23Ms + 3600_000L)
        store.seedWallShown("visitor:c2", weekW23Ms)  // no checkout

        val cohorts = client.get("/api/v1/stats/cohorts")
            .body<List<CohortStatsResponse>>()
        val cohort = cohorts.single()
        assertEquals(2, cohort.visitors, "AN-13: 2 visitors in cohort")
        assertEquals(1, cohort.conversions, "AN-13: 1 conversion in cohort")
        assertEquals(0.5, cohort.conversionRate, 0.001, "AN-13: conversion rate = 0.5")
    }

    @Test
    fun retentionCountedWhenEventAppearsAfter30Days() = apiTest { client, store ->
        val thirtyOneDaysMs = 31L * 24 * 3600 * 1000
        store.seedWallShown("visitor:r1", weekW23Ms)
        store.seedWallShown("visitor:r1", weekW23Ms + thirtyOneDaysMs) // retained
        store.seedWallShown("visitor:r2", weekW23Ms) // not retained

        val cohorts = client.get("/api/v1/stats/cohorts")
            .body<List<CohortStatsResponse>>()
        // Both visitors started in W23; the retention event of r1 falls in W24 but
        // r1's cohort is still W23 (first visit date determines cohort week).
        val w23 = cohorts.first()
        assertEquals(2, w23.visitors, "AN-13: 2 visitors in cohort")
        assertEquals(1, w23.retainedAt30Days, "AN-13: 1 visitor retained at 30 days")
        assertEquals(0.5, w23.retentionRate, 0.001, "AN-13: retention rate = 0.5")
    }

    @Test
    fun isoWeekKeyFormatIsCorrect() {
        // 2025-06-02 is Monday of ISO W23 2025 (verified: June 1 = Sunday)
        assertEquals("2025-W23", CohortProjection.isoWeekKey(weekW23Ms))
        // 2026-01-05 (Monday) should be W02 2026 (W01 starts Dec 29, 2025)
        val monday20260105 = 1_767_571_200_000L // 2026-01-05 00:00:00 UTC
        assertEquals("2026-W02", CohortProjection.isoWeekKey(monday20260105))
    }
}
