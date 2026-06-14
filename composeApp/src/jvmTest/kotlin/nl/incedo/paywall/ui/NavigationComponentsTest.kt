package nl.incedo.paywall.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import nl.incedo.paywall.theme.CrmTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NavigationComponentsTest {

    // ── CrmTabs ──────────────────────────────────────────────────────────────

    @Test
    fun `tabs render all tab labels`() = runComposeUiTest {
        setContent {
            CrmTheme { CrmTabs(tabs = listOf("Config", "Targeting", "History"), selectedIndex = 0, onSelect = {}) }
        }
        onNodeWithText("Config").assertIsDisplayed()
        onNodeWithText("Targeting").assertIsDisplayed()
        onNodeWithText("History").assertIsDisplayed()
    }

    @Test
    fun `clicking a tab invokes onSelect with correct index`() = runComposeUiTest {
        var selected = 0
        setContent {
            CrmTheme {
                CrmTabs(tabs = listOf("Alpha", "Beta", "Gamma"), selectedIndex = selected, onSelect = { selected = it })
            }
        }
        onNodeWithText("Beta").performClick()
        assertEquals(1, selected)
        onNodeWithText("Gamma").performClick()
        assertEquals(2, selected)
    }

    // ── CrmBreadcrumb ────────────────────────────────────────────────────────

    @Test
    fun `breadcrumb renders all item labels`() = runComposeUiTest {
        val items = listOf(
            BreadcrumbItem("Walls", onClick = {}),
            BreadcrumbItem("Metered wall", onClick = {}),
            BreadcrumbItem("Config"),
        )
        setContent { CrmTheme { CrmBreadcrumb(items = items) } }
        onNodeWithText("Walls").assertIsDisplayed()
        onNodeWithText("Metered wall").assertIsDisplayed()
        onNodeWithText("Config").assertIsDisplayed()
    }

    @Test
    fun `breadcrumb last item has no click action`() = runComposeUiTest {
        val items = listOf(
            BreadcrumbItem("Home", onClick = {}),
            BreadcrumbItem("Current page"),
        )
        setContent { CrmTheme { CrmBreadcrumb(items = items) } }
        onNodeWithText("Current page").assertHasNoClickAction()
    }

    @Test
    fun `breadcrumb non-last items with onClick are clickable`() = runComposeUiTest {
        var navigated = false
        val items = listOf(
            BreadcrumbItem("Home", onClick = { navigated = true }),
            BreadcrumbItem("Current"),
        )
        setContent { CrmTheme { CrmBreadcrumb(items = items) } }
        onNodeWithText("Home").performClick()
        assertTrue(navigated)
    }

    // ── CrmPagination ────────────────────────────────────────────────────────

    @Test
    fun `pagination hidden when totalPages is 0`() = runComposeUiTest {
        setContent { CrmTheme { CrmPagination(page = 1, totalPages = 0, onPageChange = {}) } }
        // Component returns early — page indicator must not be in the tree
        onNodeWithText("1 / 0").assertDoesNotExist()
    }

    @Test
    fun `pagination shows page indicator`() = runComposeUiTest {
        setContent { CrmTheme { CrmPagination(page = 2, totalPages = 5, onPageChange = {}) } }
        onNodeWithText("2 / 5").assertIsDisplayed()
    }

    @Test
    fun `pagination next invokes onPageChange`() = runComposeUiTest {
        var page = 1
        setContent { CrmTheme { CrmPagination(page = page, totalPages = 3, onPageChange = { page = it }) } }
        onNodeWithText("›").performClick()
        assertEquals(2, page)
    }

    @Test
    fun `pagination previous invokes onPageChange`() = runComposeUiTest {
        var page = 3
        setContent { CrmTheme { CrmPagination(page = page, totalPages = 5, onPageChange = { page = it }) } }
        onNodeWithText("‹").performClick()
        assertEquals(2, page)
    }

    @Test
    fun `pagination previous disabled on first page`() = runComposeUiTest {
        var called = false
        setContent { CrmTheme { CrmPagination(page = 1, totalPages = 3, onPageChange = { called = true }) } }
        onNodeWithText("‹").performClick()
        // Disabled — callback not invoked
        assertTrue(!called)
    }

    @Test
    fun `pagination next disabled on last page`() = runComposeUiTest {
        var called = false
        setContent { CrmTheme { CrmPagination(page = 5, totalPages = 5, onPageChange = { called = true }) } }
        onNodeWithText("›").performClick()
        assertTrue(!called)
    }
}
