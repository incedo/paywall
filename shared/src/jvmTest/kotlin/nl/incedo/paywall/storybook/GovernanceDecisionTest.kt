package nl.incedo.paywall.storybook

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GovernanceDecisionTest {

    private fun registered(
        id: String = "gov1",
        key: String = "access-policy",
        title: String = "Access Policy",
        lifecycle: LifecycleState = LifecycleState.DRAFT,
    ) = GovernancePolicyRegistered(
        policyId = GovernancePolicyId(id),
        policyKey = GovernancePolicyKey(key),
        title = title,
        lifecycle = lifecycle,
        registeredAtEpochMs = 1_000L,
    )

    @Test fun `model does not exist before any events`() {
        assertFalse(GovernanceDecisionModel().exists)
    }

    @Test fun `exists after GovernancePolicyRegistered`() {
        val d = GovernanceDecisionModel().also { it.apply(registered()) }
        assertTrue(d.exists)
        assertEquals("access-policy", d.policyKey)
        assertEquals("Access Policy", d.title)
        assertEquals(LifecycleState.DRAFT, d.lifecycle)
    }

    @Test fun `QualityGateAttached adds gate to list (BR-3)`() {
        val d = GovernanceDecisionModel().also {
            it.apply(registered())
            it.apply(QualityGateAttached(GovernancePolicyId("gov1"), QualityGateKey("ux-review"), "UX review sign-off", 2_000L))
            it.apply(QualityGateAttached(GovernancePolicyId("gov1"), QualityGateKey("a11y-audit"), "Accessibility audit", 3_000L))
        }
        assertEquals(2, d.qualityGates.size)
        assertEquals("ux-review", d.qualityGates[0].gateKey)
        assertEquals("a11y-audit", d.qualityGates[1].gateKey)
    }

    @Test fun `OwnerAssigned sets owner (BR-4)`() {
        val d = GovernanceDecisionModel().also {
            it.apply(registered())
            it.apply(OwnerAssigned(GovernancePolicyId("gov1"), OwnerRef("team-platform"), 2_000L))
        }
        assertEquals("team-platform", d.owner)
        assertNull(GovernanceDecisionModel().owner)
    }

    @Test fun `OwnerAssigned overwrites previous owner`() {
        val d = GovernanceDecisionModel().also {
            it.apply(registered())
            it.apply(OwnerAssigned(GovernancePolicyId("gov1"), OwnerRef("team-alpha"), 2_000L))
            it.apply(OwnerAssigned(GovernancePolicyId("gov1"), OwnerRef("team-beta"), 3_000L))
        }
        assertEquals("team-beta", d.owner)
    }

    @Test fun `EvidenceLinked accumulates evidence (BR-5)`() {
        val d = GovernanceDecisionModel().also {
            it.apply(registered())
            it.apply(EvidenceLinked(GovernancePolicyId("gov1"), EvidenceRef("test-report-42"), "test-report", 2_000L))
            it.apply(EvidenceLinked(GovernancePolicyId("gov1"), EvidenceRef("screenshot-7"), "screenshot", 3_000L))
        }
        assertEquals(2, d.evidenceRefs.size)
        assertEquals("test-report", d.evidenceRefs[0].evidenceType)
        assertEquals("screenshot-7", d.evidenceRefs[1].evidenceRef)
    }

    @Test fun `GovernanceDecisionRecorded accumulates decisions (BR-6)`() {
        val d = GovernanceDecisionModel().also {
            it.apply(registered())
            it.apply(GovernanceDecisionRecorded(GovernancePolicyId("gov1"), GovernanceDecisionOutcome.APPROVED, null, 2_000L))
            it.apply(GovernanceDecisionRecorded(GovernancePolicyId("gov1"), GovernanceDecisionOutcome.WAIVED, "no longer needed", 3_000L))
        }
        assertEquals(2, d.decisions.size)
        assertEquals(GovernanceDecisionOutcome.APPROVED, d.decisions[0].decision)
        assertNull(d.decisions[0].rationale)
        assertEquals(GovernanceDecisionOutcome.WAIVED, d.decisions[1].decision)
        assertEquals("no longer needed", d.decisions[1].rationale)
    }

    @Test fun `LifecycleGoverned records lifecycle governance events (BR-7)`() {
        val d = GovernanceDecisionModel().also {
            it.apply(registered())
            it.apply(LifecycleGoverned(GovernancePolicyId("gov1"), "story:sb-001", "story", LifecycleState.ACTIVE, 2_000L))
        }
        assertEquals(1, d.lifecycleGovernance.size)
        assertEquals("story:sb-001", d.lifecycleGovernance[0].targetRef)
        assertEquals(LifecycleState.ACTIVE, d.lifecycleGovernance[0].newLifecycle)
    }

    @Test fun `applyAll processes a full event sequence`() {
        val events = listOf(
            registered(),
            QualityGateAttached(GovernancePolicyId("gov1"), QualityGateKey("gate-1"), "Gate one", 2_000L),
            OwnerAssigned(GovernancePolicyId("gov1"), OwnerRef("owner-42"), 3_000L),
            GovernanceDecisionRecorded(GovernancePolicyId("gov1"), GovernanceDecisionOutcome.APPROVED, null, 4_000L),
        )
        val d = GovernanceDecisionModel().also { it.applyAll(events) }
        assertTrue(d.exists)
        assertEquals(1, d.qualityGates.size)
        assertEquals("owner-42", d.owner)
        assertEquals(GovernanceDecisionOutcome.APPROVED, d.decisions.last().decision)
    }

    @Test fun `PolicyKeyUniquenessDecision tracks used keys`() {
        val u = PolicyKeyUniquenessDecision().also {
            it.apply(registered(key = "access-policy"))
            it.apply(registered(id = "gov2", key = "content-policy"))
        }
        assertTrue(u.isTaken("access-policy"))
        assertTrue(u.isTaken("content-policy"))
        assertFalse(u.isTaken("other-policy"))
    }

    @Test fun `PolicyKeyUniquenessDecision ignores non-GovernancePolicyRegistered events`() {
        val u = PolicyKeyUniquenessDecision().also {
            it.apply(registered())
            it.apply(OwnerAssigned(GovernancePolicyId("gov1"), OwnerRef("team-x"), 2_000L))
        }
        assertFalse(u.isTaken("team-x"))
        assertTrue(u.isTaken("access-policy"))
    }
}
