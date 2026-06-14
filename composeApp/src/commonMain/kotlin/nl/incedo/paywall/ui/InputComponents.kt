package nl.incedo.paywall.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import nl.incedo.paywall.theme.CrmBorder
import nl.incedo.paywall.theme.CrmTheme

/** Checkbox with optional label. Accessible via toggleable semantics. */
@Composable
fun CrmCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .toggleable(
                value = checked,
                onValueChange = { if (enabled) onCheckedChange(it) },
                role = Role.Checkbox,
            )
            .padding(vertical = CrmTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
    ) {
        val boxColor by animateColorAsState(
            targetValue = when {
                !enabled -> CrmTheme.colors.disabled
                checked -> CrmTheme.colors.primary
                else -> CrmTheme.colors.surface
            },
            animationSpec = tween(CrmTheme.animation.fast),
        )
        val borderColor by animateColorAsState(
            targetValue = when {
                !enabled -> CrmTheme.colors.disabled
                checked -> CrmTheme.colors.primary
                else -> CrmTheme.colors.onSurfaceVariant
            },
            animationSpec = tween(CrmTheme.animation.fast),
        )
        Box(
            modifier = Modifier
                .size(CrmTheme.iconSize.md)
                .clip(CrmTheme.shapes.sm)
                .background(boxColor)
                .border(CrmBorder.thick, borderColor, CrmTheme.shapes.sm),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                CrmText("✓", style = CrmTheme.typography.label, color = CrmTheme.colors.onPrimary)
            }
        }
        if (label != null) {
            CrmText(
                label,
                style = CrmTheme.typography.body,
                color = if (enabled) CrmTheme.colors.onSurface else CrmTheme.colors.onDisabled,
            )
        }
    }
}

/** Radio button for single-select groups. */
@Composable
fun CrmRadio(
    selected: Boolean,
    onClick: () -> Unit,
    label: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .semantics { role = Role.RadioButton }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = CrmTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
    ) {
        val ringColor by animateColorAsState(
            targetValue = when {
                !enabled -> CrmTheme.colors.disabled
                selected -> CrmTheme.colors.primary
                else -> CrmTheme.colors.onSurfaceVariant
            },
            animationSpec = tween(CrmTheme.animation.fast),
        )
        Box(
            modifier = Modifier
                .size(CrmTheme.iconSize.md)
                .clip(CrmTheme.shapes.pill)
                .border(CrmBorder.thick, ringColor, CrmTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(CrmTheme.iconSize.xs)
                        .clip(CrmTheme.shapes.pill)
                        .background(if (enabled) CrmTheme.colors.primary else CrmTheme.colors.disabled),
                )
            }
        }
        if (label != null) {
            CrmText(
                label,
                style = CrmTheme.typography.body,
                color = if (enabled) CrmTheme.colors.onSurface else CrmTheme.colors.onDisabled,
            )
        }
    }
}

/** On/off toggle switch with animated thumb. */
@Composable
fun CrmSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    // Track: xxl × lg; thumb: md; padding: xxs on each side.
    // Thumb travel = xxl - md - xxs*2 = 32 - 12 - 4 = 16dp = spacing.lg.
    val trackW = CrmTheme.spacing.xxl
    val thumbSz = CrmTheme.spacing.md
    val pad = CrmTheme.spacing.xxs

    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled -> CrmTheme.colors.disabled
            checked -> CrmTheme.colors.primary
            else -> CrmTheme.colors.onSurfaceVariant
        },
        animationSpec = tween(CrmTheme.animation.fast),
    )
    val thumbX by animateDpAsState(
        targetValue = if (checked) pad + trackW - thumbSz - pad * 2 else pad,
        animationSpec = tween(CrmTheme.animation.fast),
    )
    Row(
        modifier = modifier
            .toggleable(
                value = checked,
                onValueChange = { if (enabled) onCheckedChange(it) },
                role = Role.Switch,
            )
            .padding(vertical = CrmTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .width(trackW)
                .height(CrmTheme.spacing.lg)
                .clip(CrmTheme.shapes.pill)
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbX)
                    .padding(vertical = pad)
                    .size(thumbSz - pad)
                    .clip(CrmTheme.shapes.pill)
                    .background(CrmTheme.colors.onPrimary),
            )
        }
        if (label != null) {
            CrmText(
                label,
                style = CrmTheme.typography.body,
                color = if (enabled) CrmTheme.colors.onSurface else CrmTheme.colors.onDisabled,
            )
        }
    }
}

/** Multi-line text area with label and optional character counter. */
@Composable
fun CrmTextArea(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    rows: Int = 4,
    maxLength: Int? = null,
    enabled: Boolean = true,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CrmText(label, style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
            if (maxLength != null) {
                CrmText(
                    "${value.length}/$maxLength",
                    style = CrmTheme.typography.caption,
                    color = if (value.length > maxLength) CrmTheme.colors.error else CrmTheme.colors.onSurfaceVariant,
                )
            }
        }
        BasicTextField(
            value = value,
            onValueChange = { new ->
                if (maxLength == null || new.length <= maxLength) onValueChange(new)
            },
            enabled = enabled,
            singleLine = false,
            minLines = rows,
            maxLines = rows,
            textStyle = CrmTheme.typography.body.copy(
                color = if (enabled) CrmTheme.colors.onSurface else CrmTheme.colors.onDisabled,
            ),
            cursorBrush = SolidColor(CrmTheme.colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .clip(CrmTheme.shapes.md)
                .background(if (enabled) CrmTheme.colors.surface else CrmTheme.colors.disabled)
                .border(CrmBorder.default, CrmTheme.colors.divider, CrmTheme.shapes.md)
                .padding(horizontal = CrmTheme.spacing.md, vertical = CrmTheme.spacing.sm),
        )
    }
}
