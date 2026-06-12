package nl.incedo.paywall.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import nl.incedo.paywall.backend.persistence.PostgresEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * The API-05 speed test against the REAL persistence stack: Ktor CIO over
 * a loopback socket with the PostgreSQL event store behind it. This is the
 * measurement that matters for the < 50 ms p95 budget. Skipped when
 * PAYWALL_TEST_PG_URL is not set.
 */
class PostgresDecideSpeedTest {

    @Test
    fun decideEndpointOnPostgres_p95UnderApiBudget() = runBlocking {
        val url = System.getenv("PAYWALL_TEST_PG_URL")
        if (url == null) {
            println("SKIPPED: set PAYWALL_TEST_PG_URL to run the Postgres speed test")
            return@runBlocking
        }
        val store = PostgresEventStore.connect(
            jdbcUrl = url,
            username = System.getenv("PAYWALL_TEST_PG_USER") ?: "",
            password = System.getenv("PAYWALL_TEST_PG_PASSWORD") ?: "",
        )
        val service = AccessService(
            eventStore = store,
            experiment = defaultExperiment,
            clock = { System.currentTimeMillis() },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        val server = embeddedServer(CIO, port = 0) { module(service, store) }.start(wait = false)
        val port = server.engine.resolvedConnectors().first().port
        val client = HttpClient(ClientCIO) {
            install(ContentNegotiation) { json() }
        }
        val run = System.nanoTime() // unique visitor namespace per test run

        try {
            suspend fun call(i: Int): Long {
                val start = System.nanoTime()
                val response = client.post("http://127.0.0.1:$port/api/v1/decide") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        DecideRequest(
                            visitorId = "pg-load-$run-${i % 200}",
                            articleId = "a-${i % 50}",
                            tier = "premium",
                        ),
                    )
                }
                val elapsed = System.nanoTime() - start
                assertEquals(HttpStatusCode.OK, response.status)
                return elapsed
            }

            repeat(300) { call(it) } // warmup: JIT, pools, PG plans

            val sequential = LongArray(1_000) { call(it) }.sorted()
            val concurrent = (0 until 8).map { worker ->
                async(Dispatchers.IO) { LongArray(250) { call(worker * 250 + it) }.toList() }
            }.awaitAll().flatten().sorted()

            fun p(samples: List<Long>, q: Int) = samples[(samples.size * q) / 100] / 1_000_000.0
            println("decide+postgres sequential: p50=${p(sequential, 50)}ms p95=${p(sequential, 95)}ms p99=${p(sequential, 99)}ms")
            println("decide+postgres concurrent (8 workers): p50=${p(concurrent, 50)}ms p95=${p(concurrent, 95)}ms p99=${p(concurrent, 99)}ms")

            assertTrue(p(sequential, 95) < 50.0, "sequential p95 ${p(sequential, 95)}ms exceeds 50 ms (API-05)")
            assertTrue(p(concurrent, 95) < 50.0, "concurrent p95 ${p(concurrent, 95)}ms exceeds 50 ms (API-05)")
        } finally {
            client.close()
            server.stop(100, 500)
        }
    }
}
