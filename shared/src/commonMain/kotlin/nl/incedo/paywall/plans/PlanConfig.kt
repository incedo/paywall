package nl.incedo.paywall.plans

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.PlanId

/**
 * PAY-01/01a: subscription plan definition.
 *
 * Two tiers (basic, complete) × two billing periods (monthly, annual) = 4 plans.
 * [rank] enables upsell/downsell logic to reason about "higher" and "lower" plans
 * generically without hard-coding tier names (PAY-01a).
 *
 * PAY-02: optional introductory pricing — first [introPeriodsCount] billing periods
 * are billed at [introPriceMinorUnits] instead of [priceMinorUnits]. When present,
 * the gate and checkout must display the intro price so they match exactly (PAY-02).
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
    /** PAY-02: intro price in minor units, e.g. 100 = €1.00. Null = no intro offer. */
    val introPriceMinorUnits: Long? = null,
    /** PAY-02: number of billing periods at the intro price (e.g. 1 = first month). */
    val introPeriodsCount: Int? = null,
)

/**
 * PAY-01: default plan catalogue for the experiment (configuration-driven; hot-
 * reloadable via API-03). The real payment provider integration replaces these
 * prices without touching paywall logic (PAY-05).
 */
object DefaultPlans {
    val all: List<Plan> = listOf(
        // PAY-02: basic-monthly has a first-month intro offer at €1.00 to drive conversion.
        Plan(PlanId("basic-monthly"),  tier = "basic",    billingPeriod = "monthly", rank = 1, displayName = "Basic Monthly",  priceMinorUnits = 799,   introPriceMinorUnits = 100, introPeriodsCount = 1),
        Plan(PlanId("basic-annual"),   tier = "basic",    billingPeriod = "annual",  rank = 1, displayName = "Basic Annual",   priceMinorUnits = 6999),
        Plan(PlanId("complete-monthly"), tier = "complete", billingPeriod = "monthly", rank = 2, displayName = "Complete Monthly", priceMinorUnits = 1399),
        Plan(PlanId("complete-annual"),  tier = "complete", billingPeriod = "annual",  rank = 2, displayName = "Complete Annual",  priceMinorUnits = 12999),
    )

    fun findById(planId: PlanId): Plan? = all.firstOrNull { it.planId == planId }

    /** PAY-01a: compare two plan IDs by rank; null planId = unsubscribed (rank 0). */
    fun rankOf(planId: PlanId?): Int = if (planId == null) 0 else findById(planId)?.rank ?: 0
}
