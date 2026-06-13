package nl.incedo.paywall.designer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nl.incedo.paywall.api.OfferStatsResponse
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmText

/**
 * AN-14: offer performance view — per offer_id triggered/accepted/declined/suppressed
 * counts, acceptance rate, and channel breakdown from /api/v1/stats/offers.
 */
@Composable
fun OfferStatsScreen(offers: List<OfferStatsResponse>, statusMessage: String?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xxs)) {
            CrmText("Offer performance", style = CrmTheme.typography.h1, color = CrmTheme.colors.onBackground)
            CrmText(
                statusMessage ?: "Per offer_id: triggered, accepted, declined, suppressed and acceptance rate (AN-14)",
                style = CrmTheme.typography.bodySmall,
                color = CrmTheme.colors.onSurfaceVariant,
            )
        }

        // Summary table
        CrmCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = CrmTheme.spacing.lg,
                    vertical = CrmTheme.spacing.md,
                ),
            ) {
                OfferHeaderCell("Offer ID", 2f)
                OfferHeaderCell("Triggered", 1f)
                OfferHeaderCell("Accepted", 1f)
                OfferHeaderCell("Declined", 1f)
                OfferHeaderCell("Suppressed", 1f)
                OfferHeaderCell("Accept rate", 1f)
            }
            CrmDivider()
            if (offers.isEmpty()) {
                CrmText(
                    "No data",
                    style = CrmTheme.typography.body,
                    color = CrmTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(CrmTheme.spacing.lg),
                )
            }
            offers.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = CrmTheme.spacing.lg,
                        vertical = CrmTheme.spacing.md,
                    ),
                ) {
                    CrmText(row.offerId, modifier = Modifier.weight(2f))
                    OfferCell(row.triggered.toString())
                    OfferCell(row.accepted.toString())
                    OfferCell(row.declined.toString())
                    OfferCell(row.suppressed.toString())
                    OfferCell(row.acceptanceRate.asOfferPercent())
                }
                if (index < offers.lastIndex) CrmDivider()
            }
        }

        // AN-14: channel breakdown — one card per offer that has channel data
        val withChannels = offers.filter { it.channels.isNotEmpty() }
        withChannels.forEach { offer ->
            CrmCard {
                Column(modifier = Modifier.padding(CrmTheme.spacing.lg)) {
                    CrmText(
                        "Channel breakdown — ${offer.offerId}",
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
                    OfferHeaderCell("Channel", 2f)
                    OfferHeaderCell("Triggered", 1f)
                    OfferHeaderCell("Accepted", 1f)
                    OfferHeaderCell("Declined", 1f)
                    OfferHeaderCell("Suppressed", 1f)
                }
                CrmDivider()
                offer.channels.entries.toList().forEachIndexed { index, (channel, ch) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(
                            horizontal = CrmTheme.spacing.lg,
                            vertical = CrmTheme.spacing.md,
                        ),
                    ) {
                        CrmText(channel, modifier = Modifier.weight(2f))
                        OfferCell(ch.triggered.toString())
                        OfferCell(ch.accepted.toString())
                        OfferCell(ch.declined.toString())
                        OfferCell(ch.suppressed.toString())
                    }
                    if (index < offer.channels.size - 1) CrmDivider()
                }
            }
        }
    }
}

private fun Double.asOfferPercent(): String {
    val tenths = (this * 1000).toInt()
    return "${tenths / 10}.${tenths % 10}%"
}

@Composable
private fun RowScope.OfferCell(text: String) {
    CrmText(text, color = CrmTheme.colors.onSurfaceVariant, modifier = Modifier.weight(1f))
}

@Composable
private fun RowScope.OfferHeaderCell(text: String, weight: Float) {
    CrmText(
        text,
        style = CrmTheme.typography.label,
        color = CrmTheme.colors.onSurfaceVariant,
        modifier = Modifier.weight(weight),
    )
}
