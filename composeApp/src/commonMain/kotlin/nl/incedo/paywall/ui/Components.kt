package nl.incedo.paywall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import nl.incedo.paywall.theme.CrmBorder
import nl.incedo.paywall.theme.CrmTheme

@Composable
fun CrmText(
    text: String,
    style: TextStyle = CrmTheme.typography.body,
    color: Color = CrmTheme.colors.onSurface,
    modifier: Modifier = Modifier,
) {
    BasicText(text = text, style = style.copy(color = color), modifier = modifier)
}

@Composable
fun CrmPrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .clip(CrmTheme.shapes.md)
            .background(CrmTheme.colors.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = CrmTheme.spacing.lg, vertical = CrmTheme.spacing.sm + CrmTheme.spacing.xxs),
        contentAlignment = Alignment.Center,
    ) {
        CrmText(text, style = CrmTheme.typography.button, color = CrmTheme.colors.onPrimary)
    }
}

@Composable
fun CrmSecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .clip(CrmTheme.shapes.md)
            .border(CrmBorder.default, CrmTheme.colors.divider, CrmTheme.shapes.md)
            .background(CrmTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = CrmTheme.spacing.lg, vertical = CrmTheme.spacing.sm + CrmTheme.spacing.xxs),
        contentAlignment = Alignment.Center,
    ) {
        CrmText(text, style = CrmTheme.typography.button, color = CrmTheme.colors.primary)
    }
}

@Composable
fun CrmTextButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .clip(CrmTheme.shapes.sm)
            .clickable(onClick = onClick)
            .padding(horizontal = CrmTheme.spacing.sm, vertical = CrmTheme.spacing.xs),
    ) {
        CrmText(text, style = CrmTheme.typography.button, color = CrmTheme.colors.link)
    }
}

@Composable
fun CrmCard(
    modifier: Modifier = Modifier,
    borderColor: Color = CrmTheme.colors.divider,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(CrmTheme.shapes.lg)
            .background(CrmTheme.colors.surface)
            .border(CrmBorder.default, borderColor, CrmTheme.shapes.lg),
        content = content,
    )
}

@Composable
fun CrmTag(text: String, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .clip(CrmTheme.shapes.pill)
            .background(container)
            .padding(horizontal = CrmTheme.spacing.sm, vertical = CrmTheme.spacing.xxs),
    ) {
        CrmText(text, style = CrmTheme.typography.label, color = content)
    }
}

@Composable
fun CrmDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(CrmBorder.default).background(CrmTheme.colors.divider))
}

/** Two-state segmented control, e.g. Monthly / Annual. */
@Composable
fun CrmSegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CrmTheme.shapes.pill)
            .background(CrmTheme.colors.surfaceVariant)
            .padding(CrmTheme.spacing.xxs),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(CrmTheme.shapes.pill)
                    .background(if (selected) CrmTheme.colors.surface else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = CrmTheme.spacing.lg, vertical = CrmTheme.spacing.xs + CrmTheme.spacing.xxs),
            ) {
                CrmText(
                    option,
                    style = CrmTheme.typography.button,
                    color = if (selected) CrmTheme.colors.onSurface else CrmTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** Usage meter — "Free documents used · 3 of 3". */
@Composable
fun CrmUsageMeter(label: String, used: Int, limit: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CrmText(label, style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
            CrmText("$used of $limit", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurface)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CrmTheme.spacing.sm)
                .clip(CrmTheme.shapes.pill)
                .background(CrmTheme.colors.surfaceVariant),
        ) {
            val fraction = (used.toFloat() / limit).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(CrmTheme.shapes.pill)
                    .background(if (fraction >= 1f) CrmTheme.colors.warning else CrmTheme.colors.primary),
            )
        }
    }
}

/** Feature line with a leading check mark. */
@Composable
fun CrmCheckRow(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        CrmText("✓", style = CrmTheme.typography.body, color = CrmTheme.colors.success)
        CrmText(text, style = CrmTheme.typography.body, color = CrmTheme.colors.onSurface)
    }
}

/** Selectable pill chip, e.g. channel toggles. */
@Composable
fun CrmToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CrmTheme.shapes.pill)
            .background(if (selected) CrmTheme.colors.infoContainer else CrmTheme.colors.surfaceVariant)
            .border(
                CrmBorder.default,
                if (selected) CrmTheme.colors.primary else CrmTheme.colors.divider,
                CrmTheme.shapes.pill,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = CrmTheme.spacing.md, vertical = CrmTheme.spacing.xs),
    ) {
        CrmText(
            label,
            style = CrmTheme.typography.label,
            color = if (selected) CrmTheme.colors.primary else CrmTheme.colors.onSurfaceVariant,
        )
    }
}

/** Labelled single/multi-line text input. */
@Composable
fun CrmTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
        CrmText(label, style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = CrmTheme.typography.body.copy(color = CrmTheme.colors.onSurface),
            cursorBrush = SolidColor(CrmTheme.colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .clip(CrmTheme.shapes.md)
                .background(CrmTheme.colors.surface)
                .border(CrmBorder.default, CrmTheme.colors.divider, CrmTheme.shapes.md)
                .padding(horizontal = CrmTheme.spacing.md, vertical = CrmTheme.spacing.sm),
        )
    }
}

@Composable
fun CrmAvatar(initials: String) {
    Box(
        modifier = Modifier
            .size(CrmTheme.spacing.xxl)
            .clip(CrmTheme.shapes.pill)
            .background(CrmTheme.colors.infoContainer),
        contentAlignment = Alignment.Center,
    ) {
        CrmText(initials, style = CrmTheme.typography.label, color = CrmTheme.colors.primary)
    }
}

