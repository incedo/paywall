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
    /** e.g. "day_pass", "share_token", "ad_gated", "support" */
    val grantedBy: String,
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
