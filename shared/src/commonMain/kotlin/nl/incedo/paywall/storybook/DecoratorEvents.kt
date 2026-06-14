package nl.incedo.paywall.storybook

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent

// ── Value objects ─────────────────────────────────────────────────────────────

@Serializable @JvmInline value class DecoratorId(val value: String)
@Serializable @JvmInline value class DecoratorKey(val value: String)

/** Classification of what a decorator provides to the preview environment. */
enum class DecoratorType {
    THEME, SURFACE, LOCALE, VIEWPORT, NAVIGATION_SHELL,
    DEPENDENCY_SCOPE, DENSITY, ACCESSIBILITY, DEVICE_FRAME, CUSTOM
}

/** Scope determines which previews the decorator applies to. */
enum class DecoratorScope { GLOBAL, STORY, SCENARIO }

enum class DecoratorLifecycle { ACTIVE, EXPERIMENTAL, DEPRECATED, ARCHIVED }

// ── Tag helpers ───────────────────────────────────────────────────────────────

fun decoratorTag(id: DecoratorId) = "decorator:${id.value}"
fun decoratorKeyTag(key: DecoratorKey) = "decorator-key:${key.value}"
fun decoratorLifecycleTag(lifecycle: DecoratorLifecycle) = "lifecycle:${lifecycle.name.lowercase()}"

// ── Events ────────────────────────────────────────────────────────────────────

/**
 * Establishes a decorator. BR-2: type, renderRef, priority, and scope are mandatory.
 * BR-3: decoratorKey must be globally unique.
 */
@Serializable
data class DecoratorRegistered(
    val decoratorId: DecoratorId,
    val decoratorKey: DecoratorKey,
    val title: String,
    val type: DecoratorType,
    val renderRef: String,
    val priority: Int,
    val scope: DecoratorScope,
    val lifecycle: DecoratorLifecycle = DecoratorLifecycle.ACTIVE,
    val configurationRef: String? = null,
    val registeredAtEpochMs: Long,
    override val tags: Set<String> = setOf(
        decoratorTag(decoratorId),
        decoratorKeyTag(decoratorKey),
        "decorators",
    ),
) : DomainEvent

/** Updates display metadata; mandatory fields may not be cleared (BR-4). */
@Serializable
data class DecoratorMetadataUpdated(
    val decoratorId: DecoratorId,
    val title: String? = null,
    val configurationRef: String? = null,
    val updatedAtEpochMs: Long,
    override val tags: Set<String> = setOf(decoratorTag(decoratorId), "decorators"),
) : DomainEvent

/** Changes the application order. BR-5: priority must be positive. */
@Serializable
data class DecoratorPriorityChanged(
    val decoratorId: DecoratorId,
    val newPriority: Int,
    val changedAtEpochMs: Long,
    override val tags: Set<String> = setOf(decoratorTag(decoratorId), "decorators"),
) : DomainEvent

/** Associates the decorator with a specific story (BR-7: GLOBAL scope disallowed). */
@Serializable
data class DecoratorLinkedToStory(
    val decoratorId: DecoratorId,
    val storyId: StoryId,
    val linkedAtEpochMs: Long,
    override val tags: Set<String> = setOf(
        decoratorTag(decoratorId),
        storyTag(storyId),
        "decorators",
    ),
) : DomainEvent

/** Associates the decorator with a specific scenario (BR-8: scenario must exist). */
@Serializable
data class DecoratorLinkedToScenario(
    val decoratorId: DecoratorId,
    val scenarioId: ScenarioId,
    val linkedAtEpochMs: Long,
    override val tags: Set<String> = setOf(
        decoratorTag(decoratorId),
        scenarioTag(scenarioId),
        "decorators",
    ),
) : DomainEvent

/** Archives the decorator; it may no longer participate in preview assembly (BR-10). */
@Serializable
data class DecoratorArchived(
    val decoratorId: DecoratorId,
    val archivedAtEpochMs: Long,
    override val tags: Set<String> = setOf(
        decoratorTag(decoratorId),
        decoratorLifecycleTag(DecoratorLifecycle.ARCHIVED),
        "decorators",
    ),
) : DomainEvent
