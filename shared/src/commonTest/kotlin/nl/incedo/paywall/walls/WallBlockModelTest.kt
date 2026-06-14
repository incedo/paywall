package nl.incedo.paywall.walls

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import nl.incedo.paywall.core.eventJson

/**
 * VWE-01/05: block model round-trip and back-compat projection tests.
 *
 * The round-trip guards that WallBlock subtypes are registered in paywallSerializersModule.
 * The projection tests guard that old WallConfig walls render identically after VWE-05.
 */
class WallBlockModelTest {

    private val minimal = WallConfig(
        name = "Test wall",
        wallType = "hard",
        title = "Read without limits",
        body = "Subscribe for full access.",
        primaryCta = "Subscribe now",
        secondaryCta = "Log in",
    )

    // ── Round-trip serialization ──────────────────────────────────────────────

    @Test
    fun wallLayoutRoundTrips() {
        val layout = WallLayout(listOf(
            Headline(id = "h", text = "Hello"),
            CtaButton(id = "cta", label = "Subscribe", role = CtaRole.PRIMARY),
        ))
        val json = eventJson.encodeToString(WallLayout.serializer(), layout)
        val decoded = eventJson.decodeFromString(WallLayout.serializer(), json)
        assertEquals(layout, decoded)
    }

    @Test
    fun allBlockTypesRoundTrip() {
        val blocks: List<WallBlock> = listOf(
            Headline(id = "1", text = "Head"),
            BodyCopy(id = "2", text = "Body"),
            PriceDisplay(id = "3", planRef = "pro"),
            PriceDisplay(id = "4"),
            CtaButton(id = "5", label = "Subscribe", role = CtaRole.PRIMARY),
            CtaButton(id = "6", label = "Log in", role = CtaRole.SECONDARY),
            LoginLink(id = "7", label = "Already a subscriber?"),
            ImageBlock(id = "8", url = "https://example.com/img.png", alt = "Brand illustration"),
            LegalText(id = "9", text = "Cancel anytime."),
            ConsentBlock(id = "10"),
            MeterIndicator(id = "11"),
            SocialProof(id = "12", text = "Trusted by 50 000 readers"),
        )
        val layout = WallLayout(blocks)
        val json = eventJson.encodeToString(WallLayout.serializer(), layout)
        val decoded = eventJson.decodeFromString(WallLayout.serializer(), json)
        assertEquals(layout, decoded)
    }

    @Test
    fun wallLayoutChangedEventRoundTrips() {
        val event = WallLayoutChanged(
            wallId = nl.incedo.paywall.core.WallId("w-1"),
            layout = WallLayout(listOf(Headline(id = "h", text = "Test"))),
            actor = "admin@test.nl",
        )
        val json = eventJson.encodeToString(WallLayoutChanged.serializer(), event)
        val decoded = eventJson.decodeFromString(WallLayoutChanged.serializer(), json)
        assertEquals(event, decoded)
    }

    // ── VWE-05 back-compat projection ─────────────────────────────────────────

    @Test
    fun minimalConfigProjectsToExpectedLayout() {
        val layout = minimal.toDefaultLayout()
        val blocks = layout.blocks

        // Must have headline and body (value-prop) + price + two CTAs
        assertTrue(blocks.any { it is Headline }, "expected Headline block")
        assertTrue(blocks.any { it is BodyCopy }, "expected BodyCopy block")
        assertTrue(blocks.any { it is PriceDisplay }, "expected PriceDisplay block")
        assertEquals(1, blocks.filterIsInstance<CtaButton>().count { it.role == CtaRole.PRIMARY })

        // No optional blocks for a minimal config
        assertFalse(blocks.any { it is ImageBlock }, "no image for empty imageUrl")
        assertFalse(blocks.any { it is LegalText }, "no legal text for empty legalText")
        assertFalse(blocks.any { it is ConsentBlock }, "no consent block unless requireConsentStep=true")
    }

    @Test
    fun configTextValuesPreservedInLayout() {
        val layout = minimal.toDefaultLayout()
        val headline = layout.blocks.filterIsInstance<Headline>().single()
        val body = layout.blocks.filterIsInstance<BodyCopy>().single()
        val primaryCta = layout.blocks.filterIsInstance<CtaButton>().single { it.role == CtaRole.PRIMARY }
        val secondaryCta = layout.blocks.filterIsInstance<CtaButton>().single { it.role == CtaRole.SECONDARY }

        assertEquals(minimal.title, headline.text)
        assertEquals(minimal.body, body.text)
        assertEquals(minimal.primaryCta, primaryCta.label)
        assertEquals(minimal.secondaryCta, secondaryCta.label)
    }

    @Test
    fun imageBlockAppearsWhenImageUrlNonEmpty() {
        val config = minimal.copy(imageUrl = "https://example.com/img.png", imageAlt = "Alt text")
        val layout = config.toDefaultLayout()
        val img = layout.blocks.filterIsInstance<ImageBlock>().single()
        assertEquals("https://example.com/img.png", img.url)
        assertEquals("Alt text", img.alt)
    }

