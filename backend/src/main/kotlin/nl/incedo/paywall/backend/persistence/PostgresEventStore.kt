package nl.incedo.paywall.backend.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import javax.sql.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.PolymorphicSerializer
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.eventJson
import nl.incedo.paywall.core.port.AppendCondition
import nl.incedo.paywall.core.port.ConcurrencyException
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.core.port.EventQueryResult
import nl.incedo.paywall.core.port.EventStore

/**
 * PostgreSQL event store (tech-stack Q-1): JSONB event data plus a tag index
 * table, exactly the schema documented in architecture/tech-stack.md.
 *
 * Append conditions are checked inside the appending transaction under
 * per-tag advisory locks (DM-06): appends for different subjects proceed in
 * parallel, while appends whose tags overlap — the only ones a DCB condition
 * can conflict with — are serialized. Locks are taken in sorted tag order to
 * prevent deadlocks; unconditional appends lock their tags too, so a
 * condition check can never race a concurrent insert it should have seen.
 */
class PostgresEventStore(private val dataSource: DataSource) : EventStore {

    companion object {
        fun connect(jdbcUrl: String, username: String = "", password: String = ""): PostgresEventStore {
            val config = HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                if (username.isNotEmpty()) this.username = username
                if (password.isNotEmpty()) this.password = password
                maximumPoolSize = 10
            }
            return PostgresEventStore(HikariDataSource(config)).also { it.ensureSchema() }
        }
    }

    fun ensureSchema() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { st ->
                // DM-07: events partitioned by month. The PK includes the
                // partition key (PostgreSQL requirement); the tag table keeps
                // a plain position column (FKs to partitioned tables would
                // need the full key) with its own index. A DEFAULT partition
                // catches anything outside provisioned months.
                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS events (
                        position    BIGINT GENERATED ALWAYS AS IDENTITY,
                        event_type  VARCHAR(255) NOT NULL,
                        data        JSONB NOT NULL,
                        timestamp   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        PRIMARY KEY (position, timestamp)
                    ) PARTITION BY RANGE (timestamp);
                    CREATE TABLE IF NOT EXISTS events_default PARTITION OF events DEFAULT;
                    CREATE TABLE IF NOT EXISTS event_tags (
                        event_position  BIGINT NOT NULL,
                        tag             VARCHAR(255) NOT NULL,
                        PRIMARY KEY (event_position, tag)
                    );
                    CREATE INDEX IF NOT EXISTS idx_event_tags_tag ON event_tags(tag);
                    """.trimIndent(),
                )
            }
        }
        ensureMonthPartitions()
    }

    /** DM-07: provision the current and next month so the boundary never fails. */
    fun ensureMonthPartitions(now: java.time.YearMonth = java.time.YearMonth.now(java.time.ZoneOffset.UTC)) {
        dataSource.connection.use { conn ->
            conn.createStatement().use { st ->
                for (month in listOf(now, now.plusMonths(1))) {
                    val name = "events_p%04d%02d".format(month.year, month.monthValue)
                    val from = month.atDay(1)
                    val to = month.plusMonths(1).atDay(1)
                    st.execute(
                        "CREATE TABLE IF NOT EXISTS $name PARTITION OF events " +
                            "FOR VALUES FROM ('$from') TO ('$to')",
                    )
                }
            }
        }
    }

    override suspend fun query(query: EventQuery): EventQueryResult = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            conn.autoCommit = false
            try {
                val position = conn.prepareStatement("SELECT COALESCE(MAX(position), 0) FROM events").use { st ->
                    st.executeQuery().use { rs -> rs.next(); rs.getLong(1) }
                }
                val sql = if (query.tags.isEmpty()) {
                    "SELECT data FROM events WHERE position > ? ORDER BY position"
                } else {
                    """
                    SELECT e.data FROM events e
                    WHERE e.position > ? AND EXISTS (
                        SELECT 1 FROM event_tags t
                        WHERE t.event_position = e.position AND t.tag = ANY(?)
                    )
                    ORDER BY e.position
                    """.trimIndent()
                }
                val events = conn.prepareStatement(sql).use { st ->
                    st.setLong(1, query.since)
                    if (query.tags.isNotEmpty()) {
                        st.setArray(2, conn.createArrayOf("varchar", query.tags.toTypedArray()))
                    }
                    st.executeQuery().use { rs ->
                        buildList {
                            while (rs.next()) {
                                add(eventJson.decodeFromString(PolymorphicSerializer(DomainEvent::class), rs.getString(1)))
                            }
                        }
                    }
                }
                conn.commit()
                EventQueryResult(events, position)
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    override suspend fun append(events: List<DomainEvent>, condition: AppendCondition?): Unit =
        withContext(Dispatchers.IO) {
            dataSource.connection.use { conn ->
                conn.autoCommit = false
                try {
                    lockTags(conn, events, condition)
                    if (condition != null) checkCondition(conn, condition)
                    insertEvents(conn, events)
                    conn.commit()
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
                }
            }
        }

    /** DM-06: lock every tag this append touches (events + condition), in sorted order. */
    private fun lockTags(conn: Connection, events: List<DomainEvent>, condition: AppendCondition?) {
        val tags = buildSet {
            events.forEach { addAll(it.tags) }
            condition?.query?.tags?.let { addAll(it) }
        }.sorted()
        if (tags.isEmpty()) return
        conn.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))").use { st ->
            tags.forEach { tag ->
                st.setString(1, tag)
                st.executeQuery().close()
            }
        }
    }

    private fun checkCondition(conn: Connection, condition: AppendCondition) {
        val sql = if (condition.query.tags.isEmpty()) {
            "SELECT EXISTS(SELECT 1 FROM events WHERE position > ?)"
        } else {
            """
            SELECT EXISTS(
                SELECT 1 FROM events e
                JOIN event_tags t ON t.event_position = e.position
                WHERE e.position > ? AND t.tag = ANY(?)
            )
            """.trimIndent()
        }
        val conflict = conn.prepareStatement(sql).use { st ->
            st.setLong(1, condition.expectedPosition)
            if (condition.query.tags.isNotEmpty()) {
                st.setArray(2, conn.createArrayOf("varchar", condition.query.tags.toTypedArray()))
            }
            st.executeQuery().use { rs -> rs.next(); rs.getBoolean(1) }
        }
        if (conflict) {
            throw ConcurrencyException(
                "New events matching ${condition.query.tags} appeared after position ${condition.expectedPosition}",
            )
        }
    }

    private fun insertEvents(conn: Connection, events: List<DomainEvent>) {
        events.forEach { event ->
            val position = conn.prepareStatement(
                "INSERT INTO events (event_type, data) VALUES (?, ?::jsonb) RETURNING position",
            ).use { st ->
                st.setString(1, event::class.simpleName ?: "Unknown")
                st.setString(2, eventJson.encodeToString(PolymorphicSerializer(DomainEvent::class), event))
                st.executeQuery().use { rs -> rs.next(); rs.getLong(1) }
            }
            conn.prepareStatement("INSERT INTO event_tags (event_position, tag) VALUES (?, ?)").use { st ->
                event.tags.forEach { tag ->
                    st.setLong(1, position)
                    st.setString(2, tag)
                    st.addBatch()
                }
                st.executeBatch()
            }
        }
    }
}
