package nl.incedo.paywall.experiments

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.core.ExperimentId
import nl.incedo.paywall.core.VisitorId

class VariantAssignerTest {

    private val experiment = ExperimentDefinition(
        id = ExperimentId("exp-1"),
        name = "Wall strategy comparison",
        variants = listOf(
            Variant("hard", StrategyConfig.Hard, weight = 25),
            Variant("metered", StrategyConfig.Metered(limit = 5), weight = 25),
            Variant("freemium", StrategyConfig.Freemium, weight = 25),
            Variant("control", StrategyConfig.Metered(limit = Int.MAX_VALUE), weight = 25), // EX-04 holdout
        ),
    )

    @Test
    fun assignmentIsDeterministic() {
        // EX-01: stable across visits, no flicker
        val visitor = VisitorId("v-42")
        val first = VariantAssigner.assign(visitor, experiment)
        repeat(100) {
            assertEquals(first, VariantAssigner.assign(visitor, experiment))
        }
    }

    @Test
    fun differentExperimentsAssignIndependently() {
        val other = experiment.copy(id = ExperimentId("exp-2"))
        val assignments = (0 until 200).map { VisitorId("v-$it") }
        val differing = assignments.count {
            VariantAssigner.assign(it, experiment).name != VariantAssigner.assign(it, other).name
        }
        assertTrue(differing > 0, "experiment id must be part of the hash (EX-01)")
    }

    @Test
    fun distributionRoughlyFollowsWeights() {
        val counts = mutableMapOf<String, Int>()
        repeat(4000) { i ->
            val variant = VariantAssigner.assign(VisitorId("visitor-$i"), experiment)
            counts[variant.name] = (counts[variant.name] ?: 0) + 1
        }
        // 25% each of 4000 = 1000; allow generous tolerance for a hash distribution
        counts.forEach { (name, count) ->
            assertTrue(count in 800..1200, "variant $name got $count of 4000")
        }
    }

    @Test
    fun skewedWeightsAreRespected() {
        val skewed = ExperimentDefinition(
            id = ExperimentId("exp-3"),
            name = "90/10",
            variants = listOf(
                Variant("a", StrategyConfig.Hard, weight = 90),
                Variant("b", StrategyConfig.Freemium, weight = 10),
            ),
        )
        val aCount = (0 until 2000).count {
            VariantAssigner.assign(VisitorId("v-$it"), skewed).name == "a"
        }
        assertTrue(aCount in 1700..1900, "expected ~1800 of 2000 in variant a, got $aCount")
    }
}
