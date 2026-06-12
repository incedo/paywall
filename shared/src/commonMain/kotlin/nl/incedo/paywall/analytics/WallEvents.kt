package nl.incedo.paywall.analytics

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId

/**
 * The minimum funnel event set (AN-02). Server-observed events (wall_shown,
 * article_read) are appended by the decide path; client-originated events
 * (gate_cta_click, wall_dismissed, …) arrive via the events endpoint.
 */
@Serializable
enum class WallEventType {
    PAGE_VIEW,
    ARTICLE_READ,
    WALL_SHOWN,
    WALL_DISMISSED,
    GATE_CTA_CLICK,
    REGISTER_START,
    REGISTER_COMPLETE,
    CHECKOUT_START,
    CHECKOUT_COMPLETE,
    LOGIN,
    CANCEL,
    OFFER_TRIGGERED,
    OFFER_SUPPRESSED,
    OFFER_ACCEPTED,
    OFFER_DECLINED,
}

/**
 * Server-side logged funnel event (AN-01): subject, variant, channel
 * (web|app|chat, API-06), article and free-form context. WALL_SHOWN events
 * carry the decision context (AN-03): wall type, meter state, CEP advice
 * and floor state. Tagged for the export/stats stream (AN-04/AN-10) and for
 * the per-subject inspector (ADM-04).
 */
@Serializable
data class WallEventRecorded(
    val type: WallEventType,
    val subjectId: SubjectId,
    val variant: String,
    val channel: String,
    val occurredAtEpochMs: Long,
    val articleId: ArticleId? = null,
    val context: Map<String, String> = emptyMap(),
    override val tags: Set<String> = setOf(
        "wall-event",
        "subject:${subjectId.value}",
    ),
) : DomainEvent
