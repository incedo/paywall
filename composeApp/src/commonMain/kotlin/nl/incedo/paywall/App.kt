package nl.incedo.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.random.Random
import kotlinx.coroutines.launch
import nl.incedo.paywall.api.ConsoleApi
import nl.incedo.paywall.api.SaveWallRequest
import nl.incedo.paywall.api.VariantStatsResponse
import nl.incedo.paywall.api.WallResponse
import nl.incedo.paywall.designer.BrandsScreen
import nl.incedo.paywall.designer.ConfigScreen
import nl.incedo.paywall.designer.DashboardScreen
import nl.incedo.paywall.designer.SubjectInspectorScreen
import nl.incedo.paywall.designer.WallDesignerScreen
import nl.incedo.paywall.designer.WallsOverviewScreen
import nl.incedo.paywall.model.WallDefinition
import nl.incedo.paywall.model.WallStatus
import nl.incedo.paywall.model.WallSummary
import nl.incedo.paywall.model.apiName
import nl.incedo.paywall.model.channelFromApi
import nl.incedo.paywall.model.demoWalls
import nl.incedo.paywall.model.wallTypeFromApi
import nl.incedo.paywall.theme.CrmTheme
import nl.incedo.paywall.ui.CrmAvatar
import nl.incedo.paywall.ui.CrmDivider
import nl.incedo.paywall.ui.CrmText
import nl.incedo.paywall.ui.CrmTextButton

private sealed interface ConsoleScreen {
    data object Overview : ConsoleScreen
    data object Dashboard : ConsoleScreen
    data object Inspector : ConsoleScreen
    data object Config : ConsoleScreen
    data object Brands : ConsoleScreen
    data class Designer(val wallId: String) : ConsoleScreen
}

/**
 * Admin console (ADM-01: the console owns no logic — everything goes through
 * the API). One Compose source tree for web (Wasm) and desktop (JVM); falls
 * back to demo data when the backend is unreachable so the designer remains
 * explorable offline.
 */
