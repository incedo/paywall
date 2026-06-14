package nl.incedo.paywall.storybook

import nl.incedo.paywall.core.DomainEvent

/**
 * Ephemeral decision model for a single decorator.
 * Enforces BR-1 (must exist), BR-4 (metadata), BR-5 (priority),
 * BR-7 (GLOBAL scope not linkable as STORY-exclusive), BR-9 (lifecycle),
 * BR-10 (archived = read-only).
 */
class DecoratorDecision {
    var exists: Boolean = false
        private set
    var archived: Boolean = false
        private set
    var decoratorKey: String = ""
        private set
    var title: String = ""
        private set
    var type: DecoratorType = DecoratorType.CUSTOM
        private set
    var scope: DecoratorScope = DecoratorScope.GLOBAL
        private set
    var priority: Int = 0
        private set
    var lifecycle: DecoratorLifecycle = DecoratorLifecycle.ACTIVE
        private set

    fun apply(event: DomainEvent) {
        when (event) {
            is DecoratorRegistered -> {
                exists = true
                decoratorKey = event.decoratorKey.value
                title = event.title
                type = event.type
                scope = event.scope
                priority = event.priority
                lifecycle = event.lifecycle
            }
            is DecoratorMetadataUpdated -> event.title?.let { title = it }
            is DecoratorPriorityChanged -> priority = event.newPriority
            is DecoratorArchived -> {
                archived = true
                lifecycle = DecoratorLifecycle.ARCHIVED
            }
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)
}

/** Global key uniqueness guard (BR-3). */
class DecoratorKeyUniquenessDecision {
    private val usedKeys = mutableSetOf<String>()

    fun apply(event: DomainEvent) {
        if (event is DecoratorRegistered) usedKeys += event.decoratorKey.value
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)
    fun isTaken(key: String) = key in usedKeys
}
