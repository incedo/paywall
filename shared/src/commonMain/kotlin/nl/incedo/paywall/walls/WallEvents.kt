package nl.incedo.paywall.walls

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent
import nl.incedo.paywall.core.WallId

/**
 * Wall definitions (ADM-11): the visual editor's output is structured
 * content, never code (GAP-10). Events carry the wall tag for per-wall
 * queries plus the catalog tag for the overview projection (admin-rate
 * writes — no contention concern).
 */
@Serializable
data class WallConfig(
    val name: String,
    /** hard | metered | freemium | dynamic (Doc 1). */
    val wallType: String,
    val title: String,
    val body: String,
    val primaryCta: String,
    val secondaryCta: String,
    val channels: Set<String> = setOf("web"),
    /** ADM-10: brand this wall design belongs to; null = unbranded (default brand). */
    val brandId: String? = null,
)

@Serializable
data class WallCreated(
    val wallId: WallId,
    val config: WallConfig,
    val actor: String,
    override val tags: Set<String> = setOf("wall:${wallId.value}", "walls"),
) : DomainEvent

@Serializable
data class WallConfigChanged(
    val wallId: WallId,
    val config: WallConfig,
    val actor: String,
    override val tags: Set<String> = setOf("wall:${wallId.value}", "walls"),
) : DomainEvent

/** Draft → published (ADM-06: every change is a new version; rollback = re-apply an old config). */
@Serializable
data class WallPublished(
    val wallId: WallId,
    val actor: String,
    override val tags: Set<String> = setOf("wall:${wallId.value}", "walls"),
) : DomainEvent
