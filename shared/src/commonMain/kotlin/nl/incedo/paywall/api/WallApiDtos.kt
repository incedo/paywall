package nl.incedo.paywall.api

import kotlinx.serialization.Serializable
import nl.incedo.paywall.walls.WallCopy

/**
 * Wall designer API contract (ADM-01: everything the console does is also
 * possible via API). Shared between the Ktor backend and the Compose
 * console — one model, every platform (multiplatform rule).
 */
@Serializable
data class SaveWallRequest(
    val name: String,
    val wallType: String,
    val title: String,
    val body: String,
    val primaryCta: String,
    val secondaryCta: String,
    val channels: Set<String> = setOf("web"),
    val actor: String = "console",
    /** Optimistic concurrency: the version the editor was looking at (ADM-06). */
    val expectedVersion: Int? = null,
    /** ADM-10: optional brand this wall design belongs to. */
    val brandId: String? = null,
    /**
     * AC-14: when true the client must show a GDPR cookie-consent step before
     * the subscription gate. Distinct from DG-03 (data-exchange consent) and
     * PAY-* (payment consent). Configured in the wall editor (ADM-11).
     */
    val requireConsentStep: Boolean = false,
    /** ADM-11: optional image URL shown in the gate (empty = no image block). */
    val imageUrl: String = "",
    /** ADM-17: alt text for the image (empty = decorative / aria-hidden). */
    val imageAlt: String = "",
    /** ADM-11: optional legal/disclaimer text rendered below CTAs (empty = no block). */
    val legalText: String = "",
    /**
     * ADM-15: per-locale copy overrides. Keys are BCP-47 locale tags (e.g. "nl-NL").
     * Only the fields that differ from the default copy need to be provided.
     */
    val translations: Map<String, WallCopy> = emptyMap(),
)

@Serializable
data class WallResponse(
    val id: String,
    val name: String,
    val wallType: String,
    val title: String,
    val body: String,
    val primaryCta: String,
    val secondaryCta: String,
    val channels: Set<String>,
    val status: String,
    val version: Int,
    val lastEditedBy: String,
    /** ADM-10: brand this wall design is associated with, if any. */
    val brandId: String? = null,
    /** AC-14: whether this wall design requires a consent step before the gate. */
    val requireConsentStep: Boolean = false,
    /** ADM-11: optional image URL shown in the gate (empty = no image block). */
    val imageUrl: String = "",
    /** ADM-17: alt text for the image (empty = decorative / aria-hidden). */
    val imageAlt: String = "",
    /** ADM-11: optional legal/disclaimer text rendered below CTAs (empty = no block). */
    val legalText: String = "",
    /** ADM-15: per-locale copy overrides stored with this wall design. */
    val translations: Map<String, WallCopy> = emptyMap(),
)

/**
 * ADM-13: one entry in the audit history of a wall design.
 * Returned by GET /api/v1/walls/{id}/history (newest first).
 */
@Serializable
data class WallVersionSummary(
    val version: Int,
    /** "draft" or "published" — state after this event. */
    val status: String,
    /** Staff user who recorded this change. */
    val actor: String,
)

/**
 * ADM-16: create or update a wall design template.
 * Templates are brand-neutral; theme tokens come from the target brand at render time.
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
    val imageUrl: String = "",
    val imageAlt: String = "",
    val legalText: String = "",
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
    val imageUrl: String = "",
    val imageAlt: String = "",
    val legalText: String = "",
    val translations: Map<String, nl.incedo.paywall.walls.WallCopy>,
    val createdBy: String,
)

/** AN-10/AN-11/AN-12 dashboard numbers per variant. */
@Serializable
data class VariantStatsResponse(
    val variant: String,
    val visitors: Int,
    val pageViews: Int,
    val articleReads: Int,
    val wallsShown: Int,
    val gateCtaClicks: Int,
    val gateCtr: Double,
    val registrations: Int,
    val checkoutStarts: Int,
    val conversions: Int,
    val conversionRate: Double,
    /** AN-12: Wilson 95% confidence interval lower bound. */
    val conversionRateLow: Double,
    /** AN-12: Wilson 95% confidence interval upper bound. */
    val conversionRateHigh: Double,
    /** AN-12: true when n < 100 — CI too wide to draw reliable conclusions. */
    val sampleSizeTooSmall: Boolean,
    /**
     * AN-11: change in page views vs. the EX-04 control variant.
     * Negative = fewer views than control (reach cost); positive = more.
     * Null when no control variant is configured in the experiment.
     */
    val pageViewsDeltaVsControl: Int? = null,
    /**
     * AN-11: change in article reads vs. the EX-04 control variant.
     * Null when no control variant is configured.
     */
    val articleReadsDeltaVsControl: Int? = null,
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
 * BP-06: bypass-rate estimation — ratio of gated renders with bypass markers.
 * DL-03: reported only, not auto-blocked in experiment phase.
 */
@Serializable
data class BypassRateResponse(
    val gatedRenders: Int,
    val markedGatedRenders: Int,
    val flaggedReads: Int,
    /** markedGatedRenders / gatedRenders; 0.0 when no gated renders observed. */
    val bypassRate: Double,
    val storePosition: Long,
    val note: String = "DL-03: reported only — not auto-blocked in experiment phase",
)
