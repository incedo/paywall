package nl.incedo.paywall.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import nl.incedo.paywall.model.WallDefinition
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmPrimaryButton
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmText
import nl.incedo.paywall.ui.CrmUsageMeter

private val nudgeInvoices = listOf(
    "2026-0142 · Hosting May" to "€482.00",
    "2026-0136 · Licences Q2" to "€1,240.00",
    "2026-0129 · Support plan" to "€96.00",
)

/**
 * Meter-warning nudge (PW-23, ADM-12): inline banner shown when the visitor
 * has 1 free article remaining. Rendered above the content so the article
 * stays readable; the gate card appears below when the limit is reached.
 * Decision model emits nudge=true when used == limit - 1 (PW-23).
 */
@Composable
fun MeterNudgeBanner(definition: WallDefinition) {
    val freeLimit = 3
    val used = freeLimit - 1 // PW-23: 1 article remaining

    Column(
        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
        ) {
            // ── nudge banner (PW-23) ──────────────────────────────────────────
            CrmCard(borderColor = CrmTheme.colors.warning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CrmTheme.colors.warningContainer)
                        .padding(horizontal = CrmTheme.spacing.lg, vertical = CrmTheme.spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        CrmText(
                            "1 free document left this month",
                            style = CrmTheme.typography.bodySmall,
                            color = CrmTheme.colors.onSurface,
                        )
                        CrmText(
                            "Upgrade to Pro for unlimited documents.",
                            style = CrmTheme.typography.caption,
                            color = CrmTheme.colors.onSurfaceVariant,
                        )
                    }
                    CrmPrimaryButton(definition.primaryCta)
                }
            }

            // ── article content (still accessible — 1 remaining) ──────────────
            CrmCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(
                            horizontal = CrmTheme.spacing.lg,
                            vertical = CrmTheme.spacing.md,
                        ),
                    ) {
                        CrmText("Invoice", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant, modifier = Modifier.weight(2f))
                        CrmText("Amount", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                    }
                    CrmDivider()
                    nudgeInvoices.forEachIndexed { index, (name, amount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(
                                horizontal = CrmTheme.spacing.lg,
                                vertical = CrmTheme.spacing.md,
                            ),
                        ) {
                            CrmText(name, modifier = Modifier.weight(2f))
                            CrmText(amount, modifier = Modifier.weight(1f))
                        }
                        if (index < nudgeInvoices.lastIndex) CrmDivider()
                    }
                }
            }

            // ── usage meter ───────────────────────────────────────────────────
            CrmUsageMeter(
                label = "Free documents used",
                used = used,
                limit = freeLimit,
                modifier = Modifier.widthIn(max = 320.dp).align(Alignment.CenterHorizontally),
            )
        }
    }
}
