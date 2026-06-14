package nl.incedo.paywall.storybook

import nl.incedo.paywall.core.DomainEvent

/**
 * ControlSchemaDecisionModel — folds all control-schema:{schemaId} events.
 *
 * Enforced business rules:
 *   BR-1  schema must exist before controls can be added/removed/changed
 *   BR-4  ControlKey unique within schema
 *   BR-5  default value type-compatibility — enforced at validation layer (handler)
 *   BR-7  default change must keep existing type — enforced at handler
 *   BR-10 removed controls excluded from active runtime view
 *   BR-11 archived schema = read-only
 */
class ControlSchemaDecisionModel {
    var exists: Boolean = false
        private set
    var archived: Boolean = false
        private set
    var scenarioId: String = ""
        private set

    // controlId → ControlState; removed controls stay in map with removed=true (BR-10)
    private val _controls = mutableMapOf<String, ControlState>()
    val controls: Map<String, ControlState> get() = _controls

    fun apply(event: DomainEvent) {
        when (event) {
            is ControlSchemaRegistered -> {
                exists = true
                scenarioId = event.scenarioId.value
            }
            is ControlAdded -> _controls[event.controlId.value] = ControlState(
                controlId = event.controlId.value,
                key = event.key.value,
                label = event.label,
                type = event.type,
                defaultValue = event.defaultValue,
                bindingRef = event.bindingRef,
                optionsRef = event.optionsRef,
                validationRules = event.validationRules,
                removed = false,
            )
            is ControlRemoved -> _controls[event.controlId.value]?.let {
                _controls[event.controlId.value] = it.copy(removed = true)
            }
            is ControlDefaultChanged -> _controls[event.controlId.value]?.let {
                _controls[event.controlId.value] = it.copy(defaultValue = event.newDefaultValue)
            }
            is ControlSchemaArchived -> archived = true
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)

    /** Active (non-removed) controls for runtime rendering. */
    fun activeControls(): List<ControlState> = _controls.values.filter { !it.removed }

    /** BR-4: check if a key is already taken by an active control. */
    fun isKeyTaken(key: String): Boolean =
        _controls.values.any { !it.removed && it.key == key }

    /** BR-10: check if a control exists and is not removed. */
    fun isActive(controlId: String): Boolean =
        _controls[controlId]?.removed == false
}

data class ControlState(
    val controlId: String,
    val key: String,
    val label: String,
    val type: ControlType,
    val defaultValue: String,
    val bindingRef: String,
    val optionsRef: String? = null,
    val validationRules: Set<String> = emptySet(),
    val groupId: String? = null,
    val removed: Boolean = false,
)
