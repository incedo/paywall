package nl.incedo.paywall.designer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import nl.incedo.paywall.api.ExperimentConfigResponse
import nl.incedo.paywall.api.ExperimentConfigVersionSummary
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.experiments.Variant
import nl.incedo.paywall.access.StrategyConfig
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmPrimaryButton
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmTag
import nl.incedo.paywall.ui.CrmText
import nl.incedo.paywall.ui.CrmTextField

/**
 * ADM-02 MUST: full access-control configuration management — meter limits and periods
 * (MT-10), paywall-type parameters per variant (PW-06), dynamic thresholds (DY-02),
 * experiment definitions and weights (EX-02) — with diff preview and audited publish.
 * ADM-06 SHOULD: config version history + rollback (newest-first list, rollback by version).
 * NFR-15: variant kill-switch section shows killed variants and kill/restore buttons.
 * All writes go through the API (ADM-01: console owns no logic).
 */
@Composable
fun ConfigScreen(
    onLoadConfig: suspend () -> ExperimentConfigResponse?,
    onPublishConfig: suspend (ExperimentDefinition) -> Boolean,
    onLoadHistory: suspend () -> List<ExperimentConfigVersionSummary>,
    onRollback: suspend (Int) -> Boolean,
    onLoadKilledVariants: suspend () -> List<String>,
    onKillVariant: suspend (String) -> Boolean,
    onRestoreVariant: suspend (String) -> Boolean,
    statusMessage: String?,
) {
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf<ExperimentConfigResponse?>(null) }
    var configHistory by remember { mutableStateOf<List<ExperimentConfigVersionSummary>>(emptyList()) }
    var killedVariants by remember { mutableStateOf<List<String>>(emptyList()) }
    var status by remember { mutableStateOf(statusMessage ?: "Loading config…") }

    // ── editable state (all as strings for text-field binding) ────────────────
    var editMaxGrantTtlDays by remember { mutableStateOf("") }
    // Per-variant maps keyed by variant name
    var editWeights by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // MT-10: meter limits + period type (Metered strategy)
    var editLimits by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var editRegisteredLimits by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var editPeriodTypes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    // DY-02: dynamic thresholds (Dynamic strategy)
    var editTSoft by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var editTHard by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    fun initEdits(cfg: ExperimentConfigResponse) {
        val exp = cfg.experiment
        editMaxGrantTtlDays = exp.maxGrantTtlDays.toString()
        editWeights = exp.variants.associate { it.name to it.weight.toString() }
        editLimits = exp.variants.associate { v ->
            v.name to ((v.strategy as? StrategyConfig.Metered)?.limit?.toString() ?: "")
        }
        editRegisteredLimits = exp.variants.associate { v ->
            v.name to ((v.strategy as? StrategyConfig.Metered)?.registeredLimit?.toString() ?: "")
        }
        editPeriodTypes = exp.variants.associate { v ->
            v.name to ((v.strategy as? StrategyConfig.Metered)?.periodType ?: "")
        }
        editTSoft = exp.variants.associate { v ->
            v.name to ((v.strategy as? StrategyConfig.Dynamic)?.tSoft?.toString() ?: "")
        }
        editTHard = exp.variants.associate { v ->
            v.name to ((v.strategy as? StrategyConfig.Dynamic)?.tHard?.toString() ?: "")
        }
    }

    LaunchedEffect(Unit) {
        config = runCatching { onLoadConfig() }.getOrElse {
            status = "Backend unreachable — cannot load config"
            null
        }
        config?.let { initEdits(it) }
        if (config != null) {
            status = if (config!!.isDefault) "Showing default config (no override published)" else "Config loaded"
        }
        killedVariants = runCatching { onLoadKilledVariants() }.getOrDefault(emptyList())
        configHistory = runCatching { onLoadHistory() }.getOrDefault(emptyList())
    }

    /** Build the updated ExperimentDefinition from all edit state. */
    fun buildUpdated(exp: ExperimentDefinition): ExperimentDefinition {
        val ttl = editMaxGrantTtlDays.toIntOrNull()?.coerceAtLeast(1) ?: exp.maxGrantTtlDays
        val newVariants: List<Variant> = exp.variants.map { v ->
            val newWeight = editWeights[v.name]?.toIntOrNull()?.coerceAtLeast(1) ?: v.weight
            val newStrategy = when (val s = v.strategy) {
                is StrategyConfig.Metered -> s.copy(
                    limit = editLimits[v.name]?.toIntOrNull()?.coerceAtLeast(1) ?: s.limit,
                    registeredLimit = editRegisteredLimits[v.name]?.toIntOrNull()?.coerceAtLeast(1)
                        ?: s.registeredLimit,
                    periodType = editPeriodTypes[v.name]?.takeIf { it.isNotBlank() } ?: s.periodType,
                )
                is StrategyConfig.Dynamic -> s.copy(
                    tSoft = editTSoft[v.name]?.toIntOrNull()?.coerceIn(0, 100) ?: s.tSoft,
                    tHard = editTHard[v.name]?.toIntOrNull()?.coerceIn(0, 100) ?: s.tHard,
                )
                else -> s
            }
            v.copy(weight = newWeight, strategy = newStrategy)
        }
        return exp.copy(maxGrantTtlDays = ttl, variants = newVariants)
    }

    /** Diff summary: list of "field: old → new" for changed fields. */
    fun diffLines(exp: ExperimentDefinition): List<String> {
        val lines = mutableListOf<String>()
        val newExp = buildUpdated(exp)
        if (newExp.maxGrantTtlDays != exp.maxGrantTtlDays)
            lines += "maxGrantTtlDays: ${exp.maxGrantTtlDays} → ${newExp.maxGrantTtlDays}"
        exp.variants.zip(newExp.variants).forEach { (old, new) ->
            if (old.weight != new.weight) lines += "${old.name} weight: ${old.weight} → ${new.weight}"
            val os = old.strategy; val ns = new.strategy
            if (os is StrategyConfig.Metered && ns is StrategyConfig.Metered) {
                if (os.limit != ns.limit) lines += "${old.name} limit: ${os.limit} → ${ns.limit}"
                if (os.registeredLimit != ns.registeredLimit)
                    lines += "${old.name} registeredLimit: ${os.registeredLimit} → ${ns.registeredLimit}"
                if (os.periodType != ns.periodType)
                    lines += "${old.name} periodType: ${os.periodType} → ${ns.periodType}"
            }
            if (os is StrategyConfig.Dynamic && ns is StrategyConfig.Dynamic) {
                if (os.tSoft != ns.tSoft) lines += "${old.name} tSoft: ${os.tSoft} → ${ns.tSoft}"
                if (os.tHard != ns.tHard) lines += "${old.name} tHard: ${os.tHard} → ${ns.tHard}"
            }
        }
        return lines
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(CrmTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xxs)) {
            CrmText("Experiment config", style = CrmTheme.typography.h1, color = CrmTheme.colors.onBackground)
            CrmText(status, style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurfaceVariant)
        }

        config?.let { cfg ->
            val exp = cfg.experiment

            // ── experiment metadata ─────────────────────────────────────────────
            CrmCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                ) {
                    CrmText("Experiment", style = CrmTheme.typography.h3)
                    CrmDivider()
                    ConfigRow("ID", exp.id.value)
                    ConfigRow("Name", exp.name)
                    cfg.publishedBy?.let { ConfigRow("Published by", it) }
                    if (cfg.isDefault) {
                        CrmText(
                            "Using default config — no override has been published via the API.",
                            style = CrmTheme.typography.caption,
                            color = CrmTheme.colors.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── FGA-02 / ADM-02: max grant TTL ─────────────────────────────────
            CrmCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                ) {
                    CrmText("Grant TTL ceiling (FGA-02)", style = CrmTheme.typography.h3)
                    CrmDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CrmText(
                            "maxGrantTtlDays",
                            style = CrmTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        CrmTextField(
                            "days",
                            editMaxGrantTtlDays,
                            { editMaxGrantTtlDays = it },
                            modifier = Modifier.width(80.dp),
                        )
                    }
                }
            }

            // ── EX-02 / MT-10 / PW-06 / DY-02: per-variant parameters ─────────
            exp.variants.forEach { v ->
                CrmCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                    ) {
                        CrmText(
                            "${v.name}${if (v.isControl) " (control)" else ""} — ${strategyLabel(v.strategy)}",
                            style = CrmTheme.typography.h3,
                        )
                        CrmDivider()

                        // EX-02: traffic weight
                        EditRow(
                            "Traffic weight (EX-02)",
                            editWeights[v.name] ?: v.weight.toString(),
                        ) { editWeights = editWeights + (v.name to it) }

                        // MT-10: metered strategy parameters
                        if (v.strategy is StrategyConfig.Metered) {
                            EditRow(
                                "Meter limit (MT-10)",
                                editLimits[v.name] ?: "",
                            ) { editLimits = editLimits + (v.name to it) }
                            EditRow(
                                "Registered limit (PW-25, blank = same)",
                                editRegisteredLimits[v.name] ?: "",
                            ) { editRegisteredLimits = editRegisteredLimits + (v.name to it) }
                            EditRow(
                                "Period type (MT-10)",
                                editPeriodTypes[v.name] ?: "",
                            ) { editPeriodTypes = editPeriodTypes + (v.name to it) }
                        }

                        // DY-02: dynamic threshold parameters
                        if (v.strategy is StrategyConfig.Dynamic) {
                            EditRow(
                                "tSoft — score threshold for soft gate (DY-02)",
                                editTSoft[v.name] ?: "",
                            ) { editTSoft = editTSoft + (v.name to it) }
                            EditRow(
                                "tHard — score threshold for hard gate (DY-02)",
                                editTHard[v.name] ?: "",
                            ) { editTHard = editTHard + (v.name to it) }
                        }

                        if (v.registrationWall) {
                            CrmText(
                                "Registration wall active (PW-50)",
                                style = CrmTheme.typography.caption,
                                color = CrmTheme.colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ── ADM-06: config version history + rollback ──────────────────────
            if (configHistory.isNotEmpty()) {
                CrmCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                    ) {
                        CrmText("Config history (ADM-06)", style = CrmTheme.typography.h3)
                        CrmText(
                            "Roll back to any previous config — creates a new publish event from the old definition.",
                            style = CrmTheme.typography.caption,
                            color = CrmTheme.colors.onSurfaceVariant,
                        )
                        CrmDivider()
                        configHistory.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    CrmText("v${entry.version} — ${entry.variantNames}", style = CrmTheme.typography.bodySmall)
                                    CrmText(
                                        "by ${entry.actor}",
                                        style = CrmTheme.typography.caption,
                                        color = CrmTheme.colors.onSurfaceVariant,
                                    )
                                }
                                CrmSecondaryButton("Roll back") {
                                    scope.launch {
                                        val ok = onRollback(entry.version)
                                        if (ok) {
                                            runCatching { onLoadConfig() }.getOrNull()?.let { fresh ->
                                                config = fresh
                                                initEdits(fresh)
                                            }
                                            configHistory = runCatching { onLoadHistory() }.getOrDefault(configHistory)
                                            status = "Rolled back to v${entry.version} (ADM-06)"
                                        } else {
                                            status = "Rollback failed"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── NFR-15: variant kill switches ──────────────────────────────────
            CrmCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                ) {
                    CrmText("Variant kill switches (NFR-15)", style = CrmTheme.typography.h3)
                    CrmText(
                        "Kill any variant instantly — reverts traffic to control. Restoring re-enables it.",
                        style = CrmTheme.typography.caption,
                        color = CrmTheme.colors.onSurfaceVariant,
                    )
                    CrmDivider()
                    exp.variants.forEach { v ->
                        val killed = v.name in killedVariants
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CrmText(v.name, style = CrmTheme.typography.bodySmall)
                                if (killed) CrmTag("KILLED", CrmTheme.colors.error, CrmTheme.colors.onError)
                            }
                            if (killed) {
                                CrmSecondaryButton("Restore") {
                                    scope.launch {
                                        if (onRestoreVariant(v.name)) {
                                            killedVariants = killedVariants - v.name
                                            status = "${v.name} restored"
                                        } else {
                                            status = "Restore failed"
                                        }
                                    }
                                }
                            } else {
                                CrmSecondaryButton("Kill") {
                                    scope.launch {
                                        if (onKillVariant(v.name)) {
                                            killedVariants = killedVariants + v.name
                                            status = "${v.name} killed — traffic to control"
                                        } else {
                                            status = "Kill failed"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── diff preview + audited publish (ADM-02) ────────────────────────
            CrmCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                ) {
                    CrmText("Publish (ADM-02)", style = CrmTheme.typography.h3)
                    CrmText(
                        "Changes are versioned and audited. Publishing replaces the active config.",
                        style = CrmTheme.typography.caption,
                        color = CrmTheme.colors.onSurfaceVariant,
                    )
                    CrmDivider()

                    // Diff preview (ADM-02: diff preview before publish)
                    val diff = diffLines(exp)
                    if (diff.isEmpty()) {
                        CrmText(
                            "No changes from current config.",
                            style = CrmTheme.typography.caption,
                            color = CrmTheme.colors.onSurfaceVariant,
                        )
                    } else {
                        CrmText("Changes:", style = CrmTheme.typography.bodySmall)
                        diff.forEach { line ->
                            CrmText(
                                "  · $line",
                                style = CrmTheme.typography.caption,
                                color = CrmTheme.colors.onSurfaceVariant,
                            )
                        }
                    }

                    CrmPrimaryButton("Publish config") {
                        scope.launch {
                            val ok = onPublishConfig(buildUpdated(exp))
                            if (ok) {
                                // Reload so diff resets to zero and status reflects new baseline
                                runCatching { onLoadConfig() }.getOrNull()?.let { fresh ->
                                    config = fresh
                                    initEdits(fresh)
                                    status = "Config published (ADM-02)"
                                }
                            } else {
                                status = "Publish failed"
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrmText(label, style = CrmTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        CrmTextField(
            "",
            value,
            onValueChange,
            modifier = Modifier.width(120.dp),
        )
    }
}

@Composable
private fun ConfigRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        CrmText(label, style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurfaceVariant)
        CrmText(value, style = CrmTheme.typography.bodySmall)
    }
}

private fun strategyLabel(strategy: StrategyConfig): String = when (strategy) {
    is StrategyConfig.Hard -> "Hard paywall"
    is StrategyConfig.Freemium -> "Freemium"
    is StrategyConfig.Metered ->
        "Metered — ${strategy.limit} articles/${strategy.periodType}"
    is StrategyConfig.Dynamic ->
        "Dynamic — soft ≥ ${strategy.tSoft}, hard ≥ ${strategy.tHard}"
}
