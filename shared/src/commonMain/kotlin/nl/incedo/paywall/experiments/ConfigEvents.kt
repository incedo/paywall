package nl.incedo.paywall.experiments

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent

/**
 * MT-10/API-03: experiment and meter configuration stored as versioned events
 * so every change is auditable and the config is hot-reloadable without a
 * redeploy. The full [ExperimentDefinition] is embedded so each event is
 * self-contained (additive-schema friendly, no need to replay history).
 */
@Serializable
data class ExperimentConfigPublished(
    val experiment: ExperimentDefinition,
    val actor: String,
    val publishedAtEpochMs: Long,
    override val tags: Set<String> = setOf("config:experiment"),
) : DomainEvent

/**
 * Projects the latest published experiment configuration.
 * Falls back to null when no config event exists — callers use the hard-coded
 * default in that case.
 */
class ExperimentConfigProjection {
    var current: ExperimentConfigPublished? = null
        private set

    fun apply(event: DomainEvent) {
        if (event is ExperimentConfigPublished) current = event
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)
}
