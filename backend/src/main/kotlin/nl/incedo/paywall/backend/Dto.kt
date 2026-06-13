package nl.incedo.paywall.backend

import kotlinx.serialization.Serializable
import nl.incedo.paywall.access.AccessDecision
import nl.incedo.paywall.access.StrategyConfig
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
    /** FGA-01: source system (e.g. "day_pass", "ad_gated", "support"). */
    val grantedBy: String = "support",
    /** FGA-01: human-readable audit reason (e.g. "support ticket 4711"). */
    val reason: String = "",
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

/**
 * FGA-08: full audit trail entry for a single grant — shown in the
 * internal admin/debug endpoint (GET /api/v1/admin/subjects/{id}/grants).
 * Includes who granted what and whether the grant is still live.
 */
@Serializable
data class GrantAuditEntry(
    val grantId: String,
    val articleId: String,
    /** FGA-01: source system (e.g. "day_pass", "ad_gated", "support", "ai_engine"). */
    val grantedBy: String,
    /** FGA-01: human-readable audit reason. */
    val reason: String = "",
    /** Null = no expiry. Milliseconds since epoch. */
    val expiresAtEpochMs: Long? = null,
    /** True when not revoked and not expired. */
    val isLive: Boolean,
)

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

/**
 * ADM-16: wall design template — brand-neutral layout/copy that can be
 * instantiated for any brand. Theme tokens come from the target brand at
 * render time; they are NOT stored in the template.
 */
@Serializable
data class WallTemplateRequest(
    val name: String,
    val wallType: String,
    val title: String,
    val body: String,
    val primaryCta: String,
    val secondaryCta: String,
    val channels: Set<String> = setOf("web"),
    val translations: Map<String, nl.incedo.paywall.walls.WallCopy> = emptyMap(),
)

/** ADM-16: response shape for a saved wall template. */
@Serializable
data class WallTemplateResponse(
    val id: String,
    val name: String,
    val wallType: String,
    val title: String,
    val body: String,
    val primaryCta: String,
    val secondaryCta: String,
    val channels: Set<String>,
    val translations: Map<String, nl.incedo.paywall.walls.WallCopy>,
    val createdBy: String,
)

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
    validForSeconds = this?.validForSeconds,
    cta = this?.cta,
    fromPlanId = this?.fromPlanId,
    toPlanId = this?.toPlanId,
    pauseMonths = this?.pauseMonths,
    source = this?.source,
    trigger = this?.trigger,
    channels = this?.channels ?: emptySet(),
)
