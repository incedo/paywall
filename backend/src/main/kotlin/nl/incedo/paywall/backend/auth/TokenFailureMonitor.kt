package nl.incedo.paywall.backend.auth

import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * NFR-24: structured logging of token validation failures with failure reason
 * and per-source-IP rate tracking.
 *
 * Each request carrying a Bearer token that fails validation is logged with the
 * classified reason. When a single IP exceeds [TokenFailureTracker.threshold]
 * failures within [TokenFailureTracker.windowMs], an abuse alert is emitted —
 * feeding the AC-06 / NFR-04 abuse monitoring pipeline.
 */

private val log = LoggerFactory.getLogger("nl.incedo.paywall.TokenFailureMonitor")

class TokenFailureMonitorConfig {
    var validator: CiamJwtValidator? = null
    var tracker: TokenFailureTracker = TokenFailureTracker()
}

val TokenFailureMonitorPlugin: ApplicationPlugin<TokenFailureMonitorConfig> =
    createApplicationPlugin("TokenFailureMonitor", ::TokenFailureMonitorConfig) {
        val validator = pluginConfig.validator ?: return@createApplicationPlugin
        val tracker = pluginConfig.tracker

        onCall { call ->
            val authHeader = call.request.header("Authorization") ?: return@onCall
            if (!authHeader.startsWith("Bearer ")) return@onCall
            val (_, reason) = validator.verifyWithReason(authHeader)
            if (reason != null && reason != "missing") {
                val sourceIp = call.request.headers["X-Forwarded-For"]
                    ?.substringBefore(",")?.trim()
                    ?: call.request.origin.remoteHost
                val abuseThresholdReached = tracker.record(sourceIp)
                log.warn(
                    "token_validation_failure reason={} sourceIp={} abuseAlert={}",
                    reason, sourceIp, abuseThresholdReached,
                )
                if (abuseThresholdReached) {
                    // AC-06 / NFR-04: repeated failures from same source → abuse signal
                    log.error(
                        "token_failure_abuse_threshold_reached sourceIp={} count={}",
                        sourceIp, tracker.countFor(sourceIp),
                    )
                }
            }
        }
    }

/**
 * Per-source-IP sliding-window failure counter (NFR-24 / AC-06).
 * Thread-safe; uses [ConcurrentHashMap.compute] for atomic per-key updates.
 */
class TokenFailureTracker(
    internal val clock: () -> Long = { System.currentTimeMillis() },
    val windowMs: Long = 5 * 60 * 1_000L,
    val threshold: Int = 10,
) {
    private data class Entry(val windowStart: Long, val count: Int)

    private val perIp = ConcurrentHashMap<String, Entry>()

    /** Records one failure for [sourceIp]. Returns true when the abuse threshold is reached. */
    fun record(sourceIp: String): Boolean {
        val now = clock()
        val entry = perIp.compute(sourceIp) { _, existing ->
            if (existing == null || now - existing.windowStart > windowMs) {
                Entry(now, 1)
            } else {
                existing.copy(count = existing.count + 1)
            }
        }!!
        return entry.count >= threshold
    }

    /** Returns the current failure count for [sourceIp] within the active window. */
    fun countFor(sourceIp: String): Int {
        val now = clock()
        val entry = perIp[sourceIp] ?: return 0
        return if (now - entry.windowStart > windowMs) 0 else entry.count
    }
}
