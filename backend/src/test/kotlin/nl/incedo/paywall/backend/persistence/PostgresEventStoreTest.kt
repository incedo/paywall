package nl.incedo.paywall.backend.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import nl.incedo.paywall.accounts.AccountLinked
import nl.incedo.paywall.core.ArticleId
import nl.incedo.paywall.core.GrantId
import nl.incedo.paywall.core.PlanId
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.SubscriptionId
import nl.incedo.paywall.core.UserId
import nl.incedo.paywall.core.VisitorId
import nl.incedo.paywall.core.port.AppendCondition
import nl.incedo.paywall.core.port.ConcurrencyException
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.entitlements.EntitlementGranted
import nl.incedo.paywall.entitlements.EntitlementRevoked
import nl.incedo.paywall.grants.GrantIssued
import nl.incedo.paywall.grants.GrantRevoked
import nl.incedo.paywall.metering.MeterIncremented
import nl.incedo.paywall.metering.MeterPeriod
import nl.incedo.paywall.metering.MeterReset

/**
 * Integration tests for the PostgreSQL event store (Q-1). They run against
 * a real database when PAYWALL_TEST_PG_URL is set (CI / local Postgres) and
 * are skipped otherwise, so the suite stays runnable without infrastructure.
 */
class PostgresEventStoreTest {

    private val period = MeterPeriod("2026-06")
    private var suffix = 0
    private fun freshSubject() = SubjectId("visitor:pg-test-${System.nanoTime()}-${suffix++}")

    private fun storeOrSkip(): PostgresEventStore? {
        val url = System.getenv("PAYWALL_TEST_PG_URL")
        if (url == null) {
            println("SKIPPED: set PAYWALL_TEST_PG_URL to run Postgres integration tests")
            return null
        }
        return PostgresEventStore.connect(
            jdbcUrl = url,
            username = System.getenv("PAYWALL_TEST_PG_USER") ?: "",
            password = System.getenv("PAYWALL_TEST_PG_PASSWORD") ?: "",
        )
    }

    @Test
    fun queryFiltersByTagAndSince() = runTest {
        val store = storeOrSkip() ?: return@runTest
        val subject = freshSubject()
        val other = freshSubject()
        store.append(
            listOf(
                MeterIncremented(subject, ArticleId("a-1"), period),
                MeterIncremented(other, ArticleId("a-2"), period),
            ),
            condition = null,
        )

        val result = store.query(EventQuery(setOf("subject:${subject.value}")))
        assertEquals(1, result.events.size)
        assertTrue(result.position > 0)

        val later = store.query(EventQuery(setOf("subject:${subject.value}"), since = result.position))
        assertEquals(0, later.events.size)
    }

    @Test
    fun allEventTypesRoundTripThroughJsonb() = runTest {
        val store = storeOrSkip() ?: return@runTest
        val subject = freshSubject()
        // One of each registered event type — fails when a new event is not
        // added to paywallSerializersModule
        val events = listOf(
            MeterIncremented(subject, ArticleId("a-1"), period),
            MeterReset(subject, period, actor = "m.visser", reason = "support"),
            EntitlementGranted(subject, PlanId("pro"), SubscriptionId("sub-1"), validUntilEpochMs = 1L),
            EntitlementRevoked(subject, SubscriptionId("sub-1")),
            GrantIssued(GrantId("g-1"), subject, ArticleId("a-1"), grantedBy = "day_pass", expiresAtEpochMs = 2L),
            GrantRevoked(GrantId("g-1"), subject, ArticleId("a-1")),
            AccountLinked(VisitorId("v-1"), UserId("u-roundtrip-${System.nanoTime()}")),
        )
        store.append(events, condition = null)

        val stored = store.query(EventQuery(setOf("subject:${subject.value}"))).events
        assertEquals(events.dropLast(1), stored.filterNot { it is AccountLinked })
    }

    @Test
    fun appendConditionRejectsConflictingWrite() = runTest {
        val store = storeOrSkip() ?: return@runTest
        val subject = freshSubject()
        val query = EventQuery(setOf("subject:${subject.value}"))
        val position = store.query(query).position

        store.append(listOf(MeterIncremented(subject, ArticleId("a-other"), period)), condition = null)

        assertFailsWith<ConcurrencyException> {
            store.append(
                listOf(MeterIncremented(subject, ArticleId("a-1"), period)),
                AppendCondition(query, position),
            )
        }
    }

    @Test
    fun appendConditionIgnoresUnrelatedEvents() = runTest {
        val store = storeOrSkip() ?: return@runTest
        val subject = freshSubject()
        val unrelated = freshSubject()
        val query = EventQuery(setOf("subject:${subject.value}"))
        val position = store.query(query).position

        store.append(listOf(MeterIncremented(unrelated, ArticleId("a-9"), period)), condition = null)
        store.append(listOf(MeterIncremented(subject, ArticleId("a-1"), period)), AppendCondition(query, position))

        assertEquals(1, store.query(query).events.size)
    }

    @Test
    fun concurrentConditionalAppendsAdmitExactlyOne() = runTest {
        val store = storeOrSkip() ?: return@runTest
        val subject = freshSubject()
        val query = EventQuery(setOf("subject:${subject.value}"))
        val position = store.query(query).position

        // Two writers race with the same expected position: the advisory-lock
        // protected condition must admit exactly one
        val outcomes = (1..2).map { i ->
            async(Dispatchers.IO) {
                runCatching {
                    store.append(
                        listOf(MeterIncremented(subject, ArticleId("a-$i"), period)),
                        AppendCondition(query, position),
                    )
                }
            }
        }.awaitAll()

        assertEquals(1, outcomes.count { it.isSuccess }, "exactly one writer wins")
        assertEquals(1, outcomes.count { it.exceptionOrNull() is ConcurrencyException })
        assertEquals(1, store.query(query).events.size)
    }
}
