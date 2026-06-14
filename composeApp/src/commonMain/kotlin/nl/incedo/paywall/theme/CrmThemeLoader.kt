package nl.incedo.paywall.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses a JSON theme string into a [CrmDesignTheme] and serializes themes back.
 *
 * JSON format:
 * ```json
 * {
 *   "name": "Publisher Brand",
 *   "base": "light",          // "light" (default) or "dark"
 *   "colors": { "primary": "#6200EE", "success": "#1E8E3E" },
 *   "spacing": { "md": 14 },
 *   "typography": { "h1": { "size": 28, "weight": "bold" } },
 *   "shapes": { "sm": 4, "md": 10 }
 * }
 * ```
 * Unspecified fields fall back to the base theme. Invalid hex colors or missing
 * fields degrade gracefully (GAP-10: per-publisher gate branding).
 */
object CrmThemeLoader {

    private val lenient = Json { ignoreUnknownKeys = true; isLenient = true }

    // Density(1f) means 1 px/dp, so toPx() returns the raw dp value as float.
    private val unitDensity = Density(1f, 1f)

    fun fromJson(jsonStr: String): CrmDesignTheme = runCatching {
        val root = lenient.parseToJsonElement(jsonStr).jsonObject
        val base = if (root["base"]?.jsonPrimitive?.contentOrNull == "dark") DarkCrmTheme else LightCrmTheme
        CrmDesignTheme(
            colors = mergeColors(root["colors"]?.jsonObject, base.colors),
            typography = mergeTypography(root["typography"]?.jsonObject, base.typography),
            spacing = mergeSpacing(root["spacing"]?.jsonObject, base.spacing),
            shapes = mergeShapes(root["shapes"]?.jsonObject, base.shapes),
            elevation = base.elevation,
            animation = base.animation,
            opacity = base.opacity,
            iconSize = base.iconSize,
            focus = base.focus,
        )
    }.getOrElse { LightCrmTheme }

    fun toJson(theme: CrmDesignTheme, name: String = "Custom"): String {
        val c = theme.colors
        val t = theme.typography
        val s = theme.spacing
        val sh = theme.shapes
        return buildString {
            appendLine("{")
            appendLine("""  "name": ${jsonStr(name)},""")
            appendLine("""  "base": "light",""")
            appendLine("""  "colors": {""")
            appendLine("""    "primary": "${colorHex(c.primary)}",""")
            appendLine("""    "primaryVariant": "${colorHex(c.primaryVariant)}",""")
            appendLine("""    "onPrimary": "${colorHex(c.onPrimary)}",""")
            appendLine("""    "background": "${colorHex(c.background)}",""")
            appendLine("""    "surface": "${colorHex(c.surface)}",""")
            appendLine("""    "surfaceVariant": "${colorHex(c.surfaceVariant)}",""")
            appendLine("""    "onBackground": "${colorHex(c.onBackground)}",""")
            appendLine("""    "onSurface": "${colorHex(c.onSurface)}",""")
            appendLine("""    "onSurfaceVariant": "${colorHex(c.onSurfaceVariant)}",""")
            appendLine("""    "error": "${colorHex(c.error)}",""")
            appendLine("""    "onError": "${colorHex(c.onError)}",""")
            appendLine("""    "success": "${colorHex(c.success)}",""")
            appendLine("""    "warning": "${colorHex(c.warning)}",""")
            appendLine("""    "info": "${colorHex(c.info)}",""")
            appendLine("""    "onInfo": "${colorHex(c.onInfo)}",""")
            appendLine("""    "link": "${colorHex(c.link)}",""")
            appendLine("""    "focus": "${colorHex(c.focus)}",""")
            appendLine("""    "errorContainer": "${colorHex(c.errorContainer)}",""")
            appendLine("""    "successContainer": "${colorHex(c.successContainer)}",""")
            appendLine("""    "warningContainer": "${colorHex(c.warningContainer)}",""")
            appendLine("""    "infoContainer": "${colorHex(c.infoContainer)}",""")
            appendLine("""    "divider": "${colorHex(c.divider)}",""")
            appendLine("""    "disabled": "${colorHex(c.disabled)}",""")
            appendLine("""    "onDisabled": "${colorHex(c.onDisabled)}"  """)
            appendLine("""  },""")
            appendLine("""  "spacing": {""")
            appendLine("""    "xxs": ${s.xxs.value.toInt()},""")
            appendLine("""    "xs": ${s.xs.value.toInt()},""")
            appendLine("""    "sm": ${s.sm.value.toInt()},""")
            appendLine("""    "md": ${s.md.value.toInt()},""")
            appendLine("""    "lg": ${s.lg.value.toInt()},""")
            appendLine("""    "xl": ${s.xl.value.toInt()},""")
            appendLine("""    "xxl": ${s.xxl.value.toInt()}""")
            appendLine("""  },""")
            appendLine("""  "typography": {""")
            appendLine("""    "h1": ${typographyJson(t.h1)},""")
            appendLine("""    "h2": ${typographyJson(t.h2)},""")
            appendLine("""    "h3": ${typographyJson(t.h3)},""")
            appendLine("""    "body": ${typographyJson(t.body)},""")
            appendLine("""    "bodySmall": ${typographyJson(t.bodySmall)},""")
            appendLine("""    "label": ${typographyJson(t.label)},""")
            appendLine("""    "caption": ${typographyJson(t.caption)},""")
            appendLine("""    "button": ${typographyJson(t.button)}""")
            appendLine("""  },""")
            appendLine("""  "shapes": {""")
            appendLine("""    "sm": ${cornerDp(sh.sm)},""")
            appendLine("""    "md": ${cornerDp(sh.md)},""")
            appendLine("""    "lg": ${cornerDp(sh.lg)}""")
            appendLine("""  }""")
            append("}")
        }
    }

