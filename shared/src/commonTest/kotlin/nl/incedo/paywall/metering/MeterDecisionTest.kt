package nl.incedo.paywall.metering

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.VisitorId

class MeterDecisionTest {

    private val subject = SubjectId.of(VisitorId("v-1"))
    private val june = MeterPeriod("2026-06")
    private val july = MeterPeriod("2026-07")

    @Test
    fun countsDistinctArticles() {
        val meter = MeterDecision(june)
        meter.apply(MeterIncremented(subject, ArticleId("a-1"), june))
        meter.apply(MeterIncremented(subject, ArticleId("a-2"), june))
        assertEquals(2, meter.used)
    }

    @Test
    fun reReadConsumesNoExtraCredit() {
        // PW-21
        val meter = MeterDecision(june)
        meter.apply(MeterIncremented(subject, ArticleId("a-1"), june))
        meter.apply(MeterIncremented(subject, ArticleId("a-1"), june))
        assertEquals(1, meter.used)
        assertTrue(meter.isCounted(ArticleId("a-1")))
    }

    @Test
    fun limitBoundary_articleLimitPlusOneHasNoCredit() {
        // PW-20: reading article limit+1 triggers the gate
        val meter = MeterDecision(june)
        repeat(3) { meter.apply(MeterIncremented(subject, ArticleId("a-$it"), june)) }
        assertTrue(meter.hasCreditFor(ArticleId("a-0"), limit = 3), "counted article stays readable")
        assertFalse(meter.hasCreditFor(ArticleId("a-new"), limit = 3), "new article beyond limit gates")
    }

    @Test
    fun eventsFromAnotherPeriodAreIgnored() {
        // PW-24: the meter is per calendar month
        val meter = MeterDecision(july)
        meter.apply(MeterIncremented(subject, ArticleId("a-1"), june))
        assertEquals(0, meter.used)
    }

    @Test
    fun resetClearsThePeriod() {
        // ADM-04: audited support action
        val meter = MeterDecision(june)
        meter.apply(MeterIncremented(subject, ArticleId("a-1"), june))
        meter.apply(MeterReset(subject, june, actor = "m.visser", reason = "support ticket 4711"))
        assertEquals(0, meter.used)
        assertFalse(meter.isCounted(ArticleId("a-1")))
    }

    @Test
    fun mergedMeterIsTheUnionOfBothSubjects() {
        // MT-03: anonymous meter merges into the user's on login — union of read articles
        val meter = MeterDecision(june)
        val visitorSubject = SubjectId.of(VisitorId("v-1"))
        val userSubject = SubjectId.of(nl.incedo.paywall.core.UserId("u-1"))
        meter.apply(MeterIncremented(visitorSubject, ArticleId("a-1"), june))
        meter.apply(MeterIncremented(userSubject, ArticleId("a-1"), june)) // same article on both sides
        meter.apply(MeterIncremented(userSubject, ArticleId("a-2"), june))
        assertEquals(2, meter.used, "union, not sum")
    }
}
