package nl.incedo.paywall.designer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import nl.incedo.paywall.api.GrantAuditEntry
import nl.incedo.paywall.api.SubjectInspectorResponse
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmPrimaryButton
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmTag
import nl.incedo.paywall.ui.CrmText
import nl.incedo.paywall.ui.CrmTextField

/**
 * ADM-04: support/QA view — look up any subject by ID and inspect their
 * meter state, entitlements, active grants, CIAM sessions, variant assignment,
 * and recent wall events. Meter reset and grant revoke are available here (ADM-03).
 */
@Composable
fun SubjectInspectorScreen(
    onInspect: suspend (String) -> SubjectInspectorResponse?,
    onGrants: suspend (String) -> List<GrantAuditEntry>,
    onMeterReset: suspend (String) -> Boolean,
    statusMessage: String?,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var inspector by remember { mutableStateOf<SubjectInspectorResponse?>(null) }
    var grants by remember { mutableStateOf<List<GrantAuditEntry>>(emptyList()) }
    var status by remember { mutableStateOf(statusMessage) }

    fun lookup() = scope.launch {
        val id = query.trim()
        if (id.isBlank()) return@launch
        status = "Loading…"
        inspector = runCatching { onInspect(id) }.getOrElse { status = "Not found or backend unreachable"; null }
        grants = if (inspector != null) runCatching { onGrants(id) }.getOrDefault(emptyList()) else emptyList()
        if (inspector != null) status = null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(CrmTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xxs)) {
            CrmText("Subject inspector", style = CrmTheme.typography.h1, color = CrmTheme.colors.onBackground)
            CrmText(
                status ?: "Look up any visitor_id or user_id to inspect their state (ADM-04)",
                style = CrmTheme.typography.bodySmall,
                color = CrmTheme.colors.onSurfaceVariant,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrmTextField(
                label = "visitor:… or user:…",
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
            )
            CrmPrimaryButton("Inspect", onClick = { lookup() })
        }

        inspector?.let { sub ->
            // ── identity & meter ─────────────────────────────────────────────
            CrmCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                ) {
                    CrmText("Identity & meter", style = CrmTheme.typography.h3)
                    CrmDivider()
                    LabelValue("Subject", sub.subjectId)
                    LabelValue("Variant", sub.variant ?: "—")
                    LabelValue("Entitled", if (sub.entitled) "Yes" else "No")
                    LabelValue("Meter period", sub.meterPeriod)
                    LabelValue("Meter used", "${sub.meterUsed} articles")
                    if (sub.meteredArticles.isNotEmpty()) {
                        LabelValue("Counted articles", sub.meteredArticles.joinToString(", "))
                    }
                    if (sub.linkedSubjects.size > 1) {
                        LabelValue("Linked subjects", sub.linkedSubjects.joinToString(", "))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm)) {
                        CrmSecondaryButton("Reset meter") {
                            scope.launch {
                                val ok = onMeterReset(sub.subjectId)
                                status = if (ok) "Meter reset — reload to confirm" else "Reset failed"
                            }
                        }
                    }
                }
            }

            // ── active grants ─────────────────────────────────────────────────
            if (grants.isNotEmpty()) {
                CrmCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                    ) {
                        CrmText("Grants (FGA-08)", style = CrmTheme.typography.h3)
                        CrmDivider()
                        grants.forEach { g ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    CrmText(g.grantId, style = CrmTheme.typography.bodySmall)
                                    CrmText(
                                        "Article: ${g.articleId} · by: ${g.grantedBy}",
                                        style = CrmTheme.typography.caption,
                                        color = CrmTheme.colors.onSurfaceVariant,
                                    )
                                    if (g.reason.isNotBlank()) {
                                        CrmText(
                                            g.reason,
                                            style = CrmTheme.typography.caption,
                                            color = CrmTheme.colors.onSurfaceVariant,
                                        )
                                    }
                                }
                                CrmTag(
                                    if (g.isLive) "live" else "expired/revoked",
                                    if (g.isLive) CrmTheme.colors.primary else CrmTheme.colors.surfaceVariant,
                                    if (g.isLive) CrmTheme.colors.onPrimary else CrmTheme.colors.onSurfaceVariant,
                                )
                            }
                            CrmDivider()
                        }
                    }
                }
            }

            // ── CIAM sessions ────────────────────────────────────────────────
            if (sub.sessions.isNotEmpty()) {
                CrmCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                    ) {
                        CrmText("Active sessions (ADM-04)", style = CrmTheme.typography.h3)
                        CrmDivider()
                        sub.sessions.forEach { s ->
                            LabelValue(s.device ?: "Unknown device", s.ipAddress ?: "—")
                        }
                    }
                }
            }

            // ── recent wall events ───────────────────────────────────────────
            if (sub.recentWallEvents.isNotEmpty()) {
                CrmCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                    ) {
                        CrmText("Recent wall events", style = CrmTheme.typography.h3)
                        CrmDivider()
                        sub.recentWallEvents.takeLast(10).reversed().forEach { e ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                CrmText(e.type, style = CrmTheme.typography.bodySmall)
                                CrmText(
                                    "${e.variant} · ${e.channel}${e.articleId?.let { " · $it" } ?: ""}",
                                    style = CrmTheme.typography.caption,
                                    color = CrmTheme.colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        CrmText(label, style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurfaceVariant)
        CrmText(value, style = CrmTheme.typography.bodySmall)
    }
}
