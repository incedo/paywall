package nl.incedo.paywall.metering

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId

/** Meter period, e.g. "2026-06" — one calendar month (PW-24). Deriving the current period from a clock is an adapter concern; the domain treats it as opaque. */
@Serializable @JvmInline value class MeterPeriod(val value: String)

/**
 * A counted read of a premium article (MT-04: only when the full article was
 * actually served). Tagged with the composite subject+period tag ONLY
 * (DM-05): decide-path reads stay bounded to the current period regardless
 * of history length. Cross-period views (ADM-04 inspector) enumerate period
 * tags; they do not scan subject history.
 */
@Serializable
data class MeterIncremented(
    val subjectId: SubjectId,
    val articleId: ArticleId,
    val period: MeterPeriod,
    override val tags: Set<String> = setOf(meterTag(subjectId, period)),
) : DomainEvent

/** Audited meter reset (ADM-04: support action, with actor and reason). */
@Serializable
data class MeterReset(
    val subjectId: SubjectId,
    val period: MeterPeriod,
    val actor: String,
    val reason: String,
    override val tags: Set<String> = setOf(meterTag(subjectId, period)),
) : DomainEvent

/** DM-05 composite tag: bounds every meter query to one subject and one period. */
fun meterTag(subjectId: SubjectId, period: MeterPeriod): String =
    "meter:${subjectId.value}:${period.value}"
