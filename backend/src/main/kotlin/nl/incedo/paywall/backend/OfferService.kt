package nl.incedo.paywall.backend

import kotlinx.coroutines.withTimeoutOrNull
import nl.incedo.paywall.access.Subject
import nl.incedo.paywall.cep.CepClient
import nl.incedo.paywall.cep.Offer
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.port.EventQuery
import nl.incedo.paywall.core.port.EventStore
import nl.incedo.paywall.offers.OfferAccepted
import nl.incedo.paywall.offers.OfferDeclined
import nl.incedo.paywall.offers.OfferFrequencyDecision
import nl.incedo.paywall.offers.OfferSuppressed
import nl.incedo.paywall.offers.OfferTriggered
import nl.incedo.paywall.offers.offerTag
import nl.incedo.paywall.plans.DefaultPlans
import nl.incedo.paywall.core.PlanId

/**
 * UP-01/01a: single entry point for all offer decisions.
 *
 * Flow:
 * 1. Load frequency history for this subject + offer candidates (UP-05).
 * 2. Call the CEP client within the configured timeout (UP-07).
 * 3. Validate the returned offer against local guardrails (UP-09).
 * 4. Log the outcome as an event (UP-04/AN-02).
 *
 * A CEP failure never blocks checkout, cancellation, or page render (NFR-11).
 */
