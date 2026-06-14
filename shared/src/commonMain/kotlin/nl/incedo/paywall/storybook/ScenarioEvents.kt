package nl.incedo.paywall.storybook

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent

// ── Value objects ─────────────────────────────────────────────────────────────

@Serializable @JvmInline value class ScenarioId(val value: String)
@Serializable @JvmInline value class ScenarioKey(val value: String)

enum class ScenarioType { STATE, VARIANT, EDGE_CASE, RESPONSIVE, A11Y }
enum class ScenarioLifecycle { ACTIVE, EXPERIMENTAL, DEPRECATED, ARCHIVED }

// ── Tag helpers ───────────────────────────────────────────────────────────────

fun scenarioTag(id: ScenarioId) = "scenario:${id.value}"
fun scenarioKeyTag(storyId: StoryId, key: ScenarioKey) = "scenario-key:${storyId.value}:${key.value}"

// ── Events ────────────────────────────────────────────────────────────────────

/** Registers a scenario under a parent story. Scenario keys are unique per story. */
@Serializable
data class ScenarioRegistered(
    val scenarioId: ScenarioId,
    val storyId: StoryId,
    val scenarioKey: ScenarioKey,
    val title: String,
    val type: ScenarioType,
    val lifecycle: ScenarioLifecycle = ScenarioLifecycle.ACTIVE,
    val registeredAtEpochMs: Long,
    override val tags: Set<String> = setOf(
        scenarioTag(scenarioId),
        storyTag(storyId),
        scenarioKeyTag(storyId, scenarioKey),
        "scenarios",
    ),
) : DomainEvent

/** Scenario archived; its control schemas become read-only. */
@Serializable
data class ScenarioArchived(
    val scenarioId: ScenarioId,
    val archivedAtEpochMs: Long,
    override val tags: Set<String> = setOf(scenarioTag(scenarioId), "scenarios"),
) : DomainEvent
