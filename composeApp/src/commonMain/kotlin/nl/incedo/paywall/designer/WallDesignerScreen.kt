package nl.incedo.paywall.designer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import nl.incedo.paywall.model.WallDefinition
import nl.incedo.paywall.model.WallType
import nl.incedo.paywall.screens.MeteredWall
import nl.incedo.paywall.screens.PricingWall
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmPrimaryButton
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmSegmentedToggle
import nl.incedo.paywall.ui.CrmTag
import nl.incedo.paywall.ui.CrmText
import nl.incedo.paywall.ui.CrmTextField

/**
 * Visual wall editor, workspace variant (ADM-11/12 · design "Wall Designer", variant A):
 * configuration left, live preview rendered from the structured [WallDefinition] right.
 */
@Composable
fun WallDesignerScreen(
    definition: WallDefinition,
    onDefinitionChange: (WallDefinition) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DesignerToolbar()
        CrmDivider()
        Row(modifier = Modifier.fillMaxWidth()) {
            ConfigPanel(
                definition = definition,
                onDefinitionChange = onDefinitionChange,
                modifier = Modifier
                    .width(320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(CrmTheme.spacing.xl),
            )
            PreviewPanel(
                definition = definition,
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(CrmTheme.spacing.xl),
            )
        }
    }
}

@Composable
private fun DesignerToolbar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CrmTheme.colors.surface)
            .padding(horizontal = CrmTheme.spacing.xl, vertical = CrmTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
    ) {
        CrmText("Walls / Metered limit — invoices", style = CrmTheme.typography.h3)
        CrmTag("Draft", CrmTheme.colors.surfaceVariant, CrmTheme.colors.onSurfaceVariant)
        Row(modifier = Modifier.weight(1f)) {}
        CrmSecondaryButton("Save draft")
        CrmPrimaryButton("Publish")
    }
}

@Composable
private fun ConfigPanel(
    definition: WallDefinition,
    onDefinitionChange: (WallDefinition) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg)) {
        CrmText("Wall type", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
        CrmSegmentedToggle(
            options = WallType.entries.map { it.label },
            selectedIndex = WallType.entries.indexOf(definition.type),
            onSelect = { onDefinitionChange(definition.copy(type = WallType.entries[it])) },
        )

        CrmText("Content", style = CrmTheme.typography.h3)
        CrmTextField("Title", definition.title, { onDefinitionChange(definition.copy(title = it)) })
        CrmTextField("Body", definition.body, { onDefinitionChange(definition.copy(body = it)) }, singleLine = false)
        CrmTextField("Primary CTA", definition.primaryCta, { onDefinitionChange(definition.copy(primaryCta = it)) })
        CrmTextField("Secondary CTA", definition.secondaryCta, { onDefinitionChange(definition.copy(secondaryCta = it)) })

        CrmText("Theme", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
        CrmSegmentedToggle(
            options = listOf("Light", "Dark"),
            selectedIndex = if (definition.darkPreview) 1 else 0,
            onSelect = { onDefinitionChange(definition.copy(darkPreview = it == 1)) },
        )
    }
}

@Composable
private fun PreviewPanel(definition: WallDefinition, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md)) {
        CrmText(
            "Preview · Web · ${if (definition.darkPreview) "Dark" else "Light"} · ${definition.type.label}",
            style = CrmTheme.typography.label,
            color = CrmTheme.colors.onSurfaceVariant,
        )
        CrmTheme(darkTheme = definition.darkPreview) {
            CrmCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CrmTheme.shapes.lg)
                        .background(CrmTheme.colors.background),
                ) {
                    when (definition.type) {
                        WallType.Hard -> PricingWall(definition)
                        WallType.Metered -> MeteredWall(definition)
                    }
                }
            }
        }
    }
}
