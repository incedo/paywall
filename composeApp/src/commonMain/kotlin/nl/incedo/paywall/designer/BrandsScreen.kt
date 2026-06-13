package nl.incedo.paywall.designer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.incedo.paywall.api.BrandResponse
import nl.incedo.paywall.api.CreateBrandRequest
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmPrimaryButton
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmText
import nl.incedo.paywall.ui.CrmTextField

/**
 * ADM-10: brand management screen — list, create, and update brand theme tokens.
 * A brand is a first-class entity (theme tokens: colours, logo, fonts; domain; locale).
 * Wall designs are created per brand; the gate renderer resolves brand → wall design.
 */
@Composable
fun BrandsScreen(
    onLoadBrands: suspend () -> List<BrandResponse>,
    onCreateBrand: suspend (CreateBrandRequest) -> Boolean,
    onUpdateTheme: suspend (brandId: String, themeJson: String) -> Boolean,
    statusMessage: String?,
) {
    val scope = rememberCoroutineScope()
    var brands by remember { mutableStateOf<List<BrandResponse>>(emptyList()) }
    var selected by remember { mutableStateOf<BrandResponse?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf(statusMessage) }

    suspend fun reload() {
        brands = runCatching { onLoadBrands() }.getOrDefault(emptyList())
        selected = selected?.let { s -> brands.find { it.brandId == s.brandId } }
    }

    LaunchedEffect(Unit) { reload() }

    Row(modifier = Modifier.fillMaxWidth()) {
        // ── left: brand list ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .verticalScroll(rememberScrollState())
                .padding(CrmTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrmText("Brands", style = CrmTheme.typography.h3)
                CrmPrimaryButton("+ New brand", onClick = { showCreate = true; selected = null })
            }
            if (brands.isEmpty()) {
                CrmText(
                    "No brands yet — create one to associate wall designs with a domain.",
                    style = CrmTheme.typography.bodySmall,
                    color = CrmTheme.colors.onSurfaceVariant,
                )
            }
            brands.forEach { brand ->
                CrmCard(
                    modifier = Modifier.fillMaxWidth().clickable { selected = brand; showCreate = false },
                    borderColor = if (selected?.brandId == brand.brandId) CrmTheme.colors.primary else CrmTheme.colors.divider,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xxs),
                    ) {
                        CrmText(brand.name, style = CrmTheme.typography.bodySmall)
                        CrmText(brand.domain, style = CrmTheme.typography.caption, color = CrmTheme.colors.onSurfaceVariant)
                        CrmText(brand.locale, style = CrmTheme.typography.caption, color = CrmTheme.colors.onSurfaceVariant)
                    }
                }
            }
        }

        CrmDivider()

        // ── right: detail / create ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(CrmTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
        ) {
            message?.let {
                CrmText(it, style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.primary)
            }

            when {
                showCreate -> CreateBrandPanel(
                    onCreate = { req ->
                        scope.launch {
                            val ok = onCreateBrand(req)
                            message = if (ok) "Brand '${req.name}' created" else "Create failed"
                            if (ok) { reload(); showCreate = false }
                        }
                    },
                    onCancel = { showCreate = false },
                )
                selected != null -> BrandDetailPanel(
                    brand = selected!!,
                    onUpdateTheme = { themeJson ->
                        scope.launch {
                            val ok = onUpdateTheme(selected!!.brandId, themeJson)
                            message = if (ok) "Theme updated for ${selected!!.name}" else "Update failed"
                            if (ok) reload()
                        }
                    },
                )
                else -> CrmText(
                    "Select a brand to view details, or create a new one.",
                    style = CrmTheme.typography.bodySmall,
                    color = CrmTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BrandDetailPanel(
    brand: BrandResponse,
    onUpdateTheme: (String) -> Unit,
) {
    var themeJson by remember(brand.brandId) { mutableStateOf(brand.themeJson) }

    Column(
        modifier = Modifier.widthIn(max = 600.dp),
        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
    ) {
        CrmText(brand.name, style = CrmTheme.typography.h2)
        CrmText("${brand.domain} · ${brand.locale}", style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurfaceVariant)
        CrmText("Brand ID: ${brand.brandId}", style = CrmTheme.typography.caption, color = CrmTheme.colors.onSurfaceVariant)
        CrmDivider()
        CrmText("Theme tokens", style = CrmTheme.typography.h3)
        CrmText(
            "Opaque JSON object — colours, logo URL, and font tokens interpreted by the wall renderer at gate time.",
            style = CrmTheme.typography.caption,
            color = CrmTheme.colors.onSurfaceVariant,
        )
        CrmTextField(
            label = "themeJson",
            value = themeJson,
            onValueChange = { themeJson = it },
            singleLine = false,
        )
        CrmPrimaryButton("Save theme", onClick = { onUpdateTheme(themeJson) })
    }
}

@Composable
private fun CreateBrandPanel(
    onCreate: (CreateBrandRequest) -> Unit,
    onCancel: () -> Unit,
) {
    var brandId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf("") }
    var locale by remember { mutableStateOf("nl-NL") }
    var themeJson by remember { mutableStateOf("{}") }

    Column(
        modifier = Modifier.widthIn(max = 480.dp),
        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
    ) {
        CrmText("New brand", style = CrmTheme.typography.h2)
        CrmTextField("Brand ID (slug)", brandId, { brandId = it })
        CrmTextField("Name", name, { name = it })
        CrmTextField("Domain", domain, { domain = it })
        CrmTextField("Locale (BCP-47)", locale, { locale = it })
        CrmTextField("Theme JSON", themeJson, { themeJson = it }, singleLine = false)
        Row(horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md)) {
            CrmPrimaryButton(
                "Create",
                onClick = {
                    if (brandId.isNotBlank() && name.isNotBlank() && domain.isNotBlank()) {
                        onCreate(CreateBrandRequest(brandId, name, domain, locale, themeJson))
                    }
                },
            )
            CrmSecondaryButton("Cancel", onClick = onCancel)
        }
    }
}
