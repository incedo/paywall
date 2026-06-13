package nl.incedo.paywall.entitlements

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.PlanId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.SubscriptionId

/**
 * DM-02/AC-02/AC-08: entitlements are derived exclusively from subscription
 * integration events — the paywall never sets them directly. [EntitlementGranted]
 * arrives via the entitlement webhook (or checkout completion); [EntitlementRevoked]
 * cancels access. The paywall records and enforces; the subscription administration
 * system is the authority (EA-* concern, external).
 */
@Serializable
data class EntitlementGranted(
    val subjectId: SubjectId,
    val planId: PlanId,
    val subscriptionRef: SubscriptionId,
    /** End of the paid period (AC-02: access is lost at the end of it). Null = open-ended until revoked. */
    val validUntilEpochMs: Long? = null,
    override val tags: Set<String> = setOf("subject:${subjectId.value}"),
) : DomainEvent

@Serializable
data class EntitlementRevoked(
    val subjectId: SubjectId,
    val subscriptionRef: SubscriptionId,
    override val tags: Set<String> = setOf("subject:${subjectId.value}"),
) : DomainEvent

/**
 * SUB-07: subscription billing paused → access off immediately.
 * The subscription record is retained so billing can auto-resume.
 * Decision model treats any paused ref as no-access even if active map still holds it.
 */
@Serializable
data class SubscriptionPaused(
    val subjectId: SubjectId,
    val subscriptionRef: SubscriptionId,
    val planId: PlanId,
    val pausedAtEpochMs: Long,
    override val tags: Set<String> = setOf("subject:${subjectId.value}"),
) : DomainEvent

/** SUB-07: scheduled resume — clears the paused state, restoring access. */
@Serializable
data class SubscriptionResumed(
    val subjectId: SubjectId,
    val subscriptionRef: SubscriptionId,
    val resumedAtEpochMs: Long,
    override val tags: Set<String> = setOf("subject:${subjectId.value}"),
) : DomainEvent

/**
 * SUB-06: optional single-question cancellation survey. Logged when the
 * subscriber submits (or skips) the survey during the cancel flow.
 * [reason] is one of the offered choices (e.g. "too_expensive",
 * "not_enough_content", "technical_issues", "temporary_break", "other").
 * [freeText] captures any open-ended elaboration (trimmed to 500 chars).
 * Both are null when the subscriber skips the survey.
 */
@Serializable
data class CancellationSurveySubmitted(
    val subjectId: SubjectId,
    val reason: String? = null,
    val freeText: String? = null,
    val submittedAtEpochMs: Long,
    override val tags: Set<String> = setOf("subject:${subjectId.value}", CANCEL_SURVEY_TAG),
) : DomainEvent

const val CANCEL_SURVEY_TAG = "cancel_survey"
