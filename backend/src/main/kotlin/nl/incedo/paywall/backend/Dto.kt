package nl.incedo.paywall.backend

import kotlinx.serialization.Serializable
import nl.incedo.paywall.access.AccessDecision
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.api.AddPartnerMemberRequest
import nl.incedo.paywall.api.BrandResponse
import nl.incedo.paywall.api.CiamSession
import nl.incedo.paywall.api.CreateBrandRequest
import nl.incedo.paywall.api.CreatePartnerRequest
import nl.incedo.paywall.api.GrantChangeRequest
import nl.incedo.paywall.api.PartnerIpRangeRequest
import nl.incedo.paywall.api.PartnerResponse
import nl.incedo.paywall.api.WallTemplateRequest
import nl.incedo.paywall.api.WallTemplateResponse
import nl.incedo.paywall.api.ExperimentConfigResponse
import nl.incedo.paywall.api.GrantAuditEntry
import nl.incedo.paywall.api.InspectorWallEvent
import nl.incedo.paywall.api.MeterResetRequest
import nl.incedo.paywall.api.OfferChannelStatsResponse
import nl.incedo.paywall.api.OfferStatsResponse
import nl.incedo.paywall.api.PublishExperimentConfigRequest
import nl.incedo.paywall.api.SubjectInspectorResponse
import nl.incedo.paywall.api.UpdateBrandThemeRequest
import nl.incedo.paywall.cep.Offer

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
    /** DY-06: caller-supplied propensity score (0–100) from an external model (e.g. CDP LtC score).
     *  When present, replaces the heuristic for the Dynamic strategy. */
    val externalScore: Int? = null,
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

// SubjectInspectorResponse, InspectorWallEvent, CiamSession, MeterResetRequest
// are defined in shared module (nl.incedo.paywall.api.AdminApiDtos) and imported above.

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
    /** NFR-03/SUB-02: provider-assigned event ID for idempotent processing (e.g. Stripe-Event-Id). */
    val webhookEventId: String? = null,
)

/**
 * PAY-03/PAY-05: checkout initiation request. The backend creates a payment session
 * via the [PaymentProvider] interface and returns the session ID + redirect URL.
 * [offerId] is the upsell offer accepted at checkout (UP-10/UP-11, optional).
 * [paymentMethod] selects the payment method for the session (PAY-06: "ideal" or
 * "credit_card"; null = no preference, provider picks default).
 */
@kotlinx.serialization.Serializable
data class CheckoutRequest(
    val subjectId: String,
    val planId: String,
    val channel: String = "web",
    val offerId: String? = null,
    /** AC-12: article URL to return to after payment completes. */
    val returnUrl: String? = null,
    /** PAY-06: selected payment method ("ideal" | "credit_card"). Null = no preference. */
    val paymentMethod: String? = null,
)

/**
 * SUB-04: subscriber-initiated cancellation request. Access is retained until
 * [validUntilEpochMs] (the current billing period end, supplied by the provider;
 * null = revoke immediately). [channel] is forwarded to OfferService for DN-01.
 */
@kotlinx.serialization.Serializable
data class CancelSubscriptionRequest(
    val subjectId: String,
    val planId: String? = null,
    /** Current billing period end — access retained until this time (SUB-03). Null = immediate. */
    val validUntilEpochMs: Long? = null,
    val channel: String = "web",
)

/**
 * SUB-06: optional cancel-flow survey payload. Both fields may be null when
 * the subscriber skips the survey (still logs the skip for funnel completeness).
 * [reason] is one of: too_expensive, not_enough_content, technical_issues,
 * temporary_break, other.
 * [freeText] is trimmed to 500 chars server-side.
 */
@kotlinx.serialization.Serializable
data class CancellationSurveyRequest(
    val subjectId: String,
    val reason: String? = null,
    val freeText: String? = null,
)

// GrantChangeRequest, ExperimentConfigResponse, and PublishExperimentConfigRequest
// are defined in the shared module (nl.incedo.paywall.api.AdminApiDtos) and imported above.

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
 * API-07/UP-02: response from POST /api/v1/offers/request — the outbound CEP call result.
 * Returns null offer when the CEP decides no offer applies.
 *
 * All fields from the UP-02 offer object are exposed so the client can render the offer
 * without further lookups (gate copy, checkout pre-fill, decline flow).
 */
@Serializable
data class OfferResponse(
    val offerId: String?,
    val kind: String?,
    val discountPercent: Int? = null,
    /** UP-02: fixed discount in minor currency units (e.g. cents); alternative to discountPercent. */
    val discountFixed: Int? = null,
    /** UP-02: how many billing periods the discount applies (null = indefinite). */
    val discountDurationPeriods: Int? = null,
    val validForSeconds: Long? = null,
    val cta: String? = null,
    /** UP-02: plan the subscriber is currently on (null if not subscribed or not applicable). */
    val fromPlanId: String? = null,
    /** UP-02: plan being offered (null for access_grant offers). */
    val toPlanId: String? = null,
    /** UP-02: pause duration in months; non-null only for pause offers. */
    val pauseMonths: Int? = null,
    /** UP-02: CEP campaign/rule reference for audit trail. */
    val source: String? = null,
    /** UP-02: trigger that caused this offer (e.g. "cancel_intent", "gate_shown"). */
    val trigger: String? = null,
    /** UP-02a: channels on which this offer may be presented; empty = all channels. */
    val channels: Set<String> = emptySet(),
)

