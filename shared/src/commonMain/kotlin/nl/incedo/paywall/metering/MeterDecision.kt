package nl.incedo.paywall.metering

import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.DomainEvent

/**
 * DCB decision model for the meter invariant (PW-20/21, MT-02): built from
 * the subject's events for one period, then queried by the command handler
 * and the access decision engine.
 */
class MeterDecision(private val period: MeterPeriod) {

    private val countedArticles = mutableSetOf<ArticleId>()

    fun apply(event: DomainEvent) {
        when (event) {
            is MeterIncremented -> if (event.period == period) countedArticles.add(event.articleId)
            is MeterReset -> if (event.period == period) countedArticles.clear()
            else -> {}
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)

    /** Credits used this period (PW-22: "2 of 5 free articles this month"). */
    val used: Int get() = countedArticles.size

    /** PW-21: a re-read of an already-counted article consumes no extra credit. */
    fun isCounted(articleId: ArticleId): Boolean = articleId in countedArticles

    /** PW-20: the visitor may read `limit` premium articles; article limit+1 hits the gate. */
    fun hasCreditFor(articleId: ArticleId, limit: Int): Boolean =
        isCounted(articleId) || used < limit
}
