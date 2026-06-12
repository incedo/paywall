package nl.incedo.paywall.accounts

import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.SubjectId

/**
 * Decision model over the identity graph (MT-13): which subjects belong to
 * the same person? Built from the link events returned by a subject-tagged
 * query; [linkedSubjects] yields the connected component within those edges.
 */
class IdentityLinkDecision {

    private val edges = mutableSetOf<Pair<SubjectId, SubjectId>>()

    fun apply(event: DomainEvent) {
        when (event) {
            is IdentityLinked -> edges.add(normalize(event.subjectA, event.subjectB))
            is IdentityUnlinked -> edges.remove(normalize(event.subjectA, event.subjectB))
            else -> {}
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)

    /** Transitive closure over the known edges, starting from [start]. */
    fun linkedSubjects(start: Set<SubjectId>): Set<SubjectId> {
        val reached = start.toMutableSet()
        var grew = true
        while (grew) {
            grew = false
            for ((a, b) in edges) {
                if (a in reached && reached.add(b)) grew = true
                if (b in reached && reached.add(a)) grew = true
            }
        }
        return reached
    }

    private fun normalize(a: SubjectId, b: SubjectId): Pair<SubjectId, SubjectId> =
        if (a.value <= b.value) a to b else b to a
}
