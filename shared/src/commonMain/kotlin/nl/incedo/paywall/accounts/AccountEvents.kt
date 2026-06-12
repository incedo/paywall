package nl.incedo.paywall.accounts

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.UserId
import nl.incedo.paywall.core.VisitorId

/**
 * US-04/MT-03: on login or registration the anonymous visitor is linked to
 * the user; meter state merges as the union of counted articles. Tagged with
 * both subjects so either side's queries see the link.
 */
@Serializable
data class AccountLinked(
    val visitorId: VisitorId,
    val userId: UserId,
    override val tags: Set<String> = setOf(
        "subject:${SubjectId.of(visitorId).value}",
        "subject:${SubjectId.of(userId).value}",
    ),
) : DomainEvent
