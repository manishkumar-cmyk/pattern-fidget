package com.fidget.patternlock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.fidget.patternlock.Theme
import com.fidget.patternlock.Themes
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * A small landscape in a theme's own colours, one scene per theme: misty ridges under a glowing moon (Dusk, Fog),
 * layered leaves (Sage), water and peaks (Tide), drifting smoke (Ink). Used wherever a theme is previewed.
 */
@Composable
fun ThemeArt(theme: Theme, modifier: Modifier = Modifier.fillMaxSize()) {
    Canvas(modifier.clearAndSetSemantics { }) {
        drawRect(Brush.verticalGradient(listOf(Color(theme.bgTop), Color(theme.bgMid), Color(theme.bgBottom))))
        when (theme.name) {
            "Sage" -> sage(theme)
            "Ink" -> ink(theme)
            "Ember" -> ember(theme)
            "Tide" -> { moon(theme, 0.74f, 0.28f); ridges(theme, 3, sharp = true); waves(theme) }
            "Fog" -> { moon(theme, 0.74f, 0.30f); ridges(theme, 4, sharp = false, haze = 0.35f) }
            else -> { moon(theme, 0.76f, 0.28f); ridges(theme, 3, sharp = true) }
        }
    }
}

private fun DrawScope.moon(theme: Theme, fx: Float, fy: Float) {
    val c = Offset(size.width * fx, size.height * fy)
    val g = theme.glow.coerceAtLeast(0.3f)
    drawCircle(
        Brush.radialGradient(
            listOf(Color(theme.glowColor).copy(alpha = 0.6f * g), Color(theme.glowColor).copy(alpha = 0.18f * g), Color.Transparent),
            center = c, radius = size.height * 1.0f),
        radius = size.height * 1.0f, center = c)
    drawCircle(Color(theme.secondaryGlow).copy(alpha = 0.7f), size.height * 0.05f, c)
}

/** Layered ridges, each paler towards the horizon, with mist where one layer meets the next. */
private fun DrawScope.ridges(theme: Theme, layers: Int, sharp: Boolean, haze: Float = 0.2f) {
    val w = size.width
    val h = size.height
    val far = Color(Themes.mix(theme.bgBottom, theme.glowColor, 0.28f))
    val near = Color(theme.bgTop)
    for (k in 0 until layers) {
        val t = k / (layers - 1f).coerceAtLeast(1f)
        val base = h * (0.52f + 0.14f * k)
        val amp = h * (0.2f - 0.035f * k)
        val path = Path()
        path.moveTo(0f, h)
        var x = 0f
        while (x <= w + 1f) {
            val u = x / w
            val wave = sin((u * (1.6f + k * 0.55f) + k * 0.43f) * 2 * PI).toFloat() * 0.6f +
                sin((u * (4.1f + k) + k) * 2 * PI).toFloat() * 0.25f
            val peak = if (sharp) 1f - abs(wave) * 0.9f else 0.5f + wave * 0.5f
            path.lineTo(x, base - amp * peak)
            x += w / 60f
        }
        path.lineTo(w, h)
        path.close()
        val body = Color(Themes.mix(far.toArgb(), near.toArgb(), t * 0.85f))
        drawPath(path, Brush.verticalGradient(
            listOf(body.copy(alpha = 0.9f), Color(theme.bgTop).copy(alpha = 0.95f)), startY = base - amp, endY = h))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(theme.glowColor).copy(alpha = haze * 0.25f), Color.Transparent),
            startY = base - amp * 0.4f, endY = base + amp * 0.9f))
    }
}

private fun DrawScope.waves(theme: Theme) {
    val h = size.height
    val w = size.width
    for (k in 0 until 4) {
        val y = h * (0.82f + 0.045f * k)
        val p = Path()
        p.moveTo(0f, y)
        var x = 0f
        while (x <= w) { p.lineTo(x, y + sin((x / w * (3f + k) + k) * 2 * PI).toFloat() * h * 0.012f); x += w / 40f }
        drawPath(p, Color(theme.glowColor).copy(alpha = 0.22f - 0.04f * k), style = Stroke(1.2f))
    }
}

