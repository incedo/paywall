package nl.incedo.paywall.backend

import java.util.concurrent.atomic.AtomicReference
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.core.port.EventStore
import nl.incedo.paywall.experiments.ExperimentConfigProjection
import nl.incedo.paywall.experiments.ExperimentDefinition

/**
 * API-03: hot-reloadable experiment configuration backed by the event store.
 *
 * The latest [ExperimentConfigPublished] event wins. Refreshed at most once
 * per [cacheTtlMs] (default 5 s) so config changes propagate within seconds
 * without adding per-request event-store latency.
 *
 * Falls back to [fallback] when no config event has been published yet (cold
 * start or newly provisioned environment).
 */
class ConfigStore(
    private val eventStore: EventStore,
    private val fallback: ExperimentDefinition,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val cacheTtlMs: Long = 5_000L,
) {
    private data class Snapshot(val experiment: ExperimentDefinition, val expiresAtMs: Long)
    private val cached = AtomicReference<Snapshot?>(null)

    suspend fun experiment(): ExperimentDefinition {
        val now = clock()
        val snapshot = cached.get()
        if (snapshot != null && snapshot.expiresAtMs > now) return snapshot.experiment

        val events = eventStore.query(EventQuery(setOf("config:experiment"))).events
        val projection = ExperimentConfigProjection().also { it.applyAll(events) }
        val resolved = projection.current?.experiment ?: fallback
        cached.set(Snapshot(resolved, now + cacheTtlMs))
        return resolved
    }
}
