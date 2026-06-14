package nl.incedo.paywall.storybook

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResponsiveDecisionTest {

    private fun registered(
        pid: String = "rp1",
        key: String = "mobile/phone-first",
        sid: String = "s1",
        lifecycle: ResponsiveLifecycle = ResponsiveLifecycle.ACTIVE,
    ) = ResponsiveProfileRegistered(
        profileId = ResponsiveProfileId(pid),
        profileKey = ResponsiveProfileKey(key),
        storyId = StoryId(sid),
        lifecycle = lifecycle,
        registeredAtEpochMs = 1_000L,
        tags = setOf(responsiveTag(ResponsiveProfileId(pid)), storyTag(StoryId(sid)), "responsive-profiles"),
    )

    @Test fun `profile does not exist before any events`() {
        assertFalse(ResponsiveDecisionModel().exists)
    }

    @Test fun `profile exists after ResponsiveProfileRegistered`() {
        val d = ResponsiveDecisionModel().also { it.apply(registered()) }
        assertTrue(d.exists)
        assertFalse(d.archived)
        assertEquals("mobile/phone-first", d.profileKey)
        assertEquals("s1", d.storyId)
        assertEquals(ResponsiveLifecycle.ACTIVE, d.lifecycle)
    }

    @Test fun `ResponsiveFormFactorSupported adds form factor`() {
        val d = ResponsiveDecisionModel().also {
            it.apply(registered())
            it.apply(ResponsiveFormFactorSupported(ResponsiveProfileId("rp1"), FormFactor.PHONE, 2_000L))
            it.apply(ResponsiveFormFactorSupported(ResponsiveProfileId("rp1"), FormFactor.TABLET, 3_000L))
        }
        assertEquals(setOf(FormFactor.PHONE, FormFactor.TABLET), d.formFactors)
    }

    @Test fun `ResponsiveWidthClassesDefined sets width classes (replaces previous)`() {
        val d = ResponsiveDecisionModel().also {
            it.apply(registered())
            it.apply(ResponsiveWidthClassesDefined(ResponsiveProfileId("rp1"), setOf(WidthClass.COMPACT), 2_000L))
            it.apply(ResponsiveWidthClassesDefined(ResponsiveProfileId("rp1"), setOf(WidthClass.MEDIUM, WidthClass.EXPANDED), 3_000L))
        }
        assertEquals(setOf(WidthClass.MEDIUM, WidthClass.EXPANDED), d.widthClasses)
    }

    @Test fun `ResponsiveNavigationPatternSet maps context to pattern`() {
        val d = ResponsiveDecisionModel().also {
            it.apply(registered())
            it.apply(ResponsiveNavigationPatternSet(ResponsiveProfileId("rp1"), "PHONE", NavigationPattern.BOTTOM_BAR, 2_000L))
            it.apply(ResponsiveNavigationPatternSet(ResponsiveProfileId("rp1"), "TABLET", NavigationPattern.NAV_RAIL, 3_000L))
        }
        assertEquals(NavigationPattern.BOTTOM_BAR, d.navigationPatterns["PHONE"])
        assertEquals(NavigationPattern.NAV_RAIL, d.navigationPatterns["TABLET"])
    }

    @Test fun `ResponsiveDensityProfileSet maps context to density`() {
        val d = ResponsiveDecisionModel().also {
            it.apply(registered())
            it.apply(ResponsiveDensityProfileSet(ResponsiveProfileId("rp1"), "PHONE", DensityProfile.COMPACT, 2_000L))
            it.apply(ResponsiveDensityProfileSet(ResponsiveProfileId("rp1"), "DESKTOP", DensityProfile.COMFORTABLE, 3_000L))
        }
        assertEquals(DensityProfile.COMPACT, d.densityProfiles["PHONE"])
        assertEquals(DensityProfile.COMFORTABLE, d.densityProfiles["DESKTOP"])
    }

    @Test fun `ResponsiveExpectationLinked appends expectation refs`() {
        val d = ResponsiveDecisionModel().also {
            it.apply(registered())
            it.apply(ResponsiveExpectationLinked(ResponsiveProfileId("rp1"), ScenarioId("sc1"), "specs.phone.portrait", 2_000L))
            it.apply(ResponsiveExpectationLinked(ResponsiveProfileId("rp1"), null, "specs.tablet.landscape", 3_000L))
        }
        assertEquals(listOf("specs.phone.portrait", "specs.tablet.landscape"), d.expectationRefs)
    }

    @Test fun `ResponsiveLayoutRuleAdded appends rule refs`() {
        val d = ResponsiveDecisionModel().also {
            it.apply(registered())
            it.apply(ResponsiveLayoutRuleAdded(ResponsiveProfileId("rp1"), "rules.adaptive-grid", 2_000L))
            it.apply(ResponsiveLayoutRuleAdded(ResponsiveProfileId("rp1"), "rules.safe-area", 3_000L))
        }
        assertEquals(listOf("rules.adaptive-grid", "rules.safe-area"), d.layoutRules)
    }

    @Test fun `ResponsiveProfileArchived marks profile as archived (BR-9)`() {
        val d = ResponsiveDecisionModel().also {
            it.apply(registered())
            it.apply(ResponsiveProfileArchived(ResponsiveProfileId("rp1"), 2_000L))
        }
        assertTrue(d.archived)
        assertEquals(ResponsiveLifecycle.ARCHIVED, d.lifecycle)
    }

    @Test fun `applyAll processes events in sequence`() {
        val events = listOf(
            registered(),
            ResponsiveFormFactorSupported(ResponsiveProfileId("rp1"), FormFactor.PHONE, 2_000L),
            ResponsiveWidthClassesDefined(ResponsiveProfileId("rp1"), setOf(WidthClass.COMPACT), 3_000L),
            ResponsiveLayoutRuleAdded(ResponsiveProfileId("rp1"), "rules.baseline", 4_000L),
        )
        val d = ResponsiveDecisionModel().also { it.applyAll(events) }
        assertTrue(d.exists)
        assertTrue(FormFactor.PHONE in d.formFactors)
        assertTrue(WidthClass.COMPACT in d.widthClasses)
        assertEquals(listOf("rules.baseline"), d.layoutRules)
    }

    @Test fun `all FormFactor variants are representable`() {
        FormFactor.entries.forEach { ff ->
            val d = ResponsiveDecisionModel().also {
                it.apply(registered())
                it.apply(ResponsiveFormFactorSupported(ResponsiveProfileId("rp1"), ff, 2_000L))
            }
            assertTrue(ff in d.formFactors)
        }
    }

    @Test fun `all WidthClass variants are representable`() {
        val d = ResponsiveDecisionModel().also {
            it.apply(registered())
            it.apply(ResponsiveWidthClassesDefined(ResponsiveProfileId("rp1"), WidthClass.entries.toSet(), 2_000L))
        }
        assertEquals(WidthClass.entries.toSet(), d.widthClasses)
    }

    @Test fun `all NavigationPattern variants are representable`() {
        NavigationPattern.entries.forEachIndexed { i, p ->
            val d = ResponsiveDecisionModel().also {
                it.apply(registered())
                it.apply(ResponsiveNavigationPatternSet(ResponsiveProfileId("rp1"), "ctx$i", p, 2_000L))
            }
            assertEquals(p, d.navigationPatterns["ctx$i"])
        }
    }
}
