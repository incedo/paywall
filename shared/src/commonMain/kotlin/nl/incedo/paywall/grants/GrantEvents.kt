package nl.incedo.paywall.grants

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.GrantId
import nl.incedo.paywall.core.SubjectId

/**
 * Fine-grained access grants (FGA-*): article-scoped access independent of
 * subscriptions — day/week passes (PW-08, TTL), share tokens (BP-05),
 * ad-gated and data-gated grants (AG-*, DG-*). The grant store behind this
 * is Ory Keto (TS-05); these events mirror grant lifecycle locally.
 */
@Serializable
data class GrantIssued(
    val grantId: GrantId,
    val subjectId: SubjectId,
    val articleId: ArticleId,
    /** FGA-01: source system that issued the grant (e.g. "day_pass", "ad_gated", "support"). */
    val grantedBy: String,
    /** FGA-01: human-readable reason for audit (e.g. "support ticket 4711", "ad completion"). */
    val reason: String = "",
    /** Null = no expiry. Day/week passes carry TTL = pass duration (PW-08). */
    val expiresAtEpochMs: Long? = null,
    override val tags: Set<String> = setOf(
        "subject:${subjectId.value}",
        "article:${articleId.value}",
    ),
) : DomainEvent

@Serializable
data class GrantRevoked(
    val grantId: GrantId,
    val subjectId: SubjectId,
    val articleId: ArticleId,
    override val tags: Set<String> = setOf(
        "subject:${subjectId.value}",
        "article:${articleId.value}",
    ),
) : DomainEvent
