package nl.incedo.paywall.storybook

import nl.incedo.paywall.core.DomainEvent

/**
 * Ephemeral decision model for a single story. Rebuilt from tagged events on
 * every command. Enforces BR-1 (must exist), BR-6 (lifecycle transitions),
 * BR-7 (single group), BR-11 (archived = read-only).
 */
class StoryDecision {
    var exists: Boolean = false
        private set
    var archived: Boolean = false
        private set
    var storyKey: String = ""
        private set
    var title: String = ""
        private set
    var lifecycle: StoryLifecycle = StoryLifecycle.ACTIVE
        private set
    var groupId: String = ""
        private set
    var storyType: String = ""
        private set

    fun apply(event: DomainEvent) {
        when (event) {
            is StoryRegistered -> {
                exists = true
                storyKey = event.storyKey.value
                title = event.title
                lifecycle = event.lifecycle
                groupId = event.groupId
                storyType = event.type.name
            }
            is StoryArchived -> {
                archived = true
                lifecycle = StoryLifecycle.ARCHIVED
            }
            is StoryMetadataUpdated -> {
                event.title?.let { title = it }
            }
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)
}

/** Checks whether a story key is already taken (for BR-3 uniqueness). */
class StoryKeyUniquenessDecision {
    val usedKeys = mutableSetOf<String>()

    fun apply(event: DomainEvent) {
        if (event is StoryRegistered) usedKeys += event.storyKey.value
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)
    fun isTaken(key: String) = key in usedKeys
}
