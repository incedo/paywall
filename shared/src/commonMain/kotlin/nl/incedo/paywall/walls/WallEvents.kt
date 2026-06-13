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
)

/**
 * Wall definitions (ADM-11): the visual editor's output is structured
 * content, never code (GAP-10). Events carry the wall tag for per-wall
 * queries plus the catalog tag for the overview projection (admin-rate
 * writes — no contention concern).
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
     * ADM-15: per-locale copy overrides keyed by BCP-47 locale tag (e.g. "nl-NL").
     * The default [title]/[body]/[primaryCta]/[secondaryCta] serve as the
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
            ?: return WallCopy(title, body, primaryCta, secondaryCta)
        return WallCopy(
            title = override.title ?: title,
            body = override.body ?: body,
            primaryCta = override.primaryCta ?: primaryCta,
            secondaryCta = override.secondaryCta ?: secondaryCta,
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

/** Draft → published (ADM-06: every change is a new version; rollback = re-apply an old config). */
@Serializable
data class WallPublished(
    val wallId: WallId,
    val actor: String,
    override val tags: Set<String> = setOf("wall:${wallId.value}", "walls"),
) : DomainEvent
