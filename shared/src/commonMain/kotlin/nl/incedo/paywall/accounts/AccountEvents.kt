package nl.incedo.paywall.accounts

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId

/**
 * First-party identity graph, as events (MT-13): consent-based signals that
 * tie two identifiers to one person. Every link carries its cause —
 * "login" (US-04), "newsletter_token", "share_token" (BP-05),
 * "device_login" (US-08) — so the graph stays auditable. Wrong links are
 * corrected by compensating [IdentityUnlinked] events, never by mutation.
 * Covert fingerprinting is excluded (MT-07) — causes are explicit actions.
 */
@Serializable
data class IdentityLinked(
    val subjectA: SubjectId,
    val subjectB: SubjectId,
    val cause: String,
    override val tags: Set<String> = setOf(
        "subject:${subjectA.value}",
        "subject:${subjectB.value}",
    ),
) : DomainEvent

@Serializable
data class IdentityUnlinked(
    val subjectA: SubjectId,
    val subjectB: SubjectId,
    val reason: String,
    override val tags: Set<String> = setOf(
        "subject:${subjectA.value}",
        "subject:${subjectB.value}",
    ),
) : DomainEvent
