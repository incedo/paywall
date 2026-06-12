package nl.incedo.paywall.cep

import nl.incedo.paywall.core.DomainEvent

/**
 * Decision model: has the CEP advised gating this subject (PW-40 verdict)?
 * Built from the subject's ingested CEP events; the latest advice wins.
 */
class CepAdviceDecision {

    private var advised = false
    private var validUntil: Long? = null

    fun apply(event: DomainEvent) {
        when (event) {
            is CepGateAdvised -> {
                advised = true
                validUntil = event.validUntilEpochMs
            }
            is CepGateAdviceWithdrawn -> {
                advised = false
                validUntil = null
            }
            else -> {}
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)

    fun gateAdvised(nowEpochMs: Long): Boolean =
        advised && (validUntil?.let { it > nowEpochMs } ?: true)
}
