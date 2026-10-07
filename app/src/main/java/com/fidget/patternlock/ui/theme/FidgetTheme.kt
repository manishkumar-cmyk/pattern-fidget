package com.fidget.patternlock.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fidget.patternlock.Theme
import com.fidget.patternlock.Themes

/** Colour tokens for the current theme: background, surfaces, borders, accent, dots, text, glow, ripple, error. */
@Immutable
class FidgetColors(val theme: Theme) {
    val bgTop = Color(theme.bgTop)
    val bgMid = Color(theme.bgMid)
    val bgBottom = Color(theme.bgBottom)
    val background = Brush.verticalGradient(listOf(bgTop, bgMid, bgBottom))
    /** Translucent card surface, e.g. rgba(18, 28, 48, 0.75) on Dusk. */
    val surface = Color(theme.surface).copy(alpha = 0.75f)
    val elevatedSurface = Color(Themes.mix(theme.surface, theme.text, 0.08f)).copy(alpha = 0.85f)
    val border = Color(theme.text).copy(alpha = 0.07f)
    val accent = Color(theme.active)
    val accentSurface = Color(theme.glowColor).copy(alpha = 0.22f)
    val inactiveDot = Color(theme.dot)
    val textPrimary = Color(theme.text)
    val textSecondary = Color(theme.muted)
    val glow = Color(theme.glowColor)
    val secondaryGlow = Color(theme.secondaryGlow)
    val ripple = Color(theme.ripple)
    val error = Color(theme.error)
    /** Text or icon colour that sits on top of the solid accent. */
    val onAccent = Color(theme.bgTop)
}

val LocalFidget = compositionLocalOf { FidgetColors(Themes.all[0]) }

object FidgetType {
    private val sans = FontFamily.SansSerif
    val title = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 22.sp, letterSpacing = 0.1.sp)
    val screenTitle = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 18.sp)
    val hero = TextStyle(fontFamily = sans, fontWeight = FontWeight.Light, fontSize = 34.sp, letterSpacing = 0.5.sp)
    val body = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 15.sp)
    val bodyMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    val caption = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 13.sp)
    val label = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.2.sp)
}

@Composable
fun FidgetTheme(theme: Theme, content: @Composable () -> Unit) {
    val colors = remember(theme) { FidgetColors(theme) }
    val scheme = remember(theme) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            background = colors.bgTop, onBackground = colors.textPrimary,
            surface = colors.bgMid, onSurface = colors.textPrimary,
            surfaceVariant = colors.surface, onSurfaceVariant = colors.textSecondary,
            error = colors.error,
        )
    }
    CompositionLocalProvider(LocalFidget provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
