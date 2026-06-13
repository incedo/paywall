package nl.incedo.paywall.plans

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.PlanId

/**
 * PAY-01/01a: subscription plan definition.
 *
 * Two tiers (basic, complete) × two billing periods (monthly, annual) = 4 plans.
 * [rank] enables upsell/downsell logic to reason about "higher" and "lower" plans
 * generically without hard-coding tier names (PAY-01a).
 */
@Serializable
data class Plan(
    val planId: PlanId,
    /** "basic" or "complete" — the entitlement tier granted on subscription. */
    val tier: String,
    /** "monthly" or "annual". */
    val billingPeriod: String,
    /** PAY-01a: basic=1, complete=2. Higher rank = higher tier. */
    val rank: Int,
    val displayName: String,
    val priceMinorUnits: Long, // e.g. 999 = €9.99
    val currency: String = "EUR",
)

/**
 * PAY-01: default plan catalogue for the experiment (configuration-driven; hot-
 * reloadable via API-03). The real payment provider integration replaces these
 * prices without touching paywall logic (PAY-05).
 */
object DefaultPlans {
    val all: List<Plan> = listOf(
        Plan(PlanId("basic-monthly"),  tier = "basic",    billingPeriod = "monthly", rank = 1, displayName = "Basic Monthly",  priceMinorUnits = 799),
        Plan(PlanId("basic-annual"),   tier = "basic",    billingPeriod = "annual",  rank = 1, displayName = "Basic Annual",   priceMinorUnits = 6999),
        Plan(PlanId("complete-monthly"), tier = "complete", billingPeriod = "monthly", rank = 2, displayName = "Complete Monthly", priceMinorUnits = 1399),
        Plan(PlanId("complete-annual"),  tier = "complete", billingPeriod = "annual",  rank = 2, displayName = "Complete Annual",  priceMinorUnits = 12999),
    )

    fun findById(planId: PlanId): Plan? = all.firstOrNull { it.planId == planId }

    /** PAY-01a: compare two plan IDs by rank; null planId = unsubscribed (rank 0). */
    fun rankOf(planId: PlanId?): Int = if (planId == null) 0 else findById(planId)?.rank ?: 0
}
