package nl.incedo.paywall.accounts

import kotlin.test.Test
import kotlin.test.assertEquals
import nl.incedo.paywall.core.SubjectId

class IdentityLinkDecisionTest {

    private val a = SubjectId("visitor:a")
    private val b = SubjectId("visitor:b")
    private val c = SubjectId("user:c")

    @Test
    fun noLinksMeansOnlyTheStartSubjects() {
        assertEquals(setOf(a), IdentityLinkDecision().linkedSubjects(setOf(a)))
    }

    @Test
    fun directLinkIsResolved() {
        val decision = IdentityLinkDecision()
        decision.apply(IdentityLinked(a, b, cause = "newsletter_token"))
        assertEquals(setOf(a, b), decision.linkedSubjects(setOf(a)))
    }

    @Test
    fun transitiveLinksFormOnePerson() {
        // visitor a -> visitor b (newsletter), b -> user c (login): one person
        val decision = IdentityLinkDecision()
        decision.apply(IdentityLinked(a, b, cause = "newsletter_token"))
        decision.apply(IdentityLinked(b, c, cause = "login"))
        assertEquals(setOf(a, b, c), decision.linkedSubjects(setOf(a)))
    }

    @Test
    fun unlinkCompensatesAWrongLink() {
        val decision = IdentityLinkDecision()
        decision.apply(IdentityLinked(a, b, cause = "share_token"))
        decision.apply(IdentityUnlinked(b, a, reason = "support correction")) // order-independent
        assertEquals(setOf(a), decision.linkedSubjects(setOf(a)))
    }

    @Test
    fun linkDirectionDoesNotMatter() {
        val decision = IdentityLinkDecision()
        decision.apply(IdentityLinked(b, a, cause = "device_login"))
        assertEquals(setOf(a, b), decision.linkedSubjects(setOf(a)))
        assertEquals(setOf(a, b), decision.linkedSubjects(setOf(b)))
    }
}
