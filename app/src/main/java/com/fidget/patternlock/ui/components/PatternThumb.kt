package com.fidget.patternlock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.fidget.patternlock.Theme
import com.fidget.patternlock.Themes
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.theme.LocalFidget
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

enum class ThumbStyle { NORMAL, RIPPLE, MIRROR, CONSTELLATION, ZEN }

/**
 * A miniature grid that quietly redraws its pattern in a slow loop. Used by Collection cards, mode cards and
 * theme previews. [offsetMs] staggers neighbours so a screen of cards never animates in lockstep.
 */
@Composable
fun PatternThumb(
    n: Int,
    dots: List<Int>,
    modifier: Modifier = Modifier,
    theme: Theme = LocalFidget.current.theme,
    style: ThumbStyle = ThumbStyle.NORMAL,
    animate: Boolean = true,
    offsetMs: Long = 0L,
    sizeFraction: Float = 0.72f,
) {
    val reduce = LocalEnv.current.settings.reduceMotion
    val running = animate && !reduce
    var clock by remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameMillis { t -> if (t - last >= 33) { last = t; clock = t } }
        }
    }
    val path = remember { Path() }
    Canvas(modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val side = min(w, h) * sizeFraction
        val spacing = side / n
        val ox = (w - side) / 2f
        val oy = (h - side) / 2f
        fun cx(i: Int) = ox + spacing * (i % n + 0.5f)
        fun cy(i: Int) = oy + spacing * (i / n + 0.5f)
        val r = spacing * 0.08f
        val now = clock + offsetMs
        val segs = (dots.size - 1).coerceAtLeast(1)
        val drawMs = segs * 260L
        val cycle = drawMs + 2600L
        val tt = if (running) now % cycle else drawMs + 400L
        val progress = (tt.toFloat() / drawMs).coerceIn(0f, 1f) * segs
        val fade = if (tt > drawMs + 1600L) 1f - ((tt - drawMs - 1600L) / 1000f).coerceIn(0f, 1f) else 1f
        val dotColor = Color(theme.dot)
        val active = Color(theme.active)
        val line = Color(theme.line)
        val glow = Color(theme.glowColor)

        for (i in 0 until n * n) {
            var rr = r
            if (style == ThumbStyle.ZEN && running) rr *= 1f + 0.25f * (0.5f + 0.5f * sin(2 * PI * ((now + i * 150) % 3000) / 3000.0).toFloat())
            drawCircle(dotColor, rr, Offset(cx(i), cy(i)))
        }
        if (style == ThumbStyle.RIPPLE) {
            for (k in dots.indices.take(3)) {
                val p = ((now + k * 700) % 2100) / 2100f
                drawCircle(Color(theme.ripple).copy(alpha = 0.8f * (1 - p)), r * 1.5f + spacing * 0.9f * p,
                    Offset(cx(dots[k]), cy(dots[k])), style = Stroke(spacing * 0.03f))
            }
            return@Canvas
        }
        if (style == ThumbStyle.ZEN || dots.size < 2) return@Canvas

        val lists = ArrayList<List<Int>>()
        lists.add(dots)
        if (style == ThumbStyle.MIRROR) lists.add(dots.map { (it / n) * n + (n - 1 - it % n) })
        lists.forEachIndexed { li, ds ->
            val a = (if (li == 0) 1f else 0.55f) * fade * (if (style == ThumbStyle.CONSTELLATION) 0.7f else 1f)
            path.reset()
            path.moveTo(cx(ds[0]), cy(ds[0]))
            val full = progress.toInt()
            for (k in 1..min(full, segs)) path.lineTo(cx(ds[k]), cy(ds[k]))
            if (full < segs) {
                val f = progress - full
                val a0 = ds[full]; val b0 = ds[full + 1]
                path.lineTo(cx(a0) + (cx(b0) - cx(a0)) * f, cy(a0) + (cy(b0) - cy(a0)) * f)
            }
            val cap = StrokeCap.Round
            if (theme.glow > 0f) {
                drawPath(path, glow.copy(alpha = a * theme.glow * 0.10f), style = Stroke(spacing * 0.3f, cap = cap, join = StrokeJoin.Round))
                drawPath(path, glow.copy(alpha = a * theme.glow * 0.22f), style = Stroke(spacing * 0.14f, cap = cap, join = StrokeJoin.Round))
            }
            drawPath(path, Color(Themes.mix(theme.line, 0xFFFFFFFF.toInt(), 0.2f)).copy(alpha = a),
                style = Stroke(spacing * (if (style == ThumbStyle.CONSTELLATION) 0.025f else 0.05f), cap = cap, join = StrokeJoin.Round))
            for (k in 0..min(full, segs)) {
                val rr = if (k == 0) r * 2f else r * 1.55f
                if (theme.glow > 0f) drawCircle(glow.copy(alpha = a * theme.glow * 0.2f), rr * 2.4f, Offset(cx(ds[k]), cy(ds[k])))
                drawCircle(active.copy(alpha = a), rr, Offset(cx(ds[k]), cy(ds[k])))
            }
        }
    }
}
