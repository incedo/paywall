package nl.incedo.paywall.brands

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.BrandId
import nl.incedo.paywall.core.DomainEvent

/**
 * ADM-10: a brand is a first-class entity with theme tokens, domain, and locale.
 * Wall designs are created per brand; every gated rendering resolves brand → design.
 */
@Serializable
data class BrandCreated(
    val brandId: BrandId,
    val name: String,
    /** Public domain the brand serves, e.g. "magazine.nl". */
    val domain: String,
    /** BCP-47 locale tag, e.g. "nl-NL". */
    val locale: String = "nl-NL",
    /**
     * Theme tokens as a JSON object (colors, fonts, logo URL etc.).
     * Stored opaque — the wall renderer interprets them; the domain doesn't.
     */
    val themeJson: String = "{}",
    val createdAtEpochMs: Long,
    override val tags: Set<String> = setOf(brandTag(brandId), "brands"),
) : DomainEvent

/** ADM-10: update theme tokens (e.g. seasonal rebrand or new logo). */
@Serializable
data class BrandThemeUpdated(
    val brandId: BrandId,
    val themeJson: String,
    val actor: String,
    val updatedAtEpochMs: Long,
    override val tags: Set<String> = setOf(brandTag(brandId), "brands"),
) : DomainEvent

fun brandTag(brandId: BrandId): String = "brand:${brandId.value}"
