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

/**
 * Article rendering (AC-01/BP-01): for an unentitled request the premium
 * body is ABSENT — not hidden. The teaser is server-generated (AC-05).
 *
 * [structuredData]: SEO-01 JSON-LD for paywalled-content schema markup, included
 * in responses served to verified crawlers (MT-05/SEO-02). The edge embeds it in
 * the server-rendered HTML <script type="application/ld+json"> tag (SEO-03).
 */
@Serializable
data class ArticleResponse(
    val id: String,
    val title: String,
    val tier: String,
    /** "full" or "gate". */
    val access: String,
    val body: String? = null,
    val teaser: String? = null,
    val gate: GateInfo? = null,
    val meterUsed: Int? = null,
    val structuredData: String? = null,
)

@Serializable
data class GateInfo(
    val wallType: String,
    val variant: String,
    val meterUsed: Int? = null,
    val meterLimit: Int? = null,
)

/** ADM-04/MT-11 subject inspector: everything support/QA needs about one subject. */
@Serializable
data class SubjectInspectorResponse(
    val subjectId: String,
    /** Assigned variant (EX-01) — only for visitor subjects. */
    val variant: String? = null,
    val meterPeriod: String,
    val meterUsed: Int,
    /** MT-11: article IDs counted toward the meter this period. */
    val meteredArticles: List<String>,
    val entitled: Boolean,
    val liveGrants: List<String>,
    val linkedSubjects: List<String>,
    val recentWallEvents: List<InspectorWallEvent>,
)

@Serializable
data class InspectorWallEvent(
    val type: String,
    val articleId: String? = null,
    val variant: String,
    val channel: String,
    val occurredAtEpochMs: Long,
)

/** ADM-04: meter reset is a support action — audited with actor and reason. */
@Serializable
data class MeterResetRequest(
    val actor: String,
    val reason: String,
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

/**
 * MT-10/ADM-02/API-03: experiment config read and write DTOs.
 * GET /api/v1/admin/config returns [ExperimentConfigResponse].
 * POST /api/v1/admin/config takes [PublishExperimentConfigRequest].
 */
@Serializable
data class ExperimentConfigResponse(
    val experiment: nl.incedo.paywall.experiments.ExperimentDefinition,
    val publishedBy: String?,
    val publishedAtEpochMs: Long?,
    val isDefault: Boolean,
)

@Serializable
data class PublishExperimentConfigRequest(
    val experiment: nl.incedo.paywall.experiments.ExperimentDefinition,
)

/**
 * BP-05: response from POST /api/v1/articles/{id}/share.
 * [token] is the signed opaque token; the caller embeds it in a share URL.
 */
@Serializable
data class ShareTokenResponse(
    val token: String,
    val expiresAtEpochMs: Long,
    val remainingThisMonth: Int,
)

/** BP-05: request body for POST /api/v1/shares/redeem. */
@Serializable
data class RedeemShareTokenRequest(
    val visitorId: String,
    val token: String,
)

/** AN-21/US-07: GDPR account deletion request from the CIAM webhook. */
@Serializable
data class AccountDeletionRequest(
    /** The Ory Kratos / OIDC user ID whose account is being deleted. */
    val userId: String,
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
    /** PW-23: one credit remaining — show the soft registration/subscribe banner. */
    val nudge: Boolean = false,
) {
    companion object {
        fun from(outcome: AccessService.Outcome): DecideResponse = when (val d = outcome.decision) {
            is AccessDecision.Full -> DecideResponse(
                access = "full",
                reason = d.reason.name.lowercase(),
                variant = outcome.variant.name,
                meterUsed = outcome.meterUsedAfter,
                meterLimit = outcome.meterLimit,
                // PW-23: soft banner when exactly one free credit remains
                nudge = outcome.meterLimit?.let { it - outcome.meterUsedAfter == 1 } ?: false,
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
