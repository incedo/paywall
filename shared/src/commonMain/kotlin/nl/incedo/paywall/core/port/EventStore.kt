package nl.incedo.paywall.core.port

import nl.incedo.paywall.core.DomainEvent

/**
 * Event store port (hexagonal): the single write target of the system.
 * Append conditions implement DCB optimistic concurrency — an append is
 * rejected when new events matching the decision's query have appeared
 * since the decision model was built.
 */
interface EventStore {
    suspend fun query(query: EventQuery): EventQueryResult
    suspend fun append(events: List<DomainEvent>, condition: AppendCondition?)
}

data class EventQuery(
    /** Events must match at least one of these tags. Empty = match all. */
    val tags: Set<String>,
    /** Exclusive lower bound on event position. */
    val since: Long = 0,
)

data class EventQueryResult(
    val events: List<DomainEvent>,
    /** Latest store position at query time; feeds the append condition. */
    val position: Long,
)

data class AppendCondition(
    /** Must be the query the decision model was built from. */
    val query: EventQuery,
    /** Reject when matching events exist beyond this position. */
    val expectedPosition: Long,
)

class ConcurrencyException(message: String) : Exception(message)
