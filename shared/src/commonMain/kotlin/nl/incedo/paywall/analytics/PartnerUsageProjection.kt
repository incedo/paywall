package nl.incedo.paywall.analytics

import nl.incedo.paywall.core.DomainEvent

/**
 * PA-04: aggregates per-partner read counts and unique-user counts from the
 * wall-event stream for contract-management reporting.
 *
 * Processes [WallEventRecorded] events whose context contains "partner_id"
 * and whose eventType is [WallEventType.ARTICLE_READ].
 */
class PartnerUsageProjection {

    data class PartnerUsage(
        val partnerId: String,
        val totalReads: Int,
        val uniqueUsers: Int,
    )

    private val reads = mutableMapOf<String, Int>()
    private val users = mutableMapOf<String, MutableSet<String>>()

    fun apply(event: DomainEvent) {
        if (event !is WallEventRecorded) return
        if (event.eventType != WallEventType.ARTICLE_READ) return
        val partnerId = event.context["partner_id"] ?: return
        reads[partnerId] = (reads[partnerId] ?: 0) + 1
        users.getOrPut(partnerId) { mutableSetOf() }.add(event.subjectId.value)
    }

    fun applyAll(events: List<DomainEvent>) = events.forEach { apply(it) }

    fun usage(): List<PartnerUsage> = reads.keys.sorted().map { partnerId ->
        PartnerUsage(
            partnerId = partnerId,
            totalReads = reads[partnerId] ?: 0,
            uniqueUsers = users[partnerId]?.size ?: 0,
        )
    }
}
