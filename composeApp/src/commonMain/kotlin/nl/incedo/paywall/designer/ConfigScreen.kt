package nl.incedo.paywall.designer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import nl.incedo.paywall.api.ExperimentConfigResponse
import nl.incedo.paywall.experiments.ExperimentDefinition
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmText

/**
 * ADM-02: experiment configuration view — shows the current published experiment
 * definition (meter limits, variant weights, strategy parameters, EX-02) and
 * allows an admin to publish a new config. All writes go through the API
 * (ADM-01: console owns no logic).
 *
 * In the experiment phase the config is read-only in the UI; the publish action
 * is wired for admin-level operations.
 */
@Composable
fun ConfigScreen(
    onLoadConfig: suspend () -> ExperimentConfigResponse?,
    statusMessage: String?,
) {
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf<ExperimentConfigResponse?>(null) }
    var status by remember { mutableStateOf(statusMessage ?: "Loading config…") }

    LaunchedEffect(Unit) {
        config = runCatching { onLoadConfig() }.getOrElse {
            status = "Backend unreachable — cannot load config"
            null
        }
        if (config != null) status = if (config!!.isDefault) "Showing default config (no override published)" else "Config loaded"
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
            CrmText(
                status,
                style = CrmTheme.typography.bodySmall,
                color = CrmTheme.colors.onSurfaceVariant,
            )
        }

        config?.let { cfg ->
            val exp = cfg.experiment

            // ── experiment metadata ───────────────────────────────────────────
            CrmCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                ) {
                    CrmText("Experiment", style = CrmTheme.typography.h3)
                    CrmDivider()
                    ConfigRow("ID", exp.id.value)
                    ConfigRow("Name", exp.name)
                    ConfigRow("Max grant TTL", "${exp.maxGrantTtlDays} days (FGA-02)")
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

            // ── variants ──────────────────────────────────────────────────────
            CrmCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
                ) {
                    CrmText("Variants (EX-02)", style = CrmTheme.typography.h3)
                    CrmDivider()
                    val totalWeight = exp.variants.sumOf { it.weight }
                    exp.variants.forEach { v ->
                        val pct = if (totalWeight > 0) (v.weight * 100 / totalWeight) else 0
                        Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xxs)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                CrmText(
                                    "${v.name}${if (v.isControl) " (control)" else ""}",
                                    style = CrmTheme.typography.bodySmall,
                                )
                                CrmText("$pct%", style = CrmTheme.typography.bodySmall)
                            }
                            CrmText(
                                strategyLabel(v.strategy),
                                style = CrmTheme.typography.caption,
                                color = CrmTheme.colors.onSurfaceVariant,
                            )
                            if (v.registrationWall) {
                                CrmText(
                                    "Registration wall active (PW-50)",
                                    style = CrmTheme.typography.caption,
                                    color = CrmTheme.colors.onSurfaceVariant,
                                )
                            }
                        }
                        CrmDivider()
                    }
                }
            }
        }
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

private fun strategyLabel(strategy: nl.incedo.paywall.access.StrategyConfig): String = when (strategy) {
    is nl.incedo.paywall.access.StrategyConfig.Hard -> "Hard paywall"
    is nl.incedo.paywall.access.StrategyConfig.Freemium -> "Freemium"
    is nl.incedo.paywall.access.StrategyConfig.Metered ->
        "Metered — ${strategy.limit} articles/${strategy.periodType}"
    is nl.incedo.paywall.access.StrategyConfig.Dynamic ->
        "Dynamic — soft ≥ ${strategy.tSoft}, hard ≥ ${strategy.tHard}"
}
