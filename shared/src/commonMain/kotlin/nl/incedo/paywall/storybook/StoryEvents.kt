package nl.incedo.paywall.storybook

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent

// ── Value objects ─────────────────────────────────────────────────────────────

@Serializable @JvmInline value class StoryId(val value: String)
@Serializable @JvmInline value class StoryKey(val value: String)

enum class StoryType { COMPONENT, SCREEN, LAYOUT, TOKEN, FOUNDATION }
enum class StoryLifecycle { ACTIVE, EXPERIMENTAL, DEPRECATED, ARCHIVED }

// ── Tag helpers ───────────────────────────────────────────────────────────────

fun storyTag(id: StoryId) = "story:${id.value}"
fun storyKeyTag(key: StoryKey) = "story-key:${key.value}"
fun storyGroupTag(groupId: String) = "story-group:$groupId"

// ── Events ────────────────────────────────────────────────────────────────────

/** First event for a story; establishes its key and classification. BR-2/4. */
@Serializable
data class StoryRegistered(
    val storyId: StoryId,
    val storyKey: StoryKey,
    val title: String,
    val type: StoryType,
    val groupId: String,
    val owner: String,
    val renderContractRef: String,
    val lifecycle: StoryLifecycle = StoryLifecycle.ACTIVE,
    val coverageScope: String = "ALL",
    val registeredAtEpochMs: Long,
    override val tags: Set<String> = setOf(storyTag(storyId), storyKeyTag(storyKey), storyGroupTag(groupId), "stories"),
) : DomainEvent

/** Story moved to archived lifecycle; no further mutations allowed. BR-11. */
@Serializable
data class StoryArchived(
    val storyId: StoryId,
    val archivedAtEpochMs: Long,
    override val tags: Set<String> = setOf(storyTag(storyId), "stories"),
) : DomainEvent

/** Title, owner, or type metadata changed. BR-5: mandatory fields may not be cleared. */
@Serializable
data class StoryMetadataUpdated(
    val storyId: StoryId,
    val title: String? = null,
    val description: String? = null,
    val owner: String? = null,
    val updatedAtEpochMs: Long,
    override val tags: Set<String> = setOf(storyTag(storyId), "stories"),
) : DomainEvent
