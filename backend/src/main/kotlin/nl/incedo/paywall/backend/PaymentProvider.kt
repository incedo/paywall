package nl.incedo.paywall.backend

import java.util.concurrent.ConcurrentHashMap

/**
 * PAY-05: payment provider abstraction — the sandbox/mock implementation is used in the
 * experiment; swap for a real Stripe/Mollie/Adyen adapter without touching paywall logic.
 * PAY-06: the interface exposes [availablePaymentMethods] so the checkout page can render
 * only the methods the provider supports (iDEAL + credit card for the Dutch market).
 */
interface PaymentProvider {
    /**
     * PAY-06: payment methods offered to the user at checkout.
     * Real adapters return the provider-supported subset; the mock returns all NL methods.
     */
    val availablePaymentMethods: List<String>

    /**
     * Initiates a checkout session and returns the session ID and redirect URL.
     * [paymentMethod] pre-selects the method (PAY-06); null = provider default.
     */
    suspend fun createCheckoutSession(
        planId: String,
        subjectId: String,
        /** AC-12: article URL to return to after payment completes. */
        returnUrl: String? = null,
        /** PAY-06: "ideal" | "credit_card" | null (no preference). */
        paymentMethod: String? = null,
    ): PaymentSession
}

data class PaymentSession(val sessionId: String, val checkoutUrl: String)

/**
 * PAY-05/PAY-06: in-experiment mock — immediately creates a session with a synthetic ID.
 * The confirm endpoint (`POST /api/v1/checkout/{sessionId}/confirm`) completes the
 * purchase without real payment processing, allowing end-to-end experiment flows.
 * PAY-07: sessions persist through a simulated payment failure so the retry call uses
 * the same sessionId (no data loss).
 */
class MockPaymentProvider : PaymentProvider {

    data class PendingSession(
        val planId: String,
        val subjectId: String,
        val returnUrl: String? = null,
        /** PAY-06: which method the user chose (null = provider default). */
        val paymentMethod: String? = null,
    )

    /** PAY-06: both NL payment methods supported in the sandbox. */
    override val availablePaymentMethods: List<String> = listOf("ideal", "credit_card")

    private val sessions = ConcurrentHashMap<String, PendingSession>()

    override suspend fun createCheckoutSession(
        planId: String,
        subjectId: String,
        returnUrl: String?,
        paymentMethod: String?,
    ): PaymentSession {
        val sessionId = "mock-${java.util.UUID.randomUUID()}"
        sessions[sessionId] = PendingSession(planId, subjectId, returnUrl, paymentMethod)
        return PaymentSession(sessionId, "/checkout/mock/$sessionId")
    }

    /**
     * Returns and removes the pending session (idempotent: returns null on repeat calls).
     * PAY-07: [consume] = false peeks without removing, leaving the session alive for retry.
     */
    fun consumeSession(sessionId: String, consume: Boolean = true): PendingSession? =
        if (consume) sessions.remove(sessionId) else sessions[sessionId]
}
