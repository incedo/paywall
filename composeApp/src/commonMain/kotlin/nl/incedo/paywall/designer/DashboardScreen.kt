package nl.incedo.paywall.designer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nl.incedo.paywall.api.BypassRateResponse
import nl.incedo.paywall.api.VariantStatsResponse
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmText

/**
 * Experiment dashboard (AN-10/AN-11/AN-12): per-variant funnel numbers
 * from /api/v1/stats. Columns: visitors, reads, walls shown, gate CTR,
 * registrations, checkouts, conversions, conversion rate with Wilson CI
 * (AN-12), and reach cost vs. control (AN-11).
 * BP-06: bypass-rate card shown when bypassRate is available.
 */
@Composable
fun DashboardScreen(stats: List<VariantStatsResponse>, bypassRate: BypassRateResponse?, statusMessage: String?) {
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

        // Main funnel table (AN-10)
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
                HeaderCell("Registrations", 1f)
                HeaderCell("Checkouts", 1f)
                HeaderCell("Conversions", 1f)
                HeaderCell("Conv. rate", 1f)
                // AN-12: Wilson 95% CI bounds
                HeaderCell("CI low", 1f)
                HeaderCell("CI high", 1f)
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
                    Cell(row.registrations.toString())
                    Cell(row.checkoutStarts.toString())
                    Cell(row.conversions.toString())
                    // AN-12: flag small samples so analysts know not to conclude
                    val rateLabel = if (row.sampleSizeTooSmall) "${row.conversionRate.asPercent()}*" else row.conversionRate.asPercent()
                    Cell(rateLabel)
                    Cell(row.conversionRateLow.asPercent())
                    Cell(row.conversionRateHigh.asPercent())
                }
                if (index < stats.lastIndex) CrmDivider()
            }
            if (stats.any { it.sampleSizeTooSmall }) {
                CrmText(
                    "* Sample size < 100 — confidence interval too wide for reliable conclusions (AN-12)",
                    style = CrmTheme.typography.bodySmall,
                    color = CrmTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(CrmTheme.spacing.md),
                )
            }
        }

        // BP-06: bypass-rate card — only shown when data is available
        if (bypassRate != null) {
            CrmCard {
                Column(modifier = Modifier.padding(CrmTheme.spacing.lg), verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm)) {
                    CrmText(
                        "Bypass rate (BP-06)",
                        style = CrmTheme.typography.label,
                        color = CrmTheme.colors.onSurfaceVariant,
                    )
                    CrmDivider()
                    Row(modifier = Modifier.fillMaxWidth()) {
                        HeaderCell("Gated renders", 1f)
                        HeaderCell("Marked renders", 1f)
                        HeaderCell("Flagged reads", 1f)
                        HeaderCell("Bypass rate", 1f)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Cell(bypassRate.gatedRenders.toString())
                        Cell(bypassRate.markedGatedRenders.toString())
                        Cell(bypassRate.flaggedReads.toString())
                        Cell(bypassRate.bypassRate.asPercent())
                    }
                    CrmText(
                        bypassRate.note,
                        style = CrmTheme.typography.bodySmall,
                        color = CrmTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }

        // AN-11: reach cost table — only shown when a control variant exists
        val withReachCost = stats.filter { it.pageViewsDeltaVsControl != null }
        if (withReachCost.isNotEmpty()) {
            CrmCard {
                Column(modifier = Modifier.padding(CrmTheme.spacing.lg)) {
                    CrmText(
                        "Reach cost vs. control (AN-11)",
                        style = CrmTheme.typography.label,
                        color = CrmTheme.colors.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = CrmTheme.spacing.lg,
                        vertical = CrmTheme.spacing.md,
                    ),
                ) {
                    HeaderCell("Variant", 2f)
                    HeaderCell("ΔPage views", 1f)
                    HeaderCell("ΔArticle reads", 1f)
                }
                CrmDivider()
                withReachCost.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(
                            horizontal = CrmTheme.spacing.lg,
                            vertical = CrmTheme.spacing.md,
                        ),
                    ) {
                        CrmText(row.variant, modifier = Modifier.weight(2f))
                        Cell(row.pageViewsDeltaVsControl!!.signedString())
                        Cell((row.articleReadsDeltaVsControl ?: 0).signedString())
                    }
                    if (index < withReachCost.lastIndex) CrmDivider()
                }
            }
        }
    }
}

private fun Double.asPercent(): String {
    val tenths = (this * 1000).toInt()
    return "${tenths / 10}.${tenths % 10}%"
}

private fun Int.signedString(): String = if (this >= 0) "+$this" else "$this"

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
