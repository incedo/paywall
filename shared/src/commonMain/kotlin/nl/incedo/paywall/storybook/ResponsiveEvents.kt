package nl.incedo.paywall.storybook

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent

// ── Value objects ─────────────────────────────────────────────────────────────

@Serializable @JvmInline value class ResponsiveProfileId(val value: String)
@Serializable @JvmInline value class ResponsiveProfileKey(val value: String)

enum class FormFactor { PHONE, TABLET, DESKTOP, WEB }
enum class WidthClass { COMPACT, MEDIUM, EXPANDED }
enum class NavigationPattern { BOTTOM_BAR, TOP_BAR, NAV_RAIL, SIDE_NAV, DRAWER, RESPONSIVE }
enum class DensityProfile { COMPACT, COMFORTABLE, SPACIOUS }
enum class ResponsiveLifecycle { ACTIVE, EXPERIMENTAL, DEPRECATED, ARCHIVED }

// ── Tag helpers ───────────────────────────────────────────────────────────────

fun responsiveTag(id: ResponsiveProfileId) = "responsive:${id.value}"

// ── Events ────────────────────────────────────────────────────────────────────

/**
 * Establishes a responsive profile for a story. BR-2: story must exist.
 */
@Serializable
data class ResponsiveProfileRegistered(
    val profileId: ResponsiveProfileId,
    val profileKey: ResponsiveProfileKey,
    val storyId: StoryId,
    val lifecycle: ResponsiveLifecycle = ResponsiveLifecycle.ACTIVE,
    val registeredAtEpochMs: Long,
    override val tags: Set<String> = setOf(responsiveTag(profileId), storyTag(storyId), "responsive-profiles"),
) : DomainEvent

/** Declares support for a form factor. BR-3: must be a valid FormFactor enum value. */
@Serializable
data class ResponsiveFormFactorSupported(
    val profileId: ResponsiveProfileId,
    val formFactor: FormFactor,
    val addedAtEpochMs: Long,
    override val tags: Set<String> = setOf(responsiveTag(profileId), "form-factor:${formFactor.name}", "responsive-profiles"),
) : DomainEvent

/** Sets the supported width classes for this profile. BR-4: only COMPACT/MEDIUM/EXPANDED. */
@Serializable
data class ResponsiveWidthClassesDefined(
    val profileId: ResponsiveProfileId,
    val widthClasses: Set<WidthClass>,
    val definedAtEpochMs: Long,
    override val tags: Set<String> = setOf(responsiveTag(profileId), "responsive-profiles"),
) : DomainEvent

/** Maps a navigation pattern to a context (form factor name). BR-5: context must be supported. */
@Serializable
data class ResponsiveNavigationPatternSet(
    val profileId: ResponsiveProfileId,
    val context: String,
    val navigationPattern: NavigationPattern,
    val setAtEpochMs: Long,
    override val tags: Set<String> = setOf(responsiveTag(profileId), "responsive-profiles"),
) : DomainEvent

/** Maps a density profile to a context. BR-6: must be token-aligned. */
@Serializable
data class ResponsiveDensityProfileSet(
    val profileId: ResponsiveProfileId,
    val context: String,
    val densityProfile: DensityProfile,
    val setAtEpochMs: Long,
    override val tags: Set<String> = setOf(responsiveTag(profileId), "responsive-profiles"),
) : DomainEvent

/** Links a responsive expectation to a scenario. BR-7: scenarioId must exist. */
@Serializable
data class ResponsiveExpectationLinked(
    val profileId: ResponsiveProfileId,
    val scenarioId: ScenarioId?,
    val expectationRef: String,
    val linkedAtEpochMs: Long,
    override val tags: Set<String> = buildSet {
        add(responsiveTag(profileId))
        add("responsive-profiles")
        scenarioId?.let { add(scenarioTag(it)) }
    },
) : DomainEvent

/** Adds a centrally-defined layout rule. BR-8: rule ref required. */
@Serializable
data class ResponsiveLayoutRuleAdded(
    val profileId: ResponsiveProfileId,
    val ruleRef: String,
    val addedAtEpochMs: Long,
    override val tags: Set<String> = setOf(responsiveTag(profileId), "responsive-profiles"),
) : DomainEvent

/** Archives the responsive profile; becomes read-only. BR-9. */
@Serializable
data class ResponsiveProfileArchived(
    val profileId: ResponsiveProfileId,
    val archivedAtEpochMs: Long,
    override val tags: Set<String> = setOf(responsiveTag(profileId), "responsive-profiles", "lifecycle:archived"),
) : DomainEvent
