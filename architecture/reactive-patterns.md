# Reactive UI Patterns — Multiplatform

**Status**: AGREED
**Last Updated**: 2026-06-12
**Depends On**: architecture/design-system.md, architecture/module-structure.md

---

## 1. Overview

The paywall platform UI (the SPA and the admin console) uses **reactive patterns** to manage state and side effects across all platforms (web, desktop, Android, iOS). The architecture follows **unidirectional data flow** (UDF) with an **MVI-inspired** (Model-View-Intent) pattern, fully built on Kotlin coroutines and Compose state.

### Principles
- **Single source of truth**: Each screen has one state object
- **Unidirectional flow**: Intent → Reducer → State → UI → (user action) → Intent
- **Platform-agnostic**: All state management is in `commonMain` — zero platform-specific UI logic
- **Reactive**: State changes automatically recompose the UI via Compose
- **Testable**: State machines are pure functions, easily unit-tested without Compose

---

## 2. Architecture — MVI (Model-View-Intent)

```
┌──────────────────────────────────────────────────────┐
│                      UI Layer                         │
│                                                       │
│  ┌─────────┐    ┌──────────┐    ┌──────────────────┐ │
│  │  Screen  │───→│  Intent  │───→│   ViewModel /    │ │
│  │  (View)  │    │  (Event) │    │   StateHolder    │ │
│  │          │←───│          │←───│                  │ │
│  │ @Compose │    └──────────┘    │  ┌────────────┐  │ │
│  │          │                    │  │  Reducer   │  │ │
│  └─────────┘      State         │  └────────────┘  │ │
│       ▲          emission       │  ┌────────────┐  │ │
│       │                         │  │ Side Effect│  │ │
│       └─────── recompose ───────│  │  Handler   │  │ │
│                                 │  └──────┬─────┘  │ │
│                                 └─────────┼────────┘ │
└───────────────────────────────────────────┼──────────┘
                                            │
                                            ▼
                                   ┌────────────────┐
                                   │   API / Ports   │
                                   │   (outbound)    │
                                   └────────────────┘
```

---

## 3. Core Types

```kotlin
// ── Intent: What the user wants to do ────────────────

sealed interface WallListIntent {
    data object LoadWalls : WallListIntent
    data class Search(val query: String) : WallListIntent
    data class ChangePage(val page: Int) : WallListIntent
    data class DeleteWall(val id: WallId) : WallListIntent
    data object DismissError : WallListIntent
}

// ── State: What the UI displays ──────────────────────

data class WallListState(
    val walls: List<WallView> = emptyList(),
    val pagination: Pagination = Pagination(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: UiError? = null,
    val deletingId: WallId? = null,
)

// ── Side Effect: One-shot events (navigation, snackbar) ──

sealed interface WallListEffect {
    data class NavigateToDetail(val id: WallId) : WallListEffect
    data class ShowSnackbar(val message: String) : WallListEffect
}
```

---

## 4. StateHolder (ViewModel Equivalent)

The `StateHolder` is a platform-agnostic class in `commonMain` that manages state via coroutines. It does **not** extend Android's `ViewModel` — it's pure Kotlin.

```kotlin
class WallListStateHolder(
    private val wallApi: WallApi,  // Outbound port adapter
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(WallListState())
    val state: StateFlow<WallListState> = _state.asStateFlow()

    private val _effects = Channel<WallListEffect>(Channel.BUFFERED)
    val effects: Flow<WallListEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: WallListIntent) {
        when (intent) {
            is WallListIntent.LoadWalls -> loadWalls()
            is WallListIntent.Search -> search(intent.query)
            is WallListIntent.ChangePage -> changePage(intent.page)
            is WallListIntent.DeleteWall -> deleteWall(intent.id)
            is WallListIntent.DismissError -> dismissError()
        }
    }

    private fun loadWalls() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val result = wallApi.listWalls(
                    page = _state.value.pagination.page,
                    size = _state.value.pagination.size,
                    search = _state.value.searchQuery,
                )
                _state.update { it.copy(
                    walls = result.data,
                    pagination = result.pagination,
                    isLoading = false,
                ) }
            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = UiError.fromException(e),
                ) }
            }
        }
    }

    private fun deleteWall(id: WallId) {
        scope.launch {
            _state.update { it.copy(deletingId = id) }
            try {
                wallApi.deleteWall(id)
                _state.update { it.copy(deletingId = null) }
                _effects.send(WallListEffect.ShowSnackbar("Wall deleted"))
                loadWalls() // Refresh list
            } catch (e: Exception) {
                _state.update { it.copy(
                    deletingId = null,
                    error = UiError.fromException(e),
                ) }
            }
        }
    }

    private fun search(query: String) {
        _state.update { it.copy(searchQuery = query, pagination = Pagination()) }
        loadWalls()
    }

    private fun changePage(page: Int) {
        _state.update { it.copy(pagination = it.pagination.copy(page = page)) }
        loadWalls()
    }

    private fun dismissError() {
        _state.update { it.copy(error = null) }
    }
}
```

