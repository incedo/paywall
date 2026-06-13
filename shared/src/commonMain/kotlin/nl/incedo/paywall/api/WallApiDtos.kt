package nl.incedo.paywall.api

import kotlinx.serialization.Serializable

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
