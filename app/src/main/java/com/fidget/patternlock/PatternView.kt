package com.fidget.patternlock

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.min

/**
 * A lock-screen style dot grid. People draw on it, and it can also play a pattern back by itself
 * (the landing page demo, Memory rounds, and saved-pattern replays).
 */
class PatternView(context: Context) : View(context) {

    interface Listener {
        fun onDotAdded(index: Int, countInPattern: Int, fromPlayback: Boolean)
        fun onPatternReleased(pattern: List<Int>)
    }

    var listener: Listener? = null

    /** When false, touches are ignored (used while a pattern is being shown). */
    var interactive = true

    /** When true, a drawn pattern fades away on its own after release. */
    var autoFade = true

    /** Overrides the active and line colors, e.g. to mark a wrong answer. */
    var tint: Int? = null
        set(value) { field = value; invalidate() }

    var gridSize = 3
        set(value) {
            field = value.coerceIn(3, 5)
            stopPlayback()
            fadeAnimator?.cancel()
            computeLayout()
            clearPattern()
        }

    var theme: Theme = Themes.all[0]
        set(value) {
            field = value
            dotPaint.color = value.dot
            invalidate()
        }

    var isPlaying = false
        private set

    // Layout
    private var centersX = FloatArray(0)
    private var centersY = FloatArray(0)
    private var hitRadius = 0f
    private var dotRadius = 0f

    // Pattern state
    private val selected = ArrayList<Int>()
    private var isSelected = BooleanArray(0)
    private var hitTimes = LongArray(0)
    private var drawing = false
    private var lastX = 0f
    private var lastY = 0f
    private var patternAlpha = 1f
    private var releaseTime = 0L
    private var fadeAnimator: ValueAnimator? = null
    private var playToken = 0

    // Paints
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val linePaint = strokePaint()
    private val glowPaint = strokePaint()
    private val path = Path()

    init {
        computeLayout()
        dotPaint.color = theme.dot
    }

