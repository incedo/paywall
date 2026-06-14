package nl.incedo.paywall.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import nl.incedo.paywall.theme.CrmBorder
import nl.incedo.paywall.theme.CrmTheme

/**
 * Modal dialog with animated fade-in entrance. Content is constrained to 560 dp max width
 * (desktop) and centers on a dimmed scrim. Escape / outside-click calls [onDismiss].
 */
@Composable
fun CrmDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel",
    onConfirm: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return
    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(CrmTheme.animation.fast)) +
                slideInVertically(initialOffsetY = { -it / 8 }, animationSpec = tween(CrmTheme.animation.fast)),
            exit = fadeOut(animationSpec = tween(CrmTheme.animation.fast)),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .clip(CrmTheme.shapes.lg)
                    .shadow(CrmTheme.elevation.xl, CrmTheme.shapes.lg)
                    .background(CrmTheme.colors.surface)
                    .padding(CrmTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
            ) {
                if (title != null) {
                    CrmText(title, style = CrmTheme.typography.h2, color = CrmTheme.colors.onSurface)
                }
                content()
                if (onConfirm != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm, Alignment.End),
                    ) {
                        CrmSecondaryButton(text = dismissLabel, onClick = onDismiss)
                        CrmPrimaryButton(text = confirmLabel, onClick = {
                            onConfirm()
                            onDismiss()
                        })
                    }
                }
            }
        }
    }
}

/**
 * Side drawer that slides in from the start (left) edge. Renders a scrim behind it;
 * clicking the scrim calls [onDismiss].
 */
@Composable
fun CrmDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    width: androidx.compose.ui.unit.Dp = 320.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return
    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CrmTheme.colors.onBackground.copy(alpha = CrmTheme.opacity.overlay))
                .clickable(onClick = onDismiss),
        )
        // Panel
        AnimatedVisibility(
            visible = true,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(CrmTheme.animation.normal),
            ) + fadeIn(animationSpec = tween(CrmTheme.animation.normal)),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(CrmTheme.animation.normal),
            ) + fadeOut(animationSpec = tween(CrmTheme.animation.fast)),
        ) {
            Column(
                modifier = Modifier
                    .width(width)
                    .fillMaxHeight()
                    .background(CrmTheme.colors.surface)
                    .padding(CrmTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                content = content,
            )
        }
    }
}

/** Context / action menu rendered as a positioned column of clickable items. */
@Composable
fun CrmMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    items: List<CrmMenuItem>,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(animationSpec = tween(CrmTheme.animation.fast)) +
            slideInVertically(initialOffsetY = { -it / 4 }, animationSpec = tween(CrmTheme.animation.fast)),
        exit = fadeOut(animationSpec = tween(CrmTheme.animation.fast)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 160.dp)
                .shadow(CrmTheme.elevation.lg, CrmTheme.shapes.md)
                .clip(CrmTheme.shapes.md)
                .background(CrmTheme.colors.surface),
        ) {
            items.forEachIndexed { i, item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !item.destructive || true) {
                            item.onClick()
                            onDismiss()
                        }
                        .padding(horizontal = CrmTheme.spacing.lg, vertical = CrmTheme.spacing.md),
                ) {
                    CrmText(
                        item.label,
                        style = CrmTheme.typography.body,
                        color = if (item.destructive) CrmTheme.colors.error else CrmTheme.colors.onSurface,
                    )
                }
                if (i < items.lastIndex) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CrmBorder.thin)
                            .background(CrmTheme.colors.divider),
                    )
                }
            }
        }
    }
}

data class CrmMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val destructive: Boolean = false,
)

/** Bottom sheet that slides up from the bottom edge. Mobile-first overlay. */
@Composable
fun CrmBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CrmTheme.colors.onBackground.copy(alpha = CrmTheme.opacity.overlay))
                .clickable(onClick = onDismiss),
        )
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(CrmTheme.animation.normal),
            ) + fadeIn(animationSpec = tween(CrmTheme.animation.normal)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(CrmTheme.animation.normal),
            ) + fadeOut(animationSpec = tween(CrmTheme.animation.fast)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CrmTheme.shapes.lg)
                    .background(CrmTheme.colors.surface)
                    .padding(CrmTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(CrmTheme.spacing.xxl)
                        .height(CrmTheme.spacing.xxs)
                        .clip(CrmTheme.shapes.pill)
                        .background(CrmTheme.colors.divider)
                        .align(Alignment.CenterHorizontally),
                )
                content()
            }
        }
    }
}
