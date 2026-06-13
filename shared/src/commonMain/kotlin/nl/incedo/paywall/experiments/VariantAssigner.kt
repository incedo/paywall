package nl.incedo.paywall.experiments

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.ExperimentId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.fnv1a32
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
    /**
     * ADM-14: optional reference to a specific wall design version. When set,
     * the gate renderer uses this design instead of the default for the brand.
     * Enables copy/layout A/B tests through the standard experiment framework (PW-07).
     */
    val wallDesignId: String? = null,
)

/**
 * EX-01: deterministic assignment via hash(id + experiment_id) mod total
 * weight — stable across visits, no flicker, platform-independent
 * (FNV-1a, not Kotlin's hashCode).
 *
 * EX-03: when a userId is present the user-keyed assignment takes precedence
 * over the visitor-keyed one, ensuring the same variant is seen across
 * devices after login.
 */
object VariantAssigner {

    fun assign(visitorId: VisitorId, experiment: ExperimentDefinition): Variant =
        assignByKey(visitorId.value, experiment)

    /** EX-03: use userId as the assignment key for authenticated subjects. */
    fun assign(subject: nl.incedo.paywall.access.Subject, experiment: ExperimentDefinition): Variant =
        assignByKey(subject.userId?.value ?: subject.visitorId.value, experiment)

    private fun assignByKey(key: String, experiment: ExperimentDefinition): Variant {
        val total = experiment.variants.sumOf { it.weight }
        val bucket = (fnv1a32("$key:${experiment.id.value}").toLong() and 0xFFFFFFFFL) % total
        var cumulative = 0L
        for (variant in experiment.variants) {
            cumulative += variant.weight
            if (bucket < cumulative) return variant
        }
        return experiment.variants.last()
    }
}
