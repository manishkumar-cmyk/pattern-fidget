package com.fidget.patternlock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.min

/** A static miniature of a saved pattern. The start dot is drawn slightly larger. */
class PatternThumb(context: Context, private val n: Int, private val dots: List<Int>, private val theme: Theme) : View(context) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val rect = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, w)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        rect.set(0f, 0f, w, w)
        fill.color = theme.surface
        canvas.drawRoundRect(rect, w * 0.12f, w * 0.12f, fill)

        val side = w * 0.74f
        val spacing = side / n
        val off = (w - side) / 2f
        fun cx(i: Int) = off + spacing * (i % n + 0.5f)
        fun cy(i: Int) = off + spacing * (i / n + 0.5f)
        val r = spacing * 0.08f

        fill.color = theme.dot
        for (i in 0 until n * n) canvas.drawCircle(cx(i), cy(i), r, fill)

        path.reset()
        dots.forEachIndexed { k, i -> if (k == 0) path.moveTo(cx(i), cy(i)) else path.lineTo(cx(i), cy(i)) }
        line.color = theme.line
        line.strokeWidth = spacing * 0.07f
        canvas.drawPath(path, line)

        fill.color = theme.active
        dots.forEachIndexed { k, i -> canvas.drawCircle(cx(i), cy(i), if (k == 0) r * 2.4f else r * 1.5f, fill) }
    }
}

/** A thin back chevron. */
class BackIcon(context: Context, color: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 2f * context.resources.displayMetrics.density
    }
    private val path = Path()

    init { contentDescription = "Back" }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val s = min(width, height) * 0.17f
        path.reset()
        path.moveTo(cx + s * 0.5f, cy - s)
        path.lineTo(cx - s * 0.5f, cy)
        path.lineTo(cx + s * 0.5f, cy + s)
        canvas.drawPath(path, paint)
    }
}

/** A theme preview circle: the theme's background with its accent dot; ringed when chosen. */
class SwatchView(context: Context, private val swatch: Theme, private val chosen: Boolean, private val ringColor: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val d = context.resources.displayMetrics.density

    init { contentDescription = swatch.name }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f
        if (chosen) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f * d
            paint.color = ringColor
            canvas.drawCircle(cx, cy, r - d, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = swatch.background
        canvas.drawCircle(cx, cy, r * 0.72f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * d
        paint.color = swatch.dot
        canvas.drawCircle(cx, cy, r * 0.72f, paint)
        paint.style = Paint.Style.FILL
        paint.color = swatch.active
        canvas.drawCircle(cx, cy, r * 0.24f, paint)
    }
}
