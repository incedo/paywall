package nl.incedo.paywall.walls

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * VWE-01: constrained block library — the sealed hierarchy defines every block type
 * that can appear in a wall. No free-form block type is permitted without a spec change.
 *
 * Each block carries a stable [id] so the editor can track selection and reorder state
 * without reordering entire objects. IDs for blocks projected from a flat [WallConfig]
 * are deterministic (see [WallConfig.toDefaultLayout]).
 *
 * VWE-04: text-bearing blocks carry a [textOverrides] map (locale code → translated text)
 * so the renderer can pick the brand locale, falling back to the default text. The field
 * defaults to an empty map so existing serialised layouts (without the field) round-trip
 * without modification.
 */
@Serializable
sealed interface WallBlock {
    val id: String
}

/** A primary or secondary heading — the value proposition headline. */
@Serializable
@SerialName("Headline")
data class Headline(
    override val id: String,
    val text: String,
    /** VWE-04: locale code → translated text; renderer picks by locale, falls back to [text]. */
    val textOverrides: Map<String, String> = emptyMap(),
) : WallBlock

/** Body copy beneath the headline — further elaborates the value proposition. */
@Serializable
@SerialName("BodyCopy")
data class BodyCopy(
    override val id: String,
    val text: String,
    /** VWE-04: locale code → translated text. */
    val textOverrides: Map<String, String> = emptyMap(),
) : WallBlock

/**
 * Renders the plan price from the CEP at render time.
 * [planRef] is optional; null means use the active plan for the experiment variant.
 */
@Serializable
@SerialName("PriceDisplay")
data class PriceDisplay(override val id: String, val planRef: String? = null) : WallBlock

/** A call-to-action button. [role] controls visual treatment and semantic ranking. */
@Serializable
@SerialName("CtaButton")
data class CtaButton(
    override val id: String,
    val label: String,
    val role: CtaRole,
    /** VWE-04: locale code → translated label. */
    val textOverrides: Map<String, String> = emptyMap(),
) : WallBlock

/** A "Log in" or "I already subscribe" link — lower emphasis than a CtaButton. */
@Serializable
@SerialName("LoginLink")
data class LoginLink(
    override val id: String,
    val label: String = "Log in",
    /** VWE-04: locale code → translated label. */
    val textOverrides: Map<String, String> = emptyMap(),
) : WallBlock

/** An illustration or brand image. [alt] is the WCAG 2.1 alt text (ADM-17). */
@Serializable
@SerialName("ImageBlock")
data class ImageBlock(override val id: String, val url: String, val alt: String = "") : WallBlock

/** Disclaimer / legal text. VWE-02: MUST follow the CTAs. */
@Serializable
@SerialName("LegalText")
data class LegalText(
    override val id: String,
    val text: String,
    /** VWE-04: locale code → translated text. */
    val textOverrides: Map<String, String> = emptyMap(),
) : WallBlock

/**
 * AC-14: explicit GDPR cookie-consent step before the subscription gate.
 * Singleton per layout; VWE-02 requires it to be the first block when present.
 */
@Serializable
@SerialName("ConsentBlock")
data class ConsentBlock(override val id: String) : WallBlock

/** PW-22/23: article-count meter bar shown to metered visitors. */
@Serializable
@SerialName("MeterIndicator")
data class MeterIndicator(override val id: String) : WallBlock

/** Social proof text (subscriber counts, editorial endorsements, etc.). */
@Serializable
@SerialName("SocialProof")
data class SocialProof(
    override val id: String,
    val text: String,
    /** VWE-04: locale code → translated text. */
    val textOverrides: Map<String, String> = emptyMap(),
) : WallBlock

enum class CtaRole { PRIMARY, SECONDARY }

/**
 * VWE-01: the ordered list of blocks that a wall design is composed from.
 * This is the visual editor's output and the renderer's input.
 */
@Serializable
data class WallLayout(val blocks: List<WallBlock>)

/**
 * VWE-04: resolve the display text for a text-bearing block.
 * Returns the [locale] override if one exists, otherwise the block's default text.
 * Returns an empty string for non-text blocks (PriceDisplay, ConsentBlock, MeterIndicator, ImageBlock).
 */
fun WallBlock.resolvedText(locale: String?): String = when {
    locale == null -> defaultText()
    else -> textOverridesMap()[locale] ?: defaultText()
}

private fun WallBlock.textOverridesMap(): Map<String, String> = when (this) {
    is Headline -> textOverrides
    is BodyCopy -> textOverrides
    is CtaButton -> textOverrides
    is LoginLink -> textOverrides
    is LegalText -> textOverrides
    is SocialProof -> textOverrides
    else -> emptyMap()
}

private fun WallBlock.defaultText(): String = when (this) {
    is Headline -> text
    is BodyCopy -> text
    is CtaButton -> label
    is LoginLink -> label
    is LegalText -> text
    is SocialProof -> text
    else -> ""
}

/**
 * VWE-05: deterministic back-compat projection from a flat [WallConfig] to a
 * default [WallLayout]. Old walls render identically because the same fields
 * are rendered in the same order — the only change is the representation.
 *
 * Ordering follows VWE-02 structural rules:
 *  1. ConsentBlock (if requireConsentStep=true) — always first (AC-14)
 *  2. Headline
 *  3. BodyCopy
 *  4. ImageBlock (if imageUrl is non-empty)
 *  5. PriceDisplay
 *  6. CtaButton(PRIMARY)
 *  7. CtaButton(SECONDARY) — uses secondaryCta text
 *  8. LegalText (if legalText is non-empty) — after CTAs (VWE-02)
 */
fun WallConfig.toDefaultLayout(): WallLayout {
    val blocks = mutableListOf<WallBlock>()
    if (requireConsentStep) blocks += ConsentBlock(id = "consent")
    blocks += Headline(id = "headline", text = title)
    blocks += BodyCopy(id = "body", text = body)
    if (imageUrl.isNotEmpty()) blocks += ImageBlock(id = "image", url = imageUrl, alt = imageAlt)
    blocks += PriceDisplay(id = "price")
    blocks += CtaButton(id = "cta-primary", label = primaryCta, role = CtaRole.PRIMARY)
    blocks += CtaButton(id = "cta-secondary", label = secondaryCta, role = CtaRole.SECONDARY)
    if (legalText.isNotEmpty()) blocks += LegalText(id = "legal", text = legalText)
    return WallLayout(blocks)
}