---

## 5. Screen Composable (View)

```kotlin
@Composable
fun WallListScreen(
    stateHolder: WallListStateHolder,
) {
    val state by stateHolder.state.collectAsState()

    // Handle one-shot effects
    LaunchedEffect(Unit) {
        stateHolder.effects.collect { effect ->
            when (effect) {
                is WallListEffect.NavigateToDetail -> { /* nav logic */ }
                is WallListEffect.ShowSnackbar -> { /* show snackbar */ }
            }
        }
    }

    // Load on first composition
    LaunchedEffect(Unit) {
        stateHolder.onIntent(WallListIntent.LoadWalls)
    }

    // Pure rendering — state in, intents out
    WallListContent(
        state = state,
        onSearch = { stateHolder.onIntent(WallListIntent.Search(it)) },
        onPageChange = { stateHolder.onIntent(WallListIntent.ChangePage(it)) },
        onDelete = { stateHolder.onIntent(WallListIntent.DeleteWall(it)) },
        onDismissError = { stateHolder.onIntent(WallListIntent.DismissError) },
    )
}

@Composable
private fun WallListContent(
    state: WallListState,
    onSearch: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onDelete: (WallId) -> Unit,
    onDismissError: () -> Unit,
) {
    // Pure UI — no side effects, no state management
    // Uses CrmTheme tokens and design system components
    Column {
        CrmSearchBar(
            value = state.searchQuery,
            onValueChange = onSearch,
            placeholder = "Search walls..."
        )

        if (state.isLoading) {
            CrmLoadingIndicator()
        } else {
            CrmDataTable(
                data = state.walls,
                // ... columns, sorting, etc.
            )
        }

        CrmPagination(
            pagination = state.pagination,
            onPageChange = onPageChange,
        )

        state.error?.let { error ->
            CrmSnackbar(
                message = error.message,
                onDismiss = onDismissError,
            )
        }
    }
}
```

---

## 6. Platform-Specific Wiring

The `StateHolder` is created with a platform-appropriate `CoroutineScope`:

### Web (wasmJs)
```kotlin
@Composable
fun rememberStateHolder(): WallListStateHolder {
    val scope = rememberCoroutineScope()
    return remember { WallListStateHolder(wallApi, scope) }
}
```

### Android
```kotlin
// Use Compose's lifecycle-aware scope, or wrap in Android ViewModel
class WallListViewModel(wallApi: WallApi) : ViewModel() {
    val stateHolder = WallListStateHolder(wallApi, viewModelScope)
}
```

### Desktop (JVM)
```kotlin
@Composable
fun rememberStateHolder(): WallListStateHolder {
    val scope = rememberCoroutineScope()
    return remember { WallListStateHolder(wallApi, scope) }
}
```

### iOS
```kotlin
// Use Compose Multiplatform's scope — same as desktop/web
@Composable
fun rememberStateHolder(): WallListStateHolder {
    val scope = rememberCoroutineScope()
    return remember { WallListStateHolder(wallApi, scope) }
}
```

> The **only** platform difference is lifecycle management. The `StateHolder` itself is identical everywhere.

---

## 7. Reactive Patterns Catalog

### 7a. Debounced Search

```kotlin
private fun search(query: String) {
    searchJob?.cancel()
    _state.update { it.copy(searchQuery = query) }
    searchJob = scope.launch {
        delay(300) // Debounce
        loadWalls()
    }
}
```

### 7b. Optimistic Updates

