package nl.incedo.paywall.backend

import kotlinx.serialization.Serializable
import nl.incedo.paywall.access.AccessDecision
import nl.incedo.paywall.access.StrategyConfig

@Serializable
data class DecideRequest(
    val visitorId: String,
    val userId: String? = null,
    val articleId: String,
    /** "free" or "premium" (PW-01). */
    val tier: String,
)

/**
 * Inbound integration payload: the CEP publishes gate advice as events.
 * `gate = true` records [nl.incedo.paywall.cep.CepGateAdvised],
 * `gate = false` records [nl.incedo.paywall.cep.CepGateAdviceWithdrawn].
 */
@Serializable
data class CepAdviceEvent(
    val subjectId: String,
    val gate: Boolean,
    val validUntilEpochMs: Long? = null,
)

@Serializable
data class DecideResponse(
    /** "full" or "gate" — the premium body itself never travels with a gate decision (AC-01). */
    val access: String,
    val reason: String? = null,
    val variant: String,
    val wallType: String? = null,
    val meterUsed: Int? = null,
    val meterLimit: Int? = null,
) {
    companion object {
        fun from(outcome: AccessService.Outcome): DecideResponse = when (val d = outcome.decision) {
            is AccessDecision.Full -> DecideResponse(
                access = "full",
                reason = d.reason.name.lowercase(),
                variant = outcome.variant.name,
                meterUsed = outcome.meterUsedAfter,
            )
            is AccessDecision.Gated -> DecideResponse(
                access = "gate",
                variant = outcome.variant.name,
                wallType = d.strategy.wallTypeName(),
                meterUsed = d.meterUsed,
                meterLimit = d.meterLimit,
            )
        }

        private fun StrategyConfig.wallTypeName() = when (this) {
            is StrategyConfig.Hard -> "hard"
            is StrategyConfig.Metered -> "metered"
            is StrategyConfig.Freemium -> "freemium"
            is StrategyConfig.Dynamic -> "dynamic"
        }
    }
}
