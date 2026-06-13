package nl.incedo.paywall.analytics

import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId

import kotlin.math.sqrt

/**
 * Projection for the experiment dashboard (AN-10/AN-11/AN-12): per variant —
 * visitors, page views, article reads, wall events, gate CTR, registrations,
 * checkout starts, conversions, conversion rate, reach cost (AN-11), and
 * Wilson confidence intervals (AN-12). Rebuilt from the wall-event stream;
 * disposable like every read model (DM-04).
 */
class VariantStatsProjection {

    data class VariantStats(
        val visitors: Int,
        /** AN-11: client-reported page views — basis for reach-cost comparison. */
        val pageViews: Int,
        val articleReads: Int,
        val wallsShown: Int,
        val wallShownUniques: Int,
        val gateCtaClicks: Int,
        val registrations: Int,
        val checkoutStarts: Int,
        val conversions: Int,
    ) {
        /** Gate click-through rate: clicks / walls shown. */
        val gateCtr: Double get() = if (wallsShown == 0) 0.0 else gateCtaClicks.toDouble() / wallsShown

        /** AN-10: conversions / unique subjects that saw a wall. */
        val conversionRate: Double get() = if (wallShownUniques == 0) 0.0 else conversions.toDouble() / wallShownUniques

        /** AN-12: Wilson 95% CI lower bound for the conversion rate. */
        val conversionRateLow: Double get() = wilsonLow(conversions, wallShownUniques)

        /** AN-12: Wilson 95% CI upper bound for the conversion rate. */
        val conversionRateHigh: Double get() = wilsonHigh(conversions, wallShownUniques)

        /** AN-12: true when n < 100; CI is too wide to compare variants reliably. */
        val sampleSizeTooSmall: Boolean get() = wallShownUniques < 100
    }

    private class Accumulator {
        val visitors = mutableSetOf<SubjectId>()
        val wallShownSubjects = mutableSetOf<SubjectId>()
        var pageViews = 0
        var articleReads = 0
        var wallsShown = 0
        var gateCtaClicks = 0
        var registrations = 0
        var checkoutStarts = 0
        var conversions = 0
    }

    private val byVariant = mutableMapOf<String, Accumulator>()

    fun apply(event: DomainEvent) {
        if (event !is WallEventRecorded) return
        val acc = byVariant.getOrPut(event.variant) { Accumulator() }
        acc.visitors.add(event.subjectId)
        when (event.eventType) {
            WallEventType.PAGE_VIEW -> acc.pageViews += 1
            WallEventType.ARTICLE_READ -> acc.articleReads += 1
            WallEventType.WALL_SHOWN -> {
                acc.wallsShown += 1
                acc.wallShownSubjects.add(event.subjectId)
            }
            WallEventType.GATE_CTA_CLICK -> acc.gateCtaClicks += 1
            WallEventType.REGISTER_COMPLETE -> acc.registrations += 1
            WallEventType.CHECKOUT_START -> acc.checkoutStarts += 1
            WallEventType.CHECKOUT_COMPLETE -> acc.conversions += 1
            else -> {}
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)

    fun stats(): Map<String, VariantStats> = byVariant.mapValues { (_, acc) ->
        VariantStats(
            visitors = acc.visitors.size,
            pageViews = acc.pageViews,
            articleReads = acc.articleReads,
            wallsShown = acc.wallsShown,
            wallShownUniques = acc.wallShownSubjects.size,
            gateCtaClicks = acc.gateCtaClicks,
            registrations = acc.registrations,
            checkoutStarts = acc.checkoutStarts,
            conversions = acc.conversions,
        )
    }
}

// AN-12: Wilson score interval at 95% confidence (z = 1.96).
private const val Z95 = 1.96

private fun wilsonLow(successes: Int, n: Int): Double {
    if (n == 0) return 0.0
    val p = successes.toDouble() / n
    val z2 = Z95 * Z95
    val denom = 1.0 + z2 / n
    val centre = p + z2 / (2 * n)
    val margin = Z95 * sqrt(p * (1 - p) / n + z2 / (4 * n * n))
    return maxOf(0.0, (centre - margin) / denom)
}

private fun wilsonHigh(successes: Int, n: Int): Double {
    if (n == 0) return 0.0
    val p = successes.toDouble() / n
    val z2 = Z95 * Z95
    val denom = 1.0 + z2 / n
    val centre = p + z2 / (2 * n)
    val margin = Z95 * sqrt(p * (1 - p) / n + z2 / (4 * n * n))
    return minOf(1.0, (centre + margin) / denom)
}