@Composable
fun App() {
    val api = remember { ConsoleApi() }
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf<ConsoleScreen>(ConsoleScreen.Overview) }
    var wallsById by remember { mutableStateOf<Map<String, WallResponse>>(emptyMap()) }
    var summaries by remember { mutableStateOf<List<WallSummary>>(emptyList()) }
    var stats by remember { mutableStateOf<List<VariantStatsResponse>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var offline by remember { mutableStateOf(false) }

    var definition by remember { mutableStateOf(WallDefinition()) }
    var editingName by remember { mutableStateOf("New wall") }
    var editingStatus by remember { mutableStateOf("draft") }
    var editingVersion by remember { mutableStateOf<Int?>(null) }

    suspend fun refreshWalls() {
        runCatching { api.walls() }
            .onSuccess { list ->
                offline = false
                wallsById = list.associateBy { it.id }
                summaries = list.map { it.toSummary() }
                statusMessage = null
            }
            .onFailure {
                offline = true
                summaries = demoWalls
                statusMessage = "Backend unreachable — showing demo data"
            }
    }

    LaunchedEffect(Unit) { refreshWalls() }
    LaunchedEffect(screen) {
        if (screen is ConsoleScreen.Dashboard) {
            runCatching { api.stats() }
                .onSuccess { stats = it; statusMessage = null }
                .onFailure { statusMessage = "Backend unreachable — no stats" }
        }
        if (screen is ConsoleScreen.Inspector || screen is ConsoleScreen.Config) {
            statusMessage = null
        }
    }

    fun openWall(summary: WallSummary) {
        val stored = wallsById[summary.id]
        definition = stored?.toDefinition() ?: WallDefinition(type = summary.type, channels = summary.channels)
        editingName = summary.name
        editingStatus = stored?.status ?: "draft"
        editingVersion = stored?.version
        screen = ConsoleScreen.Designer(summary.id)
    }

    fun newWall() {
        definition = WallDefinition()
        editingName = "New wall"
        editingStatus = "new"
        editingVersion = null
        screen = ConsoleScreen.Designer("wall-${Random.nextInt(10_000, 99_999)}")
    }

    fun saveDraft(wallId: String) = scope.launch {
        when (val outcome = api.runCatchingSave(wallId, definition.toSaveRequest(editingName, editingVersion))) {
            is ConsoleApi.SaveOutcome.Saved -> {
                editingVersion = outcome.wall.version
                editingStatus = outcome.wall.status
                statusMessage = "Draft saved — v${outcome.wall.version}"
                refreshWalls()
            }
            is ConsoleApi.SaveOutcome.Conflict -> statusMessage = outcome.message
            is ConsoleApi.SaveOutcome.Failed -> statusMessage = outcome.message
        }
    }

    fun publish(wallId: String) = scope.launch {
        // Unsaved edits are saved first, then published (one console action)
        when (val saved = api.runCatchingSave(wallId, definition.toSaveRequest(editingName, editingVersion))) {
            is ConsoleApi.SaveOutcome.Saved -> {
                when (val outcome = runCatching { api.publish(wallId) }.getOrNull()) {
                    is ConsoleApi.SaveOutcome.Saved -> {
                        editingVersion = outcome.wall.version
                        editingStatus = outcome.wall.status
                        statusMessage = "Published — v${outcome.wall.version}"
                        refreshWalls()
                    }
                    else -> statusMessage = "Publish failed"
                }
            }
            is ConsoleApi.SaveOutcome.Conflict -> statusMessage = saved.message
            is ConsoleApi.SaveOutcome.Failed -> statusMessage = saved.message
        }
    }

    CrmTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CrmTheme.colors.background),
        ) {
            AdminTopBar(
                activeItem = when (screen) {
                    is ConsoleScreen.Dashboard -> "Dashboard"
                    is ConsoleScreen.Inspector -> "Support"
                    is ConsoleScreen.Config -> "Config"
                    is ConsoleScreen.Brands -> "Brands"
                    else -> "Walls"
                },
                onNavigate = { item ->
                    screen = when (item) {
                        "Dashboard" -> ConsoleScreen.Dashboard
                        "Support" -> ConsoleScreen.Inspector
                        "Config" -> ConsoleScreen.Config
                        "Brands" -> ConsoleScreen.Brands
                        else -> ConsoleScreen.Overview
                    }
                },
            )
            CrmDivider()
            when (val current = screen) {
                is ConsoleScreen.Overview -> WallsOverviewScreen(
                    walls = summaries,
                    statusMessage = statusMessage,
                    onOpenWall = ::openWall,
                    onNewWall = ::newWall,
                )
                is ConsoleScreen.Dashboard -> Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                ) {
                    DashboardScreen(stats, statusMessage)
                }
                is ConsoleScreen.Inspector -> SubjectInspectorScreen(
                    onInspect = { id -> api.inspectSubject(id) },
                    onGrants = { id -> api.subjectGrants(id) },
                    onMeterReset = { id -> api.resetMeter(id, "console", "manual reset via admin console") },
                    onIssueGrant = { subjectId, articleId, grantId, reason ->
                        api.issueGrant(subjectId, articleId, grantId, reason)
                    },
                    onRevokeGrant = { subjectId, grantId, articleId ->
                        api.revokeGrant(subjectId, grantId, articleId)
                    },
                    statusMessage = statusMessage,
                )
                is ConsoleScreen.Config -> ConfigScreen(
                    onLoadConfig = { api.getConfig() },
                    statusMessage = statusMessage,
                )
                is ConsoleScreen.Brands -> BrandsScreen(
                    onLoadBrands = { api.brands() },
                    onCreateBrand = { req -> api.createBrand(req) },
                    onUpdateTheme = { id, theme -> api.updateBrandTheme(id, theme) },
                    statusMessage = statusMessage,
                )
                is ConsoleScreen.Designer -> WallDesignerScreen(
                    wallName = editingName,
                    wallStatus = editingStatus,
                    definition = definition,
                    onDefinitionChange = { definition = it },
                    onBack = {
                        screen = ConsoleScreen.Overview
                        scope.launch { refreshWalls() }
                    },
                    onSaveDraft = { if (!offline) saveDraft(current.wallId) else statusMessage = "Offline — cannot save" },
                    onPublish = { if (!offline) publish(current.wallId) else statusMessage = "Offline — cannot publish" },
                    onLoadHistory = { api.wallHistory(current.wallId) },
                    onRollback = { version ->
                        when (val outcome = runCatching { api.rollbackWall(current.wallId, version) }.getOrElse { ConsoleApi.SaveOutcome.Failed("Backend unreachable") }) {
                            is ConsoleApi.SaveOutcome.Saved -> {
                                editingVersion = outcome.wall.version
                                editingStatus = outcome.wall.status
                                definition = outcome.wall.toDefinition()
                                statusMessage = "Restored to v$version — now draft v${outcome.wall.version}"
                                refreshWalls()
                            }
                            is ConsoleApi.SaveOutcome.Conflict -> statusMessage = outcome.message
                            is ConsoleApi.SaveOutcome.Failed -> statusMessage = outcome.message
                        }
                    },
                )
            }
        }
    }
}