class OfferService(
    private val eventStore: EventStore,
    private val cepClient: CepClient?,
    private val clock: () -> Long = { System.currentTimeMillis() },
    /** UP-07: CEP response timeout in milliseconds (default 300 ms). */
    private val cepTimeoutMs: Long = 300L,
    /** UP-09: maximum discount percent considered valid. */
    private val maxDiscountPercent: Int = 50,
    /** UP-05: cooldown in milliseconds (default 30 days). */
    private val frequencyCooldownMs: Long = 30L * 24 * 60 * 60 * 1000,
    /** DN-05: retention offer cap window in milliseconds (default 12 months). */
    private val retentionCapWindowMs: Long = 365L * 24 * 60 * 60 * 1000,
    /** UP-03/UP-07: static fallback offer per trigger, returned when CEP is unavailable. */
    private val fallbackOffers: Map<String, Offer> = emptyMap(),
) {
    data class TriggerContext(
        val trigger: String,
        val channel: String,
        val currentPlanId: String? = null,
        val variant: String = "unknown",
    )

    sealed class OfferDecision {
        data class Triggered(val offer: Offer) : OfferDecision()
        data class Suppressed(val reason: String) : OfferDecision()
    }

    suspend fun decideOffer(subject: Subject, context: TriggerContext): OfferDecision {
        val subjectId = subject.userId?.let { SubjectId.of(it) } ?: SubjectId.of(subject.visitorId)
        val now = clock()

        // UP-04/UP-07: resolve the offer from CEP, distinguishing timeout/error from
        // deliberate null ("no offer applicable") so the suppression reason is correct.
        // UP-06: pass the variant so the CEP can tailor offer strategies per A/B arm.
        // Wrap the CEP response in Result so withTimeoutOrNull=null → timeout (not null-offer).
        val rawOffer: Offer?
        val suppressionReason: String?
        if (cepClient == null) {
            rawOffer = fallbackOffers[context.trigger]
            suppressionReason = if (rawOffer == null) "cep_error" else null
        } else {
            val cepResult: Result<Offer?>? = try {
                withTimeoutOrNull(cepTimeoutMs) {
                    Result.success(cepClient.requestOffer(subject, context.trigger, context.currentPlanId, context.variant))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
            when {
                cepResult == null -> {
                    // Timed out — UP-07: use fallback if configured, else suppress.
                    rawOffer = fallbackOffers[context.trigger]
                    suppressionReason = if (rawOffer == null) "cep_timeout" else null
                }
                cepResult.isFailure -> {
                    // CEP threw an exception — UP-07: use fallback if configured, else suppress.
                    rawOffer = fallbackOffers[context.trigger]
                    suppressionReason = if (rawOffer == null) "cep_error" else null
                }
                else -> {
                    val offer = cepResult.getOrNull()
                    // UP-04: "none_matched" when CEP returned null (no applicable offer).
                    rawOffer = offer
                    suppressionReason = if (offer == null) "none_matched" else null
                }
            }
        }

        if (rawOffer == null) {
            eventStore.append(listOf(OfferSuppressed(subjectId, context.trigger, context.channel, suppressionReason!!, now)), condition = null)
            return OfferDecision.Suppressed(suppressionReason)
        }

        // Load frequency history for this subject (UP-05)
        val offerTags = setOf("subject:${subjectId.value}", offerTag(subjectId, rawOffer.offerId))
        val offerEvents = eventStore.query(EventQuery(offerTags)).events
        val frequency = OfferFrequencyDecision(frequencyCooldownMs).also { it.applyAll(offerEvents) }

        // UP-02a: channel filter
        if (rawOffer.channels.isNotEmpty() && context.channel !in rawOffer.channels) {
            log(subjectId, context, rawOffer.offerId, "channel_mismatch", now)
            return OfferDecision.Suppressed("channel_mismatch")
        }

        // UP-05: frequency cap check
        if (frequency.isCapped(rawOffer.offerId, now)) {
            log(subjectId, context, rawOffer.offerId, "capped", now)
            return OfferDecision.Suppressed("capped")
        }

        // DN-05: at most one accepted retention offer (downsell/discount/pause) per
        // rolling 12 months — prevents cancel-to-discount farming.
        if (isRetentionKind(rawOffer.kind)) {
            val subjectEvents = eventStore.query(EventQuery(setOf("subject:${subjectId.value}"))).events
            val retentionAcceptedAt = subjectEvents
                .filterIsInstance<OfferAccepted>()
                .filter { isRetentionKind(it.kind) }
                .maxOfOrNull { it.acceptedAtEpochMs }
            if (retentionAcceptedAt != null && now - retentionAcceptedAt < retentionCapWindowMs) {
                log(subjectId, context, rawOffer.offerId, "retention_cap", now)
                return OfferDecision.Suppressed("retention_cap")
            }
        }

        // UP-09: local guardrails
        val guardReason = checkGuardrails(rawOffer, context.trigger, context.currentPlanId)
        if (guardReason != null) {
            log(subjectId, context, rawOffer.offerId, "guardrail_rejected", now)
            return OfferDecision.Suppressed("guardrail_rejected")
        }

        // Offer passed all checks — log and return
        val triggered = OfferTriggered(
            subjectId = subjectId,
            offerId = rawOffer.offerId,
            trigger = context.trigger,
            channel = context.channel,
            kind = rawOffer.kind,
            source = rawOffer.source,
            triggeredAtEpochMs = now,
        )
        eventStore.append(listOf(triggered), condition = null)
        return OfferDecision.Triggered(rawOffer)
    }

    /**
     * UP-08: receive a CEP-initiated offer for an async channel (email, chat).
     * Runs through the same guardrail (UP-09), frequency-cap (UP-05), and
     * retention-cap (DN-05) checks, then logs [OfferTriggered] with trigger
     * "cep_push" so the async delivery system can fetch it as a pending offer.
     */
    suspend fun receiveAsyncOffer(subjectId: SubjectId, offer: Offer, channel: String): OfferDecision {
        val now = clock()
        val context = TriggerContext("cep_push", channel)

        if (offer.channels.isNotEmpty() && channel !in offer.channels) {
            log(subjectId, context, offer.offerId, "channel_mismatch", now)
            return OfferDecision.Suppressed("channel_mismatch")
        }

        val offerTags = setOf("subject:${subjectId.value}", offerTag(subjectId, offer.offerId))
        val offerEvents = eventStore.query(EventQuery(offerTags)).events

        // API-08: idempotent on offerId + subjectId — if already triggered, return immediately.
        if (offerEvents.any { it is OfferTriggered && it.trigger == "cep_push" }) {
            return OfferDecision.Triggered(offer)
        }

        val frequency = OfferFrequencyDecision(frequencyCooldownMs).also { it.applyAll(offerEvents) }

        if (frequency.isCapped(offer.offerId, now)) {
            log(subjectId, context, offer.offerId, "capped", now)
            return OfferDecision.Suppressed("capped")
        }

        if (isRetentionKind(offer.kind)) {
            val subjectEvents = eventStore.query(EventQuery(setOf("subject:${subjectId.value}"))).events
            val retentionAcceptedAt = subjectEvents
                .filterIsInstance<OfferAccepted>()
                .filter { isRetentionKind(it.kind) }
                .maxOfOrNull { it.acceptedAtEpochMs }
            if (retentionAcceptedAt != null && now - retentionAcceptedAt < retentionCapWindowMs) {
                log(subjectId, context, offer.offerId, "retention_cap", now)
                return OfferDecision.Suppressed("retention_cap")
            }
        }

        val guardReason = checkGuardrails(offer, "cep_push")
        if (guardReason != null) {
            log(subjectId, context, offer.offerId, "guardrail_rejected", now)
            return OfferDecision.Suppressed("guardrail_rejected")
        }

        eventStore.append(
            listOf(
                OfferTriggered(
                    subjectId = subjectId,
                    offerId = offer.offerId,
                    trigger = "cep_push",
                    channel = channel,
                    kind = offer.kind,
                    source = offer.source,
                    triggeredAtEpochMs = now,
                ),
            ),
            condition = null,
        )
        return OfferDecision.Triggered(offer)
    }

    /** DN-05: offer kinds that count as retention offers for the 12-month cap. */
    private fun isRetentionKind(kind: String): Boolean =
        kind in setOf("downsell", "discount", "pause")

    private suspend fun log(subjectId: SubjectId, context: TriggerContext, offerId: String?, reason: String, now: Long) {
        val suppressed = OfferSuppressed(subjectId, context.trigger, context.channel, reason, now, offerId)
        eventStore.append(listOf(suppressed), condition = null)
    }

    /**
     * UP-09: validate the offer from the CEP against local guardrails.
     * Returns the rejection reason, or null when the offer passes.
     *
     * @param currentPlanId the plan the subject is currently on or checking out (UP-10/11).
     */
    private fun checkGuardrails(offer: Offer, trigger: String, currentPlanId: String? = null): String? {
        val fromPlanId = offer.fromPlanId
        val toPlanId = offer.toPlanId
        // Plans in the offer must exist in the catalogue
        if (fromPlanId != null && DefaultPlans.findById(PlanId(fromPlanId)) == null) {
            return "unknown_from_plan"
        }
        if (toPlanId != null && DefaultPlans.findById(PlanId(toPlanId)) == null) {
            return "unknown_to_plan"
        }
        // Tier transition must be coherent with rank (PAY-01a).
        // Same-rank upsells are valid for billing-period changes (UP-10: monthly → annual).
        val fromRank = DefaultPlans.rankOf(fromPlanId?.let { PlanId(it) })
        val toRank = DefaultPlans.rankOf(toPlanId?.let { PlanId(it) })
        when (offer.kind) {
            "upsell" -> if (toRank < fromRank) return "rank_incoherent_upsell"
            "downsell" -> if (toRank >= fromRank && toPlanId != null) return "rank_incoherent_downsell"
        }
        // Discount within configured bounds
        if ((offer.discountPercent ?: 0) > maxDiscountPercent) return "discount_exceeds_max"
        // DN-02: cancellation can never be blocked — a cancel_intent trigger must not
        // result in an offer kind that prevents the subscriber from cancelling.
        // All offer kinds are valid during cancel_intent; we only gate non-cancel triggers.
        return null
    }
}
