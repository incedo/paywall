package nl.incedo.paywall.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.VisitorId

class VariantStatsProjectionTest {

    private val now = 1_750_000_000_000L

    private fun event(
        type: WallEventType,
        visitor: String,
        variant: String = "metered",
    ) = WallEventRecorded(
        eventType = type,
        subjectId = SubjectId.of(VisitorId(visitor)),
        variant = variant,
        channel = "web",
        occurredAtEpochMs = now,
        articleId = ArticleId("a-1"),
    )

    @Test
    fun countsPerVariant() {
        val projection = VariantStatsProjection()
        projection.applyAll(
            listOf(
                event(WallEventType.ARTICLE_READ, "v-1"),
                event(WallEventType.WALL_SHOWN, "v-1"),
                event(WallEventType.WALL_SHOWN, "v-2"),
                event(WallEventType.GATE_CTA_CLICK, "v-2"),
                event(WallEventType.WALL_SHOWN, "v-9", variant = "hard"),
            ),
        )
        val metered = projection.stats().getValue("metered")
        assertEquals(2, metered.visitors)
        assertEquals(1, metered.articleReads)
        assertEquals(2, metered.wallsShown)
        assertEquals(1, metered.gateCtaClicks)
        assertEquals(1, projection.stats().getValue("hard").wallsShown)
    }

    @Test
    fun conversionRateUsesWallShownUniques() {
        // AN-10: conversions / unique subjects that saw a wall
        val projection = VariantStatsProjection()
        projection.applyAll(
            listOf(
                event(WallEventType.WALL_SHOWN, "v-1"),
                event(WallEventType.WALL_SHOWN, "v-1"), // same subject again
                event(WallEventType.WALL_SHOWN, "v-2"),
                event(WallEventType.CHECKOUT_COMPLETE, "v-1"),
            ),
        )
        val stats = projection.stats().getValue("metered")
        assertEquals(2, stats.wallShownUniques)
        assertEquals(0.5, stats.conversionRate)
        assertEquals(3, stats.wallsShown)
    }

    @Test
    fun gateCtrIsClicksOverWallsShown() {
        val projection = VariantStatsProjection()
        projection.applyAll(
            listOf(
                event(WallEventType.WALL_SHOWN, "v-1"),
                event(WallEventType.WALL_SHOWN, "v-2"),
                event(WallEventType.WALL_SHOWN, "v-3"),
                event(WallEventType.WALL_SHOWN, "v-4"),
                event(WallEventType.GATE_CTA_CLICK, "v-2"),
            ),
        )
        assertEquals(0.25, projection.stats().getValue("metered").gateCtr)
    }

    @Test
    fun emptyProjectionYieldsNoVariants() {
        assertEquals(emptyMap(), VariantStatsProjection().stats())
    }

    @Test
    fun pageViewsTrackedPerVariant() {
        // AN-11: client-reported PAGE_VIEW events are counted for reach-cost comparison
        val projection = VariantStatsProjection()
        projection.applyAll(
            listOf(
                event(WallEventType.PAGE_VIEW, "v-1"),
                event(WallEventType.PAGE_VIEW, "v-1"),
                event(WallEventType.PAGE_VIEW, "v-2"),
                event(WallEventType.ARTICLE_READ, "v-1"),
            ),
        )
        val stats = projection.stats().getValue("metered")
        assertEquals(3, stats.pageViews)
        assertEquals(1, stats.articleReads)
    }

    @Test
    fun wilsonCiIsWithinUnitInterval() {
        // AN-12: CI bounds are valid probabilities and bracket the point estimate
        val projection = VariantStatsProjection()
        repeat(100) { i ->
            projection.apply(event(WallEventType.WALL_SHOWN, "v-$i"))
        }
        repeat(20) { i ->
            projection.apply(event(WallEventType.CHECKOUT_COMPLETE, "v-$i"))
        }
        val stats = projection.stats().getValue("metered")
        assertEquals(0.2, stats.conversionRate)
        assertTrue(stats.conversionRateLow >= 0.0 && stats.conversionRateLow < stats.conversionRate,
            "Wilson lower must be in [0, point estimate)")
        assertTrue(stats.conversionRateHigh <= 1.0 && stats.conversionRateHigh > stats.conversionRate,
            "Wilson upper must be in (point estimate, 1]")
        assertFalse(stats.sampleSizeTooSmall, "n=100 is above the small-sample threshold")
    }

    @Test
    fun sampleSizeTooSmallFlaggedBelow100() {
        // AN-12: flag when n < 100 so the dashboard warns against comparing variants
        val projection = VariantStatsProjection()
        repeat(99) { i ->
            projection.apply(event(WallEventType.WALL_SHOWN, "v-$i"))
        }
        assertTrue(projection.stats().getValue("metered").sampleSizeTooSmall)
    }

    @Test
    fun wilsonCiHandlesZeroAndFullConversions() {
        // AN-12: edge cases must not throw and must stay in [0, 1]
        val p = VariantStatsProjection()
        repeat(10) { i -> p.apply(event(WallEventType.WALL_SHOWN, "v-$i")) }
        val noConversions = p.stats().getValue("metered")
        assertEquals(0.0, noConversions.conversionRate)
        assertTrue(noConversions.conversionRateLow >= 0.0)
        assertTrue(noConversions.conversionRateHigh <= 1.0)

        val p2 = VariantStatsProjection()
        repeat(10) { i ->
            p2.apply(event(WallEventType.WALL_SHOWN, "v-$i"))
            p2.apply(event(WallEventType.CHECKOUT_COMPLETE, "v-$i"))
        }
        val allConvert = p2.stats().getValue("metered")
        assertEquals(1.0, allConvert.conversionRate)
        assertTrue(allConvert.conversionRateLow >= 0.0 && allConvert.conversionRateLow <= 1.0)
        assertTrue(allConvert.conversionRateHigh <= 1.0)
    }
}
