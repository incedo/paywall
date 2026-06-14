package nl.incedo.paywall.api

import kotlinx.serialization.Serializable
import nl.incedo.paywall.storybook.ControlState
import nl.incedo.paywall.storybook.ControlType

// ── Story DTOs ────────────────────────────────────────────────────────────────

@Serializable
data class RegisterStoryRequest(
    val storyId: String,
    val storyKey: String,
    val title: String,
    val type: String,
    val groupId: String,
    val owner: String,
    val renderContractRef: String,
    val lifecycle: String = "ACTIVE",
    val coverageScope: String = "ALL",
    val tags: Set<String> = emptySet(),
)

@Serializable
data class StoryResponse(
    val storyId: String,
    val storyKey: String,
    val title: String,
    val type: String,
    val groupId: String,
    val lifecycle: String,
)

// ── Story update DTOs ─────────────────────────────────────────────────────────

@Serializable
data class UpdateStoryRequest(
    val title: String? = null,
    val description: String? = null,
    val owner: String? = null,
)

// ── Scenario DTOs ─────────────────────────────────────────────────────────────

@Serializable
data class RegisterScenarioRequest(
    val scenarioId: String,
    val scenarioKey: String,
    val title: String,
    val type: String,
    val lifecycle: String = "ACTIVE",
)

@Serializable
data class ScenarioResponse(
    val scenarioId: String,
    val storyId: String,
    val scenarioKey: String,
    val title: String,
    val type: String,
    val lifecycle: String,
)

@Serializable
data class UpdateScenarioRequest(
    val title: String? = null,
    val description: String? = null,
    val type: String? = null,
)

// ── ControlSchema DTOs ────────────────────────────────────────────────────────

@Serializable
data class RegisterControlSchemaRequest(
    val schemaId: String,
    val lifecycle: String = "ACTIVE",
)

@Serializable
data class ControlSchemaResponse(
    val schemaId: String,
    val scenarioId: String,
    val lifecycle: String,
    val controls: List<ControlItemResponse>,
)

@Serializable
data class AddControlRequest(
    val controlId: String,
    val key: String,
    val label: String,
    val type: String,
    val defaultValue: String,
    val bindingRef: String,
    val optionsRef: String? = null,
    val validationRules: Set<String> = emptySet(),
)

@Serializable
data class ChangeControlDefaultRequest(
    val defaultValue: String,
)

@Serializable
data class ControlItemResponse(
    val controlId: String,
    val key: String,
    val label: String,
    val type: String,
    val defaultValue: String,
    val bindingRef: String,
    val optionsRef: String? = null,
)

// ── Decorator DTOs ────────────────────────────────────────────────────────────

@Serializable
data class RegisterDecoratorRequest(
    val decoratorId: String,
    val decoratorKey: String,
    val title: String,
    val type: String,
    val renderRef: String,
    val priority: Int = 10,
    val scope: String = "GLOBAL",
    val lifecycle: String = "ACTIVE",
    val configurationRef: String? = null,
)

@Serializable
data class UpdateDecoratorPriorityRequest(
    val priority: Int,
)

@Serializable
data class DecoratorResponse(
    val decoratorId: String,
    val decoratorKey: String,
    val title: String,
    val type: String,
    val scope: String,
    val priority: Int,
    val lifecycle: String,
    val configurationRef: String? = null,
)

fun ControlState.toResponse() = ControlItemResponse(
    controlId = controlId,
    key = key,
    label = label,
    type = type.name,
    defaultValue = defaultValue,
    bindingRef = bindingRef,
    optionsRef = optionsRef,
)
