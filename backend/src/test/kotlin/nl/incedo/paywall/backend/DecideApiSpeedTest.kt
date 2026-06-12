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
import nl.incedo.paywall.core.adapter.InMemoryEventStore
import nl.incedo.paywall.metering.MeterPeriod

/**
 * Speed test for the decision endpoint over a real HTTP socket (loopback):
 * API-05 requires < 50 ms p95 excluding network, and NFR-15 gives the edge
 * a 50 ms envelope per customer response. Measured end-to-end through
 * Ktor CIO: parsing, routing, JSON, event-store query, engine, append.
 */
class DecideApiSpeedTest {

    private fun request(i: Int) = DecideRequest(
        visitorId = "load-visitor-${i % 200}", // 200 subjects accumulating meter history
        articleId = "a-${i % 50}",
        tier = "premium",
    )

    @Test
    fun decideEndpoint_p95UnderApiBudget() = runBlocking {
        val service = AccessService(
            eventStore = InMemoryEventStore(),
            experiment = defaultExperiment,
            clock = { System.currentTimeMillis() },
            currentPeriod = { MeterPeriod("2026-06") },
        )
        val server = embeddedServer(CIO, port = 0) { module(service) }.start(wait = false)
        val port = server.engine.resolvedConnectors().first().port
        val client = HttpClient(ClientCIO) {
            install(ContentNegotiation) { json() }
        }

        try {
            suspend fun call(i: Int): Long {
                val start = System.nanoTime()
                val response = client.post("http://127.0.0.1:$port/api/v1/decide") {
                    contentType(ContentType.Application.Json)
                    setBody(request(i))
                }
                val elapsed = System.nanoTime() - start
                assertEquals(HttpStatusCode.OK, response.status)
                return elapsed
            }

            repeat(500) { call(it) } // warmup: JIT + connection pool

            // Sequential latency measurement
            val sequential = LongArray(1_000) { call(it) }
            sequential.sort()

            // Concurrent load: 8 parallel callers × 250 requests
            val concurrent = (0 until 8).map { worker ->
                async(Dispatchers.IO) {
                    LongArray(250) { call(worker * 250 + it) }
                }
            }.awaitAll().flatMap { it.asIterable() }.sorted()

            fun p(samples: List<Long>, q: Int) = samples[(samples.size * q) / 100] / 1_000_000.0
            val seq = sequential.toList()
            println("decide sequential: p50=${p(seq, 50)}ms p95=${p(seq, 95)}ms p99=${p(seq, 99)}ms")
            println("decide concurrent (8 workers): p50=${p(concurrent, 50)}ms p95=${p(concurrent, 95)}ms p99=${p(concurrent, 99)}ms")

            // API-05: < 50 ms p95 — asserted under concurrency, the harder case
            assertTrue(p(seq, 95) < 50.0, "sequential p95 ${p(seq, 95)}ms exceeds the 50 ms budget (API-05)")
            assertTrue(p(concurrent, 95) < 50.0, "concurrent p95 ${p(concurrent, 95)}ms exceeds the 50 ms budget (API-05)")
        } finally {
            client.close()
            server.stop(100, 500)
        }
    }
}
