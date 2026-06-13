package nl.incedo.paywall.designer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import nl.incedo.paywall.api.WallVersionSummary
import nl.incedo.paywall.model.Channel
import nl.incedo.paywall.model.WallDefinition
import nl.incedo.paywall.model.WallType
import nl.incedo.paywall.model.defaultCopyFor
import nl.incedo.paywall.walls.WallCopy
import nl.incedo.paywall.screens.ConsentStepScreen
import nl.incedo.paywall.screens.ContentGateWall
import nl.incedo.paywall.screens.MeterNudgeBanner
import nl.incedo.paywall.screens.PricingWall
import nl.incedo.paywall.screens.RegistrationWallScreen
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmCard
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmPrimaryButton
import nl.incedo.paywall.ui.CrmSecondaryButton
import nl.incedo.paywall.ui.CrmSegmentedToggle
import nl.incedo.paywall.ui.CrmTag
import nl.incedo.paywall.ui.CrmText
import nl.incedo.paywall.ui.CrmTextButton
import nl.incedo.paywall.ui.CrmTextField
import nl.incedo.paywall.ui.CrmToggleChip

/**
 * Visual wall editor, workspace variant (ADM-11/12 · design "Wall Designer", variant A):
 * configuration left, live preview rendered from the structured [WallDefinition] centre,
 * targeting and publishing right.
 */