private suspend fun ConsoleApi.runCatchingSave(id: String, request: SaveWallRequest): ConsoleApi.SaveOutcome =
    runCatching { saveWall(id, request) }.getOrElse { ConsoleApi.SaveOutcome.Failed("Backend unreachable") }

private fun WallResponse.toSummary() = WallSummary(
    id = id,
    name = name,
    type = wallTypeFromApi(wallType),
    channels = channels.mapNotNull(::channelFromApi).toSet(),
    status = if (status == "published") WallStatus.Live else WallStatus.Draft,
    variants = 1,
    conversion = "—",
    updated = "v$version · $lastEditedBy",
)

private fun WallResponse.toDefinition() = WallDefinition(
    type = wallTypeFromApi(wallType),
    title = title,
    body = body,
    primaryCta = primaryCta,
    secondaryCta = secondaryCta,
    channels = channels.mapNotNull(::channelFromApi).toSet(),
    requireConsentStep = requireConsentStep,
)

private fun WallDefinition.toSaveRequest(name: String, expectedVersion: Int?) = SaveWallRequest(
    name = name,
    wallType = type.name.lowercase(),
    title = title,
    body = body,
    primaryCta = primaryCta,
    secondaryCta = secondaryCta,
    channels = channels.map { it.apiName() }.toSet(),
    actor = "console",
    expectedVersion = expectedVersion,
    requireConsentStep = requireConsentStep,
)

@Composable
private fun AdminTopBar(activeItem: String, onNavigate: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CrmTheme.colors.surface)
            .padding(horizontal = CrmTheme.spacing.xl, vertical = CrmTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.xl),
    ) {
        CrmText("Incedo CRM", style = CrmTheme.typography.h3, color = CrmTheme.colors.primary)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(CrmTheme.spacing.lg),
        ) {
            listOf("Dashboard", "Walls", "Brands", "Support", "Config").forEach { item ->
                CrmTextButton(item, onClick = { onNavigate(item) })
            }
            listOf("Contacts", "Deals", "Invoices", "Subscriptions", "Tickets").forEach { item ->
                CrmText(
                    item,
                    style = CrmTheme.typography.button,
                    color = CrmTheme.colors.onSurfaceVariant,
                )
            }
        }
        CrmText(
            activeItem,
            style = CrmTheme.typography.label,
            color = CrmTheme.colors.primary,
        )
        CrmAvatar("MV")
    }
}
