package nl.incedo.paywall.core.adapter

import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.port.AppendCondition
import nl.incedo.paywall.core.port.ConcurrencyException
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.core.port.EventQueryResult
import nl.incedo.paywall.core.port.EventStore

/**
 * In-memory event store for tests and local development. The production
 * adapter is PostgreSQL (JSONB events + tag index) — see architecture/tech-stack.md.
 */
class InMemoryEventStore : EventStore {

    private data class Stored(val position: Long, val event: DomainEvent)

    private val events = mutableListOf<Stored>()
    private var position = 0L

    override suspend fun query(query: EventQuery): EventQueryResult {
        val matching = events
            .filter { it.position > query.since && it.event.matches(query) }
            .map { it.event }
        return EventQueryResult(events = matching, position = position)
    }

    override suspend fun append(events: List<DomainEvent>, condition: AppendCondition?) {
        if (condition != null) {
            val conflicting = this.events.any {
                it.position > condition.expectedPosition && it.event.matches(condition.query)
            }
            if (conflicting) {
                throw ConcurrencyException(
                    "New events matching ${condition.query.tags} appeared after position ${condition.expectedPosition}",
                )
            }
        }
        events.forEach { event ->
            position += 1
            this.events.add(Stored(position, event))
        }
    }

    private fun DomainEvent.matches(query: EventQuery): Boolean =
        query.tags.isEmpty() || tags.any { it in query.tags }
}