```kotlin
private fun enableWall(id: WallId) {
    // Optimistically update UI immediately
    _state.update { state ->
        state.copy(walls = state.walls.map {
            if (it.id == id) it.copy(enabled = true) else it
        })
    }
    scope.launch {
        try {
            wallApi.enable(id)
        } catch (e: Exception) {
            // Revert on failure
            _state.update { state ->
                state.copy(walls = state.walls.map {
                    if (it.id == id) it.copy(enabled = false) else it
                })
            }
            _effects.send(ShowSnackbar("Failed to enable wall"))
        }
    }
}
```

### 7c. Pull-to-Refresh / Periodic Refresh

```kotlin
fun startAutoRefresh(intervalMs: Long = 30_000) {
    refreshJob = scope.launch {
        while (isActive) {
            delay(intervalMs)
            loadWalls() // Silent refresh — no loading indicator
        }
    }
}
```

The same pattern keeps meter state fresh on the reader side: the meter count (MT-03) is re-fetched silently so the "articles remaining" indicator never shows a stale value after the anonymous `visitor_id` has been linked to a logged-in account (US-04).

### 7d. Form State Management

```kotlin
data class WallFormState(
    val name: String = "",
    val headline: String = "",
    val ctaText: String = "",
    val subscriptionPlanId: String? = null,
    val errors: Map<String, String> = emptyMap(), // field → error message
    val isSubmitting: Boolean = false,
    val isValid: Boolean = false,
)

sealed interface WallFormIntent {
    data class UpdateField(val field: String, val value: String) : WallFormIntent
    data object Submit : WallFormIntent
    data object Cancel : WallFormIntent
}

// Validation runs on every field change — reactive feedback
private fun updateField(field: String, value: String) {
    _state.update { state ->
        val updated = when (field) {
            "name" -> state.copy(name = value)
            "headline" -> state.copy(headline = value)
            "ctaText" -> state.copy(ctaText = value)
            else -> state
        }
        val errors = validate(updated) // Shared validation from domain
        updated.copy(errors = errors, isValid = errors.isEmpty())
    }
}
```

Because the live wall preview in the admin console renders from this same state object, every keystroke recomposes the preview reactively — no separate preview pipeline.

### 7e. Kanban Drag & Drop (Experiments Board)

```kotlin
data class ExperimentBoardState(
    val stages: Map<ExperimentStage, List<ExperimentView>> = emptyMap(),
    val draggingExperiment: ExperimentView? = null,
    val dragOverStage: ExperimentStage? = null,
)

sealed interface ExperimentBoardIntent {
    data class StartDrag(val experiment: ExperimentView) : ExperimentBoardIntent
    data class DragOver(val stage: ExperimentStage) : ExperimentBoardIntent
    data class Drop(val experiment: ExperimentView, val targetStage: ExperimentStage) : ExperimentBoardIntent
    data object CancelDrag : ExperimentBoardIntent
}

// Drop triggers: optimistic move → API call → confirm or revert
private fun drop(experiment: ExperimentView, targetStage: ExperimentStage) {
    val fromStage = experiment.stage
    // Optimistic: move card immediately
    _state.update { moveExperimentToStage(it, experiment, targetStage) }
    scope.launch {
        try {
            experimentApi.changeStage(experiment.id, targetStage)
        } catch (e: Exception) {
            // Revert on failure
            _state.update { moveExperimentToStage(it, experiment, fromStage) }
            _effects.send(ShowSnackbar("Invalid stage transition"))
        }
    }
}
```

### 7f. Infinite Scroll / Lazy Loading

```kotlin
// Triggered when user scrolls near the end of the list
private fun loadMore() {
    if (_state.value.isLoadingMore || !_state.value.hasMore) return
    _state.update { it.copy(isLoadingMore = true) }
    scope.launch {
        val nextPage = _state.value.pagination.page + 1
        val result = api.list(page = nextPage)
        _state.update { it.copy(
            items = it.items + result.data,
            pagination = result.pagination,
            isLoadingMore = false,
            hasMore = result.data.isNotEmpty(),
        ) }
    }
}
```

### 7g. Real-Time Updates (WebSocket / SSE)