    private fun strokePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        computeLayout()
    }

    private fun computeLayout() {
        val n = gridSize
        val count = n * n
        if (centersX.size != count) {
            centersX = FloatArray(count)
            centersY = FloatArray(count)
            isSelected = BooleanArray(count)
            hitTimes = LongArray(count)
        }
        if (width == 0 || height == 0) return
        val side = min(width, height) * 0.86f
        val spacing = side / n
        val left = (width - side) / 2f
        val top = (height - side) / 2f
        for (r in 0 until n) for (c in 0 until n) {
            val i = r * n + c
            centersX[i] = left + spacing * (c + 0.5f)
            centersY[i] = top + spacing * (r + 0.5f)
        }
        hitRadius = spacing * 0.36f
        // Dots stay a similar physical size whatever the grid, so 5×5 doesn't look crowded.
        dotRadius = spacing * 0.06f
        linePaint.strokeWidth = spacing * 0.045f
        glowPaint.strokeWidth = spacing * 0.15f
        ringPaint.strokeWidth = spacing * 0.018f
    }

    /** Removes the current pattern immediately. */
    fun clearPattern() {
        selected.clear()
        isSelected.fill(false)
        releaseTime = 0L
        patternAlpha = 1f
        invalidate()
    }

    /** Draws [pattern] dot by dot, then calls [onDone]. The pattern stays on screen until faded or cleared. */
    fun play(pattern: List<Int>, stepMs: Long = 420L, onDone: (() -> Unit)? = null) {
        stopPlayback()
        fadeAnimator?.cancel()
        clearPattern()
        drawing = false
        isPlaying = true
        val token = playToken
        pattern.forEachIndexed { k, i ->
            postDelayed({
                if (token == playToken && i in isSelected.indices && !isSelected[i]) {
                    select(i, fromPlayback = true)
                    invalidate()
                }
            }, 150L + k * stepMs)
        }
        postDelayed({
            if (token == playToken) {
                isPlaying = false
                releaseTime = SystemClock.uptimeMillis()
                invalidate()
                onDone?.invoke()
            }
        }, 150L + pattern.size * stepMs)
    }

    /** Stops any playback and fade without running their completion callbacks. */
    fun cancelAnimations() {
        stopPlayback()
        fadeAnimator?.cancel()
    }

    fun stopPlayback() {
        playToken++
        isPlaying = false
    }

    /** Fades the current pattern out, clears it, then calls [then] (not called if interrupted). */
    fun fadeOut(delayMs: Long = 450L, durationMs: Long = 700L, then: (() -> Unit)? = null) {
        fadeAnimator?.cancel()
        fadeAnimator = ValueAnimator.ofFloat(patternAlpha, 0f).apply {
            startDelay = delayMs
            duration = durationMs
            addUpdateListener {
                patternAlpha = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) { cancelled = true }
                override fun onAnimationEnd(animation: Animator) {
                    if (cancelled) return
                    if (!drawing) clearPattern()
                    tint = null
                    then?.invoke()
                }
            })
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopPlayback()
        fadeAnimator?.cancel()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!interactive || isPlaying) return true
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                fadeAnimator?.cancel()
                tint = null
                clearPattern()
                drawing = true
                lastX = e.x
                lastY = e.y
                handleSegment(e.x, e.y, e.x, e.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!drawing) return true
                for (h in 0 until e.historySize) {
                    handleSegment(lastX, lastY, e.getHistoricalX(h), e.getHistoricalY(h))
                }
                handleSegment(lastX, lastY, e.x, e.y)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (drawing) {
                    drawing = false
                    release()
                }
            }
        }
        invalidate()
        return true
    }

    /** Checks every unused dot against the finger's path, so fast swipes never skip a dot. */
    private fun handleSegment(x0: Float, y0: Float, x1: Float, y1: Float) {
        val dx = x1 - x0
        val dy = y1 - y0
        val len2 = dx * dx + dy * dy
        val hits = ArrayList<Pair<Float, Int>>()
        for (i in centersX.indices) {
            if (isSelected[i]) continue
            val t = if (len2 == 0f) 0f
            else (((centersX[i] - x0) * dx + (centersY[i] - y0) * dy) / len2).coerceIn(0f, 1f)
            val px = x0 + t * dx
            val py = y0 + t * dy
            if (hypot(centersX[i] - px, centersY[i] - py) <= hitRadius) hits.add(t to i)
        }
        hits.sortBy { it.first }
        for (h in hits) addDot(h.second)
        lastX = x1
        lastY = y1
    }

    /** Adds a dot, auto-including any dot the straight line jumps over (like a real lock). */
    private fun addDot(i: Int) {
        if (isSelected[i]) return
        if (selected.isNotEmpty()) {
            for (m in Patterns.between(gridSize, selected.last(), i)) if (!isSelected[m]) select(m, false)
        }
        select(i, false)
    }

    private fun select(i: Int, fromPlayback: Boolean) {
        isSelected[i] = true
        selected.add(i)
        hitTimes[i] = SystemClock.uptimeMillis()
        listener?.onDotAdded(i, selected.size, fromPlayback)
    }

    private fun release() {
        if (selected.isEmpty()) return
        releaseTime = SystemClock.uptimeMillis()
        listener?.onPatternReleased(ArrayList(selected))
        if (autoFade && selected.isNotEmpty()) fadeOut()
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        var animating = false
        val alpha = (patternAlpha * 255).toInt()
        val activeColor = tint ?: theme.active
        val lineColor = tint ?: theme.line

        // Base dots (always visible)
        for (i in centersX.indices) canvas.drawCircle(centersX[i], centersY[i], dotRadius, dotPaint)

        // Lines between connected dots, plus the live segment to the finger
        if (selected.isNotEmpty()) {
            path.reset()
            path.moveTo(centersX[selected[0]], centersY[selected[0]])
            for (k in 1 until selected.size) path.lineTo(centersX[selected[k]], centersY[selected[k]])
            if (drawing) path.lineTo(lastX, lastY)
            glowPaint.color = lineColor
            glowPaint.alpha = (alpha * 0.16f).toInt()
            canvas.drawPath(path, glowPaint)
            linePaint.color = lineColor
            linePaint.alpha = alpha
            canvas.drawPath(path, linePaint)
        }

        // Active dots: a soft swell + ripple when reached, a pulse when finished
        val sinceRelease = if (releaseTime > 0) now - releaseTime else Long.MAX_VALUE
        for (i in selected) {
            val cx = centersX[i]
            val cy = centersY[i]
            val t = (now - hitTimes[i]).toFloat()

            val pop = if (t < 260f) { animating = true; 1f + 0.6f * (1f - t / 260f) } else 1f
            activePaint.color = activeColor
            activePaint.alpha = alpha
            canvas.drawCircle(cx, cy, dotRadius * 1.5f * pop, activePaint)

            if (t < 600f) {
                animating = true
                val p = t / 600f
                ringPaint.color = activeColor
                ringPaint.alpha = ((1f - p) * 140 * patternAlpha).toInt()
                canvas.drawCircle(cx, cy, dotRadius * (1.8f + p * 4f), ringPaint)
            }

            if (sinceRelease < 600) {
                animating = true
                val p = sinceRelease / 600f
                ringPaint.color = activeColor
                ringPaint.alpha = ((1f - p) * 180 * patternAlpha).toInt()
                canvas.drawCircle(cx, cy, dotRadius * (1.5f + p * 2.5f), ringPaint)
            }
        }

        if (animating) postInvalidateOnAnimation()
    }
}
