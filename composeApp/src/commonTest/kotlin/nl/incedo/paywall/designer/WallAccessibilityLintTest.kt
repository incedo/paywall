package nl.incedo.paywall.designer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import nl.incedo.paywall.model.WallDefinition
import nl.incedo.paywall.walls.BodyCopy
import nl.incedo.paywall.walls.CtaButton
import nl.incedo.paywall.walls.CtaRole
import nl.incedo.paywall.walls.Headline
import nl.incedo.paywall.walls.ImageBlock
import nl.incedo.paywall.walls.LegalText
import nl.incedo.paywall.walls.LoginLink
import nl.incedo.paywall.walls.PriceDisplay
import nl.incedo.paywall.walls.SocialProof

/**
 * ADM-17: WCAG 2.1 AA accessibility lint checks run before publish.
 * Pure function — no Compose runtime needed, runs as a commonTest.
 */
class WallAccessibilityLintTest {

    private val base = WallDefinition()

    @Test
    fun noWarningsForCleanDefinition() {
        val warnings = wallAccessibilityLint(base)
        assertTrue(warnings.isEmpty(), "default WallDefinition must pass all lint checks: $warnings")
    }

    @Test
    fun imageWithoutAltTextIsWarned() {
        val def = base.copy(imageUrl = "https://example.com/hero.png", imageAlt = "")
        val warnings = wallAccessibilityLint(def)
        assertTrue(warnings.any { "alt text" in it },
            "ADM-17/WCAG 1.1.1: image without alt text must produce a warning")
    }

    @Test
    fun imageWithAltTextPassesCheck() {
        val def = base.copy(imageUrl = "https://example.com/hero.png", imageAlt = "Brand hero illustration")
        val warnings = wallAccessibilityLint(def)
        assertTrue(warnings.none { "alt text" in it },
            "ADM-17: image with non-empty alt text must not produce an alt-text warning")
    }

    @Test
    fun emptyImageUrlNoAltWarning() {
        val def = base.copy(imageUrl = "", imageAlt = "")
        val warnings = wallAccessibilityLint(def)
        assertTrue(warnings.none { "alt text" in it },
            "ADM-17: no imageUrl means no alt-text warning needed")
    }

    @Test
    fun veryLongPrimaryCtaIsWarned() {
        val longCta = "This CTA text is far too long to fit on a small mobile button"
        val def = base.copy(primaryCta = longCta)
        val warnings = wallAccessibilityLint(def)
        assertTrue(warnings.any { "Primary CTA" in it },
            "ADM-17/WCAG 2.5.3: primary CTA > 40 chars must produce a warning")
    }

    @Test
    fun shortPrimaryCtaPassesCheck() {
        val def = base.copy(primaryCta = "Upgrade to Pro")
        val warnings = wallAccessibilityLint(def)
        assertTrue(warnings.none { "Primary CTA" in it },
            "ADM-17: short primary CTA must not produce a warning")
    }

    @Test
    fun veryLongSecondaryCtaIsWarned() {
        val longCta = "This secondary CTA is also way too long for any mobile display"
        val def = base.copy(secondaryCta = longCta)
        val warnings = wallAccessibilityLint(def)
        assertTrue(warnings.any { "Secondary CTA" in it },
            "ADM-17/WCAG 2.5.3: secondary CTA > 40 chars must produce a warning")
    }

    @Test
    fun legalTextMentioningCookieWithoutConsentStepIsWarned() {
        val def = base.copy(legalText = "We use cookies as described in our policy.", requireConsentStep = false)
        val warnings = wallAccessibilityLint(def)
        assertTrue(warnings.any { "consent" in it.lowercase() },
            "ADM-17/AC-14: cookie mention in legal text without consent step enabled must produce a warning")
    }

    @Test
    fun legalTextMentioningGdprWithoutConsentStepIsWarned() {
        val def = base.copy(legalText = "Your data is processed under GDPR.", requireConsentStep = false)
        val warnings = wallAccessibilityLint(def)
        assertTrue(warnings.any { "consent" in it.lowercase() },
            "ADM-17: GDPR mention without consent step must warn")
    }

