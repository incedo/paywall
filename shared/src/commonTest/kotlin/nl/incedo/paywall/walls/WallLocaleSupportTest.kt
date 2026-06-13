package nl.incedo.paywall.walls

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * ADM-15: per-brand locale support for wall designs.
 * WallConfig holds translatable text fields; resolveForLocale picks
 * the best available translation for the brand's locale.
 */
class WallLocaleSupportTest {

    private val defaultConfig = WallConfig(
        name = "Experiment wall",
        wallType = "hard",
        title = "Default title",
        body = "Default body",
        primaryCta = "Subscribe",
        secondaryCta = "Maybe later",
    )

    @Test
    fun noTranslationsFallsBackToDefaults() {
        val copy = defaultConfig.resolveForLocale("nl-NL")
        assertEquals("Default title", copy.title)
        assertEquals("Default body", copy.body)
        assertEquals("Subscribe", copy.primaryCta)
        assertEquals("Maybe later", copy.secondaryCta)
    }

    @Test
    fun exactLocaleMatchOverridesAllFields() {
        val config = defaultConfig.copy(
            translations = mapOf(
                "nl-NL" to WallCopy(
                    title = "Abonneer nu",
                    body = "Onbeperkt lezen",
                    primaryCta = "Ja, ik wil",
                    secondaryCta = "Misschien later",
                ),
            ),
        )
        val copy = config.resolveForLocale("nl-NL")
        assertEquals("Abonneer nu", copy.title)
        assertEquals("Onbeperkt lezen", copy.body)
        assertEquals("Ja, ik wil", copy.primaryCta)
        assertEquals("Misschien later", copy.secondaryCta)
    }

    @Test
    fun partialTranslationFallsBackToDefaultForMissingFields() {
        val config = defaultConfig.copy(
            translations = mapOf(
                "nl-NL" to WallCopy(primaryCta = "Ja, ik wil"),
            ),
        )
        val copy = config.resolveForLocale("nl-NL")
        // Overridden field uses translation
        assertEquals("Ja, ik wil", copy.primaryCta)
        // Unset fields fall back to the default config values
        assertEquals("Default title", copy.title)
        assertEquals("Default body", copy.body)
        assertEquals("Maybe later", copy.secondaryCta)
    }

    @Test
    fun languageOnlyTagMatchesRegionalLocale() {
        // "nl" covers "nl-NL", "nl-BE", etc. when no exact match
        val config = defaultConfig.copy(
            translations = mapOf("nl" to WallCopy(title = "Dutch fallback")),
        )
        val copy = config.resolveForLocale("nl-NL")
        assertEquals("Dutch fallback", copy.title, "ADM-15: language tag 'nl' must serve 'nl-NL' requests")
    }

    @Test
    fun exactLocaleMatchTakesPrecedenceOverLanguageTag() {
        val config = defaultConfig.copy(
            translations = mapOf(
                "nl" to WallCopy(title = "Generic Dutch"),
                "nl-NL" to WallCopy(title = "Netherlands Dutch"),
            ),
        )
        val copy = config.resolveForLocale("nl-NL")
        assertEquals("Netherlands Dutch", copy.title, "ADM-15: exact locale must win over language-only tag")
    }

    @Test
    fun unknownLocaleUsesDefaults() {
        val config = defaultConfig.copy(
            translations = mapOf("nl-NL" to WallCopy(title = "Abonneer nu")),
        )
        val copy = config.resolveForLocale("fr-FR")
        assertEquals("Default title", copy.title, "ADM-15: unknown locale must fall back to defaults")
    }

    @Test
    fun wallCopyFieldsAreAllNullableByDefault() {
        val empty = WallCopy()
        assertNull(empty.title)
        assertNull(empty.body)
        assertNull(empty.primaryCta)
        assertNull(empty.secondaryCta)
    }
}
