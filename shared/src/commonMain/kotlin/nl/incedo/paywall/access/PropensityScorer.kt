package nl.incedo.paywall.access

import nl.incedo.paywall.analytics.WallEventRecorded
import nl.incedo.paywall.analytics.WallEventType
import nl.incedo.paywall.core.DomainEvent

/**
 * DY-04: scoring interface for the dynamic paywall. Phase 1 = heuristic
 * (HeuristicPropensityScorer). Phase 2 = trained model or external score —
 * swap the implementation without changing callers.
 */
fun interface PropensityScorer {
    /**
     * Returns a score in [0, 100]. Higher = more likely to convert = gate sooner.
     *
     * @param isRegistered DY-01: whether the subject is a registered (logged-in) visitor.
     */
    fun score(events: List<DomainEvent>, meterUsed: Int, nowEpochMs: Long, isRegistered: Boolean): Int
}

/**
 * DY-01: phase-1 heuristic score from observable event signals:
 *   - premium reads this month (from meter) — strongest conversion predictor
 *   - wall events (page views + wall shown) in the last 30 days — engagement signal
 *   - days since first observed event, capped at [ScorerWeights.tenureCap] — tenure signal
 *   - referrer type on most-recent page_view — search/social signals intent (DY-01)
 *   - registered visitor bonus — [ScorerWeights.registeredBonus] points when logged in
 *
 * Weights are taken from the [weights] constructor parameter, which maps to
 * [StrategyConfig.Dynamic.scorerWeights] so each variant can tune independently.
 *
 * Referrer type is read from `context["referrer"]` on stored PAGE_VIEW events;
 * clients pass it via `POST /api/v1/events` context map. Values: "search", "social", "direct".
 * The most-recent page_view with a non-blank referrer wins; no entry = direct (no bonus).
 */
class HeuristicPropensityScorer(private val weights: ScorerWeights = ScorerWeights()) : PropensityScorer {

    private val thirtyDaysMs = 30L * 24 * 3600 * 1000

    override fun score(events: List<DomainEvent>, meterUsed: Int, nowEpochMs: Long, isRegistered: Boolean): Int {
        val wallEvents = events.filterIsInstance<WallEventRecorded>()
        val recentActivity = wallEvents.count { it.occurredAtEpochMs >= nowEpochMs - thirtyDaysMs }
        val daysSinceFirst = wallEvents.minOfOrNull { it.occurredAtEpochMs }
            ?.let { ((nowEpochMs - it) / (24 * 3600 * 1000)).toInt() }
            ?: 0
        // DY-01: referrer bonus from most-recent page_view with a non-blank referrer context entry.
        val referrerBonus = wallEvents
            .filter { it.eventType == WallEventType.PAGE_VIEW }
            .sortedByDescending { it.occurredAtEpochMs }
            .firstNotNullOfOrNull { it.context["referrer"]?.takeIf { r -> r.isNotBlank() } }
            .let { referrer ->
                when (referrer?.lowercase()) {
                    "search" -> weights.searchReferrerBonus
                    "social" -> weights.socialReferrerBonus
                    else -> 0
                }
            }

        val raw = meterUsed * weights.meterWeight +
            recentActivity * weights.activityWeight +
            minOf(daysSinceFirst, weights.tenureCap) +
            referrerBonus +
            if (isRegistered) weights.registeredBonus else 0
        return raw.coerceIn(0, 100)
    }
}
