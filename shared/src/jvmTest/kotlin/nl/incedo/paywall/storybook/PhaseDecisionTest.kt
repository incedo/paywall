package nl.incedo.paywall.storybook

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhaseDecisionTest {

    private fun registered(
        id: String = "ph1",
        key: String = "alpha",
        name: String = "Alpha Phase",
        order: Int = 1,
        lifecycle: PhaseLifecycle = PhaseLifecycle.PLANNED,
    ) = PhaseRegistered(
        phaseId = PhaseId(id),
        phaseKey = PhaseKey(key),
        phaseName = PhaseName(name),
        phaseOrder = order,
        lifecycle = lifecycle,
        registeredAtEpochMs = 1_000L,
    )

    @Test fun `model does not exist before any events`() {
        assertFalse(PhaseDecisionModel().exists)
    }

    @Test fun `exists after PhaseRegistered`() {
        val d = PhaseDecisionModel().also { it.apply(registered()) }
        assertTrue(d.exists)
        assertEquals("alpha", d.phaseKey)
        assertEquals("Alpha Phase", d.phaseName)
        assertEquals(1, d.phaseOrder)
        assertEquals(PhaseLifecycle.PLANNED, d.lifecycle)
    }

    @Test fun `CapabilityAddedToPhase populates capabilities map`() {
        val d = PhaseDecisionModel().also {
            it.apply(registered())
            it.apply(CapabilityAddedToPhase(PhaseId("ph1"), CapabilityKey("auth"), "Auth flow", null, 2_000L))
            it.apply(CapabilityAddedToPhase(PhaseId("ph1"), CapabilityKey("rbac"), "Role management", null, 3_000L))
        }
        assertEquals(mapOf("auth" to "Auth flow", "rbac" to "Role management"), d.capabilities)
    }

    @Test fun `second CapabilityAddedToPhase with same key overwrites title (idempotent upsert)`() {
        val d = PhaseDecisionModel().also {
            it.apply(registered())
            it.apply(CapabilityAddedToPhase(PhaseId("ph1"), CapabilityKey("auth"), "Auth v1", null, 2_000L))
            it.apply(CapabilityAddedToPhase(PhaseId("ph1"), CapabilityKey("auth"), "Auth v2", null, 3_000L))
        }
        assertEquals("Auth v2", d.capabilities["auth"])
    }

    @Test fun `PhaseActivated sets lifecycle to ACTIVE (BR-4)`() {
        val d = PhaseDecisionModel().also {
            it.apply(registered())
            it.apply(PhaseActivated(PhaseId("ph1"), 2_000L))
        }
        assertEquals(PhaseLifecycle.ACTIVE, d.lifecycle)
    }

    @Test fun `PhaseSatisfied sets lifecycle to SATISFIED (BR-5)`() {
        val d = PhaseDecisionModel().also {
            it.apply(registered())
            it.apply(PhaseActivated(PhaseId("ph1"), 2_000L))
            it.apply(PhaseSatisfied(PhaseId("ph1"), 3_000L))
        }
        assertEquals(PhaseLifecycle.SATISFIED, d.lifecycle)
    }

    @Test fun `PhaseSuperseded sets lifecycle to SUPERSEDED (BR-6)`() {
        val d = PhaseDecisionModel().also {
            it.apply(registered())
            it.apply(PhaseActivated(PhaseId("ph1"), 2_000L))
            it.apply(PhaseSuperseded(PhaseId("ph1"), 3_000L))
        }
        assertEquals(PhaseLifecycle.SUPERSEDED, d.lifecycle)
    }

    @Test fun `applyAll processes entire list`() {
        val events = listOf(
            registered(),
            CapabilityAddedToPhase(PhaseId("ph1"), CapabilityKey("feat-a"), "Feature A", null, 2_000L),
            PhaseActivated(PhaseId("ph1"), 3_000L),
        )
        val d = PhaseDecisionModel().also { it.applyAll(events) }
        assertTrue(d.exists)
        assertEquals(PhaseLifecycle.ACTIVE, d.lifecycle)
        assertEquals(1, d.capabilities.size)
    }

    @Test fun `PhaseKeyUniquenessDecision tracks used keys`() {
        val u = PhaseKeyUniquenessDecision().also {
            it.apply(registered(key = "alpha"))
            it.apply(registered(id = "ph2", key = "beta"))
        }
        assertTrue(u.isTaken("alpha"))
        assertTrue(u.isTaken("beta"))
        assertFalse(u.isTaken("gamma"))
    }

    @Test fun `PhaseKeyUniquenessDecision ignores non-PhaseRegistered events`() {
        val u = PhaseKeyUniquenessDecision().also {
            it.apply(registered())
            it.apply(PhaseActivated(PhaseId("ph1"), 2_000L))
        }
        assertFalse(u.isTaken("alpha-phase"))
        assertTrue(u.isTaken("alpha"))
    }
}
