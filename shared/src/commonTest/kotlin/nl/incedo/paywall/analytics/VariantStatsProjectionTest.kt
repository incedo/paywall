package nl.incedo.paywall.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
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
        type = type,
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
}
