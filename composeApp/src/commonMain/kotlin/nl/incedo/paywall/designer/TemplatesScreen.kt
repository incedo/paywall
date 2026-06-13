package nl.incedo.paywall.designer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import nl.incedo.paywall.api.WallTemplateResponse
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmPrimaryButton
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmTag
import nl.incedo.paywall.ui.CrmText
import nl.incedo.paywall.ui.CrmTextField

/**
 * ADM-16: wall design templates — brand-neutral layout/copy that can be
 * instantiated for any brand. Theme tokens are NOT stored in the template;
 * they come from the target brand at render time (ADM-10).
 */
@Composable
fun TemplatesScreen(
    onLoadTemplates: suspend () -> List<WallTemplateResponse>,
    onUseTemplate: suspend (templateId: String, wallId: String, brandId: String) -> Boolean,
    onOpenWall: (wallId: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var templates by remember { mutableStateOf<List<WallTemplateResponse>>(emptyList()) }
    var selected by remember { mutableStateOf<WallTemplateResponse?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { templates = runCatching { onLoadTemplates() }.getOrDefault(emptyList()) }

    Row(modifier = Modifier.fillMaxWidth()) {
        // ── left: template list ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .verticalScroll(rememberScrollState())
                .padding(CrmTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
        ) {
            CrmText("Templates", style = CrmTheme.typography.h3)
            if (templates.isEmpty()) {
                CrmText(
                    "No templates yet. Templates are brand-neutral wall designs that can be re-used across brands (ADM-16).",
                    style = CrmTheme.typography.bodySmall,
                    color = CrmTheme.colors.onSurfaceVariant,
                )
            }
            templates.forEach { tmpl ->
                CrmCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (selected?.id == tmpl.id) CrmTheme.colors.primary else CrmTheme.colors.divider,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CrmText(tmpl.name, style = CrmTheme.typography.bodySmall)
                            CrmTag(
                                tmpl.wallType,
                                CrmTheme.colors.infoContainer,
                                CrmTheme.colors.primary,
                            )
                        }
                        CrmText(
                            "by ${tmpl.createdBy}",
                            style = CrmTheme.typography.caption,
                            color = CrmTheme.colors.onSurfaceVariant,
                        )
                        CrmSecondaryButton("Use template", onClick = { selected = tmpl; message = null })
                    }
                }
            }
        }

        CrmDivider()

        // ── right: instantiate panel ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(CrmTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
        ) {
            message?.let {
                CrmText(it, style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.primary)
            }

            if (selected != null) {
                InstantiatePanel(
                    template = selected!!,
                    onInstantiate = { wallId, brandId ->
                        scope.launch {
                            val ok = onUseTemplate(selected!!.id, wallId, brandId)
                            if (ok) {
                                message = "Wall '$wallId' created from template '${selected!!.name}'"
                                onOpenWall(wallId)
                            } else {
                                message = "Failed — check that the brand exists and the wall ID is unique"
                            }
                        }
                    },
                )
            } else {
                CrmText(
                    "Select a template on the left to create a wall from it.",
                    style = CrmTheme.typography.bodySmall,
                    color = CrmTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InstantiatePanel(
    template: WallTemplateResponse,
    onInstantiate: (wallId: String, brandId: String) -> Unit,
) {
    var wallId by remember(template.id) { mutableStateOf("wall-from-${template.id}") }
    var brandId by remember(template.id) { mutableStateOf("") }

    Column(
        modifier = Modifier.widthIn(max = 480.dp),
        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
    ) {
        CrmText("Use template: ${template.name}", style = CrmTheme.typography.h2)

        Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
            CrmText("Preview", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
            CrmCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs),
                ) {
                    CrmText(template.title, style = CrmTheme.typography.h3)
                    CrmText(template.body, style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurfaceVariant)
                    CrmText(
                        "${template.primaryCta} · ${template.secondaryCta}",
                        style = CrmTheme.typography.caption,
                        color = CrmTheme.colors.link,
                    )
                }
            }
        }

        CrmDivider()
        CrmText("Instantiate", style = CrmTheme.typography.h3)
        CrmText(
            "Theme tokens (colours, logo, fonts) come from the target brand at render time — they are not stored in the template (ADM-16).",
            style = CrmTheme.typography.caption,
            color = CrmTheme.colors.onSurfaceVariant,
        )
        CrmTextField("New wall ID (slug)", wallId, { wallId = it })
        CrmTextField("Target brand ID", brandId, { brandId = it })
        CrmPrimaryButton(
            "Create wall from template",
            onClick = { if (wallId.isNotBlank() && brandId.isNotBlank()) onInstantiate(wallId, brandId) },
        )
    }
}
