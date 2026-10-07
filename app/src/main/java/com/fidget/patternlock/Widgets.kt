package com.fidget.patternlock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Switch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

enum class ThumbStyle { NORMAL, RIPPLE, MIRROR, CONSTELLATION, ZEN, LOOP }

/**
 * A miniature grid that quietly redraws its pattern in a slow loop: collection cards, mode tiles, theme tiles.
 * [offsetMs] staggers neighbours so a screen of cards never animates in lockstep.
 */
class PatternThumb(
    context: Context,
    private val n: Int,
    private val dots: List<Int>,
    var theme: Theme,
    private val style: ThumbStyle = ThumbStyle.NORMAL,
    var animate: Boolean = true,
    private val offsetMs: Long = 0L,
    private val drawBackground: Boolean = false,
    private val square: Boolean = true,
) : View(context) {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val rect = RectF()
    private val visible = android.graphics.Rect()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!square) return super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, w)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (drawBackground) {
            rect.set(0f, 0f, w, h)
            fill.color = theme.surface
            canvas.drawRoundRect(rect, w * 0.1f, w * 0.1f, fill)
        }
        val side = min(w, h) * 0.72f
        val spacing = side / n
        val ox = (w - side) / 2f
        val oy = (h - side) / 2f
        fun cx(i: Int) = ox + spacing * (i % n + 0.5f)
        fun cy(i: Int) = oy + spacing * (i / n + 0.5f)
        val r = spacing * 0.075f

        val now = SystemClock.uptimeMillis() + offsetMs
        val segs = (dots.size - 1).coerceAtLeast(1)
        val drawMs = segs * 260L
        val cycle = drawMs + 2600L
        val tt = if (animate) now % cycle else drawMs + 400L
        val progress = (tt.toFloat() / drawMs).coerceIn(0f, 1f) * segs
        val fade = if (tt > drawMs + 1600L) 1f - ((tt - drawMs - 1600L) / 1000f).coerceIn(0f, 1f) else 1f

        fill.color = theme.dot
        for (i in 0 until n * n) {
            var rr = r
            if (style == ThumbStyle.ZEN && animate) rr *= 1f + 0.25f * (0.5f + 0.5f * sin(2 * PI * ((now + i * 150) % 3000) / 3000.0).toFloat())
            canvas.drawCircle(cx(i), cy(i), rr, fill)
        }

        if (style == ThumbStyle.RIPPLE) {
            for (k in dots.indices.take(3)) {
                val p = ((now + k * 700) % 2100) / 2100f
                line.color = theme.active
                line.alpha = (200 * (1 - p)).toInt()
                line.strokeWidth = spacing * 0.03f
                canvas.drawCircle(cx(dots[k]), cy(dots[k]), r * 1.5f + spacing * 0.9f * p, line)
            }
            return
        }
        if (style == ThumbStyle.ZEN || dots.size < 2) return

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
            if (theme.glow > 0f) {
                line.color = theme.line
                line.alpha = (255 * a * theme.glow * 0.18f).toInt()
                line.strokeWidth = spacing * 0.16f
                canvas.drawPath(path, line)
            }
            line.color = theme.line
            line.alpha = (255 * a).toInt()
            line.strokeWidth = spacing * (if (style == ThumbStyle.CONSTELLATION) 0.025f else 0.05f)
            canvas.drawPath(path, line)
            fill.color = theme.active
            fill.alpha = (255 * a).toInt()
            for (k in 0..min(full, segs)) canvas.drawCircle(cx(ds[k]), cy(ds[k]), if (k == 0) r * 2f else r * 1.5f, fill)
            fill.alpha = 255
        }
        // Only cards actually on screen keep animating; scrolling redraws the rest when they appear.
        if (animate && isShown && getLocalVisibleRect(visible)) postInvalidateDelayed(33)
    }
}

enum class IconKind { BACK, GEAR, TUNE, DRAW, MEMORY, COLLECTION, HEART, HEART_OUTLINE, PLAY, CHART, CLOSE }

/** Thin line icons drawn in code, so the app needs no image assets. */
class Icon(context: Context, var kind: IconKind, var color: Int, label: String) : View(context) {
    private val d = context.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val rect = RectF()

