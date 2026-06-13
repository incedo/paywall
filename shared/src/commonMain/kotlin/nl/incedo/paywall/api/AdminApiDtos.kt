package nl.incedo.paywall.api

import kotlinx.serialization.Serializable
import nl.incedo.paywall.experiments.ExperimentDefinition

/**
 * Admin console API contracts shared between the Ktor backend and the Compose
 * console — one model, every platform (ADM-01, multiplatform rule).
 */

/** ADM-04/MT-11 subject inspector: everything support/QA needs about one subject. */
@Serializable
data class SubjectInspectorResponse(
    val subjectId: String,
    val variant: String? = null,
    val meterPeriod: String,
    val meterUsed: Int,
    val meteredArticles: List<String>,
    val entitled: Boolean,
    val liveGrants: List<String>,
    val linkedSubjects: List<String>,
    val recentWallEvents: List<InspectorWallEvent>,
    val sessions: List<CiamSession> = emptyList(),
)

@Serializable
data class InspectorWallEvent(
    val type: String,
    val articleId: String? = null,
    val variant: String,
    val channel: String,
    val occurredAtEpochMs: Long,
)

/** ADM-04: read-only CIAM session summary for the subject inspector. */
@Serializable
data class CiamSession(
    val id: String,
    val device: String? = null,
    val ipAddress: String? = null,
    val lastActiveAtEpochMs: Long? = null,
)

/** ADM-04: meter reset is a support action — audited with actor and reason. */
@Serializable
data class MeterResetRequest(
    val actor: String,
    val reason: String,
)

/**
 * FGA-08/ADM-03: full grant audit entry for a subject — used in the subject
 * inspector and the standalone grant-management view.
 */
@Serializable
data class GrantAuditEntry(
    val grantId: String,
    val articleId: String,
    val grantedBy: String,
    val reason: String = "",
    val expiresAtEpochMs: Long? = null,
    val isLive: Boolean,
)

/**
 * ADM-02/MT-10/API-03: experiment config read DTO.
 * GET /api/v1/admin/config returns this.
 */
@Serializable
data class ExperimentConfigResponse(
    val experiment: ExperimentDefinition,
    val publishedBy: String?,
    val publishedAtEpochMs: Long?,
    val isDefault: Boolean,
)

/** POST /api/v1/admin/config body. */
@Serializable
data class PublishExperimentConfigRequest(
    val experiment: ExperimentDefinition,
)

/**
 * ADM-10: brand entity — theme tokens, domain, and locale.
 * Returned by GET /api/v1/admin/brands and GET /api/v1/admin/brands/{brandId}.
 */
@Serializable
data class BrandResponse(
    val brandId: String,
    val name: String,
    val domain: String,
    val locale: String,
    /** Opaque JSON object — color/font/logo tokens interpreted by the wall renderer. */
    val themeJson: String,
)

/** ADM-10: create a new brand. */
@Serializable
data class CreateBrandRequest(
    val brandId: String,
    val name: String,
    val domain: String,
    val locale: String = "nl-NL",
    val themeJson: String = "{}",
)

/** ADM-10: update theme tokens for an existing brand. */
@Serializable
data class UpdateBrandThemeRequest(
    val themeJson: String,
    val actor: String = "admin",
)

/**
 * FGA-03/ADM-03: issue or revoke an article-scoped grant.
 * active=true → issue; active=false → revoke.
 * All writes audited by the backend with actor, timestamp, and reason (ADM-03).
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

// ── ADM-03: partner management DTOs (PA-01/02/03/05, IPW-01) ──────────────────

/** PA-01: summary of one partner — returned by GET /api/v1/admin/partners and /{id}. */
@Serializable
data class PartnerResponse(
    val partnerId: String,
    val name: String,
    val maxSeats: Int?,
    val activeSeats: Int,
    val planId: String,
    val activeCidrs: List<String>,
)

/** PA-01: create a new partner with optional seat cap. */
@Serializable
data class CreatePartnerRequest(
    val partnerId: String,
    val name: String,
    val maxSeats: Int? = null,
    val planId: String = "complete",
)

/** PA-02/PA-05: add or remove a member from a partner. */
@Serializable
data class AddPartnerMemberRequest(
    val subjectId: String,
    val addedBy: String = "admin",
)

/** IPW-01: add or deactivate an IP CIDR range for a partner (IPW-03: validated server-side). */
@Serializable
data class PartnerIpRangeRequest(
    val cidr: String,
    val active: Boolean = true,
)
