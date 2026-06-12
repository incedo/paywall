package nl.incedo.paywall.backend

import kotlinx.serialization.Serializable
import nl.incedo.paywall.access.AccessDecision
import nl.incedo.paywall.access.StrategyConfig

/**
 * AC-07: there is deliberately no userId field — logged-in identity comes
 * exclusively from the validated CIAM JWT in the Authorization header.
 */
@Serializable
data class DecideRequest(
    val visitorId: String,
    val articleId: String,
    /** "free" or "premium" (PW-01). */
    val tier: String,
    /** API-06: web | app | chat. */
    val channel: String = "web",
)

/**
 * Client-originated funnel event (AN-02 subset). The variant is resolved
 * server-side (EX-01) — never trusted from the client.
 */
@Serializable
data class ClientEventRequest(
    val type: String,
    val visitorId: String,
    val articleId: String? = null,
    val channel: String = "web",
    val context: Map<String, String> = emptyMap(),
)

@Serializable
data class VariantStatsResponse(
    val variant: String,
    val visitors: Int,
    val articleReads: Int,
    val wallsShown: Int,
    val gateCtaClicks: Int,
    val gateCtr: Double,
    val registrations: Int,
    val checkoutStarts: Int,
    val conversions: Int,
    val conversionRate: Double,
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

/**
 * Inbound integration payload (MT-13): a consent-based identity link signal.
 * `link = false` records a compensating unlink (support correction).
 */
@Serializable
data class IdentityLinkRequest(
    val subjectA: String,
    val subjectB: String,
    /** e.g. "login", "newsletter_token", "share_token", "device_login" */
    val cause: String,
    val link: Boolean = true,
)

/**
 * Inbound integration payload: entitlement changes published by the external
 * subscription administration (AC-02, EA-*). `active = false` records a
 * revocation. The paywall never manages subscriptions — it ingests changes.
 */
@Serializable
data class EntitlementChangeRequest(
    val subjectId: String,
    val subscriptionRef: String,
    val planId: String? = null,
    val validUntilEpochMs: Long? = null,
    val active: Boolean = true,
)

/**
 * Grant management payload (FGA-03): article-scoped access independent of
 * subscriptions — day/week passes with TTL (PW-08), share-token redemptions
 * (BP-05), support grants. `active = false` revokes.
 */
@Serializable
data class GrantChangeRequest(
    val grantId: String,
    val subjectId: String,
    val articleId: String,
    /** e.g. "day_pass", "share_token", "ad_gated", "support" */
    val grantedBy: String = "support",
    val expiresAtEpochMs: Long? = null,
    val active: Boolean = true,
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
