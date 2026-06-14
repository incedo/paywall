package nl.incedo.paywall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import nl.incedo.paywall.theme.CrmBorder
import nl.incedo.paywall.theme.CrmTheme

/**
 * Column definition for [CrmDataTable].
 *
 * @param header Column header label.
 * @param weight Flex weight — same as [RowScope.weight].
 * @param sortable Whether clicking the header cycles sort direction.
 * @param cell Composable that renders a single cell for a row of type [T].
 */
data class CrmTableColumn<T>(
    val header: String,
    val weight: Float = 1f,
    val sortable: Boolean = false,
    val cell: @Composable RowScope.(row: T) -> Unit,
)

enum class SortDirection { ASC, DESC, NONE }

/**
 * Generic data table with sticky header, optional column sorting, row selection,
 * and integrated [CrmPagination].
 *
 * Sorting and pagination are caller-controlled: [onSort] reports which column was
 * clicked (the caller manages sort state and re-orders [rows]); [onPageChange]
 * reports the new page (the caller re-fetches or slices [rows]).
 */
@Composable
fun <T> CrmDataTable(
    columns: List<CrmTableColumn<T>>,
    rows: List<T>,
    modifier: Modifier = Modifier,
    sortColumnIndex: Int? = null,
    sortDirection: SortDirection = SortDirection.NONE,
    onSort: ((columnIndex: Int) -> Unit)? = null,
    selectable: Boolean = false,
    selectedRows: Set<Int> = emptySet(),
    onSelectionChange: ((Set<Int>) -> Unit)? = null,
    page: Int = 1,
    totalPages: Int = 0,
    onPageChange: ((Int) -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier) {
        // ── Sticky header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CrmTheme.colors.surfaceVariant)
                .padding(horizontal = CrmTheme.spacing.lg, vertical = CrmTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectable) {
                val allSelected = rows.isNotEmpty() && selectedRows.size == rows.size
                CrmCheckbox(
                    checked = allSelected,
                    onCheckedChange = { checked ->
                        onSelectionChange?.invoke(if (checked) rows.indices.toHashSet() else emptySet())
                    },
                    modifier = Modifier.padding(end = CrmTheme.spacing.sm),
                )
            }
            columns.forEachIndexed { colIndex, col ->
                val isSorted = sortColumnIndex == colIndex
                val sortIndicator = when {
                    !isSorted -> ""
                    sortDirection == SortDirection.ASC -> " ↑"
                    sortDirection == SortDirection.DESC -> " ↓"
                    else -> ""
                }
                Box(
                    modifier = Modifier
                        .weight(col.weight)
                        .then(
                            if (col.sortable && onSort != null) {
                                Modifier.clickable { onSort(colIndex) }
                            } else Modifier,
                        ),
                ) {
                    CrmText(
                        col.header + sortIndicator,
                        style = CrmTheme.typography.label,
                        color = if (isSorted) CrmTheme.colors.primary else CrmTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Divider below header ─────────────────────────────────────────────
        CrmDivider()

        // ── Rows ─────────────────────────────────────────────────────────────
        if (rows.isEmpty() && emptyContent != null) {
            emptyContent()
        } else {
            rows.forEachIndexed { rowIndex, row ->
                val selected = rowIndex in selectedRows
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected) CrmTheme.colors.infoContainer
                            else CrmTheme.colors.surface,
                        )
                        .clickable(enabled = selectable) {
                            onSelectionChange?.invoke(
                                if (selected) selectedRows - rowIndex else selectedRows + rowIndex,
                            )
                        }
                        .padding(horizontal = CrmTheme.spacing.lg, vertical = CrmTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectable) {
                        CrmCheckbox(
                            checked = selected,
                            onCheckedChange = { checked ->
                                onSelectionChange?.invoke(
                                    if (checked) selectedRows + rowIndex else selectedRows - rowIndex,
                                )
                            },
                            modifier = Modifier.padding(end = CrmTheme.spacing.sm),
                        )
                    }
                    columns.forEach { col -> col.cell(this, row) }
                }
                if (rowIndex < rows.lastIndex) CrmDivider()
            }
        }

        // ── Pagination ────────────────────────────────────────────────────────
        if (totalPages > 1 && onPageChange != null) {
            CrmDivider()
            Box(modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.md)) {
                CrmPagination(
                    page = page,
                    totalPages = totalPages,
                    onPageChange = onPageChange,
                    pageSizes = listOf(10, 20, 50),
                    pageSize = 20,
                )
            }
        }
    }
}

/** Convenience bulk-action toolbar shown when rows are selected. */
@Composable
fun CrmBulkActionBar(
    selectedCount: Int,
    actions: List<Pair<String, () -> Unit>>,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedCount == 0) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CrmTheme.colors.infoContainer)
            .padding(horizontal = CrmTheme.spacing.lg, vertical = CrmTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
    ) {
        CrmText(
            "$selectedCount selected",
            style = CrmTheme.typography.label,
            color = CrmTheme.colors.primary,
            modifier = Modifier.weight(1f),
        )
        actions.forEach { (label, onClick) ->
            CrmTextButton(text = label, onClick = onClick)
        }
        CrmTextButton(text = "Clear", onClick = onClearSelection)
    }
}
