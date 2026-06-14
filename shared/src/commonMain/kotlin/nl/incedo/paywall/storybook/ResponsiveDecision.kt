package nl.incedo.paywall.storybook

import nl.incedo.paywall.core.DomainEvent

class ResponsiveDecisionModel {
    var exists: Boolean = false
    var archived: Boolean = false
    var storyId: String = ""
    var profileKey: String = ""
    var lifecycle: ResponsiveLifecycle = ResponsiveLifecycle.ACTIVE
    val formFactors: MutableSet<FormFactor> = mutableSetOf()
    val widthClasses: MutableSet<WidthClass> = mutableSetOf()
    val navigationPatterns: MutableMap<String, NavigationPattern> = mutableMapOf()
    val densityProfiles: MutableMap<String, DensityProfile> = mutableMapOf()
    val expectationRefs: MutableList<String> = mutableListOf()
    val layoutRules: MutableList<String> = mutableListOf()

    fun apply(event: DomainEvent) {
        when (event) {
            is ResponsiveProfileRegistered -> {
                exists = true
                storyId = event.storyId.value
                profileKey = event.profileKey.value
                lifecycle = event.lifecycle
            }
            is ResponsiveFormFactorSupported -> formFactors.add(event.formFactor)
            is ResponsiveWidthClassesDefined -> {
                widthClasses.clear()
                widthClasses.addAll(event.widthClasses)
            }
            is ResponsiveNavigationPatternSet -> navigationPatterns[event.context] = event.navigationPattern
            is ResponsiveDensityProfileSet -> densityProfiles[event.context] = event.densityProfile
            is ResponsiveExpectationLinked -> expectationRefs.add(event.expectationRef)
            is ResponsiveLayoutRuleAdded -> layoutRules.add(event.ruleRef)
            is ResponsiveProfileArchived -> {
                archived = true
                lifecycle = ResponsiveLifecycle.ARCHIVED
            }
        }
    }

    fun applyAll(events: List<DomainEvent>) = events.forEach(::apply)
}
