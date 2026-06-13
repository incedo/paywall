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
import nl.incedo.paywall.ui.CrmPrimaryButton
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmText

/**
 * Registration wall preview (PW-50, ADM-12): shown to anonymous visitors when
 * [WallDefinition.registrationWall] is enabled, before the paywall strategy
 * runs. Step 1 of progressive registration (PR-01): email + password only.
 * Later profile steps are separate Kratos flows triggered after sign-up.
 *
 * The wall is rendered here as a preview only — actual auth flows live in
 * the CIAM (Ory Kratos). The preview covers the ADM-12 "registration wall"
 * gate context requirement.
 */
@Composable
fun RegistrationWallScreen() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs),
            ) {
                CrmText(
                    "Create a free account to continue reading",
                    style = CrmTheme.typography.h2,
                    color = CrmTheme.colors.onBackground,
                )
                CrmText(
                    "Free account — no credit card required.",
                    style = CrmTheme.typography.bodySmall,
                    color = CrmTheme.colors.onSurfaceVariant,
                )
            }

            CrmCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
                ) {
                    CrmText("Email address", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
                    CrmCard {
                        CrmText(
                            "you@example.com",
                            modifier = Modifier.padding(CrmTheme.spacing.md),
                            color = CrmTheme.colors.onSurfaceVariant,
                            style = CrmTheme.typography.bodySmall,
                        )
                    }
                    CrmText("Password", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
                    CrmCard {
                        CrmText(
                            "••••••••",
                            modifier = Modifier.padding(CrmTheme.spacing.md),
                            color = CrmTheme.colors.onSurfaceVariant,
                            style = CrmTheme.typography.bodySmall,
                        )
                    }
                    CrmPrimaryButton("Create account", modifier = Modifier.fillMaxWidth())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CrmSecondaryButton("Log in to existing account")
                    }
                }
            }

            CrmText(
                "By creating an account you agree to our Terms of Service and Privacy Policy.",
                style = CrmTheme.typography.caption,
                color = CrmTheme.colors.onSurfaceVariant,
            )
        }
    }
}
