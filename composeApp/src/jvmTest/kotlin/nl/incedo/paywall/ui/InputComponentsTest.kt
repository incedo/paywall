package nl.incedo.paywall.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import nl.incedo.paywall.theme.CrmTheme
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

@OptIn(ExperimentalTestApi::class)
class InputComponentsTest {

    // ── CrmCheckbox ──────────────────────────────────────────────────────────

    @Test
    fun `checkbox unchecked by default renders off state`() = runComposeUiTest {
        setContent { CrmTheme { CrmCheckbox(checked = false, onCheckedChange = {}) } }
        onNode(hasRole(Role.Checkbox)).assertIsOff()
    }

    @Test
    fun `checkbox checked renders on state`() = runComposeUiTest {
        setContent { CrmTheme { CrmCheckbox(checked = true, onCheckedChange = {}) } }
        onNode(hasRole(Role.Checkbox)).assertIsOn()
    }

    @Test
    fun `checkbox click toggles state`() = runComposeUiTest {
        // mutableStateOf triggers recomposition so the second click sees checked=true
        var checked by mutableStateOf(false)
        setContent { CrmTheme { CrmCheckbox(checked = checked, onCheckedChange = { checked = it }) } }
        onNode(hasRole(Role.Checkbox)).performClick()
        assertTrue(checked)
        onNode(hasRole(Role.Checkbox)).performClick()
        assertFalse(checked)
    }

    @Test
    fun `checkbox disabled does not toggle`() = runComposeUiTest {
        var toggled = false
        setContent {
            CrmTheme { CrmCheckbox(checked = false, onCheckedChange = { toggled = true }, enabled = false) }
        }
        onNode(hasRole(Role.Checkbox)).performClick()
        assertFalse(toggled)
    }

    @Test
    fun `checkbox label is displayed`() = runComposeUiTest {
        setContent { CrmTheme { CrmCheckbox(checked = false, onCheckedChange = {}, label = "Accept terms") } }
        onNodeWithText("Accept terms").assertIsDisplayed()
    }

    // ── CrmRadio ─────────────────────────────────────────────────────────────

    @Test
    fun `radio click invokes onClick`() = runComposeUiTest {
        var clicked = false
        setContent { CrmTheme { CrmRadio(selected = false, onClick = { clicked = true }) } }
        onNode(hasRole(Role.RadioButton)).performClick()
        assertTrue(clicked)
    }

    @Test
    fun `radio disabled does not invoke onClick`() = runComposeUiTest {
        var clicked = false
        setContent { CrmTheme { CrmRadio(selected = false, onClick = { clicked = true }, enabled = false) } }
        onNode(hasRole(Role.RadioButton)).performClick()
        assertFalse(clicked)
    }

    @Test
    fun `radio label is displayed`() = runComposeUiTest {
        setContent { CrmTheme { CrmRadio(selected = false, onClick = {}, label = "Option A") } }
        onNodeWithText("Option A").assertIsDisplayed()
    }

    // ── CrmSwitch ────────────────────────────────────────────────────────────

    @Test
    fun `switch off when checked=false`() = runComposeUiTest {
        setContent { CrmTheme { CrmSwitch(checked = false, onCheckedChange = {}) } }
        onNode(hasRole(Role.Switch)).assertIsOff()
    }

    @Test
    fun `switch on when checked=true`() = runComposeUiTest {
        setContent { CrmTheme { CrmSwitch(checked = true, onCheckedChange = {}) } }
        onNode(hasRole(Role.Switch)).assertIsOn()
    }

    @Test
    fun `switch click toggles`() = runComposeUiTest {
        var state = false
        setContent { CrmTheme { CrmSwitch(checked = state, onCheckedChange = { state = it }) } }
        onNode(hasRole(Role.Switch)).performClick()
        assertTrue(state)
    }

    @Test
    fun `switch disabled does not toggle`() = runComposeUiTest {
        var toggled = false
        setContent {
            CrmTheme { CrmSwitch(checked = false, onCheckedChange = { toggled = true }, enabled = false) }
        }
        onNode(hasRole(Role.Switch)).performClick()
        assertFalse(toggled)
    }

    @Test
    fun `switch label is displayed`() = runComposeUiTest {
        setContent { CrmTheme { CrmSwitch(checked = false, onCheckedChange = {}, label = "Dark mode") } }
        onNodeWithText("Dark mode").assertIsDisplayed()
    }

    // ── CrmTextArea ──────────────────────────────────────────────────────────

    @Test
    fun `textarea label is displayed`() = runComposeUiTest {
        setContent { CrmTheme { CrmTextArea(label = "Notes", value = "", onValueChange = {}) } }
        onNodeWithText("Notes").assertIsDisplayed()
    }

    @Test
    fun `textarea shows current value`() = runComposeUiTest {
        setContent { CrmTheme { CrmTextArea(label = "Notes", value = "hello", onValueChange = {}) } }
        onNodeWithText("hello").assertIsDisplayed()
    }

    @Test
    fun `textarea character counter shown when maxLength set`() = runComposeUiTest {
        setContent {
            CrmTheme { CrmTextArea(label = "Bio", value = "abc", onValueChange = {}, maxLength = 100) }
        }
        onNodeWithText("3/100").assertIsDisplayed()
    }
}
