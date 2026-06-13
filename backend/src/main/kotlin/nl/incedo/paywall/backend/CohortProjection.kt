package nl.incedo.paywall.backend

import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.IsoFields
import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId

/**
 * AN-13: cohort view — conversion and 30-day retention by ISO week of first
 * visit. A "first visit" is the earliest WallEventRecorded event for a subject.
 * "Conversion" = subject had a CHECKOUT_COMPLETE event.
 * "30-day retention" = subject had any event ≥ 30 days after their first visit.
 *
 * Rebuilt on demand from the full wall-event stream; disposable read model (DM-04).
 */
class CohortProjection {

    data class CohortStats(
        /** ISO week label, e.g. "2026-W24". */
        val cohortWeek: String,
        val visitors: Int,
        val conversions: Int,
        val retainedAt30Days: Int,
    ) {
        val conversionRate: Double get() = if (visitors == 0) 0.0 else conversions.toDouble() / visitors
        val retentionRate: Double get() = if (visitors == 0) 0.0 else retainedAt30Days.toDouble() / visitors
    }

    private val firstVisitMs = mutableMapOf<SubjectId, Long>()
    private val converted = mutableSetOf<SubjectId>()
    private val retainedAt30d = mutableSetOf<SubjectId>()

    fun apply(event: DomainEvent) {
        if (event !is WallEventRecorded) return
        val subjectId = event.subjectId
        val ts = event.occurredAtEpochMs
        val first = firstVisitMs.getOrPut(subjectId) { ts }
        if (ts < first) firstVisitMs[subjectId] = ts

        if (event.eventType == WallEventType.CHECKOUT_COMPLETE) {
            converted.add(subjectId)
        }
        // 30-day retention: any event ≥ 30 days after first visit counts.
        // We check lazily once we know the first visit timestamp.
        // (Updated after processing all events.)
    }

    fun applyAll(events: List<DomainEvent>) {
        events.forEach { apply(it) }
        // Second pass: compute retention based on finalized firstVisitMs.
        events.filterIsInstance<WallEventRecorded>().forEach { event ->
            val first = firstVisitMs[event.subjectId] ?: return@forEach
            if (event.occurredAtEpochMs - first >= THIRTY_DAYS_MS) {
                retainedAt30d.add(event.subjectId)
            }
        }
    }

    fun cohorts(): List<CohortStats> {
        val byWeek = firstVisitMs.entries
            .groupBy { (_, ts) -> isoWeekKey(ts) }

        return byWeek.entries
            .sortedBy { it.key }
            .map { (week, entries) ->
                val subjects = entries.map { it.key }.toSet()
                CohortStats(
                    cohortWeek = week,
                    visitors = subjects.size,
                    conversions = subjects.count { it in converted },
                    retainedAt30Days = subjects.count { it in retainedAt30d },
                )
            }
    }

    companion object {
        private const val THIRTY_DAYS_MS = 30L * 24 * 3600 * 1000

        fun isoWeekKey(epochMs: Long): String {
            val date = Instant.ofEpochMilli(epochMs).atZone(ZoneOffset.UTC).toLocalDate()
            val year = date.get(IsoFields.WEEK_BASED_YEAR)
            val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            return "$year-W${week.toString().padStart(2, '0')}"
        }
    }
}
