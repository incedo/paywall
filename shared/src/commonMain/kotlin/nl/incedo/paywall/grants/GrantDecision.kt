package nl.incedo.paywall.grants

import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.GrantId
import nl.incedo.paywall.core.SubjectId

/**
 * Decision model: does the subject hold a live grant for this article? (FGA-01..06)
 *
 * [subjectIds] scopes the check to the querying subject's identity graph — prevents
 * cross-subject grant leakage that would otherwise occur because the union event query
 * includes all events tagged with the article ID (including other subjects' grants).
 * Empty means accept-all (admin inspector use-case where upstream filtering applies).
 */
class GrantDecision(
    private val articleId: ArticleId,
    private val subjectIds: Set<SubjectId> = emptySet(),
) {

    private data class GrantEntry(val expiresAtEpochMs: Long?, val grantedBy: String)
    private val live = mutableMapOf<GrantId, GrantEntry>()

    fun apply(event: DomainEvent) {
        when (event) {
            is GrantIssued -> {
                if (event.articleId == articleId &&
                    (subjectIds.isEmpty() || event.subjectId in subjectIds)
                ) {
                    live[event.grantId] = GrantEntry(event.expiresAtEpochMs, event.grantedBy)
                }
            }
            is GrantRevoked -> {
                if (event.articleId == articleId &&
                    (subjectIds.isEmpty() || event.subjectId in subjectIds)
                ) {
                    live.remove(event.grantId)
                }
            }
            else -> {}
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)

    fun hasLiveGrant(nowEpochMs: Long): Boolean =
        live.values.any { it.expiresAtEpochMs == null || it.expiresAtEpochMs > nowEpochMs }

    /** FGA-04: grantedBy of the first live grant, for analytics logging. */
    fun liveGrantedBy(nowEpochMs: Long): String? =
        live.values.firstOrNull { it.expiresAtEpochMs == null || it.expiresAtEpochMs > nowEpochMs }?.grantedBy

    companion object {
        /**
         * FGA-05: pre-resolve a GrantDecision from a cached (hasGrant, grantedBy) pair.
         * The synthetic entry carries no expiry so it is always "live" within this instance.
         */
        fun ofCached(articleId: ArticleId, hasGrant: Boolean, grantedBy: String?): GrantDecision {
            val d = GrantDecision(articleId)
            if (hasGrant && grantedBy != null) {
                d.live[GrantId("__cached__")] = GrantEntry(null, grantedBy)
            }
            return d
        }
    }
}
