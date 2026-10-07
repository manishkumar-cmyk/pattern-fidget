package com.fidget.patternlock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.fidget.patternlock.Theme
import kotlin.math.PI
import kotlin.math.sin

/** A small landscape in a theme's own colours: a glowing light over soft ridges. Used wherever a theme is previewed. */
@Composable
fun ThemeArt(theme: Theme, modifier: Modifier = Modifier.fillMaxSize()) {
    Canvas(modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Color(theme.bgTop), Color(theme.bgMid), Color(theme.bgBottom))))
        val g = theme.glow.coerceAtLeast(0.25f)
        drawCircle(
            Brush.radialGradient(
                listOf(Color(theme.glowColor).copy(alpha = 0.55f * g), Color(theme.glowColor).copy(alpha = 0.12f * g), Color.Transparent),
                center = Offset(w * 0.78f, h * 0.30f), radius = h * 0.9f),
            radius = h * 0.9f, center = Offset(w * 0.78f, h * 0.30f))
        drawCircle(Color(theme.secondaryGlow).copy(alpha = 0.55f), h * 0.045f, Offset(w * 0.78f, h * 0.30f))
        for (k in 0 until 3) {
            val path = Path()
            val base = h * (0.58f + 0.12f * k)
            val amp = h * (0.13f - 0.025f * k)
            path.moveTo(0f, h)
            var x = 0f
            while (x <= w) {
                val y = base - amp * (0.5f + 0.5f * sin((x / w * (1.4f + k * 0.5f) + k * 0.37f) * 2 * PI).toFloat())
                path.lineTo(x, y)
                x += w / 48f
            }
            path.lineTo(w, h)
            path.close()
            drawPath(path, Color(theme.bgTop).copy(alpha = 0.28f + 0.2f * k))
        }
    }
}
