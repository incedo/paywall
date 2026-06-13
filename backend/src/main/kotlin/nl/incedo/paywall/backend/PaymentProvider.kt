package nl.incedo.paywall.backend

import java.util.concurrent.ConcurrentHashMap

/**
 * PAY-05: payment provider abstraction — the sandbox/mock implementation is used in the
 * experiment; swap for a real Stripe/Mollie/Adyen adapter without touching paywall logic.
 */
interface PaymentProvider {
    /** Initiates a checkout session and returns the session ID and redirect URL. */
    suspend fun createCheckoutSession(planId: String, subjectId: String): PaymentSession
}

data class PaymentSession(val sessionId: String, val checkoutUrl: String)

/**
 * PAY-05: in-experiment mock — immediately creates a session with a synthetic ID.
 * The confirm endpoint (`POST /api/v1/checkout/{sessionId}/confirm`) completes the
 * purchase without real payment processing, allowing end-to-end experiment flows.
 */
class MockPaymentProvider : PaymentProvider {

    data class PendingSession(val planId: String, val subjectId: String)

    private val sessions = ConcurrentHashMap<String, PendingSession>()

    override suspend fun createCheckoutSession(planId: String, subjectId: String): PaymentSession {
        val sessionId = "mock-${java.util.UUID.randomUUID()}"
        sessions[sessionId] = PendingSession(planId, subjectId)
        return PaymentSession(sessionId, "/checkout/mock/$sessionId")
    }

    /** Returns and removes the pending session (idempotent: returns null on repeat calls). */
    fun consumeSession(sessionId: String): PendingSession? = sessions.remove(sessionId)
}