```kotlin
// Subscribe to event stream for live updates (e.g. the wall event
// stream feeding the analytics dashboard: WallShown, MeterIncremented,
// VariantAssigned)
fun subscribeToUpdates() {
    scope.launch {
        eventStream.subscribe("wall:*").collect { event ->
            when (event) {
                is WallCreated -> _state.update { addWall(it, event) }
                is WallUpdated -> _state.update { updateWall(it, event) }
                is WallDeleted -> _state.update { removeWall(it, event) }
            }
        }
    }
}
```

---

## 8. Module Structure

```
frontend/                                # Platform-specific entry points
  web/                                   # wasmJs target
    src/wasmJsMain/kotlin/nl/incedo/paywall/frontend/web/
      Main.kt                            # renderComposable entry point
      WebApp.kt                          # Web-specific wiring
  desktop/                               # JVM target
    src/jvmMain/kotlin/nl/incedo/paywall/frontend/desktop/
      Main.kt                            # application { Window { } }
      DesktopApp.kt
  android/                               # Android target
    src/androidMain/kotlin/nl/incedo/paywall/frontend/android/
      MainActivity.kt                    # setContent { }
      AndroidApp.kt
  ios/                                   # iOS target
    src/iosMain/kotlin/nl/incedo/paywall/frontend/ios/
      MainViewController.kt             # ComposeUIViewController { }
      IosApp.kt

  common/                                # Shared UI logic (commonMain)
    src/commonMain/kotlin/nl/incedo/paywall/frontend/
      app/
        App.kt                           # Root @Composable (all platforms)
        Navigation.kt                    # Navigation graph
      screen/
        walls/
          WallListScreen.kt
          WallDetailScreen.kt
          WallFormScreen.kt
        subscriptions/
          SubscriptionListScreen.kt
          SubscriptionDetailScreen.kt
          SubscriptionFormScreen.kt
        experiments/
          ExperimentBoardScreen.kt
          ExperimentDetailScreen.kt
          ExperimentFormScreen.kt
        analytics/
          WallEventTimelineScreen.kt
          AnalyticsDashboardScreen.kt
        auth/
          LoginScreen.kt
          CallbackScreen.kt
          ProfileScreen.kt
        dashboard/
          DashboardScreen.kt
        admin/
          AccountListScreen.kt
          AccountFormScreen.kt
      state/
        walls/
          WallListState.kt
          WallListIntent.kt
          WallListEffect.kt
          WallListStateHolder.kt
        subscriptions/
          SubscriptionListState.kt
          SubscriptionListStateHolder.kt
        experiments/
          ExperimentBoardState.kt
          ExperimentBoardStateHolder.kt
          ExperimentFormState.kt
          ExperimentFormStateHolder.kt
        analytics/
          WallEventTimelineState.kt
          WallEventTimelineStateHolder.kt
        auth/
          AuthState.kt
          AuthStateHolder.kt
      api/                               # Outbound adapter (HTTP client)
        ApiClient.kt
        WallApi.kt
        SubscriptionApi.kt
        ExperimentApi.kt
        AnalyticsApi.kt
        AuthApi.kt
```

---

## 9. Completion Criteria

### 9a. Reactive Pattern Tests
- [x] StateHolder emits correct initial state
- [x] Intent dispatching updates state correctly
- [x] Side effects are emitted for navigation and snackbar
- [x] Error state is set on API failure and clearable
- [x] Debounced search waits before firing
- [x] Optimistic update reverts on failure
- [x] Form validation runs reactively on field change
- [x] Pagination state updates on page change

### 9b. Platform Compilation
- [x] commonMain state holders compile
- [x] wasmJs entry point renders App
- [x] jvm (desktop) entry point opens window
- [ ] android entry point starts activity
- [ ] ios entry point creates view controller

### 9c. Integration
- [x] Screen composable collects state and dispatches intents
- [x] Effects drive navigation between screens
- [x] Design system components receive state and emit callbacks

---

## 10. Open Questions

- **Q-1**: Navigation library — Compose Navigation Multiplatform, Voyager, or Decompose? — **Decision**: Decompose (lifecycle-aware, supports all KMP targets)
- **Q-2**: Should real-time updates use WebSocket, SSE, or polling? — **Decision**: Pending (not V1 critical)
- **Q-3**: Offline support — cache read models locally for mobile? — **Decision**: Pending (not V1 critical)
- **Q-4**: Deep linking on mobile (Android intents, iOS universal links)? — **Decision**: Pending (not V1 critical)
