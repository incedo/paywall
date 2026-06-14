package nl.incedo.paywall.storybook

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the three storybook decision models.
 * Mirrors testing.md §10a.
 */
class StorybookDecisionTest {

    // ── StoryDecision ─────────────────────────────────────────────────────────

    private fun storyRegistered(id: String = "s1", key: String = "button") = StoryRegistered(
        storyId = StoryId(id), storyKey = StoryKey(key), title = "Button",
        type = StoryType.COMPONENT, groupId = "inputs", owner = "design",
        renderContractRef = "contracts/button.md", lifecycle = StoryLifecycle.ACTIVE,
        registeredAtEpochMs = 1_000L,
    )

    @Test fun `story does not exist before any events`() {
        val d = StoryDecision()
        assertFalse(d.exists)
    }

    @Test fun `story exists after StoryRegistered`() {
        val d = StoryDecision().also { it.apply(storyRegistered()) }
        assertTrue(d.exists)
        assertFalse(d.archived)
        assertEquals("button", d.storyKey)
        assertEquals("Button", d.title)
        assertEquals("COMPONENT", d.storyType)
        assertEquals("inputs", d.groupId)
    }

    @Test fun `StoryArchived marks story as archived`() {
        val d = StoryDecision().also {
            it.apply(storyRegistered())
            it.apply(StoryArchived(StoryId("s1"), archivedAtEpochMs = 2_000L))
        }
        assertTrue(d.archived)
        assertEquals(StoryLifecycle.ARCHIVED, d.lifecycle)
    }

    @Test fun `StoryMetadataUpdated patches title`() {
        val d = StoryDecision().also {
            it.apply(storyRegistered())
            it.apply(StoryMetadataUpdated(StoryId("s1"), title = "Button v2", updatedAtEpochMs = 2_000L))
        }
        assertEquals("Button v2", d.title)
    }

    @Test fun `StoryMetadataUpdated null title does not clear existing title`() {
        val d = StoryDecision().also {
            it.apply(storyRegistered())
            it.apply(StoryMetadataUpdated(StoryId("s1"), title = null, updatedAtEpochMs = 2_000L))
        }
        assertEquals("Button", d.title)
    }

    @Test fun `StoryKeyUniquenessDecision reports taken key`() {
        val d = StoryKeyUniquenessDecision().also { it.apply(storyRegistered(key = "badge")) }
        assertTrue(d.isTaken("badge"))
        assertFalse(d.isTaken("card"))
    }

    // ── ScenarioDecision ─────────────────────────────────────────────────────

    private fun scenarioRegistered(scId: String = "sc1", stId: String = "s1", key: String = "default") =
        ScenarioRegistered(
            scenarioId = ScenarioId(scId), storyId = StoryId(stId),
            scenarioKey = ScenarioKey(key), title = "Default state",
            type = ScenarioType.STATE, lifecycle = ScenarioLifecycle.ACTIVE,
            registeredAtEpochMs = 1_000L,
        )

    @Test fun `scenario does not exist before any events`() {
        assertFalse(ScenarioDecision().exists)
    }

    @Test fun `scenario exists after ScenarioRegistered`() {
        val d = ScenarioDecision().also { it.apply(scenarioRegistered()) }
        assertTrue(d.exists)
        assertFalse(d.archived)
        assertEquals("s1", d.storyId)
    }

    @Test fun `ScenarioArchived marks scenario archived`() {
        val d = ScenarioDecision().also {
            it.apply(scenarioRegistered())
            it.apply(ScenarioArchived(ScenarioId("sc1"), archivedAtEpochMs = 2_000L))
        }
        assertTrue(d.archived)
    }

    @Test fun `ScenarioKeyUniquenessDecision detects duplicate key within story`() {
        val d = ScenarioKeyUniquenessDecision().also { it.apply(scenarioRegistered(stId = "s1", key = "hover")) }
        assertTrue(d.isTaken(StoryId("s1"), ScenarioKey("hover")))
        assertFalse(d.isTaken(StoryId("s1"), ScenarioKey("focus")))
        assertFalse(d.isTaken(StoryId("s2"), ScenarioKey("hover"))) // different story — not taken
    }

