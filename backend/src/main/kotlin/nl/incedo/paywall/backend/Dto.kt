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
 * subscription administration (AC-02, EA-*).
 *
 * Prefer [status] over the legacy [active] boolean:
 *   active    → grant (open-ended or until [validUntilEpochMs])
 *   canceled  → grant with access until [validUntilEpochMs] (current period end, SUB-03)
 *   past_due  → grant with 7-day grace window (SUB-05)
 *   paused    → billing suspended, access off immediately (SUB-07)
 *   expired   → hard revoke
 *
 * [active]=false is the legacy revoke path; still accepted for back-compat.
 */
@Serializable
data class EntitlementChangeRequest(
    val subjectId: String,
    val subscriptionRef: String,
    val planId: String? = null,
    val validUntilEpochMs: Long? = null,
    /** Legacy boolean; ignored when [status] is present. */
    val active: Boolean = true,
    /** SUB-07: subscription status from the payment provider. */
    val status: String? = null,
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

/**
 * API-07: response from POST /api/v1/offers/request — the outbound CEP call result.
 * Returns null offer when the CEP decides no offer applies.
 */
@Serializable
data class OfferResponse(
    val offerId: String?,
    val kind: String?,
    val discountPercent: Int? = null,
    val validForSeconds: Long? = null,
    val cta: String? = null,
)

/**
 * ADM-10: brand creation/update request.
 * Theme tokens are an opaque JSON object interpreted by the wall renderer.
 */
@Serializable
data class CreateBrandRequest(
    val brandId: String,
    val name: String,
    val domain: String,
    val locale: String = "nl-NL",
    val themeJson: String = "{}",
)

@Serializable
data class UpdateBrandThemeRequest(
    val themeJson: String,
    val actor: String = "admin",
)

@Serializable
data class BrandResponse(
    val brandId: String,
    val name: String,
    val domain: String,
    val locale: String,
    val themeJson: String,
)

/** PA-01: partner creation request. */
@Serializable
data class CreatePartnerRequest(
    val partnerId: String,
    val name: String,
    val maxSeats: Int? = null,
    val planId: String = "complete",
)

/** PA-02/PA-05: add member to partner. */
@Serializable
data class AddPartnerMemberRequest(
    val subjectId: String,
    val addedBy: String = "admin",
)

/** IPW-01: add or deactivate an IP CIDR range for a partner. */
@Serializable
data class PartnerIpRangeRequest(
    val cidr: String,
    val active: Boolean = true,
)

/** IPW-01: edge polling response — all partner → CIDR mappings for IP matching. */
@Serializable
data class IpAllowlistEntry(
    val partnerId: String,
    val cidrs: List<String>,
)

/** PA-01: partner state summary for admin inspection. */
@Serializable
data class PartnerResponse(
    val partnerId: String,
    val name: String,
    val maxSeats: Int?,
    val activeSeats: Int,
    val planId: String,
    val activeCidrs: List<String>,
)

/** PAY-01/01a: plan catalogue entry returned by GET /api/v1/plans. */
@Serializable
data class PlanResponse(
    val planId: String,
    val tier: String,
    val billingPeriod: String,
    val rank: Int,
    val displayName: String,
    val priceMinorUnits: Long,
    val currency: String,
)

/**
 * AG-02: verified ad-completion signal from the third-party ad player.
 * The paywall issues a 24h grant for the triggering article, subject to a
 * daily cap of 2 rewarded unlocks per subject.
 */
@Serializable
data class AdCompletionRequest(
    val subjectId: String,
    val articleId: String,
    /** Provider-assigned ad play ID for idempotency tracking. */
    val adPlayId: String,
)

/**
 * DG-02: data-gate completion webhook payload from CIAM or survey platform.
 * Records explicit GDPR consent (DG-03) and issues a 7-day grant (DG-02).
 */
@Serializable
data class DataGateCompletionRequest(
    val subjectId: String,
    /** Human-readable purpose, e.g. "newsletter_signup_article_access". */
    val purposeId: String,
    /** Provider-assigned completion ID for idempotency tracking. */
    val completionId: String,
    /** Optional — if null the grant applies to any article ("*"). */
    val articleId: String? = null,
)

/** AN-14: channel breakdown within a single offer's performance stats. */
@Serializable
data class OfferChannelStatsResponse(
    val triggered: Int,
    val accepted: Int,
    val declined: Int,
    val suppressed: Int,
)

/** AN-14: per offer_id performance stats for the staff stats dashboard. */
@Serializable
data class OfferStatsResponse(
    val offerId: String,
    val triggered: Int,
    val accepted: Int,
    val declined: Int,
    val suppressed: Int,
    val acceptanceRate: Double,
    val channels: Map<String, OfferChannelStatsResponse>,
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
    /**
     * ADM-14: wall design version assigned to this variant, if any. The gate
     * renderer resolves brand → wall design → this override when non-null.
     */
    val wallDesignId: String? = null,
    /**
     * UP-12: true when a basic subscriber requests complete-tier content. The
     * client should show a tier-upgrade offer instead of the base subscribe wall.
     */
    val tierLocked: Boolean = false,
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
                wallDesignId = outcome.variant.wallDesignId, // ADM-14
            )
            is AccessDecision.Gated -> DecideResponse(
                access = "gate",
                variant = outcome.variant.name,
                wallType = d.strategy.wallTypeName(),
                meterUsed = d.meterUsed,
                meterLimit = d.meterLimit,
                wallDesignId = outcome.variant.wallDesignId, // ADM-14
                tierLocked = d.tierLocked, // UP-12
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
