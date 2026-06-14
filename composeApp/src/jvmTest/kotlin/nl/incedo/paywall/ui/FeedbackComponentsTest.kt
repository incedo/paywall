package nl.incedo.paywall.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import nl.incedo.paywall.theme.CrmTheme
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FeedbackComponentsTest {

    // ── CrmSnackbar ──────────────────────────────────────────────────────────

    @Test
    fun `snackbar visible=true shows message`() = runComposeUiTest {
        setContent {
            CrmTheme { CrmSnackbar(visible = true, message = "Wall published") }
        }
        onNodeWithText("Wall published").assertIsDisplayed()
    }

    @Test
    fun `snackbar visible=false hides message`() = runComposeUiTest {
        setContent {
            CrmTheme { CrmSnackbar(visible = false, message = "Wall published") }
        }
        onNodeWithText("Wall published").assertDoesNotExist()
    }

    @Test
    fun `snackbar action button invokes callback`() = runComposeUiTest {
        var dismissed = false
        setContent {
            CrmTheme {
                CrmSnackbar(
                    visible = true,
                    message = "Draft saved",
                    actionLabel = "Undo",
                    onAction = { dismissed = true },
                )
            }
        }
        onNodeWithText("Undo").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun `snackbar success variant renders message`() = runComposeUiTest {
        setContent {
            CrmTheme {
                CrmSnackbar(visible = true, message = "Saved!", variant = SnackbarVariant.SUCCESS)
            }
        }
        onNodeWithText("Saved!").assertIsDisplayed()
    }

    @Test
    fun `snackbar error variant renders message`() = runComposeUiTest {
        setContent {
            CrmTheme {
                CrmSnackbar(visible = true, message = "Network error", variant = SnackbarVariant.ERROR)
            }
        }
        onNodeWithText("Network error").assertIsDisplayed()
    }

    // ── CrmSpinner ──────────────────────────────────────────────────────────

    @Test
    fun `spinner renders without crash`() = runComposeUiTest {
        setContent { CrmTheme { CrmSpinner() } }
        // No assertion needed — verifies it composes successfully
    }

    // ── CrmProgressBar ──────────────────────────────────────────────────────

    @Test
    fun `progress bar renders without crash`() = runComposeUiTest {
        setContent { CrmTheme { CrmProgressBar(progress = 0.5f) } }
    }

    @Test
    fun `progress bar with label shows text`() = runComposeUiTest {
        setContent { CrmTheme { CrmProgressBar(progress = 0.75f, label = "Uploading") } }
        onNodeWithText("Uploading").assertIsDisplayed()
        onNodeWithText("75%").assertIsDisplayed()
    }

    @Test
    fun `progress bar clamps to 0-1 range`() = runComposeUiTest {
        // verify no crash with out-of-range values
        setContent { CrmTheme { CrmProgressBar(progress = 2.5f, label = "Over") } }
        onNodeWithText("100%").assertIsDisplayed()
    }

    // ── CrmSkeletonLoader ────────────────────────────────────────────────────

    @Test
    fun `skeleton loader renders without crash`() = runComposeUiTest {
        setContent { CrmTheme { CrmSkeletonLoader() } }
    }

    // ── CrmEmptyState ────────────────────────────────────────────────────────

    @Test
    fun `empty state shows title`() = runComposeUiTest {
        setContent { CrmTheme { CrmEmptyState(title = "No walls yet") } }
        onNodeWithText("No walls yet").assertIsDisplayed()
    }

    @Test
    fun `empty state shows subtitle when provided`() = runComposeUiTest {
        setContent {
            CrmTheme { CrmEmptyState(title = "No results", subtitle = "Try adjusting your filters") }
        }
        onNodeWithText("No results").assertIsDisplayed()
        onNodeWithText("Try adjusting your filters").assertIsDisplayed()
    }

    @Test
    fun `empty state action button invokes callback`() = runComposeUiTest {
        var clicked = false
        setContent {
            CrmTheme {
                CrmEmptyState(title = "Nothing here", actionLabel = "Create wall", onAction = { clicked = true })
            }
        }
        onNodeWithText("Create wall").performClick()
        assertTrue(clicked)
    }
}
