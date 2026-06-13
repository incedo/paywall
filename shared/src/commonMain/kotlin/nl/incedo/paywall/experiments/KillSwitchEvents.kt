package nl.incedo.paywall.experiments

import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent

/** Global tag on all kill-switch events so the variant kill state is cheaply queryable at startup. */
const val KILL_SWITCH_TAG = "variant_kill_switch"

/**
 * NFR-15: variant kill switch — admin event that marks a variant as disabled.
 * Visitors assigned to a killed variant receive full access (revert to open) so
 * a misbehaving paywall can be neutralised without a deployment.
 */
@Serializable
data class VariantKilled(
    val variantName: String,
    val actor: String,
    val killedAtEpochMs: Long,
    override val tags: Set<String> = setOf(KILL_SWITCH_TAG),
) : DomainEvent

/**
 * NFR-15: restores a previously killed variant; visitors are again subject to
 * their normally assigned paywall strategy.
 */
@Serializable
data class VariantRestored(
    val variantName: String,
    val actor: String,
    val restoredAtEpochMs: Long,
    override val tags: Set<String> = setOf(KILL_SWITCH_TAG),
) : DomainEvent
