package nl.incedo.paywall.storybook

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for DecoratorDecision and DecoratorKeyUniquenessDecision.
 * Mirrors testing.md §10a for the Decorator BC.
 */
class DecoratorDecisionTest {

    private fun registered(
        id: String = "d1",
        key: String = "theme/default-dark",
        type: DecoratorType = DecoratorType.THEME,
        scope: DecoratorScope = DecoratorScope.GLOBAL,
        priority: Int = 10,
    ) = DecoratorRegistered(
        decoratorId = DecoratorId(id),
        decoratorKey = DecoratorKey(key),
        title = "Default Dark",
        type = type,
        renderRef = "decorators.theme.defaultDark",
        priority = priority,
        scope = scope,
        lifecycle = DecoratorLifecycle.ACTIVE,
        registeredAtEpochMs = 1_000L,
    )

    @Test fun `decorator does not exist before any events`() {
        assertFalse(DecoratorDecision().exists)
    }

    @Test fun `decorator exists after DecoratorRegistered`() {
        val d = DecoratorDecision().also { it.apply(registered()) }
        assertTrue(d.exists)
        assertFalse(d.archived)
        assertEquals("theme/default-dark", d.decoratorKey)
        assertEquals("Default Dark", d.title)
        assertEquals(DecoratorType.THEME, d.type)
        assertEquals(DecoratorScope.GLOBAL, d.scope)
        assertEquals(10, d.priority)
        assertEquals(DecoratorLifecycle.ACTIVE, d.lifecycle)
    }

    @Test fun `DecoratorPriorityChanged updates priority`() {
        val d = DecoratorDecision().also {
            it.apply(registered())
            it.apply(DecoratorPriorityChanged(DecoratorId("d1"), 20, 2_000L))
        }
        assertEquals(20, d.priority)
    }

    @Test fun `DecoratorMetadataUpdated patches title`() {
        val d = DecoratorDecision().also {
            it.apply(registered())
            it.apply(DecoratorMetadataUpdated(DecoratorId("d1"), title = "Dark Theme v2", updatedAtEpochMs = 2_000L))
        }
        assertEquals("Dark Theme v2", d.title)
    }

    @Test fun `DecoratorMetadataUpdated with null title keeps existing title`() {
        val d = DecoratorDecision().also {
            it.apply(registered())
            it.apply(DecoratorMetadataUpdated(DecoratorId("d1"), title = null, updatedAtEpochMs = 2_000L))
        }
        assertEquals("Default Dark", d.title)
    }

    @Test fun `DecoratorArchived sets archived and lifecycle (BR-10)`() {
        val d = DecoratorDecision().also {
            it.apply(registered())
            it.apply(DecoratorArchived(DecoratorId("d1"), 2_000L))
        }
        assertTrue(d.archived)
        assertEquals(DecoratorLifecycle.ARCHIVED, d.lifecycle)
    }

    @Test fun `applyAll processes events in sequence`() {
        val events = listOf(
            registered(priority = 5),
            DecoratorPriorityChanged(DecoratorId("d1"), 15, 2_000L),
            DecoratorMetadataUpdated(DecoratorId("d1"), title = "Updated", updatedAtEpochMs = 3_000L),
        )
        val d = DecoratorDecision().also { it.applyAll(events) }
        assertEquals(15, d.priority)
        assertEquals("Updated", d.title)
    }

    @Test fun `DecoratorKeyUniquenessDecision tracks registered keys (BR-3)`() {
        val d = DecoratorKeyUniquenessDecision().also { it.apply(registered(key = "theme/dark")) }
        assertTrue(d.isTaken("theme/dark"))
        assertFalse(d.isTaken("theme/light"))
    }

    @Test fun `scope variants are stored correctly`() {
        val global = DecoratorDecision().also { it.apply(registered(scope = DecoratorScope.GLOBAL)) }
        val story = DecoratorDecision().also { it.apply(registered(scope = DecoratorScope.STORY)) }
        val scenario = DecoratorDecision().also { it.apply(registered(scope = DecoratorScope.SCENARIO)) }
        assertEquals(DecoratorScope.GLOBAL, global.scope)
        assertEquals(DecoratorScope.STORY, story.scope)
        assertEquals(DecoratorScope.SCENARIO, scenario.scope)
    }

    @Test fun `all decorator types are representable`() {
        DecoratorType.entries.forEach { t ->
            val d = DecoratorDecision().also { it.apply(registered(type = t)) }
            assertEquals(t, d.type)
        }
    }
}
