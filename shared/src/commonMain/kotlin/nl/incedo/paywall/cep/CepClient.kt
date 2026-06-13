package nl.incedo.paywall.cep

import nl.incedo.paywall.access.Subject

/**
 * API-07: outbound CEP integration interface.
 *
 * The paywall calls [requestOffer] when a trigger event fires (e.g. gate shown,
 * cancel intent, payment failure). The CEP engine applies its rules and returns
 * an [Offer] when one is available, or null when the subject is ineligible.
 *
 * In the experiment the mock implementation returns configurable fixed offers.
 * The real CEP replaces the mock without touching callers (cf. PAY-05).
 */
interface CepClient {
    /**
     * Request an offer from the CEP engine for the given subject and trigger.
     *
     * @param subject        The subject (visitor or logged-in user) requesting the offer.
     * @param trigger        The trigger context that caused the offer request (e.g.
     *                       "gate_shown", "cancel_intent", "payment_failure").
     * @param currentPlanId  The plan the subject is currently on or checking out;
     *                       used by the CEP for UP-10 (billing-period upsell) and
     *                       UP-11 (tier upsell at checkout). Null if not subscribed.
     * @param variant        UP-06: the A/B variant assigned to this subject so the CEP
     *                       can tailor offer strategies per variant. Null if unknown.
     * @return an [Offer] when the CEP decides to present one, null when none applies.
     */
    suspend fun requestOffer(
        subject: Subject,
        trigger: String,
        currentPlanId: String? = null,
        variant: String? = null,
    ): Offer?
}

/**
 * An offer returned by the CEP engine (UP-02).
 *
 * [kind]: "upsell" | "downsell" | "access_grant" (FGA-07)
 * [channels]: the channels on which this offer may be presented (UP-02a).
 * [source]: CEP campaign/rule reference for audit trail.
 */
data class Offer(
    val offerId: String,
    /** UP-02: "upsell" | "downsell" | "access_grant". */
    val kind: String,
    /** The plan the subject is currently on (null if not subscribed). */
    val fromPlanId: String? = null,
    /** The plan being offered (null for access_grant offers). */
    val toPlanId: String? = null,
    val discountPercent: Int? = null,
    /** UP-02: fixed discount in minor currency units (e.g. cents); alternative to discountPercent. */
    val discountFixed: Int? = null,
    /** UP-02: how many billing periods the discount applies (null = indefinite). */
    val discountDurationPeriods: Int? = null,
    /** How long the offer is valid (seconds from now). */
    val validForSeconds: Long? = null,
    /** Pause duration in months (UP-02 optional pause field). */
    val pauseMonths: Int? = null,
    /** The trigger that caused this offer (e.g. "gate_shown", "cancel_intent"). */
    val trigger: String = "",
    /** UP-02a: channels on which this offer may be presented. Empty = all channels. */
    val channels: Set<String> = emptySet(),
    /** UP-02: CEP campaign/rule reference. */
    val source: String = "",
    /** Human-readable label for the gate CTA button. */
    val cta: String? = null,
)

/**
 * API-07 mock: returns a configurable fixed offer for all requests.
 * Replace with the real CEP HTTP client in production.
 */
class MockCepClient(private val fixedOffer: Offer? = null) : CepClient {
    override suspend fun requestOffer(
        subject: Subject,
        trigger: String,
        currentPlanId: String?,
        variant: String?,
    ): Offer? = fixedOffer
}
