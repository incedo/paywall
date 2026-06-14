package nl.incedo.paywall.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CrmThemeLoaderTest {

    // ── fromJson — color overrides ────────────────────────────────────────────

    @Test fun `fromJson with empty object returns light theme`() {
        val t = CrmThemeLoader.fromJson("{}")
        assertEquals(LightCrmTheme.colors.primary, t.colors.primary)
    }

    @Test fun `fromJson with base dark uses dark palette`() {
        val t = CrmThemeLoader.fromJson("""{"base":"dark"}""")
        assertEquals(DarkCrmTheme.colors.primary, t.colors.primary)
        assertEquals(DarkCrmTheme.colors.background, t.colors.background)
    }

    @Test fun `fromJson overrides primary color`() {
        val t = CrmThemeLoader.fromJson("""{"colors":{"primary":"#6200EE"}}""")
        assertEquals(Color(0xFF6200EE.toInt()), t.colors.primary)
    }

    @Test fun `fromJson partial color override leaves other colors at base`() {
        val t = CrmThemeLoader.fromJson("""{"colors":{"primary":"#6200EE"}}""")
        assertEquals(LightCrmTheme.colors.background, t.colors.background)
        assertEquals(LightCrmTheme.colors.success, t.colors.success)
    }

    @Test fun `fromJson invalid hex falls back to base color`() {
        val t = CrmThemeLoader.fromJson("""{"colors":{"primary":"notacolor"}}""")
        assertEquals(LightCrmTheme.colors.primary, t.colors.primary)
    }

    @Test fun `fromJson missing hash prefix falls back to base color`() {
        val t = CrmThemeLoader.fromJson("""{"colors":{"primary":"6200EE"}}""")
        assertEquals(LightCrmTheme.colors.primary, t.colors.primary)
    }

    @Test fun `fromJson malformed JSON returns light theme`() {
        val t = CrmThemeLoader.fromJson("{ not valid json !!!")
        assertEquals(LightCrmTheme.colors.primary, t.colors.primary)
    }

    // ── fromJson — spacing ────────────────────────────────────────────────────

    @Test fun `fromJson overrides spacing md`() {
        val t = CrmThemeLoader.fromJson("""{"spacing":{"md":14}}""")
        assertEquals(14f, t.spacing.md.value)
    }

    @Test fun `fromJson partial spacing leaves others at base`() {
        val t = CrmThemeLoader.fromJson("""{"spacing":{"md":14}}""")
        assertEquals(LightCrmTheme.spacing.lg, t.spacing.lg)
    }

    // ── fromJson — typography ─────────────────────────────────────────────────

    @Test fun `fromJson overrides h1 size`() {
        val t = CrmThemeLoader.fromJson("""{"typography":{"h1":{"size":28,"weight":"bold"}}}""")
        assertEquals(28f, t.typography.h1.fontSize.value)
    }

    @Test fun `fromJson overrides body weight`() {
        val t = CrmThemeLoader.fromJson("""{"typography":{"body":{"size":15,"weight":"medium"}}}""")
        assertEquals(androidx.compose.ui.text.font.FontWeight.Medium, t.typography.body.fontWeight)
    }

    @Test fun `fromJson unknown weight leaves font weight at base`() {
        val t = CrmThemeLoader.fromJson("""{"typography":{"h1":{"size":24,"weight":"ultrablack"}}}""")
        assertEquals(LightCrmTheme.typography.h1.fontWeight, t.typography.h1.fontWeight)
    }

    // ── fromJson — shapes ─────────────────────────────────────────────────────

    @Test fun `fromJson overrides shape md corner radius`() {
        val t = CrmThemeLoader.fromJson("""{"shapes":{"md":10}}""")
        // round-trip: md corner should be 10dp
        assertEquals(10, cornerDp(t.shapes.md))
    }

    @Test fun `fromJson partial shapes leaves others at base`() {
        val t = CrmThemeLoader.fromJson("""{"shapes":{"md":10}}""")
        assertEquals(cornerDp(LightCrmTheme.shapes.sm), cornerDp(t.shapes.sm))
    }

    // ── toJson round-trip ─────────────────────────────────────────────────────

    @Test fun `toJson produces valid JSON with name field`() {
        val json = CrmThemeLoader.toJson(LightCrmTheme, "Test Brand")
        assertTrue(json.contains("\"Test Brand\""))
        assertTrue(json.contains("\"colors\""))
        assertTrue(json.contains("\"spacing\""))
        assertTrue(json.contains("\"typography\""))
        assertTrue(json.contains("\"shapes\""))
    }

    @Test fun `round-trip light theme preserves primary color`() {
        val json = CrmThemeLoader.toJson(LightCrmTheme)
        val restored = CrmThemeLoader.fromJson(json)
        assertEquals(LightCrmTheme.colors.primary, restored.colors.primary)
        assertEquals(LightCrmTheme.colors.background, restored.colors.background)
    }

    @Test fun `round-trip custom theme preserves overridden color`() {
        val custom = LightCrmTheme.copy(
            colors = LightCrmTheme.colors.copy(primary = Color(0xFF6200EE.toInt()))
        )
        val json = CrmThemeLoader.toJson(custom, "Brand")
        val restored = CrmThemeLoader.fromJson(json)
        assertEquals(Color(0xFF6200EE.toInt()), restored.colors.primary)
    }

    @Test fun `round-trip preserves spacing values`() {
        val json = CrmThemeLoader.toJson(LightCrmTheme)
        val restored = CrmThemeLoader.fromJson(json)
        assertEquals(LightCrmTheme.spacing.md.value, restored.spacing.md.value)
        assertEquals(LightCrmTheme.spacing.xl.value, restored.spacing.xl.value)
    }

    @Test fun `round-trip preserves typography size`() {
        val json = CrmThemeLoader.toJson(LightCrmTheme)
        val restored = CrmThemeLoader.fromJson(json)
        assertEquals(LightCrmTheme.typography.h1.fontSize.value, restored.typography.h1.fontSize.value)
        assertEquals(LightCrmTheme.typography.body.fontWeight, restored.typography.body.fontWeight)
    }

    // ── CrmDesignTheme ────────────────────────────────────────────────────────

    @Test fun `LightCrmTheme has correct primary color`() {
        assertNotNull(LightCrmTheme)
        assertEquals(LightCrmColors.primary, LightCrmTheme.colors.primary)
    }

    @Test fun `DarkCrmTheme has correct background`() {
        assertNotNull(DarkCrmTheme)
        assertEquals(DarkCrmColors.background, DarkCrmTheme.colors.background)
    }

    @Test fun `CrmDesignTheme copy patches a single token group`() {
        val custom = LightCrmTheme.copy(
            colors = LightCrmTheme.colors.copy(primary = Color.Red)
        )
        assertEquals(Color.Red, custom.colors.primary)
        assertEquals(LightCrmTheme.spacing, custom.spacing)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun cornerDp(shape: androidx.compose.foundation.shape.RoundedCornerShape): Int {
        val density = androidx.compose.ui.unit.Density(1f, 1f)
        return shape.topStart.toPx(androidx.compose.ui.geometry.Size(0f, 0f), density).toInt()
    }
}