    init { contentDescription = label }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val s = min(width, height) * 0.2f
        paint.color = color
        paint.strokeWidth = 1.8f * d
        paint.style = Paint.Style.STROKE
        path.reset()
        when (kind) {
            IconKind.BACK -> {
                path.moveTo(cx + s * 0.45f, cy - s); path.lineTo(cx - s * 0.5f, cy); path.lineTo(cx + s * 0.45f, cy + s)
                canvas.drawPath(path, paint)
            }
            IconKind.CLOSE -> {
                canvas.drawLine(cx - s * 0.8f, cy - s * 0.8f, cx + s * 0.8f, cy + s * 0.8f, paint)
                canvas.drawLine(cx + s * 0.8f, cy - s * 0.8f, cx - s * 0.8f, cy + s * 0.8f, paint)
            }
            IconKind.GEAR -> {
                canvas.drawCircle(cx, cy, s * 0.45f, paint)
                for (k in 0 until 8) {
                    val a = k * PI / 4
                    canvas.drawLine(cx + (s * 0.75f * kotlin.math.cos(a)).toFloat(), cy + (s * 0.75f * sin(a)).toFloat(),
                        cx + (s * 1.05f * kotlin.math.cos(a)).toFloat(), cy + (s * 1.05f * sin(a)).toFloat(), paint)
                }
                canvas.drawCircle(cx, cy, s * 0.78f, paint)
            }
            IconKind.TUNE -> {
                for ((k, f) in listOf(-0.65f to 0.35f, 0f to -0.4f, 0.65f to 0.15f)) {
                    val y = cy + k * s
                    canvas.drawLine(cx - s, y, cx + s, y, paint)
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(cx + f * s, y, s * 0.2f, paint)
                    paint.style = Paint.Style.STROKE
                }
            }
            IconKind.DRAW -> {
                // three dots joined by a line, like a tiny pattern
                path.moveTo(cx - s, cy + s * 0.6f); path.lineTo(cx, cy - s * 0.6f); path.lineTo(cx + s, cy + s * 0.2f)
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx - s, cy + s * 0.6f, s * 0.2f, paint)
                canvas.drawCircle(cx, cy - s * 0.6f, s * 0.2f, paint)
                canvas.drawCircle(cx + s, cy + s * 0.2f, s * 0.2f, paint)
            }
            IconKind.MEMORY -> {
                rect.set(cx - s * 0.9f, cy - s * 0.9f, cx + s * 0.9f, cy + s * 0.9f)
                canvas.drawArc(rect, -60f, 300f, false, paint)
                path.moveTo(cx + s * 0.45f, cy - s * 1.15f); path.lineTo(cx + s * 0.5f, cy - s * 0.75f); path.lineTo(cx + s * 0.1f, cy - s * 0.65f)
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, s * 0.2f, paint)
            }
            IconKind.COLLECTION -> {
                val q = s * 0.42f
                for ((dx, dy) in listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1)) {
                    rect.set(cx + dx * s * 0.55f - q, cy + dy * s * 0.55f - q, cx + dx * s * 0.55f + q, cy + dy * s * 0.55f + q)
                    canvas.drawRoundRect(rect, q * 0.4f, q * 0.4f, paint)
                }
            }
            IconKind.HEART, IconKind.HEART_OUTLINE -> {
                path.moveTo(cx, cy + s * 0.85f)
                path.cubicTo(cx - s * 1.5f, cy - s * 0.1f, cx - s * 0.6f, cy - s * 1.2f, cx, cy - s * 0.4f)
                path.cubicTo(cx + s * 0.6f, cy - s * 1.2f, cx + s * 1.5f, cy - s * 0.1f, cx, cy + s * 0.85f)
                if (kind == IconKind.HEART) paint.style = Paint.Style.FILL_AND_STROKE
                canvas.drawPath(path, paint)
            }
            IconKind.PLAY -> {
                path.moveTo(cx - s * 0.5f, cy - s * 0.8f); path.lineTo(cx + s * 0.8f, cy); path.lineTo(cx - s * 0.5f, cy + s * 0.8f); path.close()
                paint.style = Paint.Style.FILL_AND_STROKE
                canvas.drawPath(path, paint)
            }
            IconKind.CHART -> {
                canvas.drawLine(cx - s * 0.7f, cy + s * 0.8f, cx - s * 0.7f, cy + s * 0.1f, paint)
                canvas.drawLine(cx, cy + s * 0.8f, cx, cy - s * 0.8f, paint)
                canvas.drawLine(cx + s * 0.7f, cy + s * 0.8f, cx + s * 0.7f, cy - s * 0.3f, paint)
            }
        }
    }
}

/** A soft pill switch. Announced as a switch to screen readers. */
class Toggle(context: Context, var theme: Theme, on: Boolean, private val label: String, private val onChange: (Boolean) -> Unit) : View(context) {
    var checked = on
        private set
    private val d = context.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var pos = if (on) 1f else 0f

    init {
        contentDescription = label
        isClickable = true
        isFocusable = true
        setOnClickListener { set(!checked, true) }
    }

    fun set(v: Boolean, notify: Boolean) {
        if (v == checked) return
        checked = v
        if (notify) onChange(v)
        sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        setMeasuredDimension((52 * d).toInt(), (48 * d).toInt())

    override fun onDraw(canvas: Canvas) {
        val target = if (checked) 1f else 0f
        pos += (target - pos) * 0.3f
        if (kotlin.math.abs(target - pos) > 0.01f) postInvalidateOnAnimation() else pos = target
        val h = 28 * d
        val top = (height - h) / 2f
        rect.set(2 * d, top, width - 2 * d, top + h)
        paint.color = Themes.mix(Themes.mix(theme.surface, theme.dot, 0.5f), theme.active, pos * 0.75f)
        canvas.drawRoundRect(rect, h / 2, h / 2, paint)
        val r = h / 2 - 4 * d
        val x = rect.left + h / 2 + (rect.width() - h) * pos
        paint.color = Themes.mix(theme.muted, theme.bgTop, pos * 0.85f)
        canvas.drawCircle(x, height / 2f, r, paint)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = Switch::class.java.name
        info.isCheckable = true
        info.isChecked = checked
    }
}
