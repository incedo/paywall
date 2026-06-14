package nl.incedo.paywall.walls

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.WallId

/**
 * ADM-15: per-locale copy override for a wall design.
 * All fields are optional — only the fields that differ from the
 * wall's default copy need to be specified. Missing fields fall back
 * to the defaults in [WallConfig].
 */
@Serializable
data class WallCopy(
    val title: String? = null,
    val body: String? = null,
    val primaryCta: String? = null,
    val secondaryCta: String? = null,
    /** ADM-11: optional legal/disclaimer text rendered below CTAs; null = use wall default. */
    val legalText: String? = null,
    /** ADM-17: alt text for the image block; null = use wall default. Empty string = decorative. */
    val imageAlt: String? = null,
)

/**
 * PW-03/ADM-11: gate composition — value proposition (title/body), offer price
 * (supplied by CEP at render time), primary CTA (default "Subscribe") and secondary
 * CTA (default "Log in" for existing subscribers). The visual editor's output is
 * structured content, never code (GAP-10). Events carry the wall tag for per-wall
 * queries plus the catalog tag for the overview projection.
 */
@Serializable
data class WallConfig(
    val name: String,
    /** hard | metered | freemium | dynamic (Doc 1). */
    val wallType: String,
    val title: String,
    val body: String,
    val primaryCta: String,
    val secondaryCta: String,
    val channels: Set<String> = setOf("web"),
    /** ADM-10: brand this wall design belongs to; null = unbranded (default brand). */
    val brandId: String? = null,
    /**
     * AC-14: when true the client MUST show a distinct GDPR cookie-consent step
     * before the subscription gate. The outcome feeds AN-20 consent state.
     * Never bundled with payment consent (PAY-*) or data-exchange consent (DG-03).
     */
    val requireConsentStep: Boolean = false,
    /**
     * ADM-11: optional URL of an image shown in the gate (e.g. brand illustration).
     * Empty string = no image block rendered.
     */
    val imageUrl: String = "",
    /**
     * ADM-17 / WCAG 2.1 AA: alt text for the image block. Empty string = decorative
     * (aria-hidden); non-empty = descriptive label for screen readers.
     * The accessibility linter flags a non-empty imageUrl with an empty imageAlt.
     */
    val imageAlt: String = "",
    /**
     * ADM-11: optional legal/disclaimer text rendered below CTAs (e.g. "Cancel anytime.
     * Prices include VAT."). Empty string = no legal text block rendered.
     */
    val legalText: String = "",
    /**
     * ADM-15: per-locale copy overrides keyed by BCP-47 locale tag (e.g. "nl-NL").
     * The default [title]/[body]/[primaryCta]/[secondaryCta]/[legalText] serve as the
     * nl-NL fallback; experiment starts with one locale, structure is ready for more.
     * Use [resolveForLocale] to obtain the effective copy for a given locale.
     */
    val translations: Map<String, WallCopy> = emptyMap(),
) {
    /**
     * ADM-15: returns the effective copy for [locale], falling back to the
     * language-only tag (e.g. "nl" from "nl-NL"), then to the defaults.
     */
    fun resolveForLocale(locale: String): WallCopy {
        val override = translations[locale]
            ?: translations[locale.substringBefore("-")]
            ?: return WallCopy(title, body, primaryCta, secondaryCta, legalText, imageAlt)
        return WallCopy(
            title = override.title ?: title,
            body = override.body ?: body,
            primaryCta = override.primaryCta ?: primaryCta,
            secondaryCta = override.secondaryCta ?: secondaryCta,
            legalText = override.legalText ?: legalText,
            imageAlt = override.imageAlt ?: imageAlt,
        )
    }
}

@Serializable
data class WallCreated(
    val wallId: WallId,
    val config: WallConfig,
    val actor: String,
    override val tags: Set<String> = setOf("wall:${wallId.value}", "walls"),
) : DomainEvent

@Serializable
data class WallConfigChanged(
    val wallId: WallId,
    val config: WallConfig,
    val actor: String,
    override val tags: Set<String> = setOf("wall:${wallId.value}", "walls"),
) : DomainEvent

/**
 * VWE-03: block-level composition edit — carries the new ordered layout for the wall.
 * Versioning / rollback / audit apply unchanged (ADM-13).
 */
@Serializable
data class WallLayoutChanged(
    val wallId: WallId,
    val layout: WallLayout,
    val actor: String,
    override val tags: Set<String> = setOf("wall:${wallId.value}", "walls"),
) : DomainEvent

/** Draft → published (ADM-06: every change is a new version; rollback = re-apply an old config). */
@Serializable
data class WallPublished(
    val wallId: WallId,
    val actor: String,
    override val tags: Set<String> = setOf("wall:${wallId.value}", "walls"),
) : DomainEvent

/**
 * ADM-16: a wall design saved as a reusable template.
 * Templates carry layout/copy without a brandId; instantiating one for a brand
 * produces a new WallConfig with the template's fields + the target brandId.
 * Brand theme tokens (fonts, colors) live in BrandCreated.themeJson and are
 * applied at render time — they are deliberately NOT in the template.
 */
@Serializable
data class WallTemplateCreated(
    val templateId: String,
    /** Human-readable template name shown in the console picker. */
    val name: String,
    /** The layout/copy config, always with brandId = null (templates are brand-neutral). */
    val config: WallConfig,
    /**
     * VWE-17: optional block layout saved alongside the flat config. When non-null the
     * template was authored in the block editor; instantiating it also saves a
     * [WallLayoutChanged] event on the new wall. Null means flat-config-only template.
     * Default null preserves backward compatibility with existing serialised events.
     */
    val layout: WallLayout? = null,
    val actor: String,
    override val tags: Set<String> = setOf("wall-template:$templateId", "wall-templates"),
) : DomainEvent
