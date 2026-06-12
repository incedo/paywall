package nl.incedo.paywall.entitlements

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.PlanId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.SubscriptionId

/**
 * Integration events ingested from the external subscription administration
 * (AC-02/AC-08, EA-*). The paywall does not manage subscriptions; it records
 * entitlement changes and enforces them.
 */
@Serializable
data class EntitlementGranted(
    val subjectId: SubjectId,
    val planId: PlanId,
    val subscriptionRef: SubscriptionId,
    /** End of the paid period (AC-02: access is lost at the end of it). Null = open-ended until revoked. */
    val validUntilEpochMs: Long? = null,
    override val tags: Set<String> = setOf("subject:${subjectId.value}"),
) : DomainEvent

@Serializable
data class EntitlementRevoked(
    val subjectId: SubjectId,
    val subscriptionRef: SubscriptionId,
    override val tags: Set<String> = setOf("subject:${subjectId.value}"),
) : DomainEvent
