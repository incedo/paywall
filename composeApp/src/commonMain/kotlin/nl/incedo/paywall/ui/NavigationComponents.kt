package nl.incedo.paywall.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import nl.incedo.paywall.theme.CrmBorder
import nl.incedo.paywall.theme.CrmTheme

/**
 * Horizontal tab bar. Selected tab gets a primary-colored underline indicator.
 * Keyboard: arrow-key cycling is handled by the caller via [onSelect].
 */
@Composable
fun CrmTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            val textColor by animateColorAsState(
                targetValue = if (selected) CrmTheme.colors.primary else CrmTheme.colors.onSurfaceVariant,
                animationSpec = tween(CrmTheme.animation.fast),
            )
            Box(
                modifier = Modifier
                    .clickable { onSelect(index) }
                    .padding(horizontal = CrmTheme.spacing.lg, vertical = CrmTheme.spacing.md),
            ) {
                CrmText(tab, style = CrmTheme.typography.button, color = textColor)
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(CrmBorder.thick)
                            .clip(CrmTheme.shapes.pill)
                            .background(CrmTheme.colors.primary),
                    )
                }
            }
        }
    }
}

data class BreadcrumbItem(val label: String, val onClick: (() -> Unit)? = null)

/**
 * Breadcrumb navigation trail. The last item is non-clickable (current page).
 * Items are separated by "›" chevrons using onSurfaceVariant color.
 */
@Composable
fun CrmBreadcrumb(
    items: List<BreadcrumbItem>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs),
    ) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex
            if (item.onClick != null && !isLast) {
                CrmText(
                    item.label,
                    style = CrmTheme.typography.body,
                    color = CrmTheme.colors.link,
                    modifier = Modifier.clickable(onClick = item.onClick),
                )
            } else {
                CrmText(
                    item.label,
                    style = CrmTheme.typography.body,
                    color = if (isLast) CrmTheme.colors.onSurface else CrmTheme.colors.onSurfaceVariant,
                )
            }
            if (!isLast) {
                CrmText("›", style = CrmTheme.typography.body, color = CrmTheme.colors.onSurfaceVariant)
            }
        }
    }
}

/**
 * Page controls: previous / next buttons + page indicator. [totalPages] = 0 hides the control.
 * A page size selector can be wired via [pageSizes] + [pageSize] + [onPageSizeChange].
 */
@Composable
fun CrmPagination(
    page: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pageSizes: List<Int> = emptyList(),
    pageSize: Int = 20,
    onPageSizeChange: ((Int) -> Unit)? = null,
) {
    if (totalPages <= 0) return
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
    ) {
        if (pageSizes.isNotEmpty() && onPageSizeChange != null) {
            CrmText("Rows:", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
            pageSizes.forEach { size ->
                val selected = size == pageSize
                Box(
                    modifier = Modifier
                        .clip(CrmTheme.shapes.sm)
                        .background(if (selected) CrmTheme.colors.infoContainer else CrmTheme.colors.surface)
                        .clickable { onPageSizeChange(size) }
                        .padding(horizontal = CrmTheme.spacing.sm, vertical = CrmTheme.spacing.xxs),
                ) {
                    CrmText(
                        "$size",
                        style = CrmTheme.typography.label,
                        color = if (selected) CrmTheme.colors.primary else CrmTheme.colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        // Previous
        Box(
            modifier = Modifier
                .widthIn(min = CrmTheme.focus.minTouchTarget)
                .height(CrmTheme.focus.minTouchTarget)
                .clip(CrmTheme.shapes.sm)
                .background(if (page > 1) CrmTheme.colors.surfaceVariant else CrmTheme.colors.disabled)
                .clickable(enabled = page > 1) { onPageChange(page - 1) }
                .padding(horizontal = CrmTheme.spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            CrmText(
                "‹",
                style = CrmTheme.typography.button,
                color = if (page > 1) CrmTheme.colors.onSurface else CrmTheme.colors.onDisabled,
            )
        }
        CrmText(
            "$page / $totalPages",
            style = CrmTheme.typography.label,
            color = CrmTheme.colors.onSurface,
        )
        // Next
        Box(
            modifier = Modifier
                .widthIn(min = CrmTheme.focus.minTouchTarget)
                .height(CrmTheme.focus.minTouchTarget)
                .clip(CrmTheme.shapes.sm)
                .background(if (page < totalPages) CrmTheme.colors.surfaceVariant else CrmTheme.colors.disabled)
                .clickable(enabled = page < totalPages) { onPageChange(page + 1) }
                .padding(horizontal = CrmTheme.spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            CrmText(
                "›",
                style = CrmTheme.typography.button,
                color = if (page < totalPages) CrmTheme.colors.onSurface else CrmTheme.colors.onDisabled,
            )
        }
    }
}
