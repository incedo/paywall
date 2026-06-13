package nl.incedo.paywall.grants

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.GrantId
import nl.incedo.paywall.core.SubjectId

/**
 * DG-03: explicit, specific consent record for data-gated access (DG-01/02).
 * Stored alongside the GrantIssued event so that downstream systems can audit
 * purpose, enforce withdrawal, and demonstrate GDPR compliance.
 *
 * Withdrawal of consent stops future data-gate grants for this subject+purpose
 * but does NOT retroactively revoke already-issued grants (DG-03).
 */
@Serializable
data class DataGateConsentGiven(
    val subjectId: SubjectId,
    /** Human-readable purpose, e.g. "newsletter_signup_article_access". */
    val purposeId: String,
    /** The grant issued in exchange for this consent. */
    val grantId: GrantId,
    val consentAtEpochMs: Long,
    override val tags: Set<String> = setOf(
        "subject:${subjectId.value}",
        "data_gate_consent:${subjectId.value}:$purposeId",
    ),
) : DomainEvent
