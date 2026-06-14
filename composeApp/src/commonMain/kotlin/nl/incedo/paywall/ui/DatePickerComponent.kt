package nl.incedo.paywall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import nl.incedo.paywall.theme.CrmBorder
import nl.incedo.paywall.theme.CrmTheme

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
)
private val DAY_HEADERS = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
    else -> 0
}

// Tomohiko Sakamoto's algorithm — returns 0 = Sunday … 6 = Saturday
private fun dayOfWeek(year: Int, month: Int, day: Int): Int {
    val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    var y = year
    if (month < 3) y--
    return (y + y / 4 - y / 100 + y / 400 + t[month - 1] + day) % 7
}

private fun parseDate(value: String): Triple<Int, Int, Int>? {
    val parts = value.split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    return if (m in 1..12 && d in 1..daysInMonth(y, m)) Triple(y, m, d) else null
}

private fun formatDate(year: Int, month: Int, day: Int) =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

/**
 * Date picker field. Clicking the field opens a calendar popup.
 * [value] and [onValueChange] use "YYYY-MM-DD" format (empty string = no date selected).
 * On desktop the popup appears inline below the field; on mobile the caller can wrap
 * this in a [CrmBottomSheet] for the full-screen variant.
 */
@Composable
fun CrmDatePicker(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Pick a date…",
    enabled: Boolean = true,
) {
    val today = Triple(2026, 6, 14) // fixed as anchor; real clock via expect/actual if needed
    val parsed = parseDate(value)
    var showPopup by remember { mutableStateOf(false) }
    var viewYear by remember { mutableStateOf(parsed?.first ?: today.first) }
    var viewMonth by remember { mutableStateOf(parsed?.second ?: today.second) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
        CrmText(label, style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CrmTheme.shapes.md)
                    .background(if (enabled) CrmTheme.colors.surface else CrmTheme.colors.disabled)
                    .border(
                        if (showPopup) CrmBorder.thick else CrmBorder.default,
                        if (showPopup) CrmTheme.colors.primary else CrmTheme.colors.divider,
                        CrmTheme.shapes.md,
                    )
                    .clickable(enabled = enabled) { showPopup = !showPopup }
                    .padding(horizontal = CrmTheme.spacing.md, vertical = CrmTheme.spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrmText(
                    value.ifEmpty { placeholder },
                    style = CrmTheme.typography.body,
                    color = if (value.isEmpty()) CrmTheme.colors.onSurfaceVariant else CrmTheme.colors.onSurface,
                )
                CrmText("📅", style = CrmTheme.typography.body, color = CrmTheme.colors.onSurfaceVariant)
            }

            if (showPopup) {
                Popup(onDismissRequest = { showPopup = false }) {
                    Column(
                        modifier = Modifier
                            .widthIn(min = 280.dp)
                            .shadow(CrmTheme.elevation.lg, CrmTheme.shapes.md)
                            .clip(CrmTheme.shapes.md)
                            .background(CrmTheme.colors.surface)
                            .border(CrmBorder.default, CrmTheme.colors.divider, CrmTheme.shapes.md)
                            .padding(CrmTheme.spacing.md),
                        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                    ) {
                        // ── Month navigation ─────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CrmText(
                                "‹",
                                style = CrmTheme.typography.h3,
                                color = CrmTheme.colors.primary,
                                modifier = Modifier.clickable {
                                    if (viewMonth == 1) { viewMonth = 12; viewYear-- }
                                    else viewMonth--
                                },
                            )
                            CrmText(
                                "${MONTH_NAMES[viewMonth - 1]} $viewYear",
                                style = CrmTheme.typography.h3,
                                color = CrmTheme.colors.onSurface,
                            )
                            CrmText(
                                "›",
                                style = CrmTheme.typography.h3,
                                color = CrmTheme.colors.primary,
                                modifier = Modifier.clickable {
                                    if (viewMonth == 12) { viewMonth = 1; viewYear++ }
                                    else viewMonth++
                                },
                            )
                        }

                        // ── Day-of-week headers ──────────────────────────────
                        Row(modifier = Modifier.fillMaxWidth()) {
                            DAY_HEADERS.forEach { day ->
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    CrmText(day, style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
                                }
                            }
                        }

                        // ── Day grid ─────────────────────────────────────────
                        val firstDow = dayOfWeek(viewYear, viewMonth, 1)
                        val days = daysInMonth(viewYear, viewMonth)
                        val cells = firstDow + days // leading blanks + day count
                        val rows = (cells + 6) / 7

                        repeat(rows) { row ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                repeat(7) { col ->
                                    val cellIndex = row * 7 + col
                                    val dayNum = cellIndex - firstDow + 1
                                    val isValid = dayNum in 1..days
                                    val isSelected = isValid &&
                                        parsed?.let { (y, m, d) -> y == viewYear && m == viewMonth && d == dayNum } == true

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(CrmTheme.spacing.xxs)
                                            .size(CrmTheme.spacing.xxl)
                                            .clip(CrmTheme.shapes.pill)
                                            .background(
                                                when {
                                                    isSelected -> CrmTheme.colors.primary
                                                    else -> CrmTheme.colors.surface
                                                },
                                            )
                                            .then(
                                                if (isValid) Modifier.clickable {
                                                    onValueChange(formatDate(viewYear, viewMonth, dayNum))
                                                    showPopup = false
                                                } else Modifier,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isValid) {
                                            CrmText(
                                                "$dayNum",
                                                style = CrmTheme.typography.body,
                                                color = when {
                                                    isSelected -> CrmTheme.colors.onPrimary
                                                    else -> CrmTheme.colors.onSurface
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── Clear action ─────────────────────────────────────
                        if (value.isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                CrmTextButton(text = "Clear", onClick = {
                                    onValueChange("")
                                    showPopup = false
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
