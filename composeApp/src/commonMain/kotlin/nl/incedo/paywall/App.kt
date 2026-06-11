package nl.incedo.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import nl.incedo.paywall.designer.WallDesignerScreen
import nl.incedo.paywall.model.WallDefinition
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmAvatar
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmText

/**
 * Admin console shell (ADM-01/05): internal back-office app hosting the
 * visual wall editor. Runs from one Compose source tree on web (Wasm),
 * desktop (JVM) — Android/iOS targets can be added to the same module.
 */
@Composable
fun App() {
    var definition by remember { mutableStateOf(WallDefinition()) }

    CrmTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CrmTheme.colors.background),
        ) {
            AdminTopBar()
            CrmDivider()
            WallDesignerScreen(
                definition = definition,
                onDefinitionChange = { definition = it },
            )
        }
    }
}

@Composable
private fun AdminTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CrmTheme.colors.surface)
            .padding(horizontal = CrmTheme.spacing.xl, vertical = CrmTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xl),
    ) {
        CrmText("Incedo CRM", style = CrmTheme.typography.h3, color = CrmTheme.colors.primary)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
        ) {
            listOf("Dashboard", "Contacts", "Deals", "Invoices", "Subscriptions", "Walls", "Tickets").forEach { item ->
                CrmText(
                    item,
                    style = CrmTheme.typography.button,
                    color = if (item == "Walls") CrmTheme.colors.primary else CrmTheme.colors.onSurfaceVariant,
                )
            }
        }
        CrmAvatar("MV")
    }
}
