package nl.incedo.paywall.access

import kotlin.test.Test
import kotlin.test.assertTrue
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.UserId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.entitlements.EntitlementDecision
import nl.incedo.paywall.grants.GrantDecision
import nl.incedo.paywall.metering.MeterDecision
import nl.incedo.paywall.metering.MeterIncremented
import nl.incedo.paywall.metering.MeterPeriod

/**
 * Speed test for the pure decision path: rebuild the decision models from a
 * realistic event history and run the engine. This is the work that sits
 * inside the API-05 budget (< 50 ms p95) on every premium request — the
 * pure part must be a small fraction of it.
 *
 * Budgets here are deliberately loose (CI machines vary); the printed
 * numbers are the real signal.
 */
class EngineSpeedTest {

    private val period = MeterPeriod("2026-06")
    private val subject = SubjectId.of(VisitorId("v-speed"))
    private val now = 1_750_000_000_000L

    /** A heavy subject: 500 meter events this period (far beyond any real visitor). */
    private val history: List<DomainEvent> = (0 until 500).map {
        MeterIncremented(subject, ArticleId("a-$it"), period)
    }

    private fun decideOnce(): AccessDecision {
        val meter = MeterDecision(period).also { it.applyAll(history) }
        val entitlement = EntitlementDecision().also { it.applyAll(history) }
        val grant = GrantDecision(ArticleId("a-new")).also { it.applyAll(history) }
        return AccessDecisionEngine.decide(
            AccessRequest(
                article = Article(ArticleId("a-new"), ContentTier.PREMIUM),
                subject = Subject(VisitorId("v-speed"), UserId("u-speed")),
                strategy = StrategyConfig.Metered(limit = 5),
                entitlement = entitlement,
                grant = grant,
                meter = meter,
                nowEpochMs = now,
            ),
        )
    }

    @Test
    fun decisionModelBuildPlusDecide_p95UnderOneMillisecond() {
        repeat(2_000) { decideOnce() } // JIT warmup

        val samples = LongArray(10_000)
        for (i in samples.indices) {
            val start = System.nanoTime()
            decideOnce()
            samples[i] = System.nanoTime() - start
        }
        samples.sort()
        val p50 = samples[samples.size / 2] / 1_000.0
        val p95 = samples[(samples.size * 95) / 100] / 1_000.0
        val p99 = samples[(samples.size * 99) / 100] / 1_000.0
        println("Engine decide (500-event history): p50=${p50}us p95=${p95}us p99=${p99}us")

        assertTrue(p95 < 1_000.0, "engine p95 was ${p95}us; must stay far below the 50 ms API budget")
    }
}
