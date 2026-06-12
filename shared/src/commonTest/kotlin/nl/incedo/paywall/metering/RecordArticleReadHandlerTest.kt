package nl.incedo.paywall.metering

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.core.port.AppendCondition
import nl.incedo.paywall.core.port.ConcurrencyException
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.core.port.EventQueryResult
import nl.incedo.paywall.core.port.EventStore

class RecordArticleReadHandlerTest {

    private val subject = SubjectId.of(VisitorId("v-1"))
    private val period = MeterPeriod("2026-06")

    @Test
    fun appendsMeterIncrementedForFirstRead() = runTest {
        val store = InMemoryEventStore()
        val handler = RecordArticleReadHandler(store)

        val event = handler.handle(RecordArticleRead(subject, ArticleId("a-1"), period))

        assertNotNull(event)
        val stored = store.query(EventQuery(setOf(meterTag(subject, period)))).events
        assertEquals(listOf<DomainEvent>(event), stored)
    }

    @Test
    fun reReadIsIdempotent() = runTest {
        // PW-21: second read of the same article appends nothing
        val store = InMemoryEventStore()
        val handler = RecordArticleReadHandler(store)

        handler.handle(RecordArticleRead(subject, ArticleId("a-1"), period))
        val second = handler.handle(RecordArticleRead(subject, ArticleId("a-1"), period))

        assertNull(second)
        assertEquals(1, store.query(EventQuery(setOf(meterTag(subject, period)))).events.size)
    }

    @Test
    fun retriesOnConcurrentAppendAndSucceeds() = runTest {
        // Q-7: on ConcurrencyException the command re-queries, re-decides, re-appends
        val store = ConflictingOnceEventStore(InMemoryEventStore())
        val handler = RecordArticleReadHandler(store)

        val event = handler.handle(RecordArticleRead(subject, ArticleId("a-1"), period))

        assertNotNull(event)
        assertEquals(1, store.appendAttempts - 1, "one retry after the injected conflict")
    }

    @Test
    fun givesUpAfterMaxRetries() = runTest {
        val store = AlwaysConflictingEventStore()
        val handler = RecordArticleReadHandler(store, maxRetries = 3)

        assertFailsWith<ConcurrencyException> {
            handler.handle(RecordArticleRead(subject, ArticleId("a-1"), period))
        }
        assertEquals(4, store.appendAttempts, "initial attempt + 3 retries")
    }

    /** Delegates to a real store but injects one conflict on the first append. */
    private class ConflictingOnceEventStore(private val delegate: InMemoryEventStore) : EventStore {
        var appendAttempts = 0
        override suspend fun query(query: EventQuery): EventQueryResult = delegate.query(query)
        override suspend fun append(events: List<DomainEvent>, condition: AppendCondition?) {
            appendAttempts += 1
            if (appendAttempts == 1) throw ConcurrencyException("injected conflict")
            delegate.append(events, condition)
        }
    }

    private class AlwaysConflictingEventStore : EventStore {
        var appendAttempts = 0
        override suspend fun query(query: EventQuery): EventQueryResult = EventQueryResult(emptyList(), 0)
        override suspend fun append(events: List<DomainEvent>, condition: AppendCondition?) {
            appendAttempts += 1
            throw ConcurrencyException("always conflicting")
        }
    }
}
