package nl.incedo.paywall.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import nl.incedo.paywall.theme.CrmBorder
import nl.incedo.paywall.theme.CrmTheme

enum class SnackbarVariant { DEFAULT, SUCCESS, ERROR, WARNING }

/** Slide-in snackbar notification. Wrap in a Box aligned to BottomStart for proper placement. */
@Composable
fun CrmSnackbar(
    visible: Boolean,
    message: String,
    variant: SnackbarVariant = SnackbarVariant.DEFAULT,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val (bg, fg, border) = when (variant) {
        SnackbarVariant.SUCCESS -> Triple(CrmTheme.colors.successContainer, CrmTheme.colors.success, CrmTheme.colors.success)
        SnackbarVariant.ERROR -> Triple(CrmTheme.colors.errorContainer, CrmTheme.colors.error, CrmTheme.colors.error)
        SnackbarVariant.WARNING -> Triple(CrmTheme.colors.warningContainer, CrmTheme.colors.warning, CrmTheme.colors.warning)
        SnackbarVariant.DEFAULT -> Triple(CrmTheme.colors.surface, CrmTheme.colors.onSurface, CrmTheme.colors.divider)
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(CrmTheme.animation.normal),
        ) + fadeIn(animationSpec = tween(CrmTheme.animation.normal)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(CrmTheme.animation.normal),
        ) + fadeOut(animationSpec = tween(CrmTheme.animation.fast)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .clip(CrmTheme.shapes.md)
                .background(bg)
                .border(CrmBorder.default, border, CrmTheme.shapes.md)
                .padding(horizontal = CrmTheme.spacing.lg, vertical = CrmTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
        ) {
            CrmText(message, style = CrmTheme.typography.body, color = fg, modifier = Modifier.weight(1f))
            if (actionLabel != null && onAction != null) {
                CrmText(
                    actionLabel,
                    style = CrmTheme.typography.button,
                    color = fg,
                    modifier = Modifier.clickable(onClick = onAction),
                )
            }
        }
    }
}

/** Indeterminate loading spinner built from a rotating arc shape. */
@Composable
fun CrmSpinner(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = CrmTheme.iconSize.lg,
) {
    val transition = rememberInfiniteTransition()
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(CrmTheme.animation.slow * 2, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    Box(
        modifier = modifier
            .size(size)
            .rotate(angle)
            .clip(CrmTheme.shapes.pill)
            .border(CrmBorder.thick, CrmTheme.colors.primary, CrmTheme.shapes.pill),
    ) {
        // Mask out three-quarters of the ring to produce a spinner arc effect.
        Box(
            modifier = Modifier
                .size(size / 2)
                .background(CrmTheme.colors.surface),
        )
    }
}

/** Horizontal progress bar, progress in 0..1. */
@Composable
fun CrmProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
        if (label != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                CrmText(label, style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
                CrmText(
                    "${(clamped * 100).toInt()}%",
                    style = CrmTheme.typography.label,
                    color = CrmTheme.colors.onSurface,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CrmTheme.spacing.xs)
                .clip(CrmTheme.shapes.pill)
                .background(CrmTheme.colors.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .height(CrmTheme.spacing.xs)
                    .clip(CrmTheme.shapes.pill)
                    .background(CrmTheme.colors.primary),
            )
        }
    }
}

/** Shimmering content placeholder for loading states. */
@Composable
fun CrmSkeletonLoader(
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified,
    height: androidx.compose.ui.unit.Dp = CrmTheme.spacing.lg,
) {
    val transition = rememberInfiniteTransition()
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(CrmTheme.animation.slow, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val shimmerColor = CrmTheme.colors.surfaceVariant.copy(alpha = shimmerAlpha)
    val baseModifier = if (width == androidx.compose.ui.unit.Dp.Unspecified) {
        modifier.fillMaxWidth()
    } else {
        modifier.width(width)
    }
    Box(
        modifier = baseModifier
            .height(height)
            .clip(CrmTheme.shapes.sm)
            .background(Brush.horizontalGradient(listOf(shimmerColor, CrmTheme.colors.disabled, shimmerColor))),
    )
}

/** Empty-state placeholder: icon + title + optional subtitle + optional CTA. */
@Composable
fun CrmEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    icon: String = "○",
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(CrmTheme.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
    ) {
        CrmText(icon, style = CrmTheme.typography.h1.copy(fontSize = androidx.compose.ui.unit.TextUnit(48f, androidx.compose.ui.unit.TextUnitType.Sp)), color = CrmTheme.colors.onSurfaceVariant)
        CrmText(title, style = CrmTheme.typography.h2, color = CrmTheme.colors.onSurface)
        if (subtitle != null) {
            CrmText(subtitle, style = CrmTheme.typography.body, color = CrmTheme.colors.onSurfaceVariant)
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(CrmTheme.spacing.sm))
            CrmPrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}
