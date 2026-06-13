package nl.incedo.paywall.offers

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId

/**
 * UP-04: offer lifecycle events logged on every `decide_offer` evaluation.
 * Both triggered and suppressed evaluations are recorded for AN-02 reporting.
 */
@Serializable
data class OfferTriggered(
    val subjectId: SubjectId,
    val offerId: String,
    val trigger: String,
    val channel: String,
    val kind: String,
    val source: String,
    val triggeredAtEpochMs: Long,
    override val tags: Set<String> = setOf("subject:${subjectId.value}", offerTag(subjectId, offerId)),
) : DomainEvent

@Serializable
data class OfferSuppressed(
    val subjectId: SubjectId,
    val trigger: String,
    val channel: String,
    /** Reason: "ineligible" | "capped" | "none_matched" | "channel_mismatch" |
     *          "cep_timeout" | "cep_error" | "guardrail_rejected" (UP-04). */
    val reason: String,
    val suppressedAtEpochMs: Long,
    /** offerId present only when an offer was received but suppressed post-validation. */
    val offerId: String? = null,
    override val tags: Set<String> = setOf("subject:${subjectId.value}"),
) : DomainEvent

/**
 * UP-05: recorded when a subject explicitly declines an offer. Used for
 * cross-channel frequency capping (30-day default cooldown).
 */
@Serializable
data class OfferDeclined(
    val subjectId: SubjectId,
    val offerId: String,
    val channel: String,
    val declinedAtEpochMs: Long,
    override val tags: Set<String> = setOf("subject:${subjectId.value}", offerTag(subjectId, offerId)),
) : DomainEvent

/**
 * DN-05/DN-06: recorded when a subject explicitly accepts an offer.
 * Used for the rolling-12-month retention-offer cap (DN-05) and for measuring
 * retention value per offer type (AN-10).
 */
@Serializable
data class OfferAccepted(
    val subjectId: SubjectId,
    val offerId: String,
    val kind: String,
    val channel: String,
    val acceptedAtEpochMs: Long,
    override val tags: Set<String> = setOf("subject:${subjectId.value}", offerTag(subjectId, offerId)),
) : DomainEvent

fun offerTag(subjectId: SubjectId, offerId: String): String = "offer:${subjectId.value}:$offerId"
