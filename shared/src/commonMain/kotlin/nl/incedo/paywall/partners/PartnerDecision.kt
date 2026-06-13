package nl.incedo.paywall.partners

import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId

/**
 * PA-01: decision model for a single partner — folds creation, membership and
 * IP range events to answer: is active? what plan? how many seats? which IPs?
 *
 * Used by admin endpoints and the decide path when partner_id is present (IPW-02).
 */
class PartnerDecision {
    var name: String = ""
        private set
    var maxSeats: Int? = null
        private set
    var planId: String = "complete"
        private set
    /** PA-03: false after PartnerOffboarded — all member access is revoked. */
    var isActive: Boolean = false
        private set

    private val activeMembers = mutableSetOf<SubjectId>()
    private val ipRanges = mutableMapOf<String, Boolean>() // cidr → active

    fun apply(event: DomainEvent) {
        when (event) {
            is PartnerCreated -> {
                name = event.name
                maxSeats = event.maxSeats
                planId = event.planId
                isActive = true
            }
            is PartnerMemberAdded -> activeMembers.add(event.subjectId)
            is PartnerMemberRemoved -> activeMembers.remove(event.subjectId)
            is PartnerOffboarded -> isActive = false // PA-03: revoke all member access
            is PartnerIpRangeConfigured -> ipRanges[event.cidr] = event.active
            else -> Unit
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)

    /** PA-05: current seat count (active members). */
    fun activeSeatCount(): Int = activeMembers.size

    /** PA-05: true if a new membership can be added without exceeding max_seats. */
    fun hasSeatAvailable(): Boolean = maxSeats?.let { activeSeatCount() < it } ?: true

    /** PA-01: true if this subject is an active partner member. */
    fun isMember(subjectId: SubjectId): Boolean = subjectId in activeMembers

    /** IPW-01: active CIDR ranges for edge IP-matching. */
    fun activeCidrs(): List<String> = ipRanges.filter { it.value }.keys.sorted()
}
