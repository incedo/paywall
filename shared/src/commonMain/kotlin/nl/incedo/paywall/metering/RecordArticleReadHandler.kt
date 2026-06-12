package nl.incedo.paywall.metering

import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.port.AppendCondition
import nl.incedo.paywall.core.port.ConcurrencyException
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.core.port.EventStore

/** Command: a full premium article was served to the subject (MT-04). */
data class RecordArticleRead(
    val subjectId: SubjectId,
    val articleId: ArticleId,
    val period: MeterPeriod,
)

/**
 * DCB command handler for the meter: query the subject's events, build the
 * decision model, append `MeterIncremented` under an append condition.
 * Retries on concurrent appends for the same subject (Q-7: 3 retries).
 */
class RecordArticleReadHandler(
    private val eventStore: EventStore,
    private val maxRetries: Int = 3,
) {

    /** @return the appended event, or null when the read was already counted (PW-21). */
    suspend fun handle(cmd: RecordArticleRead): MeterIncremented? {
        var attempt = 0
        while (true) {
            try {
                return attempt(cmd)
            } catch (e: ConcurrencyException) {
                attempt += 1
                if (attempt > maxRetries) throw e
            }
        }
    }

    private suspend fun attempt(cmd: RecordArticleRead): MeterIncremented? {
        val query = EventQuery(tags = setOf("subject:${cmd.subjectId.value}"))
        val (events, position) = eventStore.query(query)

        val meter = MeterDecision(cmd.period)
        meter.applyAll(events)

        if (meter.isCounted(cmd.articleId)) return null // PW-21: no double count

        val event = MeterIncremented(
            subjectId = cmd.subjectId,
            articleId = cmd.articleId,
            period = cmd.period,
        )
        eventStore.append(listOf(event), AppendCondition(query, position))
        return event
    }
}
