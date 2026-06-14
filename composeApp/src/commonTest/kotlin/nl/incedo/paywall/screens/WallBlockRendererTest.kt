package nl.incedo.paywall.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import nl.incedo.paywall.model.WallDefinition
import nl.incedo.paywall.model.WallType
import nl.incedo.paywall.walls.ConsentBlock
import nl.incedo.paywall.walls.CtaButton
import nl.incedo.paywall.walls.CtaRole
import nl.incedo.paywall.walls.Headline
import nl.incedo.paywall.walls.LegalText
import nl.incedo.paywall.walls.MeterIndicator
import nl.incedo.paywall.walls.WallLayout

/**
 * VWE-05: visual parity — the layout projected from WallDefinition must contain
 * the same content in the same structural order as the former fixed GateCard rendering.
 */
class WallBlockRendererTest {

    private val minimal = WallDefinition(
        type = WallType.Metered,
        title = "You've reached your limit",
        body = "Subscribe for full access.",
        primaryCta = "Subscribe now",
        secondaryCta = "Log in",
    )

    @Test
    fun meteredLayoutStartsWithMeterIndicator() {
        val layout = minimal.toWallLayout()
        assertIs<MeterIndicator>(layout.blocks.first(), "Metered wall: first block must be MeterIndicator (PW-22)")
    }

    @Test
    fun headlineTextMatchesDefinitionTitle() {
        val layout = minimal.toWallLayout()
        val headline = layout.blocks.filterIsInstance<Headline>().single()
        assertEquals(minimal.title, headline.text)
    }

    @Test
    fun primaryCtaMatchesDefinition() {
        val layout = minimal.toWallLayout()
        val primary = layout.blocks.filterIsInstance<CtaButton>().single { it.role == CtaRole.PRIMARY }
        assertEquals(minimal.primaryCta, primary.label)
    }

    @Test
    fun consentBlockIsFirstForNonMeteredWhenRequired() {
        val config = WallDefinition(
            type = WallType.Hard,
            title = "Subscribe",
            body = "Full access.",
            primaryCta = "Subscribe",
            secondaryCta = "Log in",
            requireConsentStep = true,
        )
        val layout = config.toWallLayout()
        assertIs<ConsentBlock>(layout.blocks.first(), "AC-14: ConsentBlock must be first")
    }

    @Test
    fun legalTextIsLastBlockWhenPresent() {
        val config = minimal.copy(legalText = "Cancel anytime.")
        val blocks = config.toWallLayout().blocks
        assertIs<LegalText>(blocks.last(), "VWE-02: LegalText must be last")
        assertEquals("Cancel anytime.", (blocks.last() as LegalText).text)
    }

    @Test
    fun hardWallLayoutHasNoMeterIndicator() {
        val config = minimal.copy(type = WallType.Hard)
        val layout = config.toWallLayout()
        assertTrue(layout.blocks.none { it is MeterIndicator }, "Hard wall: no MeterIndicator")
    }

    @Test
    fun nonMeteredLayoutIsAValidWallLayout() {
        val config = minimal.copy(type = WallType.Freemium)
        val layout: WallLayout = config.toWallLayout()
        assertTrue(layout.blocks.any { it is Headline })
        assertTrue(layout.blocks.filterIsInstance<CtaButton>().any { it.role == CtaRole.PRIMARY })
    }
}
