package nl.incedo.paywall.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.incedo.paywall.ui.CrmCheckRow
import nl.incedo.paywall.ui.CrmPrimaryButton
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmSegmentedToggle
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmTag
import nl.incedo.paywall.ui.CrmText
import nl.incedo.paywall.model.WallDefinition
import nl.incedo.paywall.theme.CrmTheme

private data class Plan(
    val name: String,
    val tagline: String,
    val monthly: String,
    val annual: String,
    val priceNote: String,
    val features: List<String>,
    val cta: String,
    val highlighted: Boolean = false,
)

private val plans = listOf(
    Plan(
        name = "Free",
        tagline = "For occasional use",
        monthly = "€0",
        annual = "€0",
        priceNote = "per user / month",
        features = listOf(
            "3 documents per month",
            "View and pay invoices",
            "Email support",
        ),
        cta = "Continue on Free",
    ),
    Plan(
        name = "Pro",
        tagline = "For teams that bill every month",
        monthly = "€12",
        annual = "€10",
        priceNote = "per user / month",
        features = listOf(
            "Unlimited documents",
            "SEPA direct debit and payment reminders",
            "Exports — CSV and UBL",
            "Priority support",
        ),
        cta = "Upgrade to Pro",
        highlighted = true,
    ),
    Plan(
        name = "Business",
        tagline = "For finance teams and multiple entities",
        monthly = "€39",
        annual = "€31",
        priceNote = "per user / month",
        features = listOf(
            "Everything in Pro",
            "API access and webhooks",
            "Multiple entities",
            "Dunning automation",
        ),
        cta = "Upgrade to Business",
    ),
)

/** Hard wall — full pricing table (design 01-A). */
@Composable
fun PricingWall(definition: WallDefinition) {
    var annualBilling by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xl),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
        ) {
            CrmText(definition.title, style = CrmTheme.typography.h1, color = CrmTheme.colors.onBackground)
            CrmText(definition.body, color = CrmTheme.colors.onSurfaceVariant)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
        ) {
            CrmSegmentedToggle(
                options = listOf("Monthly", "Annual"),
                selectedIndex = if (annualBilling) 1 else 0,
                onSelect = { annualBilling = it == 1 },
            )
            CrmTag("Save 20%", CrmTheme.colors.successContainer, CrmTheme.colors.success)
        }

        Row(
            modifier = Modifier.widthIn(max = 960.dp),
            horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
        ) {
            plans.forEach { plan ->
                PlanCard(plan, annualBilling, Modifier.weight(1f))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xl)) {
            CrmText("Trusted by 4,800+ teams across Europe", style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurfaceVariant)
            CrmText("Cancel anytime", style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurfaceVariant)
            CrmText("VAT invoices included", style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, annualBilling: Boolean, modifier: Modifier = Modifier) {
    CrmCard(
        modifier = modifier,
        borderColor = if (plan.highlighted) CrmTheme.colors.primary else CrmTheme.colors.divider,
    ) {
        Column(
            modifier = Modifier.padding(CrmTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrmText(plan.name, style = CrmTheme.typography.h3)
                if (plan.highlighted) {
                    CrmTag("Most popular", CrmTheme.colors.infoContainer, CrmTheme.colors.primary)
                }
            }
            CrmText(plan.tagline, style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurfaceVariant)

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs),
            ) {
                CrmText(if (annualBilling) plan.annual else plan.monthly, style = CrmTheme.typography.h1)
                CrmText(plan.priceNote, style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurfaceVariant)
            }
            if (annualBilling && plan.monthly != plan.annual) {
                CrmText("${plan.monthly} when billed monthly", style = CrmTheme.typography.caption, color = CrmTheme.colors.onSurfaceVariant)
            }

            Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm)) {
                plan.features.forEach { CrmCheckRow(it) }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(top = CrmTheme.spacing.sm)) {
                if (plan.highlighted) {
                    CrmPrimaryButton(plan.cta, modifier = Modifier.fillMaxWidth())
                } else {
                    CrmSecondaryButton(plan.cta, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
