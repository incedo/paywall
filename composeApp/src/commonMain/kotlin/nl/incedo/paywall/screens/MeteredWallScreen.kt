package nl.incedo.paywall.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import nl.incedo.paywall.model.WallDefinition
import nl.incedo.paywall.model.WallType
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmTag
import nl.incedo.paywall.ui.CrmText

private data class Invoice(
    val number: String,
    val date: String,
    val amount: String,
    val paid: Boolean,
)

private val invoices = listOf(
    Invoice("2026-0142 · Hosting May", "28 May 2026", "€482.00", paid = true),
    Invoice("2026-0136 · Licences Q2", "14 May 2026", "€1,240.00", paid = false),
    Invoice("2026-0129 · Support plan", "2 May 2026", "€96.00", paid = true),
    Invoice("2026-0121 · Hosting April", "28 Apr 2026", "€482.00", paid = true),
    Invoice("2026-0117 · Onboarding", "19 Apr 2026", "€750.00", paid = true),
)

/**
 * Soft wall — gate over partially gated content (design 01-C). Renders the
 * metered, freemium and dynamic strategies; the gate copy comes from the
 * structured [WallDefinition] (ADM-11), strategy context from the type.
 */
@Composable
fun ContentGateWall(definition: WallDefinition) {
    val freeLimit = 3

    Column(
        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrmText("Invoices", style = CrmTheme.typography.h2, color = CrmTheme.colors.onBackground)
                CrmSecondaryButton("Export")
            }

            CrmCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(
                        horizontal = CrmTheme.spacing.lg,
                        vertical = CrmTheme.spacing.md,
                    ),
                ) {
                    CrmText("Invoice", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant, modifier = Modifier.weight(2f))
                    CrmText("Date", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                    CrmText("Amount", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                    CrmText("Status", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
                }
                CrmDivider()
                invoices.forEachIndexed { index, invoice ->
                    val gated = index >= freeLimit
                    InvoiceRow(
                        invoice,
                        modifier = if (gated) {
                            Modifier.blur(CrmTheme.spacing.xs).alpha(0.5f)
                        } else {
                            Modifier
                        },
                    )
                    if (index < invoices.lastIndex) CrmDivider()
                }
            }

            GateCard(definition, used = freeLimit, limit = freeLimit)
        }
    }
}

@Composable
private fun InvoiceRow(invoice: Invoice, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(
            horizontal = CrmTheme.spacing.lg,
            vertical = CrmTheme.spacing.md,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrmText(invoice.number, modifier = Modifier.weight(2f))
        CrmText(invoice.date, color = CrmTheme.colors.onSurfaceVariant, modifier = Modifier.weight(1f))
        CrmText(invoice.amount, modifier = Modifier.weight(1f))
        if (invoice.paid) {
            CrmTag("Paid", CrmTheme.colors.successContainer, CrmTheme.colors.success)
        } else {
            CrmTag("Open", CrmTheme.colors.warningContainer, CrmTheme.colors.onSurface)
        }
    }
}

@Composable
private fun GateCard(definition: WallDefinition, used: Int, limit: Int) {
    CrmCard(borderColor = CrmTheme.colors.primary) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
        ) {
            // VWE-01/05: render via WallLayout so preview and public gate share one renderer
            WallLayoutRenderer(
                layout = definition.toWallLayout(),
                meterUsed = used,
                meterLimit = limit,
            )
            val note = when (definition.type) {
                WallType.Metered -> "Your limit resets on 1 July."
                WallType.Freemium -> "Premium content — always gated on Free."
                WallType.Dynamic -> "Shown when CEP decisioning advises a gate — floor rule caps free reads."
                WallType.Hard -> null
            }
            if (note != null) {
                CrmText(note, style = CrmTheme.typography.caption, color = CrmTheme.colors.onSurfaceVariant)
            }
        }
    }
}
