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
import androidx.compose.ui.unit.dp
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmCheckRow
import nl.incedo.paywall.ui.CrmPrimaryButton
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmText

/**
 * GDPR consent step (AC-14, ADM-12): rendered as the first gate step where
 * legally required (GAP-08). It is a distinct step — never bundled with
 * payment consent (DG-03) or data-exchange consent — so withdrawal of
 * cookie consent does not retroactively affect existing subscriptions.
 *
 * Preview only — the actual consent state feeds AN-20 and is stored in
 * the CIAM (traits + consent records). Designed in the wall editor (ADM-11).
 */
@Composable
fun ConsentStepScreen() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs),
            ) {
                CrmText(
                    "We value your privacy",
                    style = CrmTheme.typography.h2,
                    color = CrmTheme.colors.onBackground,
                )
                CrmText(
                    "We use cookies and similar technologies to personalise your experience and " +
                        "measure how our site is used. Choose what you're comfortable with below.",
                    style = CrmTheme.typography.bodySmall,
                    color = CrmTheme.colors.onSurfaceVariant,
                )
            }

            CrmCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
                ) {
                    CrmText("Cookie preferences", style = CrmTheme.typography.h3)
                    CrmCheckRow("Strictly necessary (always on)")
                    CrmCheckRow("Analytics & performance")
                    CrmCheckRow("Personalisation")
                    CrmCheckRow("Marketing & advertising")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
            ) {
                CrmSecondaryButton("Accept necessary only", modifier = Modifier.weight(1f))
                CrmPrimaryButton("Accept all & continue", modifier = Modifier.weight(1f))
            }

            CrmText(
                "Your choice is stored and can be changed at any time in your account settings. " +
                    "Consent withdrawal stops future data use but does not affect your subscription (AC-14).",
                style = CrmTheme.typography.caption,
                color = CrmTheme.colors.onSurfaceVariant,
            )
        }
    }
}
