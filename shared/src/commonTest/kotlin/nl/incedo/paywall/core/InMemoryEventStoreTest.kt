package nl.incedo.paywall.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.AppendCondition
import nl.incedo.paywall.core.port.ConcurrencyException
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.metering.MeterIncremented
import nl.incedo.paywall.metering.meterTag
import nl.incedo.paywall.metering.MeterPeriod

class InMemoryEventStoreTest {

    private val period = MeterPeriod("2026-06")
    private fun event(subject: String, article: String) =
        MeterIncremented(SubjectId("visitor:$subject"), ArticleId(article), period)

    @Test
    fun queryFiltersByTag() = runTest {
        val store = InMemoryEventStore()
        store.append(listOf(event("v-1", "a-1"), event("v-2", "a-2")), condition = null)

        val result = store.query(EventQuery(setOf(meterTag(SubjectId("visitor:v-1"), period))))

        assertEquals(1, result.events.size)
        assertEquals(2, result.position, "position reflects the whole store")
    }

    @Test
    fun multiTagQueryReturnsTheUnion() = runTest {
        // Basis for the MT-03 meter merge: query visitor + user subject together
        val store = InMemoryEventStore()
        store.append(listOf(event("v-1", "a-1"), event("u-1", "a-2"), event("v-9", "a-3")), condition = null)

        val result = store.query(EventQuery(setOf(meterTag(SubjectId("visitor:v-1"), period), meterTag(SubjectId("visitor:u-1"), period))))

        assertEquals(2, result.events.size)
    }

    @Test
    fun appendConditionRejectsWhenMatchingEventsAppeared() = runTest {
        val store = InMemoryEventStore()
        val query = EventQuery(setOf(meterTag(SubjectId("visitor:v-1"), period)))
        val position = store.query(query).position

        // Concurrent write for the same subject after our query
        store.append(listOf(event("v-1", "a-other")), condition = null)

        assertFailsWith<ConcurrencyException> {
            store.append(listOf(event("v-1", "a-1")), AppendCondition(query, position))
        }
    }

    @Test
    fun appendConditionIgnoresUnrelatedEvents() = runTest {
        val store = InMemoryEventStore()
        val query = EventQuery(setOf(meterTag(SubjectId("visitor:v-1"), period)))
        val position = store.query(query).position

        // A different subject's events must not block our append
        store.append(listOf(event("v-other", "a-9")), condition = null)
        store.append(listOf(event("v-1", "a-1")), AppendCondition(query, position))

        assertEquals(1, store.query(query).events.size)
    }
}
