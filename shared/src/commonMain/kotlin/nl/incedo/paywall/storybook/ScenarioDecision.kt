package nl.incedo.paywall.storybook

import nl.incedo.paywall.core.DomainEvent

/**
 * Ephemeral decision model for a single scenario. Enforces: must exist before
 * control schemas can be added (ControlSchema BR-2); archived scenarios make
 * their control schemas read-only.
 */
class ScenarioDecision {
    var exists: Boolean = false
        private set
    var archived: Boolean = false
        private set
    var storyId: String = ""
        private set
    var title: String = ""
        private set
    var scenarioType: String = ""
        private set

    fun apply(event: DomainEvent) {
        when (event) {
            is ScenarioRegistered -> {
                exists = true
                storyId = event.storyId.value
                title = event.title
                scenarioType = event.type.name
            }
            is ScenarioMetadataUpdated -> {
                event.title?.let { title = it }
                event.type?.let { scenarioType = it.name }
            }
            is ScenarioArchived -> archived = true
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)
}

/** Uniqueness guard for scenario keys within a story (one key per story). */
class ScenarioKeyUniquenessDecision {
    private val usedKeys = mutableSetOf<String>()

    fun apply(event: DomainEvent) {
        if (event is ScenarioRegistered)
            usedKeys += "${event.storyId.value}:${event.scenarioKey.value}"
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)
    fun isTaken(storyId: StoryId, key: ScenarioKey) =
        "${storyId.value}:${key.value}" in usedKeys
}
