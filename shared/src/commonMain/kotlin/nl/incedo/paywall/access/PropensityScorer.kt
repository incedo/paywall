package nl.incedo.paywall.access

import nl.incedo.paywall.analytics.WallEventRecorded
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
 *   - registered visitor bonus — [ScorerWeights.registeredBonus] points when logged in
 *
 * Weights are taken from the [weights] constructor parameter, which maps to
 * [StrategyConfig.Dynamic.scorerWeights] so each variant can tune independently.
 *
 * Note: referrer type (DY-01) is deferred because it requires the referrer to
 * be stored in wall events at ingestion time; it is not available after the fact.
 */
class HeuristicPropensityScorer(private val weights: ScorerWeights = ScorerWeights()) : PropensityScorer {

    private val thirtyDaysMs = 30L * 24 * 3600 * 1000

    override fun score(events: List<DomainEvent>, meterUsed: Int, nowEpochMs: Long, isRegistered: Boolean): Int {
        val wallEvents = events.filterIsInstance<WallEventRecorded>()
        val recentActivity = wallEvents.count { it.occurredAtEpochMs >= nowEpochMs - thirtyDaysMs }
        val daysSinceFirst = wallEvents.minOfOrNull { it.occurredAtEpochMs }
            ?.let { ((nowEpochMs - it) / (24 * 3600 * 1000)).toInt() }
            ?: 0

        val raw = meterUsed * weights.meterWeight +
            recentActivity * weights.activityWeight +
            minOf(daysSinceFirst, weights.tenureCap) +
            if (isRegistered) weights.registeredBonus else 0
        return raw.coerceIn(0, 100)
    }
}
