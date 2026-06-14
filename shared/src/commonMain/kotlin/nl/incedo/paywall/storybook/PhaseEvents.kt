package nl.incedo.paywall.storybook

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable
import nl.incedo.paywall.core.DomainEvent

// ── Value objects ─────────────────────────────────────────────────────────────

@Serializable @JvmInline value class PhaseId(val value: String)
@Serializable @JvmInline value class PhaseKey(val value: String)
@Serializable @JvmInline value class PhaseName(val value: String)
@Serializable @JvmInline value class CapabilityKey(val value: String)

enum class PhaseLifecycle { PLANNED, ACTIVE, SATISFIED, SUPERSEDED }

// ── Tag helpers ───────────────────────────────────────────────────────────────

fun phaseTag(id: PhaseId) = "phase:${id.value}"
fun phaseKeyTag(key: PhaseKey) = "phase-key:${key.value}"

// ── Events ────────────────────────────────────────────────────────────────────

/** Registers a new phase in the roadmap. BR-2: key must be unique. */
@Serializable
data class PhaseRegistered(
    val phaseId: PhaseId,
    val phaseKey: PhaseKey,
    val phaseName: PhaseName,
    val phaseOrder: Int,
    val lifecycle: PhaseLifecycle = PhaseLifecycle.PLANNED,
    val registeredAtEpochMs: Long,
    override val tags: Set<String> = setOf(phaseTag(phaseId), phaseKeyTag(phaseKey), "phases"),
) : DomainEvent

/** Assigns a capability to a phase. BR-3: capability may not be added twice. */
@Serializable
data class CapabilityAddedToPhase(
    val phaseId: PhaseId,
    val capabilityKey: CapabilityKey,
    val title: String,
    val description: String? = null,
    val addedAtEpochMs: Long,
    override val tags: Set<String> = setOf(phaseTag(phaseId), "capability:${capabilityKey.value}", "phases"),
) : DomainEvent

/** Transitions phase from PLANNED → ACTIVE. BR-4: only from PLANNED. */
@Serializable
data class PhaseActivated(
    val phaseId: PhaseId,
    val activatedAtEpochMs: Long,
    override val tags: Set<String> = setOf(phaseTag(phaseId), "phase-status:active", "phases"),
) : DomainEvent

/** Marks phase as SATISFIED (all mandatory capabilities delivered). BR-5. */
@Serializable
data class PhaseSatisfied(
    val phaseId: PhaseId,
    val satisfiedAtEpochMs: Long,
    override val tags: Set<String> = setOf(phaseTag(phaseId), "phase-status:satisfied", "phases"),
) : DomainEvent

/** Supersedes a phase; prevents re-activation. BR-6. */
@Serializable
data class PhaseSuperseded(
    val phaseId: PhaseId,
    val supersededAtEpochMs: Long,
    override val tags: Set<String> = setOf(phaseTag(phaseId), "phase-status:superseded", "phases"),
) : DomainEvent
