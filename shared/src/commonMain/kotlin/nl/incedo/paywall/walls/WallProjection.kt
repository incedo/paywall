package nl.incedo.paywall.walls

import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.WallId

/** Read model for the designer (walls overview + workspace). */
data class WallView(
    val id: WallId,
    val config: WallConfig,
    /** draft | published — editing a published wall returns it to draft. */
    val status: String,
    /** Number of config versions so far (ADM-06 version history). */
    val version: Int,
    val lastEditedBy: String,
)

/** Folds wall events into [WallView]s; disposable like every read model (DM-04). */
class WallProjection {

    private val views = mutableMapOf<WallId, WallView>()

    fun apply(event: DomainEvent) {
        when (event) {
            is WallCreated -> views[event.wallId] = WallView(
                id = event.wallId,
                config = event.config,
                status = "draft",
                version = 1,
                lastEditedBy = event.actor,
            )
            is WallConfigChanged -> views[event.wallId]?.let { current ->
                views[event.wallId] = current.copy(
                    config = event.config,
                    status = "draft", // edits always go through draft (ADM-06)
                    version = current.version + 1,
                    lastEditedBy = event.actor,
                )
            }
            is WallPublished -> views[event.wallId]?.let { current ->
                views[event.wallId] = current.copy(status = "published", lastEditedBy = event.actor)
            }
            else -> {}
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)

    fun all(): List<WallView> = views.values.sortedBy { it.config.name }

    fun byId(id: WallId): WallView? = views[id]
}
