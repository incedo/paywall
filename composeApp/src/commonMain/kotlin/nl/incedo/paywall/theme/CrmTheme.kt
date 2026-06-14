package nl.incedo.paywall.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compose port of the Incedo CRM design tokens
 * (mirror of `designsystem/.../theme/CrmTheme.kt`, see `_ds/colors_and_type.css`).
 * Components read tokens from [CrmTheme]; hard-coded dp/sp/hex in screens is banned.
 */
@Immutable
data class CrmColors(
    val primary: Color,
    val primaryVariant: Color,
    val onPrimary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
    val onInfo: Color,
    val link: Color,
    val focus: Color,
    val errorContainer: Color,
    val successContainer: Color,
    val warningContainer: Color,
    val infoContainer: Color,
    val divider: Color,
    val disabled: Color,
    val onDisabled: Color,
)

val LightCrmColors = CrmColors(
    primary = Color(0xFF1A73E8),
    primaryVariant = Color(0xFF1557B0),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F9FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F3F4),
    onBackground = Color(0xFF202124),
    onSurface = Color(0xFF202124),
    onSurfaceVariant = Color(0xFF5F6368),
    error = Color(0xFFD93025),
    onError = Color(0xFFFFFFFF),
    success = Color(0xFF1E8E3E),
    warning = Color(0xFFF9AB00),
    info = Color(0xFF1A73E8),
    onInfo = Color(0xFFFFFFFF),
    link = Color(0xFF1A73E8),
    focus = Color(0xFF1A73E8),
    errorContainer = Color(0xFFFCE8E6),
    successContainer = Color(0xFFE6F4EA),
    warningContainer = Color(0xFFFEF7E0),
    infoContainer = Color(0xFFE8F0FE),
    divider = Color(0xFFDADCE0),
    disabled = Color(0xFFE8EAED),
    onDisabled = Color(0xFF9AA0A6),
)

val DarkCrmColors = CrmColors(
    primary = Color(0xFF8AB4F8),
    primaryVariant = Color(0xFF669DF6),
    onPrimary = Color(0xFF202124),
    background = Color(0xFF202124),
    surface = Color(0xFF303134),
    surfaceVariant = Color(0xFF3C4043),
    onBackground = Color(0xFFE8EAED),
    onSurface = Color(0xFFE8EAED),
    onSurfaceVariant = Color(0xFF9AA0A6),
    error = Color(0xFFF28B82),
    onError = Color(0xFF202124),
    success = Color(0xFF81C995),
    warning = Color(0xFFFDD663),
    info = Color(0xFF8AB4F8),
    onInfo = Color(0xFF202124),
    link = Color(0xFF8AB4F8),
    focus = Color(0xFF8AB4F8),
    errorContainer = Color(0xFF442726),
    successContainer = Color(0xFF1E3A2A),
    warningContainer = Color(0xFF3E3620),
    infoContainer = Color(0xFF1E2D3D),
    divider = Color(0xFF5F6368),
    disabled = Color(0xFF3C4043),
    onDisabled = Color(0xFF5F6368),
)

@Immutable
data class CrmTypography(
    val h1: TextStyle = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    val h2: TextStyle = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    val h3: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    val body: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    val bodySmall: TextStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal),
    val label: TextStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    val caption: TextStyle = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Normal),
    val button: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    val mono: TextStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
)

@Immutable
data class CrmSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

@Immutable
data class CrmShapes(
    val sm: RoundedCornerShape = RoundedCornerShape(4.dp),
    val md: RoundedCornerShape = RoundedCornerShape(8.dp),
    val lg: RoundedCornerShape = RoundedCornerShape(12.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50),
)

object CrmBorder {
    val thin: Dp = 0.5.dp
    val default: Dp = 1.dp
    val thick: Dp = 2.dp
}

@Immutable
data class CrmElevation(
    val none: Dp = 0.dp,
    val xs: Dp = 1.dp,
    val sm: Dp = 2.dp,
    val md: Dp = 4.dp,
    val lg: Dp = 8.dp,
    val xl: Dp = 16.dp,
)

@Immutable
data class CrmAnimation(
    val fast: Int = 150,
    val normal: Int = 300,
    val slow: Int = 500,
)

@Immutable
data class CrmOpacity(
    val hover: Float = 0.08f,
    val focus: Float = 0.12f,
    val pressed: Float = 0.16f,
    val dragged: Float = 0.24f,
    val disabled: Float = 0.38f,
    val overlay: Float = 0.50f,
    val container: Float = 0.15f,
)

@Immutable
data class CrmIconSize(
    val xs: Dp = 12.dp,
    val sm: Dp = 16.dp,
    val md: Dp = 20.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
)

@Immutable
data class CrmFocus(
    val ringWidth: Dp = 2.dp,
    val ringOffset: Dp = 2.dp,
    val minTouchTarget: Dp = 48.dp,
)

val LocalCrmColors = staticCompositionLocalOf { LightCrmColors }
val LocalCrmTypography = staticCompositionLocalOf { CrmTypography() }
val LocalCrmSpacing = staticCompositionLocalOf { CrmSpacing() }
val LocalCrmShapes = staticCompositionLocalOf { CrmShapes() }
val LocalCrmElevation = staticCompositionLocalOf { CrmElevation() }
val LocalCrmAnimation = staticCompositionLocalOf { CrmAnimation() }
val LocalCrmOpacity = staticCompositionLocalOf { CrmOpacity() }
val LocalCrmIconSize = staticCompositionLocalOf { CrmIconSize() }
val LocalCrmFocus = staticCompositionLocalOf { CrmFocus() }

object CrmTheme {
    val colors: CrmColors
        @Composable get() = LocalCrmColors.current
    val typography: CrmTypography
        @Composable get() = LocalCrmTypography.current
    val spacing: CrmSpacing
        @Composable get() = LocalCrmSpacing.current
    val shapes: CrmShapes
        @Composable get() = LocalCrmShapes.current
    val elevation: CrmElevation
        @Composable get() = LocalCrmElevation.current
    val animation: CrmAnimation
        @Composable get() = LocalCrmAnimation.current
    val opacity: CrmOpacity
        @Composable get() = LocalCrmOpacity.current
    val iconSize: CrmIconSize
        @Composable get() = LocalCrmIconSize.current
    val focus: CrmFocus
        @Composable get() = LocalCrmFocus.current
}

@Composable
fun CrmTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalCrmColors provides if (darkTheme) DarkCrmColors else LightCrmColors,
        LocalCrmTypography provides CrmTypography(),
        LocalCrmSpacing provides CrmSpacing(),
        LocalCrmShapes provides CrmShapes(),
        LocalCrmElevation provides CrmElevation(),
        LocalCrmAnimation provides CrmAnimation(),
        LocalCrmOpacity provides CrmOpacity(),
        LocalCrmIconSize provides CrmIconSize(),
        LocalCrmFocus provides CrmFocus(),
        content = content,
    )
}