    @Test
    fun legalTextWithConsentKeywordAndConsentStepEnabledPassesCheck() {
        val def = base.copy(legalText = "We use cookies.", requireConsentStep = true)
        val warnings = wallAccessibilityLint(def)
        assertTrue(warnings.none { "Consent step" in it },
            "ADM-17: cookie mention + consent step enabled must not produce a consent-step warning")
    }

    @Test
    fun multipleIssuesReportedTogether() {
        val def = base.copy(
            imageUrl = "https://example.com/img.png",
            imageAlt = "",
            primaryCta = "This very long primary call-to-action text exceeds the limit easily",
        )
        val warnings = wallAccessibilityLint(def)
        assertEquals(2, warnings.size, "ADM-17: both alt-text and CTA-length warnings must be returned together")
    }

    // ── VWE-16: per-block accessibility lint ──────────────────────────────────

    @Test
    fun imageBlockWithoutAltFlagsViolation() {
        val block = ImageBlock(id = "img", url = "https://example.com/hero.png", alt = "")
        val violations = blockAccessibilityLint(block)
        assertTrue(violations.any { "alt text" in it },
            "VWE-16/WCAG 1.1.1: ImageBlock without alt text must produce a violation")
    }

    @Test
    fun imageBlockWithAltPassesCheck() {
        val block = ImageBlock(id = "img", url = "https://example.com/hero.png", alt = "Hero image")
        assertTrue(blockAccessibilityLint(block).isEmpty(),
            "VWE-16: ImageBlock with alt text must pass")
    }

    @Test
    fun ctaButtonLongLabelFlagsViolation() {
        val block = CtaButton(id = "cta", label = "This CTA label is far too long for a small mobile viewport to handle", role = CtaRole.PRIMARY)
        val violations = blockAccessibilityLint(block)
        assertTrue(violations.any { "CTA label" in it },
            "VWE-16/WCAG 2.5.3: CtaButton with label > 40 chars must produce a violation")
    }

    @Test
    fun ctaButtonShortLabelPassesCheck() {
        val block = CtaButton(id = "cta", label = "Subscribe now", role = CtaRole.PRIMARY)
        assertTrue(blockAccessibilityLint(block).isEmpty(), "VWE-16: short CTA label must pass")
    }

    @Test
    fun loginLinkLongLabelFlagsViolation() {
        val block = LoginLink(id = "login", label = "This login link label is way too long to fit anywhere on a mobile screen")
        val violations = blockAccessibilityLint(block)
        assertTrue(violations.any { "login link" in it.lowercase() },
            "VWE-16/WCAG 2.5.3: LoginLink with label > 40 chars must produce a violation")
    }

    @Test
    fun headlineEmptyTextFlagsViolation() {
        val block = Headline(id = "h", text = "")
        val violations = blockAccessibilityLint(block)
        assertTrue(violations.any { "Headline" in it },
            "VWE-16/WCAG 1.3.1: empty Headline must produce a violation")
    }

    @Test
    fun headlineWithTextPassesCheck() {
        val block = Headline(id = "h", text = "Subscribe for full access")
        assertTrue(blockAccessibilityLint(block).isEmpty(), "VWE-16: non-empty Headline must pass")
    }

    @Test
    fun bodycopymptyTextFlagsViolation() {
        val block = BodyCopy(id = "b", text = "")
        val violations = blockAccessibilityLint(block)
        assertTrue(violations.any { "Body copy" in it },
            "VWE-16/WCAG 1.3.1: empty BodyCopy must produce a violation")
    }

    @Test
    fun noViolationsOnBlocksWithNoChecks() {
        val blocks = listOf(PriceDisplay(id = "p"), SocialProof(id = "sp", text = ""))
        // PriceDisplay has no lint checks; SocialProof with empty text does
        assertTrue(blockAccessibilityLint(blocks[0]).isEmpty(),
            "VWE-16: PriceDisplay has no lint checks")
        assertTrue(blockAccessibilityLint(blocks[1]).isNotEmpty(),
            "VWE-16: empty SocialProof text must produce a violation")
    }

    @Test
    fun legalTextEmptyFlagsViolation() {
        val block = LegalText(id = "l", text = "")
        val violations = blockAccessibilityLint(block)
        assertTrue(violations.any { "Legal text" in it },
            "VWE-16/WCAG 1.3.1: empty LegalText must produce a violation")
    }
}
