package nl.incedo.paywall.access

import kotlin.test.Test
import kotlin.test.assertEquals
import nl.incedo.paywall.core.eventJson

/**
 * MT-06: meter periods and limits are defined per variant configuration (PW-06),
 * defaulting to calendar month / limit 5.
 *
 * The periodType field on StrategyConfig.Metered carries the period type in
 * the variant config so it is versioned alongside other experiment parameters
 * (stored in ExperimentConfigPublished events — API-03).
 */
class MeterPeriodTypeTest {

    @Test
    fun defaultPeriodTypeIsCalendarMonth() {
        val metered = StrategyConfig.Metered()
        assertEquals("calendar_month", metered.periodType, "MT-06: default period type must be calendar_month")
    }

    @Test
    fun defaultLimitIsFive() {
        val metered = StrategyConfig.Metered()
        assertEquals(5, metered.limit, "MT-06: default limit must be 5 (PW-20)")
    }

    @Test
    fun periodTypeSerializesAndRoundTrips() {
        val original = StrategyConfig.Metered(limit = 7, periodType = "rolling_30d")
        val json = eventJson.encodeToString(StrategyConfig.serializer(), original)
        val decoded = eventJson.decodeFromString(StrategyConfig.serializer(), json)
        assertEquals(original, decoded, "MT-06: periodType must survive JSON round-trip")
    }

    @Test
    fun oldEventsWithoutPeriodTypeDeserializeWithDefault() {
        // Events persisted before MT-06 won't have the periodType field.
        // ignoreUnknownKeys=true + default value ensures backward compatibility.
        val legacyJson = """{"type":"nl.incedo.paywall.access.StrategyConfig.Metered","limit":3}"""
        val decoded = eventJson.decodeFromString(StrategyConfig.serializer(), legacyJson)
        assertEquals(StrategyConfig.Metered(limit = 3, periodType = "calendar_month"), decoded,
            "MT-06: old Metered events without periodType must default to calendar_month")
    }

    @Test
    fun variantsWithDifferentPeriodTypesAreDistinct() {
        val monthly = StrategyConfig.Metered(limit = 5, periodType = "calendar_month")
        val rolling = StrategyConfig.Metered(limit = 5, periodType = "rolling_30d")
        assert(monthly != rolling)
        assertEquals("calendar_month", monthly.periodType)
        assertEquals("rolling_30d", rolling.periodType)
    }
}
