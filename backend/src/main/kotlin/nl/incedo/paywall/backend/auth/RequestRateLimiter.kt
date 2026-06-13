package nl.incedo.paywall.backend.auth

import java.util.concurrent.ConcurrentHashMap

/**
 * NFR-04: per-source-IP rate limiting on sensitive endpoints (auth, checkout,
 * integration webhooks). A request that exceeds [maxRequests] within [windowMs]
 * per (IP, route key) should receive 429.
 *
 * CSRF protection note: all state-changing endpoints require
 * `Content-Type: application/json` (enforced by Ktor ContentNegotiation) and
 * the `X-Origin-Secret` header (enforced by OriginTrust, INF-02). Together,
 * these prevent cross-site request forgery for both browser and Compose clients.
 *
 * Keys are typically "sourceIp:routePath" to isolate limits per endpoint per client.
 */
class RequestRateLimiter(
    internal val clock: () -> Long = { System.currentTimeMillis() },
    val windowMs: Long = 60_000L,
    val maxRequests: Int = 30,
) {
    private data class Entry(val windowStart: Long, val count: Int)

    private val windows = ConcurrentHashMap<String, Entry>()

    /** Records one request. Returns true when the key has exceeded [maxRequests] in the window. */
    fun isExceeded(key: String): Boolean {
        val now = clock()
        val entry = windows.compute(key) { _, existing ->
            if (existing == null || now - existing.windowStart > windowMs) {
                Entry(now, 1)
            } else {
                existing.copy(count = existing.count + 1)
            }
        }!!
        return entry.count > maxRequests
    }

    /** Current request count for [key] in the active window (0 if window expired or unknown). */
    fun countFor(key: String): Int {
        val now = clock()
        val entry = windows[key] ?: return 0
        return if (now - entry.windowStart > windowMs) 0 else entry.count
    }
}