    // ── ControlSchemaDecisionModel ────────────────────────────────────────────

    private fun schemaRegistered(schId: String = "cs1", scId: String = "sc1") = ControlSchemaRegistered(
        schemaId = ControlSchemaId(schId), scenarioId = ScenarioId(scId),
        lifecycle = "ACTIVE", registeredAtEpochMs = 1_000L,
    )

    private fun controlAdded(schId: String = "cs1", ctrlId: String = "c1", key: String = "variant") = ControlAdded(
        schemaId = ControlSchemaId(schId), controlId = ControlId(ctrlId),
        key = ControlKey(key), label = "Variant", type = ControlType.ENUM,
        defaultValue = "primary", bindingRef = "props.variant",
        addedAtEpochMs = 1_000L,
    )

    @Test fun `schema does not exist before any events`() {
        assertFalse(ControlSchemaDecisionModel().exists)
    }

    @Test fun `schema exists after ControlSchemaRegistered`() {
        val d = ControlSchemaDecisionModel().also { it.apply(schemaRegistered()) }
        assertTrue(d.exists)
        assertFalse(d.archived)
        assertEquals("sc1", d.scenarioId)
    }

    @Test fun `ControlAdded creates active control`() {
        val d = ControlSchemaDecisionModel().also {
            it.apply(schemaRegistered())
            it.apply(controlAdded())
        }
        assertEquals(1, d.activeControls().size)
        val c = d.activeControls().first()
        assertEquals("c1", c.controlId)
        assertEquals("variant", c.key)
        assertEquals(ControlType.ENUM, c.type)
    }

    @Test fun `isKeyTaken returns true for existing active control`() {
        val d = ControlSchemaDecisionModel().also {
            it.apply(schemaRegistered())
            it.apply(controlAdded(key = "size"))
        }
        assertTrue(d.isKeyTaken("size"))
        assertFalse(d.isKeyTaken("color"))
    }

    @Test fun `ControlRemoved excludes control from active view (BR-10)`() {
        val d = ControlSchemaDecisionModel().also {
            it.apply(schemaRegistered())
            it.apply(controlAdded())
            it.apply(ControlRemoved(ControlSchemaId("cs1"), ControlId("c1"), removedAtEpochMs = 2_000L))
        }
        assertTrue(d.activeControls().isEmpty())
        assertFalse(d.isActive("c1"))
        assertFalse(d.isKeyTaken("variant")) // removed key is freed
    }

    @Test fun `ControlDefaultChanged updates default value`() {
        val d = ControlSchemaDecisionModel().also {
            it.apply(schemaRegistered())
            it.apply(controlAdded())
            it.apply(ControlDefaultChanged(ControlSchemaId("cs1"), ControlId("c1"), "secondary", 2_000L))
        }
        assertEquals("secondary", d.activeControls().first().defaultValue)
    }

    @Test fun `ControlSchemaArchived marks schema read-only (BR-11)`() {
        val d = ControlSchemaDecisionModel().also {
            it.apply(schemaRegistered())
            it.apply(ControlSchemaArchived(ControlSchemaId("cs1"), archivedAtEpochMs = 2_000L))
        }
        assertTrue(d.archived)
    }

    @Test fun `two controls with distinct keys both appear as active`() {
        val d = ControlSchemaDecisionModel().also {
            it.apply(schemaRegistered())
            it.apply(controlAdded(ctrlId = "c1", key = "variant"))
            it.apply(controlAdded(ctrlId = "c2", key = "size"))
        }
        assertEquals(2, d.activeControls().size)
        assertTrue(d.isKeyTaken("variant"))
        assertTrue(d.isKeyTaken("size"))
    }

    @Test fun `applyAll processes events in order`() {
        val events = listOf(
            schemaRegistered(), controlAdded(),
            ControlDefaultChanged(ControlSchemaId("cs1"), ControlId("c1"), "tertiary", 2_000L),
        )
        val d = ControlSchemaDecisionModel().also { it.applyAll(events) }
        assertEquals("tertiary", d.activeControls().first().defaultValue)
    }
}
