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
 * Append conditions are checked inside the appending transaction under an
 * advisory lock, which serializes appends. That is deliberate: appends are
 * sub-millisecond and the experiment's write volume is low; correctness of
 * the DCB check beats parallel-append throughput here.
 */
class PostgresEventStore(private val dataSource: DataSource) : EventStore {

    companion object {
        private const val APPEND_LOCK = 0x50415957L // "PAYW"

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
                st.execute(
                    """
                    CREATE TABLE IF NOT EXISTS events (
                        position    BIGSERIAL PRIMARY KEY,
                        event_type  VARCHAR(255) NOT NULL,
                        data        JSONB NOT NULL,
                        timestamp   TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    );
                    CREATE TABLE IF NOT EXISTS event_tags (
                        event_position  BIGINT NOT NULL REFERENCES events(position),
                        tag             VARCHAR(255) NOT NULL,
                        PRIMARY KEY (event_position, tag)
                    );
                    CREATE INDEX IF NOT EXISTS idx_event_tags_tag ON event_tags(tag);
                    """.trimIndent(),
                )
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
                    conn.prepareStatement("SELECT pg_advisory_xact_lock(?)").use { st ->
                        st.setLong(1, APPEND_LOCK)
                        st.executeQuery().close()
                    }
                    if (condition != null) checkCondition(conn, condition)
                    insertEvents(conn, events)
                    conn.commit()
                } catch (e: Exception) {
                    conn.rollback()
                    throw e
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
