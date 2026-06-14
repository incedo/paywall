package nl.incedo.paywall.storybook

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent

// ── Value objects ─────────────────────────────────────────────────────────────

@Serializable @JvmInline value class GovernancePolicyId(val value: String)
@Serializable @JvmInline value class GovernancePolicyKey(val value: String)
@Serializable @JvmInline value class QualityGateKey(val value: String)
@Serializable @JvmInline value class OwnerRef(val value: String)
@Serializable @JvmInline value class EvidenceRef(val value: String)

enum class LifecycleState { DRAFT, ACTIVE, EXPERIMENTAL, DEPRECATED, ARCHIVED }
enum class GovernanceDecisionOutcome { APPROVED, REJECTED, WAIVED, PENDING }

// ── Tag helpers ───────────────────────────────────────────────────────────────

fun policyTag(id: GovernancePolicyId) = "policy:${id.value}"
fun policyKeyTag(key: GovernancePolicyKey) = "policy-key:${key.value}"

// ── Events ────────────────────────────────────────────────────────────────────

/** Creates a new governance policy. BR-2: key must be unique. */
@Serializable
data class GovernancePolicyRegistered(
    val policyId: GovernancePolicyId,
    val policyKey: GovernancePolicyKey,
    val title: String,
    val lifecycle: LifecycleState = LifecycleState.DRAFT,
    val registeredAtEpochMs: Long,
    override val tags: Set<String> = setOf(policyTag(policyId), policyKeyTag(policyKey), "governance-policies"),
) : DomainEvent

/** Attaches a quality gate to a policy. BR-3: required before ACTIVE. */
@Serializable
data class QualityGateAttached(
    val policyId: GovernancePolicyId,
    val gateKey: QualityGateKey,
    val description: String,
    val attachedAtEpochMs: Long,
    override val tags: Set<String> = setOf(policyTag(policyId), "quality-gate:${gateKey.value}", "governance-policies"),
) : DomainEvent

/** Records the owner for a governed artifact. BR-4: ownership is mandatory. */
@Serializable
data class OwnerAssigned(
    val policyId: GovernancePolicyId,
    val ownerRef: OwnerRef,
    val assignedAtEpochMs: Long,
    override val tags: Set<String> = setOf(policyTag(policyId), "owner:${ownerRef.value}", "governance-policies"),
) : DomainEvent

/** Links a verifiable evidence artifact (test, screenshot, doc, review). BR-5. */
@Serializable
data class EvidenceLinked(
    val policyId: GovernancePolicyId,
    val evidenceRef: EvidenceRef,
    val evidenceType: String,
    val linkedAtEpochMs: Long,
    override val tags: Set<String> = setOf(policyTag(policyId), "evidence:${evidenceRef.value}", "governance-policies"),
) : DomainEvent

/** Records a governance decision (APPROVED / REJECTED / WAIVED). BR-6. */
@Serializable
data class GovernanceDecisionRecorded(
    val policyId: GovernancePolicyId,
    val decision: GovernanceDecisionOutcome,
    val rationale: String? = null,
    val recordedAtEpochMs: Long,
    override val tags: Set<String> = setOf(policyTag(policyId), "decision:${decision.name}", "governance-policies"),
) : DomainEvent

/** Records lifecycle governance approval for a target (story, scenario, component). BR-7. */
@Serializable
data class LifecycleGoverned(
    val policyId: GovernancePolicyId,
    val targetRef: String,
    val targetType: String,
    val newLifecycle: LifecycleState,
    val governedAtEpochMs: Long,
    override val tags: Set<String> = setOf(policyTag(policyId), "lifecycle:${newLifecycle.name}", "governance-policies"),
) : DomainEvent
