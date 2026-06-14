package nl.incedo.paywall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import nl.incedo.paywall.theme.CrmBorder
import nl.incedo.paywall.theme.CrmTheme

/**
 * Single-select dropdown. Clicking the field opens a popup list of [options].
 * Pass [searchable] = true to show a filter input at the top of the popup.
 */
@Composable
fun CrmSelectField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select…",
    searchable: Boolean = false,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val filtered = if (searchable && query.isNotEmpty()) {
        options.filter { it.contains(query, ignoreCase = true) }
    } else {
        options
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
        CrmText(label, style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CrmTheme.shapes.md)
                    .background(if (enabled) CrmTheme.colors.surface else CrmTheme.colors.disabled)
                    .border(
                        if (expanded) CrmBorder.thick else CrmBorder.default,
                        if (expanded) CrmTheme.colors.primary else CrmTheme.colors.divider,
                        CrmTheme.shapes.md,
                    )
                    .clickable(enabled = enabled) {
                        expanded = !expanded
                        if (!expanded) query = ""
                    }
                    .padding(horizontal = CrmTheme.spacing.md, vertical = CrmTheme.spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrmText(
                    value.ifEmpty { placeholder },
                    style = CrmTheme.typography.body,
                    color = if (value.isEmpty()) CrmTheme.colors.onSurfaceVariant else CrmTheme.colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                CrmText(
                    if (expanded) "▴" else "▾",
                    style = CrmTheme.typography.label,
                    color = CrmTheme.colors.onSurfaceVariant,
                )
            }

            if (expanded) {
                Popup(onDismissRequest = { expanded = false; query = "" }) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 200.dp)
                            .shadow(CrmTheme.elevation.lg, CrmTheme.shapes.md)
                            .clip(CrmTheme.shapes.md)
                            .background(CrmTheme.colors.surface)
                            .border(CrmBorder.default, CrmTheme.colors.divider, CrmTheme.shapes.md),
                    ) {
                        if (searchable) {
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = CrmTheme.typography.body.copy(color = CrmTheme.colors.onSurface),
                                cursorBrush = SolidColor(CrmTheme.colors.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = CrmTheme.spacing.md, vertical = CrmTheme.spacing.sm)
                                    .border(CrmBorder.default, CrmTheme.colors.divider, CrmTheme.shapes.sm)
                                    .padding(CrmTheme.spacing.sm),
                                decorationBox = { inner ->
                                    Box {
                                        if (query.isEmpty()) {
                                            CrmText("Search…", style = CrmTheme.typography.body, color = CrmTheme.colors.onSurfaceVariant)
                                        }
                                        inner()
                                    }
                                },
                            )
                            CrmDivider()
                        }
                        Column(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                            filtered.forEachIndexed { i, option ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (option == value) CrmTheme.colors.infoContainer else CrmTheme.colors.surface)
                                        .clickable {
                                            onSelect(option)
                                            expanded = false
                                            query = ""
                                        }
                                        .padding(horizontal = CrmTheme.spacing.lg, vertical = CrmTheme.spacing.md),
                                ) {
                                    CrmText(
                                        option,
                                        style = CrmTheme.typography.body,
                                        color = if (option == value) CrmTheme.colors.primary else CrmTheme.colors.onSurface,
                                    )
                                }
                                if (i < filtered.lastIndex) CrmDivider()
                            }
                            if (filtered.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg)) {
                                    CrmText("No options", style = CrmTheme.typography.body, color = CrmTheme.colors.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Multi-select dropdown. Selected values are shown as comma-separated chips
 * in the trigger; the popup shows a checkbox per option.
 */
@Composable
fun CrmMultiSelectField(
    label: String,
    options: List<String>,
    selectedValues: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select…",
    searchable: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val filtered = if (searchable && query.isNotEmpty()) {
        options.filter { it.contains(query, ignoreCase = true) }
    } else {
        options
    }
    val displayText = if (selectedValues.isEmpty()) "" else selectedValues.sorted().joinToString(", ")

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
        CrmText(label, style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CrmTheme.shapes.md)
                    .background(CrmTheme.colors.surface)
                    .border(
                        if (expanded) CrmBorder.thick else CrmBorder.default,
                        if (expanded) CrmTheme.colors.primary else CrmTheme.colors.divider,
                        CrmTheme.shapes.md,
                    )
                    .clickable { expanded = !expanded; if (!expanded) query = "" }
                    .padding(horizontal = CrmTheme.spacing.md, vertical = CrmTheme.spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrmText(
                    displayText.ifEmpty { placeholder },
                    style = CrmTheme.typography.body,
                    color = if (displayText.isEmpty()) CrmTheme.colors.onSurfaceVariant else CrmTheme.colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                CrmText(if (expanded) "▴" else "▾", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
            }

            if (expanded) {
                Popup(onDismissRequest = { expanded = false; query = "" }) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 200.dp)
                            .shadow(CrmTheme.elevation.lg, CrmTheme.shapes.md)
                            .clip(CrmTheme.shapes.md)
                            .background(CrmTheme.colors.surface)
                            .border(CrmBorder.default, CrmTheme.colors.divider, CrmTheme.shapes.md),
                    ) {
                        if (searchable) {
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = CrmTheme.typography.body.copy(color = CrmTheme.colors.onSurface),
                                cursorBrush = SolidColor(CrmTheme.colors.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = CrmTheme.spacing.md, vertical = CrmTheme.spacing.sm)
                                    .border(CrmBorder.default, CrmTheme.colors.divider, CrmTheme.shapes.sm)
                                    .padding(CrmTheme.spacing.sm),
                                decorationBox = { inner ->
                                    Box {
                                        if (query.isEmpty()) CrmText("Search…", style = CrmTheme.typography.body, color = CrmTheme.colors.onSurfaceVariant)
                                        inner()
                                    }
                                },
                            )
                            CrmDivider()
                        }
                        Column(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                            filtered.forEachIndexed { i, option ->
                                CrmCheckbox(
                                    checked = option in selectedValues,
                                    onCheckedChange = { checked ->
                                        onSelectionChange(
                                            if (checked) selectedValues + option else selectedValues - option,
                                        )
                                    },
                                    label = option,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = CrmTheme.spacing.lg, vertical = CrmTheme.spacing.xs),
                                )
                                if (i < filtered.lastIndex) CrmDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rich popover anchored to the current position. Content is arbitrary.
 * Click outside or calling [onDismiss] closes it.
 */
@Composable
fun CrmPopover(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!visible) return
    Popup(onDismissRequest = onDismiss) {
        Box(
            modifier = modifier
                .shadow(CrmTheme.elevation.lg, CrmTheme.shapes.md)
                .clip(CrmTheme.shapes.md)
                .background(CrmTheme.colors.surface)
                .border(CrmBorder.default, CrmTheme.colors.divider, CrmTheme.shapes.md)
                .padding(CrmTheme.spacing.lg),
        ) {
            content()
        }
    }
}
