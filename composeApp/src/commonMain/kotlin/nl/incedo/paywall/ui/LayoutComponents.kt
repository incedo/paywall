package nl.incedo.paywall.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.theme.LocalWindowSizeClass
import nl.incedo.paywall.theme.WindowSizeClass

/**
 * Wraps [content] inside a [BoxWithConstraints] that measures the available width
 * and provides the matching [WindowSizeClass] via CompositionLocal.
 * Place at the root of the screen (just inside CrmTheme) so all descendants
 * can read [CrmTheme.windowSizeClass].
 */
@Composable
fun CrmResponsiveLayout(
    modifier: Modifier = Modifier,
    content: @Composable (WindowSizeClass) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val wsc = when {
            maxWidth < 600.dp -> WindowSizeClass.COMPACT
            maxWidth < 840.dp -> WindowSizeClass.MEDIUM
            maxWidth < 1200.dp -> WindowSizeClass.EXPANDED
            else -> WindowSizeClass.LARGE
        }
        CompositionLocalProvider(LocalWindowSizeClass provides wsc) {
            content(wsc)
        }
    }
}

/**
 * Responsive scaffold that adapts navigation placement to [WindowSizeClass]:
 * - COMPACT: content fills the screen; [navContent] rendered as a bottom bar below
 * - MEDIUM: [navContent] rendered as a narrow 80 dp rail on the start edge
 * - EXPANDED / LARGE: [navContent] rendered as a full 240 dp side drawer on the start edge
 *
 * Screens themselves don't need to know about window size — they just receive content slot.
 */
@Composable
fun CrmScaffold(
    navContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    CrmResponsiveLayout(modifier = modifier.fillMaxSize()) { wsc ->
        when (wsc) {
            WindowSizeClass.COMPACT -> Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) { content() }
                navContent()
            }
            WindowSizeClass.MEDIUM -> Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.width(80.dp).fillMaxHeight()) { navContent() }
                Box(modifier = Modifier.weight(1f)) { content() }
            }
            WindowSizeClass.EXPANDED, WindowSizeClass.LARGE -> Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.width(240.dp).fillMaxHeight()) { navContent() }
                Box(modifier = Modifier.weight(1f)) { content() }
            }
        }
    }
}
