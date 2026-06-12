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

/** AN-10 dashboard numbers per variant. */
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
