package nl.incedo.paywall.storybook

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent

// ── Value objects ─────────────────────────────────────────────────────────────

@Serializable @JvmInline value class ControlSchemaId(val value: String)
@Serializable @JvmInline value class ControlId(val value: String)
@Serializable @JvmInline value class ControlKey(val value: String)

/** Supported control widget types. BR-8: only ENUM/SEGMENTED support options references. */
enum class ControlType { BOOLEAN, TEXT, ENUM, INTEGER, DECIMAL, COLOR_TOKEN, SEGMENTED, SLIDER }

// ── Tag helpers ───────────────────────────────────────────────────────────────

fun controlSchemaTag(id: ControlSchemaId) = "control-schema:${id.value}"
fun controlTag(id: ControlId) = "control:${id.value}"

// ── Events ────────────────────────────────────────────────────────────────────

/**
 * Creates the control schema for a scenario. BR-1: schema must exist before
 * controls can be managed. BR-2: scenarioId must reference a live scenario.
 */
@Serializable
data class ControlSchemaRegistered(
    val schemaId: ControlSchemaId,
    val scenarioId: ScenarioId,
    val lifecycle: String = "ACTIVE",
    val registeredAtEpochMs: Long,
    override val tags: Set<String> = setOf(
        controlSchemaTag(schemaId),
        scenarioTag(scenarioId),
        "control-schemas",
    ),
) : DomainEvent

/**
 * Adds one control to the schema. BR-3: type, default, and bindingRef are
 * mandatory. BR-4: key must be unique within the schema. BR-5: default value
 * must be compatible with the declared type.
 */
@Serializable
data class ControlAdded(
    val schemaId: ControlSchemaId,
    val controlId: ControlId,
    val key: ControlKey,
    val label: String,
    val type: ControlType,
    val defaultValue: String,
    val bindingRef: String,
    val optionsRef: String? = null,
    val validationRules: Set<String> = emptySet(),
    val addedAtEpochMs: Long,
    override val tags: Set<String> = setOf(
        controlSchemaTag(schemaId),
        controlTag(controlId),
        scenarioTag(ScenarioId("unknown")), // populated by handler
    ),
) : DomainEvent

/** Marks a control as removed. BR-10: removed controls are excluded from active views. */
@Serializable
data class ControlRemoved(
    val schemaId: ControlSchemaId,
    val controlId: ControlId,
    val removedAtEpochMs: Long,
    override val tags: Set<String> = setOf(controlSchemaTag(schemaId), controlTag(controlId)),
) : DomainEvent

/** Changes the default value. BR-7: new value must remain type-compatible. */
@Serializable
data class ControlDefaultChanged(
    val schemaId: ControlSchemaId,
    val controlId: ControlId,
    val newDefaultValue: String,
    val changedAtEpochMs: Long,
    override val tags: Set<String> = setOf(controlSchemaTag(schemaId), controlTag(controlId)),
) : DomainEvent

/** Archives the schema; all controls become read-only. BR-11. */
@Serializable
data class ControlSchemaArchived(
    val schemaId: ControlSchemaId,
    val archivedAtEpochMs: Long,
    override val tags: Set<String> = setOf(controlSchemaTag(schemaId), "control-schemas"),
) : DomainEvent
