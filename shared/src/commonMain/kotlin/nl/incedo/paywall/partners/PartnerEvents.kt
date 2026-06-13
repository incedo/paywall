package nl.incedo.paywall.partners

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.PartnerId
import nl.incedo.paywall.core.SubjectId

/** PA-01: partner (company, school, library, telco bundle) created with optional seat cap. */
@Serializable
data class PartnerCreated(
    val partnerId: PartnerId,
    val name: String,
    /** PA-05: optional maximum concurrent member seats. Null = unlimited. */
    val maxSeats: Int? = null,
    /** The plan tier granted to all partner members (e.g. "complete"). */
    val planId: String = "complete",
    val createdAtEpochMs: Long,
    override val tags: Set<String> = setOf(partnerTag(partnerId)),
) : DomainEvent

/** PA-02: member added to a partner; writes are seat-capped (PA-05). */
@Serializable
data class PartnerMemberAdded(
    val partnerId: PartnerId,
    val subjectId: SubjectId,
    val addedBy: String,
    val addedAtEpochMs: Long,
    override val tags: Set<String> = setOf(
        partnerTag(partnerId),
        "subject:${subjectId.value}",
    ),
) : DomainEvent

/** PA-03: member removed; seat freed. */
@Serializable
data class PartnerMemberRemoved(
    val partnerId: PartnerId,
    val subjectId: SubjectId,
    val removedBy: String,
    val removedAtEpochMs: Long,
    override val tags: Set<String> = setOf(
        partnerTag(partnerId),
        "subject:${subjectId.value}",
    ),
) : DomainEvent

/**
 * PA-03: partner offboarded — transitively revokes all member access immediately.
 * The partner's "subscription tuple" (active FGA relation) is invalidated; members
 * no longer gain access via the partner identity (≤ AC-03 cache window).
 */
@Serializable
data class PartnerOffboarded(
    val partnerId: PartnerId,
    val offboardedBy: String,
    val offboardedAtEpochMs: Long,
    override val tags: Set<String> = setOf(partnerTag(partnerId)),
) : DomainEvent

/**
 * IPW-01: partner IP CIDR range stored as config — the edge Worker polls
 * GET /api/v1/admin/ip-allowlist and matches the client IP before forwarding
 * partner_id in the trusted context (INF-01/02).
 */
@Serializable
data class PartnerIpRangeConfigured(
    val partnerId: PartnerId,
    /** e.g. "192.168.1.0/24" */
    val cidr: String,
    val active: Boolean = true,
    val configuredAtEpochMs: Long,
    override val tags: Set<String> = setOf(partnerTag(partnerId), "partner:ip-ranges"),
) : DomainEvent

fun partnerTag(partnerId: PartnerId): String = "partner:${partnerId.value}"
