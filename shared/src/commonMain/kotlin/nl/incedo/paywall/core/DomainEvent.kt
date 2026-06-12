package nl.incedo.paywall.core

/**
 * Base contract for all domain events (DCB style): every event carries the
 * tags it should be queryable by. Decision models are rebuilt from tagged
 * event queries; there are no fixed aggregates.
 */
interface DomainEvent {
    val tags: Set<String>
}
