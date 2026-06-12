# Design System — Token-Based, Multiplatform

**Status**: AGREED
**Last Updated**: 2026-04-16
**Depends On**: architecture/module-structure.md
**Derived Artifacts**: architecture/testing/testing.md (Component Workbench), architecture/forms-pattern.md

---

## 1. Overview

The CRM uses a **custom design system** built from scratch on Compose Foundation primitives — no Material dependency. Following the approach from [Custom Design System Using Jetpack Compose](https://medium.com/better-programming/custom-design-system-using-jetpack-compose-17a59b1ae38d), the system defines its own design tokens (colors, typography, spacing, shapes, elevation) and builds all components from `BasicText`, `Box`, `Row`, `Column`, `Layout`, `Image`, `Spacer`, and `Modifier`.

### Why No Material?
- **Full control**: CRM-specific visual language, not constrained by Material Design spec
- **Consistency across platforms**: Same tokens and components on web, desktop, Android, and iOS
- **Lighter bundle**: No Material library weight — important for WASM
- **Token-driven**: Change tokens at the foundation layer, all components update automatically

### Platform Targets
The design system compiles to **all** Compose Multiplatform targets:

| Platform | Target | Distribution |
|----------|--------|-------------|
| Web | wasmJs | Browser via WebAssembly |
| Desktop | jvm | Windows, macOS, Linux |
| Android | android | Native Android app |
| iOS | iosArm64, iosSimulatorArm64 | Native iOS app via Compose Multiplatform |

---

## 2. Architecture — Atomic Design Layers

```
┌─────────────────────────────────────────────────────────┐
│  SCREENS / TEMPLATES                                     │
│  TransactionHistory, Dashboard, ContactDetail, Pipeline  │
├─────────────────────────────────────────────────────────┤
│  ORGANISMS                                               │
│  NavBar, DataTable, KanbanBoard, ActivityTimeline,       │
│  ContactCard, DealCard, SearchBar, FormSection           │
├─────────────────────────────────────────────────────────┤
│  MOLECULES                                               │
│  Button, InputField, SelectField, Badge, Tag, Avatar,    │
│  Chip, Tab, IconButton, Tooltip, Dialog, Snackbar        │
├─────────────────────────────────────────────────────────┤
│  ATOMS                                                   │
│  Text, Icon, Divider, Surface, Spacer, Image             │
├─────────────────────────────────────────────────────────┤
│  TOKENS (Foundation)                                     │
│  Colors, Typography, Spacing, Shapes, Elevation,         │
│  Animation, Breakpoints                                  │
└─────────────────────────────────────────────────────────┘
```

Each layer only depends on the layer below it. Tokens propagate via `CompositionLocal`.

---

## 3. Design Tokens

### 3a. Colors

```kotlin
data class CrmColors(
    // Brand
    val primary: Color,
    val primaryVariant: Color,
    val onPrimary: Color,
    val secondary: Color,
    val secondaryVariant: Color,
    val onSecondary: Color,

    // Surfaces
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,

    // Semantic
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val error: Color,
    val onError: Color,
    val info: Color,
    val onInfo: Color,

    // Pipeline stages (CRM-specific)
    val stageProspecting: Color,
    val stageQualification: Color,
    val stageProposal: Color,
    val stageNegotiation: Color,
    val stageClosedWon: Color,
    val stageClosedLost: Color,

    // Misc
    val border: Color,
    val divider: Color,
    val overlay: Color,
    val disabled: Color,
    val onDisabled: Color,

    val isLight: Boolean
)

val LightCrmColors = CrmColors(
    primary = Color(0xFF1A73E8),
    // ... full palette
    isLight = true
)

val DarkCrmColors = CrmColors(
    primary = Color(0xFF8AB4F8),
    // ... full palette
    isLight = false
)

val LocalCrmColors = staticCompositionLocalOf { LightCrmColors }
val LocalContentColor = staticCompositionLocalOf { Color.Black }
```

### 3b. Typography

```kotlin
data class CrmTypography(
    // Display
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,

    // Headings
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val h4: TextStyle,

    // Body
    val bodyLarge: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,

    // Labels
    val labelLarge: TextStyle,
    val label: TextStyle,
    val labelSmall: TextStyle,

    // Code / Data
    val code: TextStyle,       // Monospace for IDs, technical data
    val data: TextStyle,       // Tabular numbers for currencies, counts
)

val LocalCrmTypography = staticCompositionLocalOf { defaultCrmTypography() }
```

### 3c. Spacing

```kotlin
data class CrmSpacing(
    val xxxs: Dp,    // 2.dp
    val xxs: Dp,     // 4.dp
    val xs: Dp,      // 8.dp
    val sm: Dp,      // 12.dp
    val md: Dp,      // 16.dp
    val lg: Dp,      // 24.dp
    val xl: Dp,      // 32.dp
    val xxl: Dp,     // 48.dp
    val xxxl: Dp,    // 64.dp
)

val LocalCrmSpacing = staticCompositionLocalOf { defaultCrmSpacing() }
```

### 3d. Shapes

```kotlin
data class CrmShapes(
    val none: RoundedCornerShape,    // 0.dp
    val xs: RoundedCornerShape,      // 4.dp
    val sm: RoundedCornerShape,      // 8.dp
    val md: RoundedCornerShape,      // 12.dp
    val lg: RoundedCornerShape,      // 16.dp
    val xl: RoundedCornerShape,      // 24.dp
    val full: RoundedCornerShape,    // 50% (pill)
)

val LocalCrmShapes = staticCompositionLocalOf { defaultCrmShapes() }
```

### 3e. Elevation

```kotlin
data class CrmElevation(
    val none: Dp,     // 0.dp
    val xs: Dp,       // 1.dp
    val sm: Dp,       // 2.dp
    val md: Dp,       // 4.dp
    val lg: Dp,       // 8.dp
    val xl: Dp,       // 16.dp
)

val LocalCrmElevation = staticCompositionLocalOf { defaultCrmElevation() }
```

### 3f. Breakpoints (Responsive)

```kotlin
data class CrmBreakpoints(
    val compact: Dp,     // 0.dp - 599.dp (phone)
    val medium: Dp,      // 600.dp - 839.dp (tablet portrait)
    val expanded: Dp,    // 840.dp - 1199.dp (tablet landscape / small desktop)
    val large: Dp,       // 1200.dp+ (desktop)
)

enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED, LARGE }

val LocalWindowSizeClass = staticCompositionLocalOf { WindowSizeClass.EXPANDED }
```

### 3g. Animation

```kotlin
data class CrmAnimation(
    val fast: Int,        // 150ms
    val normal: Int,      // 300ms
    val slow: Int,        // 500ms
    val easeIn: Easing,
    val easeOut: Easing,
    val easeInOut: Easing,
)

val LocalCrmAnimation = staticCompositionLocalOf { defaultCrmAnimation() }
```

### 3h. Opacity

Standardized alpha values for interactive states. Avoids scattered `.copy(alpha = 0.15f)`.

```kotlin
data class CrmOpacity(
    val hover: Float,       // 0.08 — subtle hover overlay (desktop only)
    val focus: Float,       // 0.12 — focus ring fill
    val pressed: Float,     // 0.16 — active/pressed state
    val dragged: Float,     // 0.24 — drag source dimming
    val disabled: Float,    // 0.38 — disabled content
    val overlay: Float,     // 0.50 — modal scrim
    val container: Float,   // 0.15 — semantic badge/chip backgrounds
)

val DefaultCrmOpacity = CrmOpacity(
    hover = 0.08f, focus = 0.12f, pressed = 0.16f, dragged = 0.24f,
    disabled = 0.38f, overlay = 0.50f, container = 0.15f,
)
```

### 3i. Borders

```kotlin
data class CrmBorder(
    val thin: Dp,     // 0.5.dp — subtle dividers
    val default: Dp,  // 1.dp — inputs, cards
    val thick: Dp,    // 2.dp — focus rings, selected tabs
)

val DefaultCrmBorder = CrmBorder(thin = 0.5.dp, default = 1.dp, thick = 2.dp)
```

### 3j. Icon Sizes

Standardized icon dimensions. Components use these tokens, not raw dp.

```kotlin
data class CrmIconSize(
    val xs: Dp,    // 12.dp — inline badge icons
    val sm: Dp,    // 16.dp — button leading icons, tag remove
    val md: Dp,    // 20.dp — nav items, search icon
    val lg: Dp,    // 24.dp — default standalone icon
    val xl: Dp,    // 32.dp — avatar fallback, empty state
    val xxl: Dp,   // 48.dp — hero empty state
)

val DefaultCrmIconSize = CrmIconSize(
    xs = 12.dp, sm = 16.dp, md = 20.dp, lg = 24.dp, xl = 32.dp, xxl = 48.dp,
)
```

### 3k. Z-Index

Layering scale for overlapping elements.

```kotlin
data class CrmZIndex(
    val base: Float,       // 0 — normal flow
    val sticky: Float,     // 100 — sticky headers, FAB
    val dropdown: Float,   // 200 — select dropdown, popover
    val drawer: Float,     // 300 — side drawer, bottom sheet
    val modal: Float,      // 400 — dialog, confirm
    val snackbar: Float,   // 500 — toast notification
    val tooltip: Float,    // 600 — tooltip (always on top)
)
```

### 3l. Focus & Accessibility

```kotlin
data class CrmFocus(
    val ringColor: Color,      // primary with focus opacity
    val ringWidth: Dp,         // 2.dp
    val ringOffset: Dp,        // 2.dp — gap between element and ring
    val minTouchTarget: Dp,    // 48.dp — WCAG minimum (Android/iOS/web)
)
```

### 3m. Semantic Colors (extended)

Additions to `CrmColors` beyond what's currently implemented:

```kotlin
// Add to CrmColors:
val info: Color,              // Informational badges, tooltips
val onInfo: Color,
val link: Color,              // Clickable text links
val focus: Color,             // Focus ring color
val overlay: Color,           // Scrim behind modals/drawers
val errorContainer: Color,    // Light error background for inline alerts
val successContainer: Color,
val warningContainer: Color,
val infoContainer: Color,
```

---

## 3n. Token Themability

All new token sets are pluggable via `CrmDesignTheme` and loadable from JSON:

```kotlin
data class CrmDesignTheme(
    val colors: CrmColors,
    val typography: CrmTypography = DefaultCrmTypography,
    val spacing: CrmSpacing = DefaultCrmSpacing,
    val shapes: CrmShapes = DefaultCrmShapes,
    val elevation: CrmElevation = DefaultCrmElevation,
    val animation: CrmAnimation = DefaultCrmAnimation,
    val opacity: CrmOpacity = DefaultCrmOpacity,
    val border: CrmBorder = DefaultCrmBorder,
    val iconSize: CrmIconSize = DefaultCrmIconSize,
    val focus: CrmFocus = DefaultCrmFocus,
)
```

JSON theme loader extended to support new token categories:
```json
{
  "elevation": { "md": 6 },
  "animation": { "fast": 100, "normal": 250 },
  "opacity": { "overlay": 0.6 },
  "border": { "default": 2 },
  "iconSize": { "md": 22 }
}
```

## 4. Theme Provider (Pluggable)

Themes are pluggable via the `CrmDesignTheme` data class. Callers can provide a fully custom theme — different brand colors, typography, spacing — without touching any component code.

```kotlin
/** Bundle of all token sets — the single entry point for theming. */
data class CrmDesignTheme(
    val colors: CrmColors,
    val typography: CrmTypography = DefaultCrmTypography,
    val spacing: CrmSpacing = DefaultCrmSpacing,
    val shapes: CrmShapes = DefaultCrmShapes,
)

val LightTheme = CrmDesignTheme(colors = LightCrmColors)
val DarkTheme = CrmDesignTheme(colors = DarkCrmColors)

object CrmTheme {
    val colors: CrmColors @Composable @ReadOnlyComposable get() = LocalCrmColors.current
    val typography: CrmTypography @Composable @ReadOnlyComposable get() = LocalCrmTypography.current
    val spacing: CrmSpacing @Composable @ReadOnlyComposable get() = LocalCrmSpacing.current
    val shapes: CrmShapes @Composable @ReadOnlyComposable get() = LocalCrmShapes.current
}

/** Primary overload — accepts a full theme bundle. */
@Composable
fun CrmTheme(theme: CrmDesignTheme = LightTheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalCrmColors provides theme.colors,
        LocalCrmTypography provides theme.typography,
        LocalCrmSpacing provides theme.spacing,
        LocalCrmShapes provides theme.shapes,
    ) { content() }
}

/** Convenience overload — dark/light toggle (backward-compatible). */
@Composable
fun CrmTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    CrmTheme(theme = if (darkTheme) DarkTheme else LightTheme, content = content)
}
```

### Custom Theme Example
```kotlin
val AcmeTheme = CrmDesignTheme(
    colors = LightCrmColors.copy(
        primary = Color(0xFF6200EE),
        primaryVariant = Color(0xFF3700B3),
    ),
    spacing = DefaultCrmSpacing.copy(md = 14.dp),
)

// Usage:
CrmTheme(theme = AcmeTheme) { App() }
```

### No Magic Numbers Rule
All components **must** use theme tokens — never hardcoded `dp`, `sp`, color, or shape values. Use `CrmTheme.spacing.sm`, `CrmTheme.typography.body`, `CrmTheme.shapes.medium` etc.

---

## 4a. JSON Theme Loader (Runtime Configuration)

Themes can be loaded from JSON files at runtime — no recompilation needed. This enables:
- Per-tenant branding (white-label)
- User-selected themes (light, dark, custom)
- Theme hot-reload during development

### JSON Format

```json
{
  "name": "Acme Corp",
  "base": "light",
  "colors": {
    "primary": "#6200EE",
    "primaryVariant": "#3700B3",
    "onPrimary": "#FFFFFF",
    "background": "#FAFAFA",
    "success": "#1E8E3E"
  },
  "spacing": {
    "md": 14
  },
  "typography": {
    "h1": { "size": 28, "weight": "bold" },
    "body": { "size": 15, "weight": "normal" }
  },
  "shapes": {
    "medium": 10
  }
}
```

**Rules:**
- `"base"` selects the base palette (`"light"` or `"dark"`). All unspecified tokens inherit from the base.
- Colors are hex strings (`"#RRGGBB"` or `"#AARRGGBB"`).
- Spacing values are integers (dp).
- Shape values are corner radius integers (dp).
- Typography entries have `size` (sp) and `weight` (`"normal"`, `"medium"`, `"semibold"`, `"bold"`).
- Any omitted field falls back to the base theme's default.

### API

```kotlin
object CrmThemeLoader {
    /** Parse a JSON string into a CrmDesignTheme. Unspecified tokens inherit from base. */
    fun fromJson(json: String): CrmDesignTheme

    /** Serialize a CrmDesignTheme to JSON (for export / editing). */
    fun toJson(theme: CrmDesignTheme, name: String = "Custom"): String
}
```

### Integration

```kotlin
// In App.kt — load theme from JSON config (e.g., fetched from API or bundled as resource)
val themeJson = loadThemeConfig() // platform-specific: fetch/read file
val theme = CrmThemeLoader.fromJson(themeJson)
CrmTheme(theme = theme) { App() }
```

---

## 5. Atom Components (Built on Foundation)

All atoms use only Compose Foundation primitives — no Material imports.

### Text
```kotlin
@Composable
fun CrmText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = CrmTheme.typography.body,
    // ...
) {
    val resolvedColor = color.takeOrElse {
        style.color.takeOrElse {
            LocalContentColor.current
        }
    }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = resolvedColor),
    )
}
```

### Surface
```kotlin
@Composable
fun CrmSurface(
    modifier: Modifier = Modifier,
    shape: Shape = CrmTheme.shapes.none,
    color: Color = CrmTheme.colors.surface,
    contentColor: Color = contentColorFor(color),
    elevation: Dp = CrmTheme.elevation.none,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .shadow(elevation, shape)
                .background(color, shape)
                .clip(shape),
        ) {
            content()
        }
    }
}
```

### Icon, Divider, Spacer
Similarly built on `Image`, `Box`, and `Spacer` foundation composables.

---

## 6. Molecule Components

| Component | Built From | CRM Usage |
|-----------|-----------|-----------|
| CrmButton | Surface + Row + CrmText + CrmIcon | Actions, form submits |
| CrmIconButton | Surface + Box + CrmIcon | Toolbar actions |
| CrmInputField | BasicTextField + Box + CrmText | Form inputs |
| CrmSelectField | CrmInputField + Popup + Column | Dropdowns (company selector) |
| CrmBadge | Surface + CrmText | Status indicators, counts |
| CrmTag | Surface + Row + CrmText + CrmIconButton | Contact/deal tags |
| CrmChip | Surface + Row + CrmText | Filter chips |
| CrmAvatar | Surface + CrmText/Image | User/contact avatars |
| CrmTab | Surface + CrmText | Tab navigation |
| CrmDialog | Foundation Dialog + CrmSurface | Confirmations, forms |
| CrmSnackbar | CrmSurface + CrmText + CrmButton | Notifications |
| CrmTooltip | Popup + CrmSurface + CrmText | Hover info |

---

## 7. Organism Components (CRM-Specific)

| Component | Composition | CRM Usage |
|-----------|------------|-----------|
| CrmNavBar | Row + CrmTab + CrmAvatar | Top navigation |
| CrmSideNav | Column + CrmButton + CrmIcon | Side navigation (desktop) |
| CrmDataTable | Column + Row + CrmText + sorting + pagination | Contact/company lists |
| CrmKanbanBoard | Row + Column + CrmDealCard + drag/drop | Deal pipeline |
| CrmDealCard | CrmSurface + CrmText + CrmBadge | Single deal in kanban |
| CrmContactCard | CrmSurface + CrmAvatar + CrmText + CrmTag | Contact summary |
| CrmActivityItem | Row + CrmIcon + CrmText + timestamp | Timeline entry |
| CrmTimeline | LazyColumn + CrmActivityItem + date headers | Activity feed |
| CrmSearchBar | CrmInputField + CrmIcon + debounce | Global/entity search |
| CrmFormSection | Column + CrmText (heading) + content slot | Grouped form fields |
| CrmEmptyState | Column + CrmIcon + CrmText + CrmButton | Empty list placeholders |
| CrmConfirmDialog | CrmDialog + CrmText + CrmButton pair | Delete confirmations |

---

## 7a. Input Components (Missing — CRM is form-heavy)

All input components follow the Carbon/Material pattern: label above, helper text below, error state with red border + message. Every input respects `CrmTheme.focus.minTouchTarget` (48dp) on touch devices.

| Component | Signature | CRM Usage | Interaction |
|-----------|-----------|-----------|-------------|
| CrmCheckbox | `checked: Boolean, onCheckedChange, label?, enabled` | Bulk selection in tables, filter toggles | Tap/click toggles. Space key when focused. |
| CrmRadio | `selected: Boolean, onClick, label?` | Single-select options (status filter, deal stage) | Tap/click selects. Arrow keys cycle group. |
| CrmSwitch | `checked: Boolean, onCheckedChange, label?` | Feature toggles (auto-renew, notifications) | Tap/click toggles. Drag thumb on touch. |
| CrmTextArea | `value, onValueChange, label?, rows, maxLength?` | Contact notes, email body, ticket reply | Multi-line input. Character counter at maxLength. |
| CrmDatePicker | `value: LocalDate?, onValueChange, label?` | Deal close date, activity due date, invoice dates | Tap opens calendar popover. Type date directly. |
| CrmFileUpload | `onFilesSelected, accept?, maxSize?, multiple?` | Attachments (V2) | Drag-drop zone + click to browse. Progress bar during upload. |

### Interaction per platform

| Input | Desktop | Tablet | Mobile |
|-------|---------|--------|--------|
| Checkbox | Click, Space key, focus ring | Tap (48dp target) | Tap (48dp target) |
| Radio | Click, Arrow keys | Tap (48dp target) | Tap (48dp target) |
| Switch | Click toggle | Tap or drag thumb | Tap or drag thumb |
| TextArea | Type, Tab to next | Tap to focus, on-screen keyboard | Tap to focus, on-screen keyboard |
| DatePicker | Click opens popover, type date | Tap opens full-screen calendar | Tap opens bottom sheet calendar |
| FileUpload | Drag-drop + click | Tap opens file picker | Tap opens file picker |

---

## 7b. Feedback Components (Missing — no user feedback UX)

| Component | Signature | CRM Usage | Behavior |
|-----------|-----------|-----------|----------|
| CrmSnackbar | `message, action?, variant, duration` | "Contact saved", "Invoice sent", "Error: network" | Slides up from bottom. Auto-dismisses after `CrmTheme.animation.slow`. Action button optional. |
| CrmSpinner | `size: Dp` | Loading states during API calls | Indeterminate rotation. Centered in container. |
| CrmProgressBar | `progress: Float, label?` | Multi-step wizards, file upload | Horizontal bar, 0→1 fill. Optional percentage label. |
| CrmSkeletonLoader | `width, height, shape` | Content placeholders during fetch | Shimmer animation (pulse). Matches shape of target content. |
| CrmEmptyState | `icon, title, subtitle?, action?, onAction?` | Empty contact list, no search results | Centered column. Icon (xxl size) + title (h2) + body + CTA button. |

### Snackbar variants

| Variant | Color | Icon | Use case |
|---------|-------|------|----------|
| DEFAULT | surface + onSurface | none | Neutral info |
| SUCCESS | successContainer + success | checkmark | "Saved", "Sent" |
| ERROR | errorContainer + error | alert | "Failed", "Network error" |
| WARNING | warningContainer + warning | triangle | "Unsaved changes" |

### Snackbar placement per platform

| Platform | Position | Width | Animation |
|----------|----------|-------|-----------|
| Desktop | Bottom-left, 360dp max width | Fixed | Slide up |
| Tablet | Bottom-center, 480dp max width | Fixed | Slide up |
| Mobile | Bottom, full width with horizontal padding | Stretch | Slide up from edge |

---

## 7c. Overlay Components (Missing — Dialog is the only overlay)

| Component | Signature | CRM Usage | Behavior |
|-----------|-----------|-----------|----------|
| CrmDrawer | `visible, onDismiss, side: START/END, width?` | Filter panel, edit form side-panel, mobile nav | Slides in from side. Scrim behind (overlay opacity). Swipe to dismiss on touch. |
| CrmMenu | `expanded, onDismiss, anchor` | Row actions (edit/delete), 3-dot overflow | Positioned below anchor. Keyboard: Arrow keys navigate, Enter selects, Escape closes. |
| CrmPopover | `visible, onDismiss, anchor, content` | Tooltip-like rich content, color picker | Arrow pointing to anchor. Click outside dismisses. |
| CrmBottomSheet | `visible, onDismiss, content` | Mobile-only: filters, date picker, action list | Slides up from bottom. Drag handle. Swipe down to dismiss. Half-screen default, full-screen on drag up. |

### Overlay behavior per platform

| Overlay | Desktop | Tablet | Mobile |
|---------|---------|--------|--------|
| CrmDrawer | Side panel (320dp), push content | Side panel (320dp), overlay content | Full-width, overlay with scrim |
| CrmMenu | Popover below anchor | Popover below anchor | Bottom sheet |
| CrmDialog | Centered card (280-560dp) | Centered card (280-560dp) | Near-full-screen card with padding |
| CrmBottomSheet | Not used → CrmDrawer instead | Half-screen from bottom | Half-screen from bottom |

---

## 7d. Navigation Components (Missing)

| Component | Signature | CRM Usage |
|-----------|-----------|-----------|
| CrmBreadcrumb | `items: List<BreadcrumbItem>, onNavigate` | Contacts > John Doe > Activity |
| CrmTabs | `tabs: List<Tab>, selectedIndex, onSelect` | Contact detail (Info / History / Notes) |
| CrmPagination | `page, totalPages, onPageChange` | Table pagination (10/20/50 per page) |

### Tabs behavior

| Platform | Style | Interaction |
|----------|-------|-------------|
| Desktop | Horizontal tabs with underline indicator | Click, keyboard arrows |
| Tablet | Horizontal tabs, scrollable if many | Tap, swipe horizontal |
| Mobile | Horizontal tabs, scrollable. Or swipe between tab content. | Tap tab, or swipe content area left/right |

---

## 7e. Enhanced Data Components

### CrmDataTable enhancements

| Feature | Interaction | Platform notes |
|---------|-------------|----------------|
| Sort by column | Click header → asc → desc → none | Touch: tap header |
| Pagination | Page controls below table | Compact: swipe or load-more button |
| Sticky header | Header stays visible on scroll | All platforms |
| Row selection | Checkbox column, select all | Bulk actions toolbar appears |
| Inline edit | Double-click cell → edit mode | Touch: long-press cell → edit |
| Row actions | Hover → action icons appear | Touch: swipe row left for actions |
| Empty state | CrmEmptyState inside table body | All platforms |
| Loading | CrmSkeletonLoader rows | All platforms |

### CrmSelectField enhancements

| Feature | Interaction |
|---------|-------------|
| Search/filter | Type to filter options in dropdown |
| Multi-select | Checkbox per option, chips in input |
| Option groups | Nested headers (e.g., Region > Country) |
| Create option | "Add new..." at bottom of dropdown |

---

## 8. Responsive Layout System

The design system adapts layout based on `WindowSizeClass`:

```kotlin
@Composable
fun CrmScaffold(
    navContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    when (CrmTheme.windowSizeClass) {
        WindowSizeClass.COMPACT -> {
            // Bottom nav + full-width content (mobile)
            Column {
                Box(Modifier.weight(1f)) { content() }
                navContent()
            }
        }
        WindowSizeClass.MEDIUM -> {
            // Rail nav + content (tablet)
            Row {
                navContent() // Narrow rail
                Box(Modifier.weight(1f)) { content() }
            }
        }
        WindowSizeClass.EXPANDED, WindowSizeClass.LARGE -> {
            // Side nav + content (desktop/web)
            Row {
                navContent() // Full side nav
                Box(Modifier.weight(1f)) { content() }
            }
        }
    }
}
```

### Platform-Specific Adaptations

| Behavior | Compact (Mobile) | Medium (Tablet) | Expanded/Large (Desktop/Web) |
|----------|-----------------|-----------------|------------------------------|
| Navigation | Bottom bar | Rail | Side drawer |
| Data tables | Card list | Compact table | Full table with columns |
| Deal pipeline | Horizontal scroll cards | 2-column kanban | Full kanban board |
| Contact detail | Stacked sections | 2-column | 3-column with sidebar |
| Forms | Full-screen | Dialog | Inline panel |
| Search | Full-screen overlay | Expandable bar | Persistent bar |

---

## 8a. Interaction Mechanics

All components implement consistent interaction patterns across platforms. Desktop gets hover states; touch devices get minimum 48dp targets and gesture support.

### Pointer interactions

| Interaction | Desktop (mouse) | Touch (tablet/mobile) | Keyboard |
|-------------|----------------|----------------------|----------|
| **Tap/Click** | `onClick` on mouse up | `onClick` on touch up (within target) | Enter or Space |
| **Hover** | Background shifts by `opacity.hover` overlay | Not applicable | Not applicable |
| **Long press** | Not used (right-click instead) | Context menu after 500ms hold | Not applicable |
| **Double tap** | Inline edit (table cells) | Inline edit (table cells) | Not applicable |
| **Drag** | Kanban cards, column resize | Kanban cards (with handle), drawer dismiss | Not applicable |
| **Swipe** | Not applicable | Table row actions (swipe left), dismiss snackbar, tab switching, bottom sheet dismiss | Not applicable |
| **Right-click** | Context menu (CrmMenu) | Long-press → same menu | Shift+F10 |
| **Pinch** | Not applicable | Future: zoom on images/charts | Not applicable |

### Focus management

All interactive components support keyboard navigation:

| Rule | Behavior |
|------|----------|
| **Tab order** | Follows DOM/compose tree order. `tabIndex` on custom components. |
| **Focus ring** | Visible only on keyboard nav (not click). Uses `CrmTheme.focus.ringColor` + `ringWidth`. |
| **Focus trap** | Modals/dialogs trap focus (Tab cycles within). Escape closes. |
| **Skip links** | First Tab on page focuses "Skip to content" (web). |
| **Arrow keys** | Navigate within radio groups, tabs, menu items, select options. |

### State layers (inspired by Material 3)

Every interactive surface shows a state layer — a translucent overlay on the surface color:

| State | Opacity token | Applies to |
|-------|---------------|------------|
| Enabled (default) | 0% | All |
| Hover | `opacity.hover` (8%) | Desktop only — buttons, table rows, nav items |
| Focus | `opacity.focus` (12%) | Keyboard navigation — all interactive elements |
| Pressed | `opacity.pressed` (16%) | All — buttons, chips, cards during tap/click |
| Dragged | `opacity.dragged` (24%) | Drag source item |
| Disabled | `opacity.disabled` (38%) | Greyed out — buttons, inputs, switches |

### Touch-specific rules

| Rule | Value | Rationale |
|------|-------|-----------|
| Min touch target | 48dp × 48dp | WCAG 2.5.8 / Android/iOS guidelines |
| Touch slop | 8dp | Movement tolerance before drag starts |
| Long press delay | 500ms | Consistent with platform conventions |
| Swipe velocity threshold | 100dp/s | Distinguishes swipe from scroll |
| Scroll lock on drag | Lock opposite axis after 10dp in one direction | Prevents diagonal jitter |

### Accessibility (a11y) baseline — WCAG AA

| Requirement | Implementation |
|-------------|----------------|
| **Color contrast** | 4.5:1 for body text, 3:1 for large text (h1-h2). Verified per theme. |
| **Focus visible** | 2dp ring, `focus` color, offset 2dp. Visible on keyboard nav only. |
| **Content descriptions** | All icons, images, and non-text content have `contentDescription`. |
| **Semantic roles** | Buttons → Role.Button, Checkboxes → Role.Checkbox, etc. via Compose semantics. |
| **Error identification** | Error messages associated with inputs via `semantics { error(msg) }`. |
| **Touch targets** | Minimum 48dp × 48dp. Padding expands small visual elements to meet target. |
| **Reduced motion** | Respect `prefers-reduced-motion`. Skip animations, show instant state changes. |

---

## 9. Module Structure

The design system is a **separate KMP module** that all platform targets depend on:

```
designsystem/
  build.gradle.kts                       # KMP: all targets
  src/
    commonMain/kotlin/crm/designsystem/
      theme/
        CrmTheme.kt                      # Theme provider + object
        CrmColors.kt                     # Color tokens + light/dark
        CrmTypography.kt                 # Typography tokens
        CrmSpacing.kt                    # Spacing scale
        CrmShapes.kt                     # Corner radius tokens
        CrmElevation.kt                  # Shadow tokens
        CrmAnimation.kt                  # Motion tokens
        CrmBreakpoints.kt               # Window size classes
        CrmOpacity.kt                    # State layer opacities
        CrmBorder.kt                     # Border width tokens
        CrmIconSize.kt                   # Standardized icon dimensions
        CrmFocus.kt                      # Focus ring + touch target tokens
        CrmZIndex.kt                     # Layering scale
        CrmThemeLoader.kt               # JSON → theme at runtime
      foundation/
        CrmText.kt                       # Atom: text
        CrmSurface.kt                    # Atom: surface/card
        CrmIcon.kt                       # Atom: icon
        CrmDivider.kt                    # Atom: divider
        CrmImage.kt                      # Atom: image
      component/
        CrmButton.kt                     # Molecule: button variants
        CrmIconButton.kt
        CrmInputField.kt                # Molecule: text input
        CrmSelectField.kt               # Molecule: dropdown (enhanced: search, multi)
        CrmBadge.kt
        CrmTag.kt
        CrmChip.kt
        CrmAvatar.kt
        CrmCheckbox.kt                  # NEW: toggle input
        CrmRadio.kt                     # NEW: single-select input
        CrmSwitch.kt                    # NEW: on/off toggle
        CrmTextArea.kt                  # NEW: multi-line input
        CrmDatePicker.kt                # NEW: date selection
        CrmTab.kt
        CrmDialog.kt
        CrmSnackbar.kt                  # NEW: toast notification
        CrmSpinner.kt                   # NEW: loading indicator
        CrmProgressBar.kt               # NEW: progress indicator
        CrmSkeletonLoader.kt            # NEW: content placeholder
        CrmTooltip.kt
        CrmMenu.kt                      # NEW: dropdown action menu
        CrmPopover.kt                   # NEW: rich popover
        CrmFileUpload.kt                # NEW: file input
      organism/
        CrmNavBar.kt
        CrmSideNav.kt
        CrmDataTable.kt                 # Enhanced: sort, pagination, selection, sticky
        CrmKanbanBoard.kt
        CrmDealCard.kt
        CrmContactCard.kt
        CrmActivityItem.kt
        CrmTimeline.kt
        CrmSearchBar.kt
        CrmFormSection.kt
        CrmEmptyState.kt                # NEW: no-data placeholder
        CrmConfirmDialog.kt
        CrmBreadcrumb.kt                # NEW: navigation trail
        CrmTabs.kt                      # NEW: tabbed content
        CrmPagination.kt                # NEW: page controls
        CrmDrawer.kt                    # NEW: side panel overlay
        CrmBottomSheet.kt               # NEW: mobile bottom sheet
      layout/
        CrmScaffold.kt                  # Responsive scaffold
        CrmResponsive.kt                # WindowSizeClass helpers
    commonTest/kotlin/crm/designsystem/
      theme/
        CrmColorsTest.kt
        CrmTypographyTest.kt
      component/
        CrmButtonTest.kt
        CrmInputFieldTest.kt
```

### Dependency Graph (Updated)

```
                 ┌────────────────┐
                 │     shared      │  Domain + Application
                 └───────┬────────┘
                         │
                 ┌───────┴────────┐
                 │  designsystem   │  Tokens + Components
                 └───────┬────────┘
                    ╱    │    ╲    ╲
                   ╱     │     ╲    ╲
         ┌────────┴┐ ┌──┴────┐ ┌┴──────┐ ┌──────────┐
         │ web      │ │desktop│ │android│ │ ios       │
         │ (wasmJs) │ │ (jvm) │ │       │ │(iosArm64)│
         └──────────┘ └───────┘ └───────┘ └──────────┘
         
         ┌─────────────┐
         │   backend    │  (no designsystem dependency)
         └─────────────┘
```

---

## 10. Gradle Configuration

```kotlin
// designsystem/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm()            // Desktop
    wasmJs { browser() }  // Web
    androidTarget()   // Android
    iosArm64()        // iOS device
    iosSimulatorArm64() // iOS simulator

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            // NO compose.material3 — we build our own
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
```

---

## 11. Completion Criteria

### 11a. Token Tests (existing + expansion)
- [x] Light and dark color palettes are fully defined (all fields non-default)
- [x] Typography tokens produce correct TextStyle for each variant
- [x] Spacing scale is monotonically increasing
- [x] CrmDesignTheme bundles colors + typography + spacing + shapes
- [x] Custom theme overrides propagate to components via CompositionLocal
- [x] CrmThemeLoader.fromJson parses JSON into CrmDesignTheme with base inheritance
- [x] CrmThemeLoader.toJson round-trips a theme to JSON
- [x] Partial JSON overrides merge correctly with base theme defaults
- [x] Invalid hex colors / missing fields degrade gracefully
- [x] Elevation tokens: 6-level scale (none → xl), CrmSurface uses elevation tokens
- [x] Animation tokens: duration (fast/normal/slow)
- [x] Opacity tokens: state layer tokens defined (CrmOpacity)
- [x] Border tokens: thin/default/thick defined (CrmBorder)
- [x] Icon size tokens: xs-xxl defined (CrmIconSize)
- [x] Focus tokens: ring color/width/offset, minTouchTarget (CrmFocus)
- [x] Semantic colors: info, link, focus, overlay, *Container variants
- [x] CrmDesignTheme extended with elevation, animation, opacity, border, iconSize
- [x] CrmThemeLoader extended to parse/export new token categories

### 11b. Component Tests (existing)
- [x] CrmText resolves color from: explicit → style → LocalContentColor
- [x] CrmSurface sets LocalContentColor for children
- [x] CrmButton renders with correct padding, shape, and colors per variant
- [x] CrmInputField shows placeholder, handles input, displays error state
- [x] All components use CrmTheme tokens — zero hardcoded dp/sp/color values
- [ ] CrmDialog shows/hides with animation

### 11c. Input Component Tests
- [ ] CrmCheckbox: checked/unchecked toggle, disabled state, label rendering
- [ ] CrmRadio: selection within group, single-select enforcement
- [ ] CrmSwitch: toggle animation, disabled state, label
- [ ] CrmTextArea: multi-line input, character counter at maxLength
- [ ] CrmDatePicker: date selection, popover/sheet per platform
- [ ] All inputs meet 48dp minimum touch target

### 11d. Feedback Component Tests
- [ ] CrmSnackbar: shows message, auto-dismisses after duration, action button
- [ ] CrmSpinner: renders at specified size, animates rotation
- [ ] CrmProgressBar: fills proportionally to progress value
- [ ] CrmSkeletonLoader: renders placeholder with shimmer
- [ ] CrmEmptyState: icon + title + subtitle + action button

### 11e. Overlay Component Tests
- [ ] CrmDrawer: slides in from side, scrim behind, swipe to dismiss
- [ ] CrmMenu: positioned below anchor, keyboard navigation, escape closes
- [ ] CrmBottomSheet: slides up, drag handle, swipe down dismisses
- [ ] CrmPopover: arrow points to anchor, click-outside dismisses

### 11f. Navigation Component Tests
- [ ] CrmTabs: selected tab indicator, keyboard arrows cycle
- [ ] CrmBreadcrumb: renders trail, last item non-clickable
- [ ] CrmPagination: page controls, page size selector

### 11g. Enhanced Data Component Tests
- [ ] CrmDataTable: sort by column (asc/desc/none), pagination controls
- [ ] CrmDataTable: row selection with checkbox column + select-all
- [ ] CrmDataTable: sticky header on scroll
- [ ] CrmSelectField: search/filter options, multi-select with chips

### 11h. Responsive Tests
- [ ] CrmScaffold renders bottom nav for COMPACT
- [ ] CrmScaffold renders side nav for EXPANDED
- [ ] CrmDataTable renders cards for COMPACT, table for EXPANDED
- [ ] WindowSizeClass computed per platform (web, desktop, mobile)
- [ ] Overlay components adapt per platform (Menu → BottomSheet on mobile)

### 11i. Interaction & A11y Tests
- [ ] Focus ring visible on keyboard navigation, hidden on mouse click
- [ ] All interactive elements have 48dp min touch target on touch devices
- [ ] State layers apply correct opacity (hover, focus, pressed, disabled)
- [ ] Dialog/modal traps focus (Tab cycles within)
- [ ] Reduced motion preference respected (animations skipped)

### 11j. Platform Compilation
- [ ] `./gradlew :designsystem:allTests` passes
- [ ] `./gradlew :designsystem:wasmJsBrowserDistribution` succeeds
- [x] `./gradlew :designsystem:jvmJar` succeeds
- [ ] Android target compiles
- [ ] iOS target compiles

---

## 12. Open Questions

- **Q-1**: Font family — IBM Plex Sans (article's choice), Inter, or custom? — **Decision**: Inter (clean, modern, designed for screens, open source)
- **Q-2**: Icon set — custom SVG icons, Phosphor Icons, or Lucide? — **Decision**: Lucide (1500+ clean stroke icons, MIT license, easy to bundle as Compose ImageVector)
- **Q-3**: Should the design system be a separate Git repo / published artifact? — **Decision**: No, in-repo module (Gradle module in this repo)
- **Q-4**: Accessibility — minimum contrast ratios, focus indicators, screen reader labels? — **Decision**: WCAG AA baseline (4.5:1 contrast, focus indicators, screen reader labels)

## 13. Blogs
- https://designstrategy.guide/the-ultimate-design-systems-resources-list/

