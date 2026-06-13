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
    /** ADM-15: per-locale copy overrides stored with this wall design. */
    val translations: Map<String, WallCopy> = emptyMap(),
)

/** AN-10/AN-11/AN-12 dashboard numbers per variant. */
@Serializable
data class VariantStatsResponse(
    val variant: String,
    val visitors: Int,
    /** AN-11: client-reported page views for reach-cost comparison vs control. */
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
)
