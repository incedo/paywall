package nl.incedo.paywall.experiments

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.ExperimentId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.access.StrategyConfig

/**
 * Experiment definition (EX-02): variants with paywall strategy + traffic
 * weights. Stored as configuration, editable without deploy.
 */
@Serializable
data class ExperimentDefinition(
    val id: ExperimentId,
    val name: String,
    val variants: List<Variant>,
) {
    init {
        require(variants.isNotEmpty()) { "An experiment needs at least one variant" }
        require(variants.all { it.weight > 0 }) { "Variant weights must be positive" }
    }
}

@Serializable
data class Variant(
    val name: String,
    val strategy: StrategyConfig,
    val weight: Int,
)

/**
 * EX-01: deterministic assignment via hash(visitor_id + experiment_id) mod
 * total weight — stable across visits, no flicker, platform-independent
 * (FNV-1a, not Kotlin's hashCode).
 */
object VariantAssigner {

    fun assign(visitorId: VisitorId, experiment: ExperimentDefinition): Variant {
        val total = experiment.variants.sumOf { it.weight }
        val bucket = (fnv1a32("${visitorId.value}:${experiment.id.value}").toLong() and 0xFFFFFFFFL) % total
        var cumulative = 0L
        for (variant in experiment.variants) {
            cumulative += variant.weight
            if (bucket < cumulative) return variant
        }
        return experiment.variants.last()
    }

    private fun fnv1a32(input: String): Int {
        var hash = -0x7ee3623b // 0x811C9DC5
        for (byte in input.encodeToByteArray()) {
            hash = hash xor (byte.toInt() and 0xFF)
            hash *= 0x01000193
        }
        return hash
    }
}