@Composable
fun WallDesignerScreen(
    wallName: String,
    wallStatus: String,
    definition: WallDefinition,
    onDefinitionChange: (WallDefinition) -> Unit,
    onBack: () -> Unit,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
    onLoadHistory: suspend () -> List<WallVersionSummary> = { emptyList() },
    onRollback: suspend (Int) -> Unit = {},
) {
    var mobilePreview by remember { mutableStateOf(false) }
    var visitorContext by remember { mutableStateOf(0) } // 0=anonymous 1=registered 2=subscriber
    var gateContext by remember { mutableStateOf(0) }    // 0=paywall 1=registration 2=consent 3=nudge
    var history by remember { mutableStateOf<List<WallVersionSummary>>(emptyList()) }

    LaunchedEffect(wallName) { history = runCatching { onLoadHistory() }.getOrDefault(emptyList()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        DesignerToolbar(wallName, wallStatus, onBack, onSaveDraft, onPublish)
        CrmDivider()
        Row(modifier = Modifier.fillMaxWidth()) {
            ConfigPanel(
                definition = definition,
                onDefinitionChange = onDefinitionChange,
                modifier = Modifier
                    .width(320.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(CrmTheme.spacing.xl),
            )
            PreviewPanel(
                definition = definition,
                mobilePreview = mobilePreview,
                onPreviewDeviceChange = { mobilePreview = it },
                visitorContext = visitorContext,
                onVisitorContextChange = { visitorContext = it },
                gateContext = gateContext,
                onGateContextChange = { gateContext = it },
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(CrmTheme.spacing.xl),
            )
            PublishPanel(
                definition = definition,
                onDefinitionChange = onDefinitionChange,
                history = history,
                onRollback = onRollback,
                modifier = Modifier
                    .width(300.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(CrmTheme.spacing.xl),
            )
        }
    }
}

@Composable
private fun DesignerToolbar(
    wallName: String,
    wallStatus: String,
    onBack: () -> Unit,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CrmTheme.colors.surface)
            .padding(horizontal = CrmTheme.spacing.xl, vertical = CrmTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
    ) {
        CrmTextButton("Walls", onClick = onBack)
        CrmText("/ $wallName", style = CrmTheme.typography.h3)
        CrmTag(wallStatus, CrmTheme.colors.surfaceVariant, CrmTheme.colors.onSurfaceVariant)
        Row(modifier = Modifier.weight(1f)) {}
        CrmSecondaryButton("Save draft", onClick = onSaveDraft)
        CrmPrimaryButton("Publish", onClick = onPublish)
    }
}

@Composable
private fun ConfigPanel(
    definition: WallDefinition,
    onDefinitionChange: (WallDefinition) -> Unit,
    modifier: Modifier = Modifier,
) {
    var newLocale by remember { mutableStateOf("") }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg)) {
        CrmText("Wall type", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
            WallType.entries.forEach { type ->
                CrmToggleChip(
                    label = type.label,
                    selected = definition.type == type,
                    onClick = {
                        // PW-11: apply type-appropriate default copy when switching types,
                        // but only if the title still matches the old type's default (not customised).
                        val oldDefault = defaultCopyFor[definition.type]
                        val newDefault = defaultCopyFor[type]
                        val updatedDefinition = if (newDefault != null && oldDefault != null &&
                            definition.title == oldDefault.title && definition.body == oldDefault.body) {
                            definition.copy(type = type, title = newDefault.title, body = newDefault.body)
                        } else {
                            definition.copy(type = type)
                        }
                        onDefinitionChange(updatedDefinition)
                    },
                )
            }
        }

        CrmText("Content", style = CrmTheme.typography.h3)
        CrmTextField("Title", definition.title, { onDefinitionChange(definition.copy(title = it)) })
        CrmTextField("Body", definition.body, { onDefinitionChange(definition.copy(body = it)) }, singleLine = false)
        CrmTextField("Primary CTA", definition.primaryCta, { onDefinitionChange(definition.copy(primaryCta = it)) })
        CrmTextField("Secondary CTA", definition.secondaryCta, { onDefinitionChange(definition.copy(secondaryCta = it)) })

        CrmText("Theme", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
        CrmSegmentedToggle(
            options = listOf("Light", "Dark"),
            selectedIndex = if (definition.darkPreview) 1 else 0,
            onSelect = { onDefinitionChange(definition.copy(darkPreview = it == 1)) },
        )

        CrmText("Gate options", style = CrmTheme.typography.label, color = CrmTheme.colors.onSurfaceVariant)
        CrmSegmentedToggle(
            options = listOf("Registration off", "Registration on"),
            selectedIndex = if (definition.registrationWall) 1 else 0,
            onSelect = { onDefinitionChange(definition.copy(registrationWall = it == 1)) },
        )
        CrmSegmentedToggle(
            options = listOf("Consent off", "Consent on"),
            selectedIndex = if (definition.requireConsentStep) 1 else 0,
            onSelect = { onDefinitionChange(definition.copy(requireConsentStep = it == 1)) },
        )

        // ADM-15: per-locale copy overrides
        CrmText("Locale overrides", style = CrmTheme.typography.h3)
        CrmText(
            "Override copy for specific locales (BCP-47 tags, e.g. nl-NL).",
            style = CrmTheme.typography.caption,
            color = CrmTheme.colors.onSurfaceVariant,
        )
        definition.translations.forEach { (locale, copy) ->
            CrmCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CrmText(locale, style = CrmTheme.typography.label)
                        CrmTextButton("Remove") {
                            onDefinitionChange(definition.copy(translations = definition.translations - locale))
                        }
                    }
                    CrmTextField(
                        "Title",
                        copy.title ?: "",
                        { onDefinitionChange(definition.copy(translations = definition.translations + (locale to copy.copy(title = it.ifBlank { null })))) },
                    )
                    CrmTextField(
                        "Body",
                        copy.body ?: "",
                        { onDefinitionChange(definition.copy(translations = definition.translations + (locale to copy.copy(body = it.ifBlank { null })))) },
                        singleLine = false,
                    )
                    CrmTextField(
                        "Primary CTA",
                        copy.primaryCta ?: "",
                        { onDefinitionChange(definition.copy(translations = definition.translations + (locale to copy.copy(primaryCta = it.ifBlank { null })))) },
                    )
                    CrmTextField(
                        "Secondary CTA",
                        copy.secondaryCta ?: "",
                        { onDefinitionChange(definition.copy(translations = definition.translations + (locale to copy.copy(secondaryCta = it.ifBlank { null })))) },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrmTextField(
                "Add locale (e.g. nl-NL)",
                newLocale,
                { newLocale = it },
                modifier = Modifier.weight(1f),
            )
            CrmSecondaryButton("Add") {
                val tag = newLocale.trim()
                if (tag.isNotEmpty() && tag !in definition.translations) {
                    onDefinitionChange(definition.copy(translations = definition.translations + (tag to WallCopy())))
                    newLocale = ""
                }
            }
        }
    }
}

/**
 * ADM-12: live preview in all gate contexts × visitor states × breakpoints.
 * gateContext: 0=paywall 1=registration-wall 2=consent-step 3=meter-nudge
 * visitorContext: 0=anonymous 1=registered 2=subscriber
 */
@Composable
private fun PreviewPanel(
    definition: WallDefinition,
    mobilePreview: Boolean,
    onPreviewDeviceChange: (Boolean) -> Unit,
    visitorContext: Int,
    onVisitorContextChange: (Int) -> Unit,
    gateContext: Int,
    onGateContextChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visitorLabels = listOf("Anonymous", "Registered", "Subscriber")
    val gateContextLabels = listOf("Paywall", "Registration wall", "Consent step", "Meter nudge")

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── top bar: device + visitor state ──────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrmText(
                "Preview · ${if (mobilePreview) "Mobile" else "Web"} · " +
                    "${if (definition.darkPreview) "Dark" else "Light"} · ${definition.type.label}",
                style = CrmTheme.typography.label,
                color = CrmTheme.colors.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm)) {
                CrmSegmentedToggle(
                    options = listOf("Web", "Mobile"),
                    selectedIndex = if (mobilePreview) 1 else 0,
                    onSelect = { onPreviewDeviceChange(it == 1) },
                )
                CrmSegmentedToggle(
                    options = visitorLabels,
                    selectedIndex = visitorContext,
                    onSelect = onVisitorContextChange,
                )
            }
        }

        // ── gate context selector ─────────────────────────────────────────────
        CrmSegmentedToggle(
            options = gateContextLabels,
            selectedIndex = gateContext,
            onSelect = onGateContextChange,
        )

        // ── preview frame ─────────────────────────────────────────────────────
        Box(modifier = Modifier.widthIn(max = if (mobilePreview) 400.dp else 880.dp)) {
            CrmTheme(darkTheme = definition.darkPreview) {
                CrmCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CrmTheme.shapes.lg)
                            .background(CrmTheme.colors.background),
                    ) {
                        when {
                            // Subscriber always sees full content — gate is not shown
                            visitorContext == 2 -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.xxl),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    CrmText(
                                        "✓ Full content — subscriber",
                                        style = CrmTheme.typography.h3,
                                        color = CrmTheme.colors.onSurfaceVariant,
                                    )
                                }
                            }
                            // Consent step (AC-14) — shown before gate when enabled
                            gateContext == 2 -> ConsentStepScreen()
                            // Registration wall (PW-50) — anonymous visitors only
                            gateContext == 1 && visitorContext == 0 -> RegistrationWallScreen()
                            gateContext == 1 -> {
                                // Registered visitor: registration wall is skipped; show paywall
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(CrmTheme.spacing.xxl),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    CrmText(
                                        "Registration wall skipped (logged-in visitor)",
                                        style = CrmTheme.typography.bodySmall,
                                        color = CrmTheme.colors.onSurfaceVariant,
                                    )
                                }
                            }
                            // Meter-warning nudge (PW-23) — 1 article remaining
                            gateContext == 3 -> MeterNudgeBanner(definition)
                            // Normal paywall
                            definition.type == WallType.Hard -> PricingWall(definition)
                            else -> ContentGateWall(definition)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishPanel(
    definition: WallDefinition,
    onDefinitionChange: (WallDefinition) -> Unit,
    history: List<WallVersionSummary>,
    onRollback: suspend (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg)) {
        CrmText("Audience", style = CrmTheme.typography.h3)
        Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
            listOf("Free plan workspaces", "Documents used ≥ 3", "Period — calendar month").forEach {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CrmText(it, style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurfaceVariant)
                    CrmText("✕", style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onDisabled)
                }
            }
            CrmTextButton("Add condition")
        }
        CrmDivider()

        CrmText("Channels", style = CrmTheme.typography.h3)
        Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
            Channel.entries.chunked(2).forEach { rowChannels ->
                Row(horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
                    rowChannels.forEach { channel ->
                        CrmToggleChip(
                            label = channel.label,
                            selected = channel in definition.channels,
                            onClick = {
                                val channels = if (channel in definition.channels) {
                                    definition.channels - channel
                                } else {
                                    definition.channels + channel
                                }
                                onDefinitionChange(definition.copy(channels = channels))
                            },
                        )
                    }
                }
            }
        }
        CrmDivider()

        CrmText("A/B test", style = CrmTheme.typography.h3)
        Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xs)) {
            listOf("Variant A — meter first" to "50%", "Variant B — price first" to "50%").forEach { (name, weight) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CrmText(name, style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurfaceVariant)
                    CrmText(weight, style = CrmTheme.typography.bodySmall, color = CrmTheme.colors.onSurface)
                }
            }
        }
        CrmDivider()

        CrmText("Version history", style = CrmTheme.typography.h3)
        if (history.isEmpty()) {
            CrmText("No history yet", style = CrmTheme.typography.caption, color = CrmTheme.colors.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.sm)) {
                history.forEachIndexed { index, entry ->
                    Column(verticalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xxs)) {
                        CrmText(
                            "v${entry.version} — ${entry.status}",
                            style = CrmTheme.typography.bodySmall,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CrmText(
                                entry.actor,
                                style = CrmTheme.typography.caption,
                                color = CrmTheme.colors.onSurfaceVariant,
                            )
                            // Restore available for all versions except the latest (index 0)
                            if (index > 0) {
                                CrmTextButton(
                                    "Restore",
                                    onClick = { scope.launch { onRollback(entry.version) } },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
