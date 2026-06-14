package nl.incedo.paywall.walls

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * VWE-02/13: structural validity rules for WallLayout.
 * Enforced server-side on save (ADM-01); also used client-side for UX feedback.
 */
class WallLayoutValidatorTest {

    private fun headline() = Headline(id = "h", text = "Title")
    private fun body() = BodyCopy(id = "b", text = "Body")
    private fun primaryCta() = CtaButton(id = "cta-p", label = "Subscribe", role = CtaRole.PRIMARY)
    private fun secondaryCta() = CtaButton(id = "cta-s", label = "Log in", role = CtaRole.SECONDARY)
    private fun legalText() = LegalText(id = "l", text = "Legal notice.")
    private fun consent() = ConsentBlock(id = "c")

    private fun valid() = listOf(headline(), primaryCta())

    @Test
    fun validMinimalLayoutPassesValidation() {
        assertTrue(WallLayoutValidator.validate(valid()).isEmpty())
    }

    @Test
    fun missingValuePropBlockFails() {
        val blocks = listOf(primaryCta())
        val errors = WallLayoutValidator.validate(blocks)
        assertTrue(errors.any { "value-proposition" in it }, "Expected value-prop error, got: $errors")
    }

    @Test
    fun missingPrimaryCtaFails() {
        val blocks = listOf(headline(), secondaryCta())
        val errors = WallLayoutValidator.validate(blocks)
        assertTrue(errors.any { "primary CTA" in it || "PRIMARY" in it }, "Expected primary-CTA error, got: $errors")
    }

    @Test
    fun duplicateConsentBlockFails() {
        val blocks = listOf(consent(), headline(), primaryCta(), consent())
        val errors = WallLayoutValidator.validate(blocks)
        assertTrue(errors.any { "singleton" in it || "ConsentBlock" in it }, "Expected singleton error, got: $errors")
    }

    @Test
    fun duplicatePriceDisplayFails() {
        val blocks = listOf(headline(), PriceDisplay(id = "p1"), PriceDisplay(id = "p2"), primaryCta())
        val errors = WallLayoutValidator.validate(blocks)
        assertTrue(errors.any { "singleton" in it || "PriceDisplay" in it }, "Expected singleton error, got: $errors")
    }

    @Test
    fun consentBlockNotFirstFails() {
        val blocks = listOf(headline(), consent(), primaryCta())
        val errors = WallLayoutValidator.validate(blocks)
        assertTrue(errors.any { "first" in it || "ConsentBlock" in it }, "Expected first-block error, got: $errors")
    }

    @Test
    fun legalTextBeforeCtaFails() {
        val blocks = listOf(headline(), legalText(), primaryCta())
        val errors = WallLayoutValidator.validate(blocks)
        assertTrue(errors.any { "LegalText" in it || "follow" in it }, "Expected legal-after-CTA error, got: $errors")
    }

    @Test
    fun legalTextAfterCtaIsValid() {
        val blocks = listOf(headline(), primaryCta(), legalText())
        assertTrue(WallLayoutValidator.validate(blocks).isEmpty())
    }

    @Test
    fun consentFirstWithOtherBlocksIsValid() {
        val blocks = listOf(consent(), headline(), primaryCta())
        assertTrue(WallLayoutValidator.validate(blocks).isEmpty())
    }

    @Test
    fun removalBlockedWhenItRemovesLastValueProp() {
        val blocks = listOf(headline(), primaryCta())
        val reason = WallLayoutValidator.removalBlockedReason(blocks, "h")
        assertNotNull(reason, "Should block removal of last value-prop block")
    }

    @Test
    fun removalAllowedWhenAnotherValuePropExists() {
        val blocks = listOf(headline(), body(), primaryCta())
        val reason = WallLayoutValidator.removalBlockedReason(blocks, "h")
        assertNull(reason, "Should allow removal when another value-prop block exists")
    }

    @Test
    fun addConsentBlockBlockedWhenOneAlreadyExists() {
        val blocks = listOf(consent(), headline(), primaryCta())
        val reason = WallLayoutValidator.additionBlockedReason(blocks, "ConsentBlock")
        assertNotNull(reason, "Should block adding a second ConsentBlock")
    }

    @Test
    fun addConsentBlockAllowedWhenNoneExists() {
        val reason = WallLayoutValidator.additionBlockedReason(valid(), "ConsentBlock")
        assertNull(reason, "Should allow adding ConsentBlock when none exists")
    }

    @Test
    fun addPriceDisplayBlockedWhenOneAlreadyExists() {
        val blocks = listOf(headline(), PriceDisplay(id = "p"), primaryCta())
        val reason = WallLayoutValidator.additionBlockedReason(blocks, "PriceDisplay")
        assertNotNull(reason, "Should block adding a second PriceDisplay")
    }
}
