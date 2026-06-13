package nl.incedo.paywall.grants

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId

/**
 * BP-05: records that a subscriber issued a signed share token for an article.
 * Tagged for cap enforcement: [shareMonthTag] bounds the monthly issuance count
 * to one subscriber and one calendar month without scanning all history.
 */
@Serializable
data class ShareTokenIssued(
    val tokenId: String,
    val subscriberSubjectId: SubjectId,
    val articleId: ArticleId,
    val expiresAtEpochMs: Long,
    val issuedAtEpochMs: Long,
    /** "YYYY-MM" — same period format as the meter (PW-24). */
    val monthPeriod: String,
    override val tags: Set<String> = setOf(
        "subject:${subscriberSubjectId.value}",
        shareMonthTag(subscriberSubjectId, monthPeriod),
    ),
) : DomainEvent

/** Composite tag for monthly cap queries: bounded to one subscriber + one month. */
fun shareMonthTag(subjectId: SubjectId, monthPeriod: String): String =
    "share:${subjectId.value}:$monthPeriod"
