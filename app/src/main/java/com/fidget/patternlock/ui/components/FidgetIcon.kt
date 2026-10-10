package com.fidget.patternlock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class FidgetIconKind {
    BACK, GEAR, TUNE, DRAW, MEMORY, COLLECTION, HEART, HEART_OUTLINE, PLAY, MORE, SEARCH, CLOSE,
    INFINITY, MIRROR, RIPPLE, CONSTELLATION, ZEN, EYE, RETRY, HOME, LOOP, CHECK, CHART, BELL, PIANO, MARIMBA, BUBBLES, WAVE, CHEVRON,
    STAR, STAR_OUTLINE, LOCK, JOURNEY,
}

/** Thin line icons drawn on a 24-unit canvas, so the app needs no image assets. */
@Composable
fun FidgetIcon(kind: FidgetIconKind, tint: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) { drawIcon(kind, tint) }
}

private fun DrawScope.drawIcon(kind: FidgetIconKind, c: Color) {
    val u = size.minDimension / 24f
    val line = Stroke(width = 1.7f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun o(x: Float, y: Float) = Offset(x * u, y * u)
    fun dot(x: Float, y: Float, r: Float = 1.5f) = drawCircle(c, r * u, o(x, y), style = Fill)
    fun seg(x0: Float, y0: Float, x1: Float, y1: Float) = drawLine(c, o(x0, y0), o(x1, y1), 1.7f * u, StrokeCap.Round)
    fun poly(vararg p: Float, close: Boolean = false, fill: Boolean = false) {
        val path = Path()
        path.moveTo(p[0] * u, p[1] * u)
        var k = 2
        while (k < p.size) { path.lineTo(p[k] * u, p[k + 1] * u); k += 2 }
        if (close) path.close()
        drawPath(path, c, style = if (fill) Fill else line)
    }
    when (kind) {
        FidgetIconKind.BACK -> poly(14.5f, 5f, 7.5f, 12f, 14.5f, 19f)
        FidgetIconKind.CLOSE -> { seg(6f, 6f, 18f, 18f); seg(18f, 6f, 6f, 18f) }
        FidgetIconKind.BELL -> {
            val bell = Path().apply {
                moveTo(6f * u, 17f * u); cubicTo(7.5f * u, 15f * u, 7f * u, 12f * u, 7f * u, 10.5f * u)
                cubicTo(7f * u, 7.5f * u, 9f * u, 5.5f * u, 12f * u, 5.5f * u)
                cubicTo(15f * u, 5.5f * u, 17f * u, 7.5f * u, 17f * u, 10.5f * u)
                cubicTo(17f * u, 12f * u, 16.5f * u, 15f * u, 18f * u, 17f * u); close()
            }
            drawPath(bell, c, style = line); seg(10.3f, 20f, 13.7f, 20f); seg(12f, 3.8f, 12f, 5.5f)
        }
        FidgetIconKind.PIANO -> {
            drawRoundRect(c, o(4f, 5f), Size(16f * u, 14f * u), androidx.compose.ui.geometry.CornerRadius(2.5f * u), style = line)
            seg(9.3f, 12f, 9.3f, 19f); seg(14.7f, 12f, 14.7f, 19f)
            for (x in listOf(7.5f, 12f, 16.5f)) seg(x, 5.5f, x, 12f)
        }
        FidgetIconKind.MARIMBA -> {
            for ((k, x) in listOf(5f, 9f, 13f, 17f).withIndex()) {
                val top = 6f + k * 1.2f
                drawRoundRect(c, o(x, top), Size(3f * u, (19f - top) * u), androidx.compose.ui.geometry.CornerRadius(1.2f * u), style = line)
            }
        }
        FidgetIconKind.BUBBLES -> {
            drawCircle(c, 4.2f * u, o(9f, 14.5f), style = line)
            drawCircle(c, 2.6f * u, o(16.5f, 8f), style = line)
            drawCircle(c, 1.3f * u, o(17f, 16.5f), style = line)
        }
        FidgetIconKind.WAVE -> {
            val w = Path().apply {
                moveTo(3f * u, 12f * u)
                cubicTo(6f * u, 5f * u, 9f * u, 5f * u, 12f * u, 12f * u)
                cubicTo(15f * u, 19f * u, 18f * u, 19f * u, 21f * u, 12f * u)
            }
            drawPath(w, c, style = line)
        }
        FidgetIconKind.CHEVRON -> poly(9f, 5f, 16f, 12f, 9f, 19f)
        FidgetIconKind.CHART -> { seg(6f, 19f, 6f, 12f); seg(12f, 19f, 12f, 5f); seg(18f, 19f, 18f, 9f) }
        FidgetIconKind.CHECK -> poly(5f, 12.5f, 10f, 17.5f, 19f, 7f)
        FidgetIconKind.GEAR -> {
            drawCircle(c, 3.2f * u, o(12f, 12f), style = line)
            drawCircle(c, 6.4f * u, o(12f, 12f), style = line)
            for (k in 0 until 8) {
                val a = k * PI / 4
                seg(12f + 7.4f * cos(a).toFloat(), 12f + 7.4f * sin(a).toFloat(),
                    12f + 9.4f * cos(a).toFloat(), 12f + 9.4f * sin(a).toFloat())
            }
        }
        FidgetIconKind.TUNE -> {
            seg(4f, 7f, 20f, 7f); seg(4f, 12f, 20f, 12f); seg(4f, 17f, 20f, 17f)
            drawCircle(c, 2.2f * u, o(15f, 7f), style = Fill); drawCircle(c, 2.2f * u, o(8f, 12f), style = Fill)
            drawCircle(c, 2.2f * u, o(13f, 17f), style = Fill)
        }
        FidgetIconKind.DRAW -> {
            // a pencil
            poly(5f, 19f, 6f, 15f, 16f, 5f, 19f, 8f, 9f, 18f, close = true)
            seg(14f, 7f, 17f, 10f)
        }
        FidgetIconKind.MEMORY -> {
            // a brain: two lobes, a centre line and a few folds
            for (mirror in listOf(false, true)) {
                fun x(v: Float) = (if (mirror) 24f - v else v) * u
                val lobe = Path().apply {
                    moveTo(x(12f), 4.6f * u)
                    cubicTo(x(9.6f), 2.8f * u, x(6f), 3.8f * u, x(5.7f), 7.2f * u)
                    cubicTo(x(3.2f), 8f * u, x(3f), 12f * u, x(5.4f), 13.2f * u)
                    cubicTo(x(4.8f), 16.6f * u, x(8.2f), 19.2f * u, x(10.2f), 17.8f * u)
                    cubicTo(x(11f), 19.4f * u, x(12f), 19.8f * u, x(12f), 19.8f * u)
                }
                drawPath(lobe, c, style = line)
                val fold1 = Path().apply { moveTo(x(5.7f), 7.2f * u); cubicTo(x(7.8f), 7.2f * u, x(9f), 8.6f * u, x(9f), 10.4f * u) }
                val fold2 = Path().apply { moveTo(x(5.4f), 13.2f * u); cubicTo(x(7.6f), 13.2f * u, x(9.2f), 12.6f * u, x(9.6f), 11.4f * u) }
                drawPath(fold1, c, style = line); drawPath(fold2, c, style = line)
            }
            seg(12f, 4.6f, 12f, 19.8f)
        }
        FidgetIconKind.COLLECTION -> {
            for ((x, y) in listOf(4f to 4f, 13.5f to 4f, 4f to 13.5f, 13.5f to 13.5f))
                drawRoundRect(c, o(x, y), Size(6.5f * u, 6.5f * u), androidx.compose.ui.geometry.CornerRadius(2f * u), style = line)
        }
        FidgetIconKind.HEART, FidgetIconKind.HEART_OUTLINE -> {
            val p = Path().apply {
                moveTo(12f * u, 19.5f * u)
                cubicTo(2f * u, 12.5f * u, 5f * u, 4.5f * u, 12f * u, 9f * u)
                cubicTo(19f * u, 4.5f * u, 22f * u, 12.5f * u, 12f * u, 19.5f * u)
                close()
            }
            drawPath(p, c, style = if (kind == FidgetIconKind.HEART) Fill else line)
        }
        FidgetIconKind.PLAY -> poly(9f, 6f, 18f, 12f, 9f, 18f, close = true, fill = true)
        FidgetIconKind.MORE -> { dot(12f, 5.5f, 1.7f); dot(12f, 12f, 1.7f); dot(12f, 18.5f, 1.7f) }
        FidgetIconKind.SEARCH -> { drawCircle(c, 5.6f * u, o(10.5f, 10.5f), style = line); seg(14.8f, 14.8f, 19.5f, 19.5f) }
        FidgetIconKind.INFINITY -> {
            val p = Path().apply {
                moveTo(12f * u, 12f * u)
                cubicTo(15f * u, 7f * u, 21f * u, 7f * u, 21f * u, 12f * u)
                cubicTo(21f * u, 17f * u, 15f * u, 17f * u, 12f * u, 12f * u)
                cubicTo(9f * u, 7f * u, 3f * u, 7f * u, 3f * u, 12f * u)
                cubicTo(3f * u, 17f * u, 9f * u, 17f * u, 12f * u, 12f * u)
            }
            drawPath(p, c, style = line)
        }
        FidgetIconKind.MIRROR -> {
            drawLine(c, o(12f, 3f), o(12f, 21f), 1.2f * u, StrokeCap.Round,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f * u, 2.5f * u)))
            poly(4f, 17f, 8f, 8f, 10f, 15f)
            poly(20f, 17f, 16f, 8f, 14f, 15f)
        }
        FidgetIconKind.RIPPLE -> {
            dot(12f, 12f, 1.8f)
            drawCircle(c, 5f * u, o(12f, 12f), style = line)
            drawCircle(c.copy(alpha = c.alpha * 0.6f), 9f * u, o(12f, 12f), style = line)
        }
        FidgetIconKind.CONSTELLATION -> {
            poly(4f, 17f, 9f, 8f, 15f, 13f, 20f, 5f)
            dot(4f, 17f); dot(9f, 8f); dot(15f, 13f); dot(20f, 5f)
        }
        FidgetIconKind.ZEN -> {
            // a lotus: three petals
            poly(12f, 19f, 12f, 8.5f, close = false)
            val left = Path().apply { moveTo(12f * u, 19f * u); cubicTo(5f * u, 18f * u, 3f * u, 12f * u, 4f * u, 9f * u); cubicTo(9f * u, 9f * u, 12f * u, 13f * u, 12f * u, 19f * u) }
            val right = Path().apply { moveTo(12f * u, 19f * u); cubicTo(19f * u, 18f * u, 21f * u, 12f * u, 20f * u, 9f * u); cubicTo(15f * u, 9f * u, 12f * u, 13f * u, 12f * u, 19f * u) }
            val mid = Path().apply { moveTo(12f * u, 19f * u); cubicTo(8f * u, 14f * u, 9f * u, 8f * u, 12f * u, 4.5f * u); cubicTo(15f * u, 8f * u, 16f * u, 14f * u, 12f * u, 19f * u) }
            drawPath(left, c, style = line); drawPath(right, c, style = line); drawPath(mid, c, style = line)
        }
        FidgetIconKind.EYE -> {
            val p = Path().apply { moveTo(3f * u, 12f * u); cubicTo(7f * u, 5f * u, 17f * u, 5f * u, 21f * u, 12f * u); cubicTo(17f * u, 19f * u, 7f * u, 19f * u, 3f * u, 12f * u) }
            drawPath(p, c, style = line)
            drawCircle(c, 2.6f * u, o(12f, 12f), style = line)
        }
        FidgetIconKind.RETRY -> {
            drawArc(c, -40f, 280f, false, Offset(5f * u, 5f * u), Size(14f * u, 14f * u), style = line)
            poly(17.5f, 3.5f, 18.2f, 7.8f, 13.9f, 8.4f)
        }
        FidgetIconKind.HOME -> poly(4f, 11f, 12f, 4f, 20f, 11f, 18f, 11f, 18f, 19f, 6f, 19f, 6f, 11f, 4f, 11f)
        FidgetIconKind.LOOP -> {
            drawArc(c, 20f, 300f, false, Offset(5f * u, 5f * u), Size(14f * u, 14f * u), style = line)
            poly(18f, 2.5f, 18.6f, 6.8f, 14.4f, 7.4f)
        }
        FidgetIconKind.STAR, FidgetIconKind.STAR_OUTLINE -> {
            // five points, rounded joins
            val pts = FloatArray(20)
            for (k in 0 until 10) {
                val r = if (k % 2 == 0) 9f else 4f
                val a = -PI / 2 + k * PI / 5
                pts[k * 2] = 12f + r * cos(a).toFloat()
                pts[k * 2 + 1] = 12.6f + r * sin(a).toFloat()
            }
            poly(*pts, close = true, fill = kind == FidgetIconKind.STAR)
            if (kind == FidgetIconKind.STAR) poly(*pts, close = true)
        }
        FidgetIconKind.LOCK -> {
            drawRoundRect(c, o(5.5f, 10.5f), Size(13f * u, 9.5f * u), androidx.compose.ui.geometry.CornerRadius(2f * u), style = line)
            drawArc(c, 180f, 180f, false, Offset(8.5f * u, 5f * u), Size(7f * u, 8f * u), style = line)
            dot(12f, 15.2f, 1.2f)
        }
        FidgetIconKind.JOURNEY -> {
            // a winding path between three stops
            val p = Path().apply {
                moveTo(5f * u, 19f * u)
                cubicTo(14f * u, 19f * u, 14f * u, 12f * u, 10f * u, 12f * u)
                cubicTo(6f * u, 12f * u, 7f * u, 5f * u, 19f * u, 5f * u)
            }
            drawPath(p, c, style = line)
            dot(5f, 19f, 2f); dot(11.5f, 12f, 1.6f); dot(19f, 5f, 2f)
        }
    }
}
