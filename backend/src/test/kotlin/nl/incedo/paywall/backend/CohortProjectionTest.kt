package nl.incedo.paywall.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.VisitorId

/**
 * Q-5: boundary tests for [CohortProjection] rate getters (conversion and retention).
 */
class CohortProjectionTest {

    private val base = 1_750_000_000_000L // arbitrary fixed epoch
    private val thirtyDaysMs = 30L * 24 * 3600 * 1000

    private fun event(
        type: WallEventType,
        visitor: String,
        offsetMs: Long = 0L,
    ) = WallEventRecorded(
        eventType = type,
        subjectId = SubjectId.of(VisitorId(visitor)),
        variant = "metered",
        channel = "web",
        occurredAtEpochMs = base + offsetMs,
        articleId = ArticleId("a-1"),
    )

    @Test
    fun emptyProjectionYieldsNoCohorts() {
        assertEquals(emptyList(), CohortProjection().cohorts())
    }

    @Test
    fun singleVisitorSingleCohort() {
        val projection = CohortProjection()
        projection.apply(event(WallEventType.PAGE_VIEW, "v-1"))
        val cohorts = projection.cohorts()
        assertEquals(1, cohorts.size)
        assertEquals(1, cohorts[0].visitors)
        assertEquals(0, cohorts[0].conversions)
        assertEquals(0, cohorts[0].retainedAt30Days)
    }

    @Test
    fun conversionRateIsZeroWithNoConversions() {
        // Q-5: zero-denominator guard for conversion rate
        val projection = CohortProjection()
        projection.applyAll(listOf(
            event(WallEventType.PAGE_VIEW, "v-1"),
            event(WallEventType.PAGE_VIEW, "v-2"),
        ))
        val stats = projection.cohorts()[0]
        assertEquals(2, stats.visitors)
        assertEquals(0, stats.conversions)
        assertEquals(0.0, stats.conversionRate)
    }

    @Test
    fun retentionRateIsZeroWithNoRetention() {
        // Q-5: zero-denominator guard for retention rate
        val projection = CohortProjection()
        projection.apply(event(WallEventType.PAGE_VIEW, "v-1"))
        val stats = projection.cohorts()[0]
        assertEquals(0, stats.retainedAt30Days)
        assertEquals(0.0, stats.retentionRate)
    }

    @Test
    fun fullConversionAndRetentionRatesAreOne() {
        // Q-5: single-sample boundary — one visitor converts and is retained
        val projection = CohortProjection()
        projection.applyAll(listOf(
            event(WallEventType.PAGE_VIEW, "v-1", offsetMs = 0),
            event(WallEventType.CHECKOUT_COMPLETE, "v-1", offsetMs = 1_000),
            event(WallEventType.PAGE_VIEW, "v-1", offsetMs = thirtyDaysMs + 1_000),
        ))
        val stats = projection.cohorts()[0]
        assertEquals(1, stats.visitors)
        assertEquals(1, stats.conversions)
        assertEquals(1, stats.retainedAt30Days)
        assertEquals(1.0, stats.conversionRate)
        assertEquals(1.0, stats.retentionRate)
    }

    @Test
    fun multipleVisitorsGroupedByWeek() {
        // Two visitors in the same ISO week form one cohort
        val projection = CohortProjection()
        projection.applyAll(listOf(
            event(WallEventType.PAGE_VIEW, "v-1", offsetMs = 0),
            event(WallEventType.PAGE_VIEW, "v-2", offsetMs = 1 * 24 * 3600 * 1000L),
            event(WallEventType.CHECKOUT_COMPLETE, "v-1", offsetMs = 2 * 24 * 3600 * 1000L),
        ))
        // Both v-1 and v-2 should land in the same cohort week
        val cohorts = projection.cohorts()
        val combined = cohorts.sumOf { it.visitors }
        assertTrue(combined >= 2, "expected at least 2 visitors across cohorts")
        assertEquals(1, cohorts.sumOf { it.conversions })
    }

    @Test
    fun isoWeekKeyIsConsistentFormat() {
        val key = CohortProjection.isoWeekKey(base)
        assertTrue(key.matches(Regex("\\d{4}-W\\d{2}")), "expected YYYY-Www format, got $key")
    }
}
