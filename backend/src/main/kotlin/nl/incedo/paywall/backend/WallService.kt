package nl.incedo.paywall.backend

import nl.incedo.paywall.core.WallId
import nl.incedo.paywall.core.port.AppendCondition
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.core.port.EventStore
import nl.incedo.paywall.walls.WallConfig
import nl.incedo.paywall.walls.WallConfigChanged
import nl.incedo.paywall.walls.WallCreated
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

    private fun wallQuery(id: WallId) = EventQuery(setOf("wall:${id.value}"))

    private suspend fun projectionFor(id: WallId): Pair<WallProjection, Long> {
        val result = eventStore.query(wallQuery(id))
        return WallProjection().also { it.applyAll(result.events) } to result.position
    }
}