/** IPW-01: edge polling response — all partner → CIDR mappings for IP matching. */
@Serializable
data class IpAllowlistEntry(
    val partnerId: String,
    val cidrs: List<String>,
)

/** PAY-01/01a/02: plan catalogue entry returned by GET /api/v1/plans. */
@Serializable
data class PlanResponse(
    val planId: String,
    val tier: String,
    val billingPeriod: String,
    val rank: Int,
    val displayName: String,
    val priceMinorUnits: Long,
    val currency: String,
    /** PAY-02: intro price in minor units; null when no introductory offer exists. */
    val introPriceMinorUnits: Long? = null,
    /** PAY-02: number of billing periods at the intro price; null when no intro offer. */
    val introPeriodsCount: Int? = null,
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

// GrantAuditEntry, OfferStatsResponse, OfferChannelStatsResponse defined in shared module and imported above.

/**
 * PA-04: partner usage stats for contract management — reads per partner and
 * unique user count, derived from the wall-event stream.
 */
@Serializable
data class PartnerUsageResponse(
    val partnerId: String,
    val totalReads: Int,
    val uniqueUsers: Int,
)

/**
 * UP-08: inbound payload from the CEP when it pushes an offer for an async channel.
 * Mirrors [nl.incedo.paywall.cep.Offer] but is a serializable backend DTO.
 */
@Serializable
data class CepOfferPushRequest(
    val subjectId: String,
    val channel: String,
    val offerId: String,
    val kind: String,
    val fromPlanId: String? = null,
    val toPlanId: String? = null,
    val discountPercent: Int? = null,
    val validForSeconds: Long? = null,
    val pauseMonths: Int? = null,
    val channels: Set<String> = emptySet(),
    val source: String = "",
    val cta: String? = null,
)

/** UP-08: an offer that has been triggered for a subject but not yet accepted/declined. */
@Serializable
data class PendingOfferResponse(
    val offerId: String,
    val kind: String,
    val channel: String,
    val triggeredAtEpochMs: Long,
)

// WallTemplateRequest and WallTemplateResponse are defined in shared (WallApiDtos.kt) and imported above.

/**
 * BP-06: bypass-rate estimation response.
 * DL-03: suspicious traffic is reported, not auto-blocked, in the experiment phase.
 */
@Serializable
data class BypassRateResponse(
    /** Total wall-shown events (gated render count) in the queried window. */
    val gatedRenders: Int,
    /** Gated renders that arrived with a bot or suspicious-IP marker. */
    val markedGatedRenders: Int,
    /** Flagged ARTICLE_READ events — possible successful bypasses. */
    val flaggedReads: Int,
    /** markedGatedRenders / gatedRenders; 0.0 when no gated renders observed. */
    val bypassRate: Double,
    /** Event store position at the time of the query — use as `since` for incremental polling. */
    val storePosition: Long,
    val note: String = "DL-03: reported only — not auto-blocked in experiment phase",
)

/**
 * PR-03: profile completeness for a subject — traits collected with consent (AN-20).
 * Exposed so the CEP can segment on profile completeness without access to raw PII.
 * Each trait maps a purposeId to when the subject consented (epoch ms).
 */
@Serializable
data class ProfileCompletenessResponse(
    val subjectId: String,
    /** purposeId → consentAtEpochMs for each active consent record (AN-20). */
    val consentedPurposes: Map<String, Long>,
    /** Derived completeness score: number of consented purposes / total known purposes (0.0–1.0). */
    val completenessScore: Double,
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
    /**
     * PW-50: true when the variant requires registration before the paywall strategy.
     * Anonymous visitors see a registration wall; once registered the normal gate applies.
     */
    val registrationRequired: Boolean = false,
    /**
     * DY-05: what each other (non-assigned, non-killed) variant would have decided for
     * this exact request, computed from the same decision models. Key = variant name,
     * value = "gate" | "full". Empty when forceVariant is active (EX-05 debug mode).
     */
    val counterfactuals: Map<String, String> = emptyMap(),
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
                counterfactuals = outcome.counterfactuals, // DY-05
            )
            is AccessDecision.Gated -> DecideResponse(
                access = "gate",
                variant = outcome.variant.name,
                wallType = d.strategy.wallTypeName(),
                meterUsed = d.meterUsed,
                meterLimit = d.meterLimit,
                wallDesignId = outcome.variant.wallDesignId, // ADM-14
                tierLocked = d.tierLocked, // UP-12
                registrationRequired = d.registrationRequired, // PW-50
                counterfactuals = outcome.counterfactuals, // DY-05
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

/** AN-13: one row in the cohort report (week of first visit). */
@Serializable
data class CohortStatsResponse(
    val cohortWeek: String,
    val visitors: Int,
    val conversions: Int,
    val conversionRate: Double,
    val retainedAt30Days: Int,
    val retentionRate: Double,
)

/** UP-02: maps a nullable [Offer] domain object to its API representation. */
fun Offer?.toOfferResponse(): OfferResponse = OfferResponse(
    offerId = this?.offerId,
    kind = this?.kind,
    discountPercent = this?.discountPercent,
    discountFixed = this?.discountFixed,
    discountDurationPeriods = this?.discountDurationPeriods,
    validForSeconds = this?.validForSeconds,
    cta = this?.cta,
    fromPlanId = this?.fromPlanId,
    toPlanId = this?.toPlanId,
    pauseMonths = this?.pauseMonths,
    source = this?.source,
    trigger = this?.trigger,
    channels = this?.channels ?: emptySet(),
)