    // ── Merge helpers ─────────────────────────────────────────────────────────

    private fun mergeColors(obj: JsonObject?, base: CrmColors): CrmColors {
        if (obj == null) return base
        fun hex(key: String, fallback: Color) = parseHex(obj[key]?.jsonPrimitive?.contentOrNull, fallback)
        return CrmColors(
            primary = hex("primary", base.primary),
            primaryVariant = hex("primaryVariant", base.primaryVariant),
            onPrimary = hex("onPrimary", base.onPrimary),
            background = hex("background", base.background),
            surface = hex("surface", base.surface),
            surfaceVariant = hex("surfaceVariant", base.surfaceVariant),
            onBackground = hex("onBackground", base.onBackground),
            onSurface = hex("onSurface", base.onSurface),
            onSurfaceVariant = hex("onSurfaceVariant", base.onSurfaceVariant),
            error = hex("error", base.error),
            onError = hex("onError", base.onError),
            success = hex("success", base.success),
            warning = hex("warning", base.warning),
            info = hex("info", base.info),
            onInfo = hex("onInfo", base.onInfo),
            link = hex("link", base.link),
            focus = hex("focus", base.focus),
            errorContainer = hex("errorContainer", base.errorContainer),
            successContainer = hex("successContainer", base.successContainer),
            warningContainer = hex("warningContainer", base.warningContainer),
            infoContainer = hex("infoContainer", base.infoContainer),
            divider = hex("divider", base.divider),
            disabled = hex("disabled", base.disabled),
            onDisabled = hex("onDisabled", base.onDisabled),
        )
    }

    private fun mergeSpacing(obj: JsonObject?, base: CrmSpacing): CrmSpacing {
        if (obj == null) return base
        fun dpVal(key: String, fallback: androidx.compose.ui.unit.Dp) =
            obj[key]?.jsonPrimitive?.intOrNull?.dp ?: fallback
        return CrmSpacing(
            xxs = dpVal("xxs", base.xxs),
            xs = dpVal("xs", base.xs),
            sm = dpVal("sm", base.sm),
            md = dpVal("md", base.md),
            lg = dpVal("lg", base.lg),
            xl = dpVal("xl", base.xl),
            xxl = dpVal("xxl", base.xxl),
        )
    }

