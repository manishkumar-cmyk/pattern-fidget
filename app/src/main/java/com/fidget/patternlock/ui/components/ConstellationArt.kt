package com.fidget.patternlock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.Themes
import com.fidget.patternlock.domain.Constellation
import com.fidget.patternlock.ui.theme.FidgetColors
import com.fidget.patternlock.ui.theme.LocalFidget
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Fits a constellation into a [w] x [h] box, keeping its shape, with [pad] around it. */
fun layoutStars(c: Constellation, w: Float, h: Float, pad: Float): List<Offset> {
    val minX = c.stars.minOf { it.first }; val maxX = c.stars.maxOf { it.first }
    val minY = c.stars.minOf { it.second }; val maxY = c.stars.maxOf { it.second }
    val bw = max(maxX - minX, 1f); val bh = max(maxY - minY, 1f)
    val s = min((w - 2 * pad) / bw, (h - 2 * pad) / bh)
    val ox = (w - bw * s) / 2f; val oy = (h - bh * s) / 2f
    return c.stars.map { Offset(ox + (it.first - minX) * s, oy + (it.second - minY) * s) }
}

/**
 * Draws a constellation: stars with soft halos and rings, joined by thin glowing lines.
 * [edgeAmount] says how far each line is drawn (0..1), [starLit] how bright each star is (0..1),
 * and [guide] the strength of the faint outline showing where the lines will go.
 */
fun DrawScope.drawConstellation(
    c: Constellation, pts: List<Offset>, colors: FidgetColors, unit: Float,
    edgeAmount: (Int) -> Float, starLit: (Int) -> Float, guide: Float = 0f, alpha: Float = 1f, boost: Float = 0f,
) {
    val glow = colors.glow
    val line = Color(colors.theme.line)
    val core = Color(Themes.mix(colors.theme.line, 0xFFFFFFFF.toInt(), 0.45f))
    val degree = IntArray(pts.size)
    for ((a, b) in c.edges) { degree[a]++; degree[b]++ }

    if (guide > 0f) for ((a, b) in c.edges) {
        drawLine(line.copy(alpha = guide * alpha), pts[a], pts[b], strokeWidth = unit * 1.1f, cap = StrokeCap.Round)
    }
    c.edges.forEachIndexed { i, (a, b) ->
        val t = edgeAmount(i)
        if (t <= 0f) return@forEachIndexed
        val end = pts[a] + (pts[b] - pts[a]) * t
        val lift = 1f + boost * 0.6f
        drawLine(glow.copy(alpha = (0.13f * lift).coerceAtMost(1f) * alpha), pts[a], end, strokeWidth = unit * 9f, cap = StrokeCap.Round)
        drawLine(glow.copy(alpha = (0.28f * lift).coerceAtMost(1f) * alpha), pts[a], end, strokeWidth = unit * 4f, cap = StrokeCap.Round)
        drawLine(core.copy(alpha = alpha), pts[a], end, strokeWidth = unit * 1.5f, cap = StrokeCap.Round)
    }
    pts.forEachIndexed { i, p ->
        val lit = starLit(i).coerceIn(0f, 1f)
        val big = if (degree[i] >= 3) 1.35f else if (degree[i] == 2) 1.0f else 0.85f
        val r = unit * 3.6f * big
        val a = (0.4f + 0.6f * lit) * alpha
        val haloR = r * (3.2f + 1.8f * lit + boost)
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.55f * a * (0.3f + 0.7f * lit)), glow.copy(alpha = 0.14f * a), Color.Transparent),
            center = p, radius = haloR), radius = haloR, center = p)
        if (lit > 0.5f && degree[i] >= 2) {
            drawCircle(core.copy(alpha = 0.45f * a), r * 2.5f, p, style = Stroke(unit * 0.9f))
        }
        drawCircle(Color(colors.theme.active).copy(alpha = a), r, p)
    }
}

/** A few faint stars around the constellation, placed deterministically from its name. */
fun DrawScope.drawDecor(c: Constellation, colors: FidgetColors, alpha: Float) {
    var seed = c.id.hashCode()
    fun next(): Float { seed = seed * 1103515245 + 12345; return (abs(seed shr 8) % 1000) / 1000f }
    repeat(9) {
        val p = Offset(next() * size.width, next() * size.height)
        drawCircle(Color(colors.theme.secondaryGlow).copy(alpha = (0.18f + next() * 0.3f) * alpha), (0.8f + next() * 1.6f) * density, p)
    }
}

/** A static or progressively drawn constellation. [progress] 0..1 draws the lines one after another. */
@Composable
fun ConstellationArt(
    c: Constellation, modifier: Modifier = Modifier.fillMaxSize(), progress: Float = 1f, alpha: Float = 1f,
    decor: Boolean = false, pad: Dp = 16.dp, unit: Dp = 1.6.dp, dim: Float = 0f,
) {
    val colors = LocalFidget.current
    Canvas(modifier.clearAndSetSemantics { }) {
        val pts = layoutStars(c, size.width, size.height, pad.toPx())
        val n = c.edges.size
        if (decor) drawDecor(c, colors, alpha)
        fun amount(i: Int) = (progress * n - i).coerceIn(0f, 1f)
        val touched = BooleanArray(pts.size)
        c.edges.forEachIndexed { i, (a, b) -> if (amount(i) > 0f) { touched[a] = true; if (amount(i) >= 1f) touched[b] = true } }
        drawConstellation(c, pts, colors, unit.toPx(), ::amount, { if (touched[it]) 1f else 0f }, guide = 0.12f, alpha = alpha * (1f - dim))
    }
}
