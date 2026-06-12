# Reactive UI Patterns — Multiplatform

**Status**: AGREED
**Last Updated**: 2026-04-09
**Depends On**: architecture/design-system.md, architecture/module-structure.md

---

## 1. Overview

The CRM UI uses **reactive patterns** to manage state and side effects across all platforms (web, desktop, Android, iOS). The architecture follows **unidirectional data flow** (UDF) with an **MVI-inspired** (Model-View-Intent) pattern, fully built on Kotlin coroutines and Compose state.

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

sealed interface ContactListIntent {
    data object LoadContacts : ContactListIntent
    data class Search(val query: String) : ContactListIntent
    data class ChangePage(val page: Int) : ContactListIntent
    data class DeleteContact(val id: ContactId) : ContactListIntent
    data object DismissError : ContactListIntent
}

// ── State: What the UI displays ──────────────────────

data class ContactListState(
    val contacts: List<ContactView> = emptyList(),
    val pagination: Pagination = Pagination(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: UiError? = null,
    val deletingId: ContactId? = null,
)

// ── Side Effect: One-shot events (navigation, snackbar) ──

sealed interface ContactListEffect {
    data class NavigateToDetail(val id: ContactId) : ContactListEffect
    data class ShowSnackbar(val message: String) : ContactListEffect
}
```

---

## 4. StateHolder (ViewModel Equivalent)

The `StateHolder` is a platform-agnostic class in `commonMain` that manages state via coroutines. It does **not** extend Android's `ViewModel` — it's pure Kotlin.

```kotlin
class ContactListStateHolder(
    private val contactApi: ContactApi,  // Outbound port adapter
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(ContactListState())
    val state: StateFlow<ContactListState> = _state.asStateFlow()

    private val _effects = Channel<ContactListEffect>(Channel.BUFFERED)
    val effects: Flow<ContactListEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: ContactListIntent) {
        when (intent) {
            is ContactListIntent.LoadContacts -> loadContacts()
            is ContactListIntent.Search -> search(intent.query)
            is ContactListIntent.ChangePage -> changePage(intent.page)
            is ContactListIntent.DeleteContact -> deleteContact(intent.id)
            is ContactListIntent.DismissError -> dismissError()
        }
    }

    private fun loadContacts() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val result = contactApi.listContacts(
                    page = _state.value.pagination.page,
                    size = _state.value.pagination.size,
                    search = _state.value.searchQuery,
                )
                _state.update { it.copy(
                    contacts = result.data,
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

    private fun deleteContact(id: ContactId) {
        scope.launch {
            _state.update { it.copy(deletingId = id) }
            try {
                contactApi.deleteContact(id)
                _state.update { it.copy(deletingId = null) }
                _effects.send(ContactListEffect.ShowSnackbar("Contact deleted"))
                loadContacts() // Refresh list
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
        loadContacts()
    }

    private fun changePage(page: Int) {
        _state.update { it.copy(pagination = it.pagination.copy(page = page)) }
        loadContacts()
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
fun ContactListScreen(
    stateHolder: ContactListStateHolder,
) {
    val state by stateHolder.state.collectAsState()

    // Handle one-shot effects
    LaunchedEffect(Unit) {
        stateHolder.effects.collect { effect ->
            when (effect) {
                is ContactListEffect.NavigateToDetail -> { /* nav logic */ }
                is ContactListEffect.ShowSnackbar -> { /* show snackbar */ }
            }
        }
    }

    // Load on first composition
    LaunchedEffect(Unit) {
        stateHolder.onIntent(ContactListIntent.LoadContacts)
    }

    // Pure rendering — state in, intents out
    ContactListContent(
        state = state,
        onSearch = { stateHolder.onIntent(ContactListIntent.Search(it)) },
        onPageChange = { stateHolder.onIntent(ContactListIntent.ChangePage(it)) },
        onDelete = { stateHolder.onIntent(ContactListIntent.DeleteContact(it)) },
        onDismissError = { stateHolder.onIntent(ContactListIntent.DismissError) },
    )
}

@Composable
private fun ContactListContent(
    state: ContactListState,
    onSearch: (String) -> Unit,
    onPageChange: (Int) -> Unit,
    onDelete: (ContactId) -> Unit,
    onDismissError: () -> Unit,
) {
    // Pure UI — no side effects, no state management
    // Uses CrmTheme tokens and design system components
    Column {
        CrmSearchBar(
            value = state.searchQuery,
            onValueChange = onSearch,
            placeholder = "Search contacts..."
        )

        if (state.isLoading) {
            CrmLoadingIndicator()
        } else {
            CrmDataTable(
                data = state.contacts,
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
fun rememberStateHolder(): ContactListStateHolder {
    val scope = rememberCoroutineScope()
    return remember { ContactListStateHolder(contactApi, scope) }
}
```

### Android
```kotlin
// Use Compose's lifecycle-aware scope, or wrap in Android ViewModel
class ContactListViewModel(contactApi: ContactApi) : ViewModel() {
    val stateHolder = ContactListStateHolder(contactApi, viewModelScope)
}
```

### Desktop (JVM)
```kotlin
@Composable
fun rememberStateHolder(): ContactListStateHolder {
    val scope = rememberCoroutineScope()
    return remember { ContactListStateHolder(contactApi, scope) }
}
```

### iOS
```kotlin
// Use Compose Multiplatform's scope — same as desktop/web
@Composable
fun rememberStateHolder(): ContactListStateHolder {
    val scope = rememberCoroutineScope()
    return remember { ContactListStateHolder(contactApi, scope) }
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
        loadContacts()
    }
}
```

### 7b. Optimistic Updates

```kotlin
private fun completeActivity(id: ActivityId) {
    // Optimistically update UI immediately
    _state.update { state ->
        state.copy(activities = state.activities.map {
            if (it.id == id) it.copy(completed = true) else it
        })
    }
    scope.launch {
        try {
            activityApi.complete(id)
        } catch (e: Exception) {
            // Revert on failure
            _state.update { state ->
                state.copy(activities = state.activities.map {
                    if (it.id == id) it.copy(completed = false) else it
                })
            }
            _effects.send(ShowSnackbar("Failed to complete activity"))
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
            loadContacts() // Silent refresh — no loading indicator
        }
    }
}
```

### 7d. Form State Management

```kotlin
data class ContactFormState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val companyId: String? = null,
    val errors: Map<String, String> = emptyMap(), // field → error message
    val isSubmitting: Boolean = false,
    val isValid: Boolean = false,
)

sealed interface ContactFormIntent {
    data class UpdateField(val field: String, val value: String) : ContactFormIntent
    data object Submit : ContactFormIntent
    data object Cancel : ContactFormIntent
}

// Validation runs on every field change — reactive feedback
private fun updateField(field: String, value: String) {
    _state.update { state ->
        val updated = when (field) {
            "firstName" -> state.copy(firstName = value)
            "lastName" -> state.copy(lastName = value)
            "email" -> state.copy(email = value)
            else -> state
        }
        val errors = validate(updated) // Shared validation from domain
        updated.copy(errors = errors, isValid = errors.isEmpty())
    }
}
```

### 7e. Kanban Drag & Drop (Deals Pipeline)

```kotlin
data class PipelineState(
    val stages: Map<DealStage, List<DealView>> = emptyMap(),
    val draggingDeal: DealView? = null,
    val dragOverStage: DealStage? = null,
)

sealed interface PipelineIntent {
    data class StartDrag(val deal: DealView) : PipelineIntent
    data class DragOver(val stage: DealStage) : PipelineIntent
    data class Drop(val deal: DealView, val targetStage: DealStage) : PipelineIntent
    data object CancelDrag : PipelineIntent
}

// Drop triggers: optimistic move → API call → confirm or revert
private fun drop(deal: DealView, targetStage: DealStage) {
    val fromStage = deal.stage
    // Optimistic: move card immediately
    _state.update { moveDealToStage(it, deal, targetStage) }
    scope.launch {
        try {
            dealApi.changeStage(deal.id, targetStage)
        } catch (e: Exception) {
            // Revert on failure
            _state.update { moveDealToStage(it, deal, fromStage) }
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
// Subscribe to event stream for live updates
fun subscribeToUpdates() {
    scope.launch {
        eventStream.subscribe("contact:*").collect { event ->
            when (event) {
                is ContactCreated -> _state.update { addContact(it, event) }
                is ContactUpdated -> _state.update { updateContact(it, event) }
                is ContactDeleted -> _state.update { removeContact(it, event) }
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
    src/wasmJsMain/kotlin/crm/frontend/web/
      Main.kt                            # renderComposable entry point
      WebApp.kt                          # Web-specific wiring
  desktop/                               # JVM target
    src/jvmMain/kotlin/crm/frontend/desktop/
      Main.kt                            # application { Window { } }
      DesktopApp.kt
  android/                               # Android target
    src/androidMain/kotlin/crm/frontend/android/
      MainActivity.kt                    # setContent { }
      AndroidApp.kt
  ios/                                   # iOS target
    src/iosMain/kotlin/crm/frontend/ios/
      MainViewController.kt             # ComposeUIViewController { }
      IosApp.kt

  common/                                # Shared UI logic (commonMain)
    src/commonMain/kotlin/crm/frontend/
      app/
        App.kt                           # Root @Composable (all platforms)
        Navigation.kt                    # Navigation graph
      screen/
        contacts/
          ContactListScreen.kt
          ContactDetailScreen.kt
          ContactFormScreen.kt
        companies/
          CompanyListScreen.kt
          CompanyDetailScreen.kt
          CompanyFormScreen.kt
        deals/
          DealPipelineScreen.kt
          DealDetailScreen.kt
          DealFormScreen.kt
        activities/
          ActivityTimelineScreen.kt
          ActivityFormScreen.kt
        auth/
          LoginScreen.kt
          CallbackScreen.kt
          ProfileScreen.kt
        dashboard/
          DashboardScreen.kt
        admin/
          UserListScreen.kt
          UserFormScreen.kt
      state/
        contacts/
          ContactListState.kt
          ContactListIntent.kt
          ContactListEffect.kt
          ContactListStateHolder.kt
        companies/
          CompanyListState.kt
          CompanyListStateHolder.kt
        deals/
          PipelineState.kt
          PipelineStateHolder.kt
          DealFormState.kt
          DealFormStateHolder.kt
        activities/
          ActivityTimelineState.kt
          ActivityTimelineStateHolder.kt
        auth/
          AuthState.kt
          AuthStateHolder.kt
      api/                               # Outbound adapter (HTTP client)
        ApiClient.kt
        ContactApi.kt
        CompanyApi.kt
        DealApi.kt
        ActivityApi.kt
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
