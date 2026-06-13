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
