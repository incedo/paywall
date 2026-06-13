package nl.incedo.paywall.analytics

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId

/**
 * AC-13: records a soft-gate dismissal by the visitor in the Dynamic variant.
 * Tagged with the subject's ID so it is loaded by the decide-path query and
 * checked for a within-session dismissal before applying the soft-gate rule.
 * Hard gates (score ≥ T_hard) are never dismissible.
 */
@Serializable
data class SoftGateDismissed(
    val subjectId: SubjectId,
    val dismissedAtEpochMs: Long,
    /** Dismissals carry the session window duration so replays remain consistent. */
    val sessionWindowMs: Long = SESSION_WINDOW_MS,
    override val tags: Set<String> = setOf("subject:${subjectId.value}"),
) : DomainEvent {

    /** True when this dismissal was recorded within its own session window. */
    fun isWithinSession(nowEpochMs: Long): Boolean =
        nowEpochMs - dismissedAtEpochMs < sessionWindowMs

    companion object {
        /** AC-13: 30-minute session window — standard server-side session approximation. */
        const val SESSION_WINDOW_MS: Long = 30 * 60 * 1_000L
    }
}
