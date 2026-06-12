package nl.incedo.paywall.cep

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId

/**
 * Integration events published by the CEP (Doc 7 scope boundary: propensity
 * scoring, thresholds and campaign logic live there). The access layer never
 * calls the CEP synchronously — it acts on the advice it has ingested, which
 * also keeps the decide path inside the API-05 budget.
 */
@Serializable
data class CepGateAdvised(
    val subjectId: SubjectId,
    /** Advice goes stale as behaviour changes; the CEP bounds its own verdicts. Null = until withdrawn. */
    val validUntilEpochMs: Long? = null,
    override val tags: Set<String> = setOf("subject:${subjectId.value}"),
) : DomainEvent

@Serializable
data class CepGateAdviceWithdrawn(
    val subjectId: SubjectId,
    override val tags: Set<String> = setOf("subject:${subjectId.value}"),
) : DomainEvent
