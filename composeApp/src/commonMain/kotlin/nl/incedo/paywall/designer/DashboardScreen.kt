package nl.incedo.paywall.designer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nl.incedo.paywall.api.VariantStatsResponse
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmText

/** Experiment dashboard (AN-10): per-variant funnel numbers from /api/v1/stats. */
@Composable
fun DashboardScreen(stats: List<VariantStatsResponse>, statusMessage: String?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xxs)) {
            CrmText("Experiment dashboard", style = CrmTheme.typography.h1, color = CrmTheme.colors.onBackground)
            CrmText(
                statusMessage ?: "Conversion per variant — conversions / wall-shown uniques (AN-10)",
                style = CrmTheme.typography.bodySmall,
                color = CrmTheme.colors.onSurfaceVariant,
            )
        }

        CrmCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = CrmTheme.spacing.lg,
                    vertical = CrmTheme.spacing.md,
                ),
            ) {
                HeaderCell("Variant", 1.5f)
                HeaderCell("Visitors", 1f)
                HeaderCell("Reads", 1f)
                HeaderCell("Walls shown", 1f)
                HeaderCell("Gate CTR", 1f)
                HeaderCell("Checkouts", 1f)
                HeaderCell("Conversions", 1f)
                HeaderCell("Conv. rate", 1f)
            }
            CrmDivider()
            if (stats.isEmpty()) {
                CrmText(
                    "No data",
                    style = CrmTheme.typography.body,
                    color = CrmTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(CrmTheme.spacing.lg),
                )
            }
            stats.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = CrmTheme.spacing.lg,
                        vertical = CrmTheme.spacing.md,
                    ),
                ) {
                    CrmText(row.variant, modifier = Modifier.weight(1.5f))
                    Cell(row.visitors.toString())
                    Cell(row.articleReads.toString())
                    Cell(row.wallsShown.toString())
                    Cell(row.gateCtr.asPercent())
                    Cell(row.checkoutStarts.toString())
                    Cell(row.conversions.toString())
                    Cell(row.conversionRate.asPercent())
                }
                if (index < stats.lastIndex) CrmDivider()
            }
        }
    }
}

private fun Double.asPercent(): String {
    val tenths = (this * 1000).toInt()
    return "${tenths / 10}.${tenths % 10}%"
}

@Composable
private fun RowScope.Cell(text: String) {
    CrmText(text, color = CrmTheme.colors.onSurfaceVariant, modifier = Modifier.weight(1f))
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    CrmText(
        text,
        style = CrmTheme.typography.label,
        color = CrmTheme.colors.onSurfaceVariant,
        modifier = Modifier.weight(weight),
    )
}