    private fun mergeTypography(obj: JsonObject?, base: CrmTypography): CrmTypography {
        if (obj == null) return base
        fun style(key: String, fallback: TextStyle): TextStyle {
            val entry = obj[key]?.jsonObject ?: return fallback
            val size = entry["size"]?.jsonPrimitive?.floatOrNull
            val weight = entry["weight"]?.jsonPrimitive?.contentOrNull?.let { parseWeight(it) }
            return fallback.copy(
                fontSize = size?.sp ?: fallback.fontSize,
                fontWeight = weight ?: fallback.fontWeight,
            )
        }
        return CrmTypography(
            h1 = style("h1", base.h1),
            h2 = style("h2", base.h2),
            h3 = style("h3", base.h3),
            body = style("body", base.body),
            bodySmall = style("bodySmall", base.bodySmall),
            label = style("label", base.label),
            caption = style("caption", base.caption),
            button = style("button", base.button),
            mono = style("mono", base.mono),
        )
    }

    private fun mergeShapes(obj: JsonObject?, base: CrmShapes): CrmShapes {
        if (obj == null) return base
        fun shape(key: String, fallback: RoundedCornerShape): RoundedCornerShape {
            val r = obj[key]?.jsonPrimitive?.intOrNull ?: return fallback
            return RoundedCornerShape(r.dp)
        }
        return CrmShapes(
            sm = shape("sm", base.sm),
            md = shape("md", base.md),
            lg = shape("lg", base.lg),
            pill = base.pill,
        )
    }

    // ── Serialization helpers ─────────────────────────────────────────────────

    private fun colorHex(color: Color): String {
        val a = (color.alpha * 255f + 0.5f).toInt().coerceIn(0, 255)
        val r = (color.red * 255f + 0.5f).toInt().coerceIn(0, 255)
        val g = (color.green * 255f + 0.5f).toInt().coerceIn(0, 255)
        val b = (color.blue * 255f + 0.5f).toInt().coerceIn(0, 255)
        return if (a == 255) "#${byteHex(r)}${byteHex(g)}${byteHex(b)}"
        else "#${byteHex(a)}${byteHex(r)}${byteHex(g)}${byteHex(b)}"
    }

    private fun byteHex(b: Int): String {
        val s = b.toString(16).uppercase()
        return if (s.length == 1) "0$s" else s
    }

    private fun typographyJson(style: TextStyle): String {
        val size = style.fontSize.value
        val weight = weightName(style.fontWeight ?: FontWeight.Normal)
        return """{"size": $size, "weight": "$weight"}"""
    }

    private fun weightName(w: FontWeight) = when (w) {
        FontWeight.Bold -> "bold"
        FontWeight.SemiBold -> "semibold"
        FontWeight.Medium -> "medium"
        else -> "normal"
    }

    private fun jsonStr(s: String) = "\"${s.replace("\"", "\\\"")}\""

    // Use density=1f so toPx() returns the dp value as-is.
    private fun cornerDp(shape: RoundedCornerShape): Int =
        shape.topStart.toPx(Size(0f, 0f), unitDensity).toInt()

    // ── Parsing helpers ───────────────────────────────────────────────────────

    private fun parseHex(hex: String?, fallback: Color): Color {
        if (hex.isNullOrBlank() || !hex.startsWith("#")) return fallback
        return runCatching {
            val cleaned = hex.removePrefix("#")
            when (cleaned.length) {
                6 -> Color((0xFF000000L or cleaned.toLong(16)).toInt())
                8 -> Color(cleaned.toLong(16).toInt())
                else -> fallback
            }
        }.getOrElse { fallback }
    }

    private fun parseWeight(w: String) = when (w.lowercase()) {
        "bold" -> FontWeight.Bold
        "semibold" -> FontWeight.SemiBold
        "medium" -> FontWeight.Medium
        "normal" -> FontWeight.Normal
        else -> null
    }
}
