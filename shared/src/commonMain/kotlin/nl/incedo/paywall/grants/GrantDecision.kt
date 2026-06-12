package nl.incedo.paywall.grants

import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.GrantId

/** Decision model: does the subject hold a live grant for this article? (FGA-01..06) */
class GrantDecision(private val articleId: ArticleId) {

    private val live = mutableMapOf<GrantId, Long?>() // grant → expiresAt

    fun apply(event: DomainEvent) {
        when (event) {
            is GrantIssued -> if (event.articleId == articleId) live[event.grantId] = event.expiresAtEpochMs
            is GrantRevoked -> if (event.articleId == articleId) live.remove(event.grantId)
            else -> {}
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)

    fun hasLiveGrant(nowEpochMs: Long): Boolean =
        live.values.any { expiresAt -> expiresAt == null || expiresAt > nowEpochMs }
}