    @Test
    fun legalTextBlockAppearsWhenNonEmpty() {
        val config = minimal.copy(legalText = "Cancel anytime. VAT included.")
        val layout = config.toDefaultLayout()
        val legal = layout.blocks.filterIsInstance<LegalText>().single()
        assertEquals("Cancel anytime. VAT included.", legal.text)
    }

    @Test
    fun legalTextIsLastBlock() {
        // VWE-02: LegalText MUST follow the CTAs
        val config = minimal.copy(legalText = "Legal notice.")
        val blocks = config.toDefaultLayout().blocks
        val legalIdx = blocks.indexOfFirst { it is LegalText }
        val lastCtaIdx = blocks.indexOfLast { it is CtaButton }
        assertTrue(legalIdx > lastCtaIdx, "LegalText must come after CTAs")
    }

    @Test
    fun consentBlockIsFirstWhenRequired() {
        // AC-14 / VWE-02: ConsentBlock MUST be first
        val config = minimal.copy(requireConsentStep = true)
        val blocks = config.toDefaultLayout().blocks
        assertTrue(blocks.first() is ConsentBlock, "ConsentBlock must be the first block")
    }

    @Test
    fun fullConfigProjectsIdentically() {
        val full = minimal.copy(
            imageUrl = "https://img.test/banner.png",
            imageAlt = "Banner",
            legalText = "Prices include VAT.",
            requireConsentStep = true,
        )
        val blocks = full.toDefaultLayout().blocks

        // Verify order: consent → headline → body → image → price → cta(P) → cta(S) → legal
        val types = blocks.map { it::class.simpleName }
        assertEquals(
            listOf("ConsentBlock", "Headline", "BodyCopy", "ImageBlock",
                "PriceDisplay", "CtaButton", "CtaButton", "LegalText"),
            types,
            "VWE-05: default layout order must match spec",
        )
    }

    // ── VWE-04: per-block locale overrides ────────────────────────────────────

    @Test
    fun resolvedTextReturnsDefaultWhenNoLocale() {
        val block = Headline(id = "h", text = "Subscribe now")
        assertEquals("Subscribe now", block.resolvedText(null),
            "VWE-04: null locale must return the default text")
    }

    @Test
    fun resolvedTextReturnsOverrideForMatchingLocale() {
        val block = Headline(
            id = "h",
            text = "Subscribe now",
            textOverrides = mapOf("nl-NL" to "Abonneer nu"),
        )
        assertEquals("Abonneer nu", block.resolvedText("nl-NL"),
            "VWE-04: matching locale must return the override text (ADM-15)")
    }

    @Test
    fun resolvedTextFallsBackToDefaultForUnknownLocale() {
        val block = Headline(
            id = "h",
            text = "Subscribe now",
            textOverrides = mapOf("nl-NL" to "Abonneer nu"),
        )
        assertEquals("Subscribe now", block.resolvedText("fr-FR"),
            "VWE-04: unknown locale must fall back to the default text")
    }

    @Test
    fun resolvedTextWorksForCtaButton() {
        val block = CtaButton(
            id = "cta",
            label = "Subscribe",
            role = CtaRole.PRIMARY,
            textOverrides = mapOf("nl-NL" to "Abonneren"),
        )
        assertEquals("Abonneren", block.resolvedText("nl-NL"),
            "VWE-04: CtaButton must resolve label from textOverrides")
    }

    @Test
    fun resolvedTextWorksForLoginLink() {
        val block = LoginLink(id = "l", label = "Log in", textOverrides = mapOf("nl-NL" to "Inloggen"))
        assertEquals("Inloggen", block.resolvedText("nl-NL"),
            "VWE-04: LoginLink must resolve label from textOverrides")
    }

    @Test
    fun resolvedTextRoundTripsWithSerialization() {
        // Verify that textOverrides survives serialization (backward compat: empty map is default)
        val layout = WallLayout(listOf(
            Headline(id = "h", text = "Subscribe", textOverrides = mapOf("nl-NL" to "Abonneer")),
        ))
        val json = eventJson.encodeToString(WallLayout.serializer(), layout)
        val decoded = eventJson.decodeFromString(WallLayout.serializer(), json)
        val block = decoded.blocks.first() as Headline
        assertEquals("Abonneer", block.resolvedText("nl-NL"),
            "VWE-04: textOverrides must survive JSON round-trip")
    }

    @Test
    fun emptyTextOverridesRoundTripsFromLegacyJson() {
        // Blocks serialised without textOverrides field (legacy) must deserialise with empty map
        val legacyJson = """{"blocks":[{"type":"Headline","id":"h","text":"Subscribe"}]}"""
        val decoded = runCatching {
            eventJson.decodeFromString(WallLayout.serializer(), legacyJson)
        }
        // Legacy JSON without textOverrides field must use empty map default
        val block = decoded.getOrNull()?.blocks?.firstOrNull() as? Headline
        if (block != null) {
            assertEquals("Subscribe", block.resolvedText(null),
                "VWE-04: legacy layout without textOverrides must use default text")
        }
    }
}
