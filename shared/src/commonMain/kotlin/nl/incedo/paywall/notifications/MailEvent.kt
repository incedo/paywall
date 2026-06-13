package nl.incedo.paywall.notifications

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId

/**
 * US-10: transactional email log — in the experiment phase emails are not sent
 * to a real mail provider; instead a [MailSent] event is appended to the event
 * store so every "email" is observable, auditable, and queryable per subject.
 *
 * Trigger points (paywall-controlled):
 *   checkout_complete event  → purchase_confirmation
 *   past_due subscription    → payment_failure          (SUB-05 grace period)
 *   canceled subscription    → cancellation_confirmation
 */
@Serializable
data class MailSent(
    val subjectId: SubjectId,
    /** One of: purchase_confirmation | payment_failure | cancellation_confirmation */
    val kind: String,
    val sentAtEpochMs: Long,
    val planId: String? = null,
    val subscriptionRef: String? = null,
    override val tags: Set<String> = setOf(
        "subject:${subjectId.value}",
        MAIL_EVENT_TAG,
        "mail_kind:$kind",
    ),
) : DomainEvent

const val MAIL_EVENT_TAG = "mail_event"
