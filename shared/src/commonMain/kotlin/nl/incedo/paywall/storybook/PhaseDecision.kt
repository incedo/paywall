package nl.incedo.paywall.storybook

import nl.incedo.paywall.core.DomainEvent

class PhaseDecisionModel {
    var exists: Boolean = false
    var phaseKey: String = ""
    var phaseName: String = ""
    var phaseOrder: Int = 0
    var lifecycle: PhaseLifecycle = PhaseLifecycle.PLANNED
    val capabilities: MutableMap<String, String> = mutableMapOf() // key → title

    fun apply(event: DomainEvent) {
        when (event) {
            is PhaseRegistered -> {
                exists = true
                phaseKey = event.phaseKey.value
                phaseName = event.phaseName.value
                phaseOrder = event.phaseOrder
                lifecycle = event.lifecycle
            }
            is CapabilityAddedToPhase -> capabilities[event.capabilityKey.value] = event.title
            is PhaseActivated -> lifecycle = PhaseLifecycle.ACTIVE
            is PhaseSatisfied -> lifecycle = PhaseLifecycle.SATISFIED
            is PhaseSuperseded -> lifecycle = PhaseLifecycle.SUPERSEDED
        }
    }

    fun applyAll(events: List<DomainEvent>) = events.forEach(::apply)
}

class PhaseKeyUniquenessDecision {
    private val usedKeys = mutableSetOf<String>()

    fun apply(event: DomainEvent) {
        if (event is PhaseRegistered) usedKeys.add(event.phaseKey.value)
    }

    fun applyAll(events: List<DomainEvent>) = events.forEach(::apply)
    fun isTaken(key: String) = key in usedKeys
}
