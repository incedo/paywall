package nl.incedo.paywall.designer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.incedo.paywall.api.CreatePartnerRequest
import nl.incedo.paywall.api.PartnerResponse
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmPrimaryButton
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmTag
import nl.incedo.paywall.ui.CrmText
import nl.incedo.paywall.ui.CrmTextField
import nl.incedo.paywall.ui.CrmTextButton

/**
 * ADM-03: partner management screen — create partners (PA-01), manage members (PA-02/03/PA-05),
 * configure IP CIDR allowlists (IPW-01/03), and offboard partners (PA-03). All writes audited.
 */
@Composable
fun PartnersScreen(
    onLoadPartners: suspend () -> List<PartnerResponse>,
    onCreatePartner: suspend (CreatePartnerRequest) -> Boolean,
    onAddMember: suspend (partnerId: String, subjectId: String) -> Boolean,
    onRemoveMember: suspend (partnerId: String, subjectId: String) -> Boolean,
    onAddIpRange: suspend (partnerId: String, cidr: String) -> Boolean,
    onOffboard: suspend (partnerId: String) -> Boolean,
    statusMessage: String?,
) {
    val scope = rememberCoroutineScope()
    var partners by remember { mutableStateOf<List<PartnerResponse>>(emptyList()) }
    var selected by remember { mutableStateOf<PartnerResponse?>(null) }
    var status by remember { mutableStateOf(statusMessage) }
    var showCreate by remember { mutableStateOf(false) }

    suspend fun reload() {
        partners = runCatching { onLoadPartners() }.getOrDefault(emptyList())
        selected = selected?.let { s -> partners.find { it.partnerId == s.partnerId } }
    }

    LaunchedEffect(Unit) { reload() }

    Row(modifier = Modifier.fillMaxWidth()) {
        // ── left: partner list ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .width(280.dp)
                .verticalScroll(rememberScrollState())
                .padding(CrmTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrmText("Partners", style = CrmTheme.typography.h1)
                CrmSecondaryButton("+ New") { showCreate = !showCreate }
            }
            status?.let {
                CrmText(it, style = CrmTheme.typography.caption, color = CrmTheme.colors.onSurfaceVariant)
            }
            if (partners.isEmpty()) {
                CrmText(
                    "No partners yet.",
                    style = CrmTheme.typography.bodySmall,
                    color = CrmTheme.colors.onSurfaceVariant,
                )
            }
            partners.forEach { p ->
                CrmCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = p; showCreate = false }
                            .padding(CrmTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xxs),
                    ) {
                        CrmText(p.name, style = CrmTheme.typography.bodySmall)
                        CrmText(
                            "${p.partnerId} · ${p.planId} · ${p.activeSeats}${p.maxSeats?.let { "/$it" } ?: ""} seats",
                            style = CrmTheme.typography.caption,
                            color = CrmTheme.colors.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        CrmDivider()

        // ── right: create form or partner detail ──────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(CrmTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
        ) {
            when {
                showCreate -> CreatePartnerForm(
                    onCreate = { req ->
                        scope.launch {
                            val ok = onCreatePartner(req)
                            status = if (ok) "Partner created" else "Create failed"
                            if (ok) { showCreate = false; reload() }
                        }
                    },
                    onCancel = { showCreate = false },
                )
                selected != null -> PartnerDetail(
                    partner = selected!!,
                    onAddMember = { subjectId ->
                        scope.launch {
                            val ok = onAddMember(selected!!.partnerId, subjectId)
                            status = if (ok) "Member added" else "Add member failed"
                            if (ok) reload()
                        }
                    },
                    onRemoveMember = { subjectId ->
                        scope.launch {
                            val ok = onRemoveMember(selected!!.partnerId, subjectId)
                            status = if (ok) "Member removed" else "Remove failed"
                            if (ok) reload()
                        }
                    },
                    onAddIpRange = { cidr ->
                        scope.launch {
                            val ok = onAddIpRange(selected!!.partnerId, cidr)
                            status = if (ok) "IP range configured (IPW-01)" else "CIDR invalid or overlapping"
                            if (ok) reload()
                        }
                    },
                    onOffboard = {
                        scope.launch {
                            val ok = onOffboard(selected!!.partnerId)
                            status = if (ok) "Partner offboarded — access revoked (PA-03)" else "Offboard failed"
                            if (ok) { selected = null; reload() }
                        }
                    },
                )
                else -> CrmText(
                    "Select a partner or create a new one.",
                    style = CrmTheme.typography.bodySmall,
                    color = CrmTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PartnerDetail(
    partner: PartnerResponse,
    onAddMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onAddIpRange: (String) -> Unit,
    onOffboard: () -> Unit,
) {
    var newMember by remember(partner.partnerId) { mutableStateOf("") }
    var newCidr by remember(partner.partnerId) { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg)) {
        // ── summary ───────────────────────────────────────────────────────────
        CrmCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
            ) {
                CrmText(partner.name, style = CrmTheme.typography.h3)
                CrmDivider()
                PartnerRow("ID", partner.partnerId)
                PartnerRow("Plan", partner.planId)
                PartnerRow(
                    "Seats",
                    "${partner.activeSeats} active${partner.maxSeats?.let { " / $it max (PA-05)" } ?: " (unlimited)"}",
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    CrmSecondaryButton("Offboard (PA-03)", onClick = onOffboard)
                }
            }
        }

        // ── IP allowlist (IPW-01) ─────────────────────────────────────────────
        CrmCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
            ) {
                CrmText("IP allowlist (IPW-01)", style = CrmTheme.typography.h3)
                CrmDivider()
                if (partner.activeCidrs.isEmpty()) {
                    CrmText("No active CIDR ranges.", style = CrmTheme.typography.caption, color = CrmTheme.colors.onSurfaceVariant)
                } else {
                    partner.activeCidrs.forEach { cidr ->
                        CrmTag(cidr, CrmTheme.colors.surfaceVariant, CrmTheme.colors.onSurfaceVariant)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CrmTextField(
                        "CIDR (e.g. 10.0.0.0/8)",
                        newCidr,
                        { newCidr = it },
                        modifier = Modifier.weight(1f),
                    )
                    CrmSecondaryButton("Add") {
                        val cidr = newCidr.trim()
                        if (cidr.isNotEmpty()) { onAddIpRange(cidr); newCidr = "" }
                    }
                }
            }
        }

        // ── member management (PA-02/03/PA-05) ───────────────────────────────
        CrmCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
            ) {
                CrmText("Members (PA-02/PA-05)", style = CrmTheme.typography.h3)
                CrmDivider()
                CrmText(
                    "Add or remove subject IDs. Seat limit enforced server-side (PA-05).",
                    style = CrmTheme.typography.caption,
                    color = CrmTheme.colors.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CrmTextField(
                        "subject_id (user:… or visitor:…)",
                        newMember,
                        { newMember = it },
                        modifier = Modifier.weight(1f),
                    )
                    CrmSecondaryButton("Add") {
                        val id = newMember.trim()
                        if (id.isNotEmpty()) { onAddMember(id); newMember = "" }
                    }
                    CrmTextButton("Remove") {
                        val id = newMember.trim()
                        if (id.isNotEmpty()) { onRemoveMember(id); newMember = "" }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePartnerForm(
    onCreate: (CreatePartnerRequest) -> Unit,
    onCancel: () -> Unit,
) {
    var partnerId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var maxSeats by remember { mutableStateOf("") }
    var planId by remember { mutableStateOf("complete") }

    Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md)) {
        CrmText("Create partner (PA-01)", style = CrmTheme.typography.h3)
        CrmTextField("Partner ID (e.g. partner-acme)", partnerId, { partnerId = it })
        CrmTextField("Name", name, { name = it })
        CrmTextField("Plan ID (basic / complete)", planId, { planId = it })
        CrmTextField("Max seats (leave blank = unlimited)", maxSeats, { maxSeats = it })
        Row(horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm)) {
            CrmPrimaryButton("Create") {
                if (partnerId.isNotBlank() && name.isNotBlank()) {
                    onCreate(
                        CreatePartnerRequest(
                            partnerId = partnerId.trim(),
                            name = name.trim(),
                            maxSeats = maxSeats.trim().toIntOrNull(),
                            planId = planId.trim().ifBlank { "complete" },
                        )
                    )
                }
            }
            CrmSecondaryButton("Cancel", onClick = onCancel)
        }
    }
}

@Composable
private fun PartnerRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        CrmText(label, style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurfaceVariant)
        CrmText(value, style = CrmTheme.typography.bodySmall)
    }
}
