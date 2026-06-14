package nl.incedo.paywall.backend

import nl.incedo.paywall.api.SaveWallRequest
import nl.incedo.paywall.api.VariantStatsResponse
import nl.incedo.paywall.api.WallResponse
import nl.incedo.paywall.core.WallId
import nl.incedo.paywall.core.port.AppendCondition
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.core.port.EventStore
import nl.incedo.paywall.walls.WallConfig
import nl.incedo.paywall.walls.WallConfigChanged
import nl.incedo.paywall.walls.WallCreated
import nl.incedo.paywall.walls.WallLayout
import nl.incedo.paywall.walls.WallLayoutChanged
import nl.incedo.paywall.walls.WallLayoutValidator
import nl.incedo.paywall.walls.WallProjection
import nl.incedo.paywall.walls.WallPublished
import nl.incedo.paywall.walls.WallView

/**
 * Wall definition management for the designer (ADM-01/02/11). Writes go
 * through DCB conditions on the wall's tag, so two editors saving the same
 * wall concurrently surface as a conflict instead of a lost update (ADM-06).
 */
class WallService(private val eventStore: EventStore) {

    sealed interface SaveResult {
        data class Saved(val view: WallView) : SaveResult
        data object NotFound : SaveResult
        data class VersionConflict(val current: Int) : SaveResult
        /** ADM-13: rollback target version does not exist in history. */
        data object VersionNotFound : SaveResult
        /** VWE-02: layout has structural violations (returned by [saveLayout]). */
        data class LayoutInvalid(val violations: List<String>) : SaveResult
    }

    suspend fun list(): List<WallView> {
        val events = eventStore.query(EventQuery(setOf("walls"))).events
        return WallProjection().also { it.applyAll(events) }.all()
    }

    suspend fun get(id: WallId): WallView? = projectionFor(id).first.byId(id)

    suspend fun create(id: WallId, config: WallConfig, actor: String): SaveResult {
        val (projection, position) = projectionFor(id)
        if (projection.byId(id) != null) return SaveResult.VersionConflict(projection.byId(id)!!.version)
        eventStore.append(
            listOf(WallCreated(id, config, actor)),
            AppendCondition(wallQuery(id), position),
        )
        return SaveResult.Saved(get(id)!!)
    }

    suspend fun update(id: WallId, config: WallConfig, actor: String, expectedVersion: Int?): SaveResult {
        val (projection, position) = projectionFor(id)
        val current = projection.byId(id) ?: return SaveResult.NotFound
        if (expectedVersion != null && expectedVersion != current.version) {
            return SaveResult.VersionConflict(current.version)
        }
        eventStore.append(
            listOf(WallConfigChanged(id, config, actor)),
            AppendCondition(wallQuery(id), position),
        )
        return SaveResult.Saved(get(id)!!)
    }

    suspend fun publish(id: WallId, actor: String): SaveResult {
        val (projection, position) = projectionFor(id)
        if (projection.byId(id) == null) return SaveResult.NotFound
        eventStore.append(
            listOf(WallPublished(id, actor)),
            AppendCondition(wallQuery(id), position),
        )
        return SaveResult.Saved(get(id)!!)
    }

    /**
     * VWE-03: save a [WallLayout] from the block editor, appending a [WallLayoutChanged] event.
     * Enforces VWE-02 structural rules server-side (ADM-01); returns [SaveResult.LayoutInvalid]
     * when violations exist so callers can surface them before appending to the stream.
     */
    suspend fun saveLayout(id: WallId, layout: WallLayout, actor: String, expectedVersion: Int?): SaveResult {
        val violations = WallLayoutValidator.validate(layout)
        if (violations.isNotEmpty()) return SaveResult.LayoutInvalid(violations)
        val (projection, position) = projectionFor(id)
        val current = projection.byId(id) ?: return SaveResult.NotFound
        if (expectedVersion != null && expectedVersion != current.version) {
            return SaveResult.VersionConflict(current.version)
        }
        eventStore.append(
            listOf(WallLayoutChanged(id, layout, actor)),
            AppendCondition(wallQuery(id), position),
        )
        return SaveResult.Saved(get(id)!!)
    }

    /**
     * ADM-13: rollback to a previous version by re-applying its config as a new
     * WallConfigChanged event. In event-sourcing terms this is "time travel forward"
     * — history is preserved; the old config becomes the new draft.
     */
    suspend fun rollback(id: WallId, targetVersion: Int, actor: String): SaveResult {
        val result = eventStore.query(wallQuery(id))
        if (result.events.isEmpty()) return SaveResult.NotFound
        // Walk the event history to find the config at the target version
        val configAtVersion = configHistory(result.events)
        val targetConfig = configAtVersion[targetVersion] ?: return SaveResult.VersionNotFound
        eventStore.append(
            listOf(WallConfigChanged(id, targetConfig, actor)),
            AppendCondition(wallQuery(id), result.position),
        )
        return SaveResult.Saved(get(id)!!)
    }

    /** Returns a map of version → WallConfig for every versioned config event. */
    private fun configHistory(events: List<nl.incedo.paywall.core.DomainEvent>): Map<Int, WallConfig> {
        val history = mutableMapOf<Int, WallConfig>()
        var version = 0
        for (event in events) {
            when (event) {
                is WallCreated -> { version = 1; history[version] = event.config }
                is WallConfigChanged -> { version++; history[version] = event.config }
                else -> {}
            }
        }
        return history
    }

    private fun wallQuery(id: WallId) = EventQuery(setOf("wall:${id.value}"))

    private suspend fun projectionFor(id: WallId): Pair<WallProjection, Long> {
        val result = eventStore.query(wallQuery(id))
        return WallProjection().also { it.applyAll(result.events) } to result.position
    }
}
