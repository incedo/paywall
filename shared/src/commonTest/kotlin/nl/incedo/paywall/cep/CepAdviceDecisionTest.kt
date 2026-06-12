package nl.incedo.paywall.cep

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import nl.incedo.paywall.core.SubjectId
import nl.incedo.paywall.core.VisitorId

class CepAdviceDecisionTest {

    private val subject = SubjectId.of(VisitorId("v-1"))
    private val now = 1_750_000_000_000L

    @Test
    fun noEventsMeansNoGateAdvice() {
        assertFalse(CepAdviceDecision().gateAdvised(now))
    }

    @Test
    fun publishedAdviceGates() {
        val decision = CepAdviceDecision()
        decision.apply(CepGateAdvised(subject, validUntilEpochMs = now + 1))
        assertTrue(decision.gateAdvised(now))
    }

    @Test
    fun openEndedAdviceGatesUntilWithdrawn() {
        val decision = CepAdviceDecision()
        decision.apply(CepGateAdvised(subject, validUntilEpochMs = null))
        assertTrue(decision.gateAdvised(now))
        decision.apply(CepGateAdviceWithdrawn(subject))
        assertFalse(decision.gateAdvised(now))
    }

    @Test
    fun expiredAdviceDoesNotGate() {
        val decision = CepAdviceDecision()
        decision.apply(CepGateAdvised(subject, validUntilEpochMs = now))
        assertFalse(decision.gateAdvised(now), "validUntil is exclusive — stale advice must not gate")
    }

    @Test
    fun latestAdviceWins() {
        val decision = CepAdviceDecision()
        decision.apply(CepGateAdvised(subject, validUntilEpochMs = now + 1))
        decision.apply(CepGateAdviceWithdrawn(subject))
        decision.apply(CepGateAdvised(subject, validUntilEpochMs = now + 60_000))
        assertTrue(decision.gateAdvised(now))
    }
}
