package nl.incedo.paywall.analytics

import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId

/**
 * Projection for the experiment dashboard (AN-10): per variant — visitors,
 * wall events, gate CTR, registrations, checkout starts, conversions, and
 * conversion rate (conversions / wall_shown uniques). Rebuilt from the
 * wall-event stream; disposable like every read model (DM-04).
 */
class VariantStatsProjection {

    data class VariantStats(
        val visitors: Int,
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
    }

    private class Accumulator {
        val visitors = mutableSetOf<SubjectId>()
        val wallShownSubjects = mutableSetOf<SubjectId>()
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
        when (event.type) {
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