private fun DrawScope.sage(theme: Theme) {
    moon(theme, 0.8f, 0.3f)
    val h = size.height
    val w = size.width
    val leaf = Path().apply {
        moveTo(0f, 0f)
        cubicTo(0.35f, -0.55f, 1.0f, -0.55f, 1.5f, 0f)
        cubicTo(1.0f, 0.55f, 0.35f, 0.55f, 0f, 0f)
    }
    for ((fx, fy, sc, rot, mixv) in listOf(
        Leaf(0.62f, 0.95f, 0.55f, -35f, 0.5f), Leaf(0.88f, 0.9f, 0.6f, -140f, 0.4f),
        Leaf(0.40f, 1.05f, 0.5f, -60f, 0.25f), Leaf(1.0f, 0.55f, 0.45f, 160f, 0.55f),
    )) {
        val s = h * sc
        rotate(rot, Offset(w * fx, h * fy)) {
            withTransform({
                translate(w * fx, h * fy); scale(s, s, Offset.Zero)
            }) {
                drawPath(leaf, Brush.linearGradient(
                    listOf(Color(Themes.mix(theme.bgBottom, theme.glowColor, mixv)), Color(theme.bgTop)),
                    start = Offset(0f, -0.4f), end = Offset(1.5f, 0.4f)))
                drawLine(Color(theme.secondaryGlow).copy(alpha = 0.35f), Offset.Zero, Offset(1.4f, 0f), strokeWidth = 0.012f)
            }
        }
    }
}

private fun DrawScope.ember(theme: Theme) {
    val w = size.width
    val h = size.height
    val glow = Color(theme.glowColor)
    val gold = Color(theme.secondaryGlow)
    val pts = listOf(Offset(0.58f, 0.62f), Offset(0.76f, 0.3f), Offset(0.4f, 0.34f), Offset(0.88f, 0.7f), Offset(0.22f, 0.7f))
        .map { Offset(it.x * w, it.y * h) }
    fun line(a: Offset, b: Offset) {
        drawLine(glow.copy(alpha = 0.18f), a, b, strokeWidth = h * 0.09f)
        drawLine(gold, a, b, strokeWidth = h * 0.016f)
    }
    line(pts[2], pts[0]); line(pts[0], pts[1])
    for ((i, p) in pts.withIndex()) {
        val r = if (i < 3) h * 0.5f else h * 0.22f
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = if (i < 3) 0.5f else 0.2f), Color.Transparent), center = p, radius = r), radius = r, center = p)
        drawCircle(Color(theme.active), h * (if (i < 3) 0.05f else 0.032f), p)
    }
    for (k in 0 until 14) {
        val x = ((k * 37) % 100) / 100f * w
        val y = ((k * 53 + 11) % 100) / 100f * h
        drawCircle(gold.copy(alpha = 0.25f + (k % 3) * 0.15f), h * 0.011f, Offset(x, y))
    }
}

private data class Leaf(val fx: Float, val fy: Float, val sc: Float, val rot: Float, val mix: Float)

private fun DrawScope.ink(theme: Theme) {
    val h = size.height
    val w = size.width
    for ((fx, fy, r, a) in listOf(Quad(0.78f, 0.3f, 0.9f, 0.16f), Quad(0.55f, 0.65f, 0.7f, 0.09f), Quad(0.95f, 0.8f, 0.6f, 0.08f))) {
        drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = a), Color.Transparent), center = Offset(w * fx, h * fy), radius = h * r),
            radius = h * r, center = Offset(w * fx, h * fy))
    }
    drawCircle(Color.White.copy(alpha = 0.5f), h * 0.04f, Offset(w * 0.78f, h * 0.3f))
    ridges(theme, 2, sharp = false, haze = 0.1f)
}

private data class Quad(val a: Float, val b: Float, val c: Float, val d: Float)
