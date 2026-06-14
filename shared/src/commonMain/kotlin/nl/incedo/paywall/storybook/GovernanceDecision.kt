package nl.incedo.paywall.storybook

import nl.incedo.paywall.api.GovernancePolicyResponse
import nl.incedo.paywall.core.DomainEvent

data class QualityGateState(val gateKey: String, val description: String)
data class EvidenceState(val evidenceRef: String, val evidenceType: String)
data class DecisionState(val decision: GovernanceDecisionOutcome, val rationale: String?)
data class LifecycleGovernanceState(val targetRef: String, val targetType: String, val newLifecycle: LifecycleState)

class GovernanceDecisionModel {
    var exists: Boolean = false
    var policyKey: String = ""
    var title: String = ""
    var lifecycle: LifecycleState = LifecycleState.DRAFT
    val qualityGates: MutableList<QualityGateState> = mutableListOf()
    var owner: String? = null
    val evidenceRefs: MutableList<EvidenceState> = mutableListOf()
    val decisions: MutableList<DecisionState> = mutableListOf()
    val lifecycleGovernance: MutableList<LifecycleGovernanceState> = mutableListOf()

    fun apply(event: DomainEvent) {
        when (event) {
            is GovernancePolicyRegistered -> {
                exists = true
                policyKey = event.policyKey.value
                title = event.title
                lifecycle = event.lifecycle
            }
            is QualityGateAttached -> qualityGates.add(QualityGateState(event.gateKey.value, event.description))
            is OwnerAssigned -> owner = event.ownerRef.value
            is EvidenceLinked -> evidenceRefs.add(EvidenceState(event.evidenceRef.value, event.evidenceType))
            is GovernanceDecisionRecorded -> decisions.add(DecisionState(event.decision, event.rationale))
            is LifecycleGoverned -> lifecycleGovernance.add(LifecycleGovernanceState(event.targetRef, event.targetType, event.newLifecycle))
        }
    }

    fun applyAll(events: List<DomainEvent>) = events.forEach(::apply)
}

fun GovernanceDecisionModel.toResponse(policyId: String) = GovernancePolicyResponse(
    policyId = policyId,
    policyKey = policyKey,
    title = title,
    lifecycle = lifecycle.name,
    owner = owner,
    qualityGates = qualityGates.map { it.gateKey },
    evidenceRefs = evidenceRefs.map { it.evidenceRef },
    lastDecision = decisions.lastOrNull()?.decision?.name,
)

class PolicyKeyUniquenessDecision {
    private val usedKeys = mutableSetOf<String>()

    fun apply(event: DomainEvent) {
        if (event is GovernancePolicyRegistered) usedKeys.add(event.policyKey.value)
    }

    fun applyAll(events: List<DomainEvent>) = events.forEach(::apply)
    fun isTaken(key: String) = key in usedKeys
}
