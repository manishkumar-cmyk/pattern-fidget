package com.fidget.patternlock

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import android.view.animation.DecelerateInterpolator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The dot grid: lock-screen rules, soft physics, ripples and per-shape completion animations,
 * plus the fidget modes (Endless Flow, Constellation, Ripple, Mirror, Pattern Loop).
 * It can also play a pattern back by itself (home idle animation, Memory, Collection playback).
 */
class PatternView(context: Context) : View(context) {

    interface Listener {
        fun onTouchStart() {}
        fun onTouchEnd() {}
        fun onNear() {}
        fun onDot(index: Int, count: Int, dx: Int, dy: Int, speed: Float, fromPlayback: Boolean) {}
        fun onRelease(pattern: List<Int>, shape: Shape) {}
        fun onCycle() {}
        fun onLongPress() {}
        fun onTwoFingerTap() {}
        /** A short touch that connected nothing. */
        fun onTap() {}
        fun onLoopSpeed(speed: Float) {}
    }

    var listener: Listener? = null

    // ---------------------------------------------------------------- configuration

    var theme: Theme = Themes.all[0]
        set(value) { field = value; computeLayout(); invalidate() }

    var gridSize = 3
        set(value) {
            val v = value.coerceIn(3, 5)
            if (v == field && centersX.size == v * v) return
            field = v
            cancelAnimations()
            computeLayout()
            clearPattern()
            ghosts.clear()
            bloomStart = SystemClock.uptimeMillis()
            invalidate()
        }

    var mode = Mode.FREE
        set(value) {
            if (field == value) return
            field = value
            stopLoop()
            clearPattern()
            ghosts.clear()
            invalidate()
        }

    var mirrorFourWay = false
    var interactive = true
    /** When false the pattern stays after release (Memory, playback). */
    var autoFade = true
    var reduceMotion = false
    var largerDots = false
        set(value) { field = value; computeLayout(); invalidate() }
    var showLines = true
    /** Draw the line as a smooth curve that follows the finger, instead of straight segments between dots (Zen). */
    var curvedLines = false
    var quietCompletion = false
    var highContrast = false
    var idleBreathing = true
    /** Fraction of the shorter side the dot grid occupies. */
    var gridFill = 0.84f
        set(value) { field = value; computeLayout(); invalidate() }
    var longPressEnabled = false
    /** When true, touching the grid during playback stops it and starts drawing (home idle animation). */
    var interruptible = false
    /** Opacity of drawn patterns, e.g. the faint home idle animation. */
    var patternOpacity = 1f
    /** A faint guide pattern (Memory, Relaxed difficulty). */
    var guide: List<Int>? = null
        set(value) { field = value; invalidate() }
    /** Space reserved above and below the grid for interface chrome, in px. */
    var insetTop = 0
        set(value) { field = value; computeLayout() }
    var insetBottom = 0
        set(value) { field = value; computeLayout() }

    var isPlaying = false
        private set

    val currentPattern: List<Int> get() = ArrayList(selected)

    // ---------------------------------------------------------------- state

    private val density = resources.displayMetrics.density
    private val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    private var centersX = FloatArray(0)
    private var centersY = FloatArray(0)
    private var spacing = 1f
    private var dotR = 1f
    private var hitR = 1f
    private var lineW = 1f
    private var gridCx = 0f
    private var gridCy = 0f

    private val selected = ArrayList<Int>()
    private var isSelected = BooleanArray(0)
    private var hitTimes = LongArray(0)
    private var springS = FloatArray(0)
    private var springV = FloatArray(0)
    private var prox = FloatArray(0)
    private var nearArmed = BooleanArray(0)
    private var rippleInside = BooleanArray(0)

    private var drawing = false
    private var fingerX = 0f
    private var fingerY = 0f
    private var tailX = 0f
    private var tailY = 0f
    private var lastMoveTime = 0L
    private var speedDp = 500f
    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f
    private var maxTravel = 0f
    private var twoFinger = false
    private var twoFingerStart = 0L
    private var lastTapTime = 0L
    private var lastTouchTime = SystemClock.uptimeMillis()
    private var rippleCount = 0
    private var lastPattern: List<Int> = emptyList()

    private var patternAlpha = 1f
    private var fadeAnimator: ValueAnimator? = null
    private var playToken = 0
    private var lastFrame = 0L
    private var bloomStart = 0L
    private var restDim = 1f

    private class Ripple(val x: Float, val y: Float, val start: Long, val dur: Float, val maxR: Float, val alpha: Float)
    private val ripples = ArrayList<Ripple>()

    private class Ghost(val xs: FloatArray, val ys: FloatArray, val start: Long, val life: Float,
                        val constellation: Boolean, val seed: Float, val color: Int, val curve: Boolean = false)
    private val ghosts = ArrayList<Ghost>()

    // Curved mode: the finger's own path, thinned out, with dot centres pinned into it.
    private var cvX = FloatArray(360)
    private var cvY = FloatArray(360)
    private var cvN = 0

    // Ember: tiny drifting sparks.
    private val sparkMax = 90
    private val spX = FloatArray(sparkMax); private val spY = FloatArray(sparkMax)
    private val spVX = FloatArray(sparkMax); private val spVY = FloatArray(sparkMax)
    private val spLife = FloatArray(sparkMax); private val spR = FloatArray(sparkMax)
    private val spStart = LongArray(sparkMax)
    private var spNext = 0
    private var lastSpark = 0L

    private var completionStart = 0L
    private var completionShape = Shape.OTHER
    private var completionDur = 0f

    private var missStart = 0L
    private var missGlow = false
    /** 0..1 pulse while a completion plays: dots and glow brighten together. */
    private var brighten = 0f
    private var glowScale = 1f

    // Pattern Loop
    private var loopPattern: List<Int>? = null
    private var loopSpeed = 1f
    private var loopTouchSpeed = 1f
    val isLooping get() = loopPattern != null

    // Paints
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowShaders = HashMap<Int, RadialGradient>()
    private var ptsX = FloatArray(32)
    private var ptsY = FloatArray(32)

    private val longPress = Runnable {
        if (drawing && maxTravel < slop * 2) {
            drawing = false
            clearPattern()
            listener?.onLongPress()
        }
    }

    init {
        contentDescription = "Pattern grid"
        isFocusable = true
        computeLayout()
    }

    // ---------------------------------------------------------------- layout

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = computeLayout()

    private fun computeLayout() {
        val n = gridSize
        val count = n * n
        if (centersX.size != count) {
            centersX = FloatArray(count); centersY = FloatArray(count)
            isSelected = BooleanArray(count); hitTimes = LongArray(count)
            springS = FloatArray(count) { 1f }; springV = FloatArray(count)
            prox = FloatArray(count); nearArmed = BooleanArray(count) { true }
            rippleInside = BooleanArray(count)
        }
        if (width == 0 || height == 0) return
        val availH = (height - insetTop - insetBottom).coerceAtLeast(height / 3)
        val side = min(width.toFloat(), availH.toFloat()) * gridFill
        spacing = side / n
        val left = (width - side) / 2f
        val top = insetTop + (availH - side) / 2f
        for (r in 0 until n) for (c in 0 until n) {
            val i = r * n + c
            centersX[i] = left + spacing * (c + 0.5f)
            centersY[i] = top + spacing * (r + 0.5f)
        }
        gridCx = left + side / 2f
        gridCy = top + side / 2f
        val sizeBoost = if (largerDots) 1.3f else 1f
        // Dots keep a similar physical size whatever the grid.
        dotR = min(spacing * 0.075f, 9f * density) * sizeBoost
        hitR = spacing * 0.36f * (if (largerDots) 1.15f else 1f)
        lineW = spacing * (if (theme.glow == 0f) 0.02f else 0.024f)
    }

    // ---------------------------------------------------------------- public actions

    fun clearPattern() {
        selected.clear()
        cvN = 0
        isSelected.fill(false)
        patternAlpha = 1f
        completionStart = 0L
        missStart = 0L
        invalidate()
    }

    fun cancelAnimations() {
        stopPlayback()
        fadeAnimator?.cancel()
        stopLoop()
    }

    fun stopPlayback() {
        playToken++
        isPlaying = false
    }

    /** Draws [pattern] dot by dot at [stepMs] per dot, then calls [onDone]. */
    fun play(pattern: List<Int>, stepMs: Long = 450L, onDone: (() -> Unit)? = null) {
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
            }, 120L + k * stepMs)
        }
        postDelayed({
            if (token == playToken) {
                isPlaying = false
                invalidate()
                onDone?.invoke()
            }
        }, 120L + pattern.size * stepMs)
    }

    /** Fades the current pattern out, clears it, then calls [then] (not called if interrupted). */
    fun fadeOut(delayMs: Long = 400L, durationMs: Long = 1200L, then: (() -> Unit)? = null) {
        fadeAnimator?.cancel()
        val d = if (reduceMotion) min(durationMs, 300L) else (durationMs * theme.pace).toLong()
        fadeAnimator = ValueAnimator.ofFloat(patternAlpha, 0f).apply {
            startDelay = delayMs
            duration = d
            interpolator = DecelerateInterpolator()
            addUpdateListener { patternAlpha = it.animatedValue as Float; invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: Animator) { cancelled = true }
                override fun onAnimationEnd(animation: Animator) {
                    if (cancelled) return
                    if (!drawing) clearPattern()
                    then?.invoke()
                }
            })
            start()
        }
    }

    /** Plays the shape-aware completion for whatever is on the grid now (Memory match, playback end). */
    fun celebrate(fadeAfter: Boolean) {
        val p = ArrayList(selected)
        if (p.size < 2) return
        startCompletion(Shapes.detect(gridSize, p))
        if (fadeAfter) fadeOut((completionDur * 0.8f).toLong(), 1200L)
    }

    /** Memory miss: the line loses its colour and drifts down as it fades. */
    fun miss(then: (() -> Unit)? = null) {
        fadeAnimator?.cancel()
        val token = SystemClock.uptimeMillis()
        missStart = token
        invalidate()
        postDelayed({ if (missStart == token) { clearPattern(); then?.invoke() } }, if (reduceMotion) 300L else 900L)
    }

    fun replayLast() {
        if (lastPattern.size >= 2 && !drawing) play(lastPattern, 300L) { celebrate(autoFade) }
    }

    // ---------------------------------------------------------------- Pattern Loop

    private fun startLoop(p: List<Int>) {
        loopPattern = p
        runLoop()
    }

    private fun runLoop() {
        val p = loopPattern ?: return
        val step = (420f / loopSpeed).toLong()
        play(p, step) {
            if (loopPattern == null) return@play
            startCompletion(Shapes.detect(gridSize, p))
            fadeOut((300 / loopSpeed).toLong(), (700 / loopSpeed).toLong()) {
                if (loopPattern != null) postDelayed({ runLoop() }, (250 / loopSpeed).toLong())
            }
        }
    }

    fun stopLoop() {
        if (loopPattern == null) return
        loopPattern = null
        stopPlayback()
        fadeAnimator?.cancel()
        clearPattern()
    }

    // ---------------------------------------------------------------- touch

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val now = SystemClock.uptimeMillis()
        lastTouchTime = now
        if (e.actionMasked == MotionEvent.ACTION_POINTER_DOWN && e.pointerCount == 2) {
            twoFinger = true
            twoFingerStart = now
            removeCallbacks(longPress)
            if (drawing) { drawing = false; clearPattern() }
            return true
        }
        if (twoFinger) {
            if (e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) {
                twoFinger = false
                if (e.actionMasked == MotionEvent.ACTION_UP && now - twoFingerStart < 600) listener?.onTwoFingerTap()
                listener?.onTouchEnd()
            }
            return true
        }

        if (isLooping) return loopTouch(e)
        if (isPlaying && interruptible && interactive && e.actionMasked == MotionEvent.ACTION_DOWN) {
            stopPlayback()
            fadeAnimator?.cancel()
            patternOpacity = 1f
        }
        if (!interactive || isPlaying) {
            if (e.actionMasked == MotionEvent.ACTION_DOWN) listener?.onTouchStart()
            if (e.actionMasked == MotionEvent.ACTION_UP) listener?.onTouchEnd()
            return true
        }

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                listener?.onTouchStart()
                fadeAnimator?.cancel()
                clearPattern()
                drawing = true
                rippleCount = 0
                rippleInside.fill(false)
                downTime = now; downX = e.x; downY = e.y; maxTravel = 0f
                fingerX = e.x; fingerY = e.y; tailX = e.x; tailY = e.y
                lastMoveTime = now
                if (longPressEnabled) postDelayed(longPress, 800L)
                handleSegment(e.x, e.y, e.x, e.y)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!drawing) return true
                for (h in 0 until e.historySize) moveTo(e.getHistoricalX(h), e.getHistoricalY(h), e.getHistoricalEventTime(h))
                moveTo(e.x, e.y, e.eventTime)
                maxTravel = max(maxTravel, hypot(e.x - downX, e.y - downY))
                if (maxTravel >= slop * 2) removeCallbacks(longPress)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                removeCallbacks(longPress)
                if (drawing) {
                    drawing = false
                    release(isTap = maxTravel < slop && now - downTime < 300)
                }
                listener?.onTouchEnd()
            }
        }
        invalidate()
        return true
    }

    private fun moveTo(x: Float, y: Float, time: Long) {
        val dt = (time - lastMoveTime).coerceAtLeast(1L)
        val inst = hypot(x - fingerX, y - fingerY) / density / (dt / 1000f)
        val k = 1f - exp(-dt / 80f)
        speedDp += (inst.coerceAtMost(5000f) - speedDp) * k
        lastMoveTime = time
        handleSegment(fingerX, fingerY, x, y)
    }

    private fun loopTouch(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                listener?.onTouchStart()
                downX = e.x; downY = e.y; maxTravel = 0f; loopTouchSpeed = loopSpeed
            }
            MotionEvent.ACTION_MOVE -> {
                maxTravel = max(maxTravel, hypot(e.x - downX, e.y - downY))
                if (maxTravel > slop) {
                    val dy = (downY - e.y) / density
                    loopSpeed = (loopTouchSpeed * 2f.pow(dy / 200f)).coerceIn(0.5f, 2f)
                    listener?.onLoopSpeed(loopSpeed)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (e.actionMasked == MotionEvent.ACTION_UP && maxTravel < slop) stopLoop()
                listener?.onTouchEnd()
            }
        }
        return true
    }

    /** Checks every dot against the finger's path, so fast swipes never skip one. */
    private fun handleSegment(x0: Float, y0: Float, x1: Float, y1: Float) {
        fingerX = x1; fingerY = y1
        val dx = x1 - x0
        val dy = y1 - y0
        val len2 = dx * dx + dy * dy

        if (mode == Mode.RIPPLE) {
            for (i in centersX.indices) {
                val inside = distToSeg(i, x0, y0, dx, dy, len2) <= hitR
                if (inside && !rippleInside[i]) rippleDot(i)
                rippleInside[i] = hypot(centersX[i] - x1, centersY[i] - y1) <= hitR
            }
            return
        }

        // Backtrack forgiveness: sliding straight back onto the previous dot undoes the last one.
        if (mode == Mode.FREE && selected.size >= 2) {
            val last = selected.last()
            val prev = selected[selected.size - 2]
            if (SystemClock.uptimeMillis() - hitTimes[last] < 350 &&
                hypot(centersX[prev] - x1, centersY[prev] - y1) <= hitR * 0.8f &&
                Patterns.between(gridSize, prev, last).isEmpty()) {
                selected.removeAt(selected.size - 1)
                isSelected[last] = false
                springV[last] -= 2f
                return
            }
        }

        val hits = ArrayList<Pair<Float, Int>>()
        for (i in centersX.indices) {
            if (isSelected[i]) continue
            val t = if (len2 == 0f) 0f
            else (((centersX[i] - x0) * dx + (centersY[i] - y0) * dy) / len2).coerceIn(0f, 1f)
            if (hypot(centersX[i] - (x0 + t * dx), centersY[i] - (y0 + t * dy)) <= hitR) hits.add(t to i)
        }
        hits.sortBy { it.first }
        for (h in hits) addDot(h.second)
        if (curvedLines && drawing && selected.isNotEmpty()) addCurvePoint(x1, y1, force = false)
    }

    private fun spawnSparks(x: Float, y: Float, count: Int) {
        val now = SystemClock.uptimeMillis()
        repeat(count) {
            val k = spNext; spNext = (spNext + 1) % sparkMax
            val a = Math.random() * 2 * PI
            val d = (4 + Math.random() * 26).toFloat() * density
            spX[k] = x + (cos(a) * d).toFloat(); spY[k] = y + (sin(a) * d).toFloat()
            val v = (6 + Math.random() * 16).toFloat() * density
            spVX[k] = (cos(a) * v).toFloat() * 0.5f; spVY[k] = (sin(a) * v).toFloat() * 0.5f - 5f * density
            spLife[k] = 900f + (Math.random() * 1400).toFloat()
            spR[k] = ((0.7 + Math.random() * 1.3) * density).toFloat()
            spStart[k] = now
        }
    }

    private fun addCurvePoint(x: Float, y: Float, force: Boolean) {
        if (cvN >= cvX.size - 2) { if (!force) return; cvN-- }
        if (!force && cvN > 0 && hypot(x - cvX[cvN - 1], y - cvY[cvN - 1]) < 14f * density) return
        cvX[cvN] = x; cvY[cvN] = y; cvN++
    }

    private fun distToSeg(i: Int, x0: Float, y0: Float, dx: Float, dy: Float, len2: Float): Float {
        val t = if (len2 == 0f) 0f else (((centersX[i] - x0) * dx + (centersY[i] - y0) * dy) / len2).coerceIn(0f, 1f)
        return hypot(centersX[i] - (x0 + t * dx), centersY[i] - (y0 + t * dy))
    }

    private fun rippleDot(i: Int) {
        rippleCount++
        hitTimes[i] = SystemClock.uptimeMillis()
        springV[i] += 6f
        addRipple(i, big = true)
        listener?.onDot(i, rippleCount, 0, 0, speedDp, false)
    }

    /** Adds a dot, auto-including any dot the straight line jumps over (like a real lock). */
    private fun addDot(i: Int) {
        if (isSelected[i]) return
        if (selected.isNotEmpty()) {
            for (m in Patterns.between(gridSize, selected.last(), i)) if (!isSelected[m]) select(m, false)
        }
        select(i, false)
        if (mode == Mode.ENDLESS && selected.size >= gridSize * gridSize) cycleEndless()
    }

    private fun select(i: Int, fromPlayback: Boolean) {
        val prev = selected.lastOrNull()
        isSelected[i] = true
        selected.add(i)
        hitTimes[i] = SystemClock.uptimeMillis()
        if (curvedLines) addCurvePoint(centersX[i], centersY[i], force = true)
        if (theme.particles && !reduceMotion) spawnSparks(centersX[i], centersY[i], 7)
        springV[i] += if (reduceMotion) 0f else 6.5f
        addRipple(i, big = false)
        val n = gridSize
        val dx = if (prev == null) 0 else i % n - prev % n
        val dy = if (prev == null) 0 else i / n - prev / n
        listener?.onDot(i, selected.size, dx, dy, if (fromPlayback) 500f else speedDp, fromPlayback)
    }

    /** Endless Flow: the trail dissolves while the finger keeps going from the current dot. */
    private fun cycleEndless() {
        if (curvedLines && cvN >= 2) {
            ghosts.add(Ghost(cvX.copyOf(cvN), cvY.copyOf(cvN), SystemClock.uptimeMillis(), 900f * theme.pace, false, 0f, theme.line, curve = true))
        } else addGhost(selected, constellation = false, life = 900f * theme.pace)
        val keep = selected.last()
        selected.clear()
        isSelected.fill(false)
        selected.add(keep)
        isSelected[keep] = true
        cvN = 0
        if (curvedLines) addCurvePoint(centersX[keep], centersY[keep], force = true)
        startCompletion(Shape.OTHER, breatheOnly = true)
        listener?.onCycle()
    }

    private fun release(isTap: Boolean) {
        val p = ArrayList(selected)
        val now = SystemClock.uptimeMillis()
        if (mode == Mode.RIPPLE) {
            if (isTap && rippleCount == 0) handleTap(now)
            return
        }
        if (p.size < 2) {
            if (isTap) handleTap(now)
            if (autoFade) fadeOut(150L, 400L)
            return
        }
        lastPattern = p
        val shape = Shapes.detect(gridSize, p)
        listener?.onRelease(p, shape)
        when (mode) {
            Mode.CONSTELLATION -> {
                addGhost(p, constellation = true, life = if (reduceMotion) 8000f else 20000f)
                clearPattern()
            }
            Mode.LOOP -> startLoop(p)
            else -> {
                startCompletion(shape)
                if (autoFade) fadeOut((completionDur * 0.7f).toLong(), 1200L)
            }
        }
    }

    private fun handleTap(now: Long) {
        listener?.onTap()
        if (!autoFade) return
        if (now - lastTapTime < 320) { lastTapTime = 0; replayLast() } else lastTapTime = now
    }

    // ---------------------------------------------------------------- effects

    private fun addRipple(i: Int, big: Boolean) {
        val now = SystemClock.uptimeMillis()
        val pace = theme.pace
        if (reduceMotion) {
            ripples.add(Ripple(centersX[i], centersY[i], now, 300f, dotR * 3f, 0.3f))
            return
        }
        val r = when {
            big -> Ripple(centersX[i], centersY[i], now, 1700f * pace, spacing * 1.3f, 0.45f)
            speedDp < 300f -> Ripple(centersX[i], centersY[i], now, 1200f * pace, spacing * 0.55f, 0.32f)
            speedDp > 1200f -> Ripple(centersX[i], centersY[i], now, 420f * pace, spacing * 0.28f, 0.5f)
            else -> Ripple(centersX[i], centersY[i], now, 800f * pace, spacing * 0.42f, 0.4f)
        }
        ripples.add(r)
        if (theme.doubleRipple) ripples.add(Ripple(r.x, r.y, now + 140, r.dur, r.maxR * 0.8f, r.alpha * 0.7f))
        if (ripples.size > 40) ripples.removeAt(0)
    }

    private fun addGhost(p: List<Int>, constellation: Boolean, life: Float) {
        if (p.size < 2) return
        val xs = FloatArray(p.size) { centersX[p[it]] }
        val ys = FloatArray(p.size) { centersY[p[it]] }
        ghosts.add(Ghost(xs, ys, SystemClock.uptimeMillis(), life, constellation, (p.sum() % 17) * 0.37f, theme.line))
        while (ghosts.count { it.constellation } > 5) ghosts.remove(ghosts.first { it.constellation })
    }

    private fun startCompletion(shape: Shape, breatheOnly: Boolean = false) {
        if (quietCompletion && !breatheOnly) { completionStart = 0L; return }
        completionStart = SystemClock.uptimeMillis()
        completionShape = if (breatheOnly) Shape.OTHER else shape
        completionDur = when {
            reduceMotion -> 300f
            breatheOnly -> 700f
            else -> when (completionShape) {
                Shape.STRAIGHT -> 500f
                Shape.LOOP -> 1300f
                Shape.ZIGZAG, Shape.SPIRAL -> 950f
                Shape.ALL_DOTS -> 1100f
                else -> 750f
            }
        }
        invalidate()
    }

    // ---------------------------------------------------------------- drawing

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrame == 0L) 16f else (now - lastFrame).coerceIn(1L, 50L).toFloat()
        lastFrame = now
        var animating = false
        val t = theme
        val n = gridSize
        val count = n * n
        if (centersX.size != count) return

        // Rest: after a minute without touch the grid dims a little and breathes slower.
        val resting = !drawing && now - lastTouchTime > 60_000
        val targetDim = if (resting) 0.85f else 1f
        restDim += (targetDim - restDim) * (1f - exp(-dt / 600f))
        if (abs(restDim - targetDim) > 0.005f) animating = true

        // Finger-follow tail with a little spring lag, so the live line trails like a thread.
        if (drawing) {
            val k = if (reduceMotion) 1f else 1f - exp(-dt / 40f)
            tailX += (fingerX - tailX) * k
            tailY += (fingerY - tailY) * k
            animating = true
        }

        // Slow strokes spread a wider, softer glow; fast ones tighten it. Subtle by design.
        val gTarget = if (drawing && !reduceMotion) 1.25f - 0.45f * ((speedDp - 300f) / 900f).coerceIn(0f, 1f) else 1f
        glowScale += (gTarget - glowScale) * (1f - exp(-dt / 140f))
        missGlow = false

        // Completion state
        val cp = if (completionStart > 0) ((now - completionStart) / completionDur).coerceIn(0f, 1f) else 1f
        val completing = completionStart > 0 && cp < 1f
        if (completing) animating = true
        val allDotsBoost = if (completing && completionShape == Shape.ALL_DOTS && !reduceMotion) sin(PI * cp).toFloat() * 0.45f else 0f
        brighten = if (completing) sin(PI * cp).toFloat() else 0f

        // Springs + proximity
        val kSpring = 400f
        val damp = 2f * 0.55f * sqrt(kSpring)
        val step = dt / 1000f
        for (i in 0 until count) {
            val d = hypot(centersX[i] - fingerX, centersY[i] - fingerY)
            val target = if (drawing && !isSelected[i] && mode != Mode.RIPPLE) (1f - d / (hitR * 1.7f)).coerceIn(0f, 1f) else 0f
            prox[i] += (target - prox[i]) * (1f - exp(-dt / 110f))
            if (prox[i] > 0.01f) animating = true
            if (drawing && nearArmed[i] && prox[i] > 0.55f && !isSelected[i]) { nearArmed[i] = false; listener?.onNear() }
            if (prox[i] < 0.2f) nearArmed[i] = true

            val rest = (if (isSelected[i]) 1.2f else 1f) + 0.15f * prox[i]
            if (reduceMotion) { springS[i] = rest; springV[i] = 0f } else {
                val a = -kSpring * (springS[i] - rest) - damp * springV[i]
                springV[i] += a * step
                springS[i] += springV[i] * step
                if (abs(springV[i]) > 0.01f || abs(springS[i] - rest) > 0.003f) animating = true
            }
        }

        // Base dots: breathing at idle, blooming after a grid change, leaning toward a nearby finger.
        val breathing = idleBreathing && !reduceMotion && !drawing && selected.isEmpty()
        val period = if (resting) 6000f else 4000f
        for (i in 0 until count) {
            if (isSelected[i]) continue
            var s = springS[i] + allDotsBoost
            if (breathing) s *= 1f + 0.06f * (0.5f + 0.5f * sin(2 * PI * ((now + i * 120) % period.toLong()) / period).toFloat())
            if (bloomStart > 0 && !reduceMotion) {
                val ring = max(abs(i % n - (n - 1) / 2f), abs(i / n - (n - 1) / 2f))
                val bp = ((now - bloomStart - ring * 80f) / 320f).coerceIn(0f, 1f)
                s *= bp
                if (bp < 1f) animating = true
            }
            val lean = 2f * density * prox[i]
            val d = hypot(fingerX - centersX[i], fingerY - centersY[i]).coerceAtLeast(1f)
            val x = centersX[i] + (fingerX - centersX[i]) / d * lean
            val y = centersY[i] + (fingerY - centersY[i]) / d * lean
            val tint = Themes.mix(t.dot, t.active, 0.3f * prox[i] + allDotsBoost)
            if (t.glow > 0f) {
                // A faint smooth halo keeps resting dots softly lit.
                drawGlow(canvas, x, y, dotR * (3.4f + 1.6f * prox[i]) * s, t.glowColor, restDim * t.glow * (0.22f + 0.35f * prox[i]))
            }
            fill.color = Themes.mix(tint, 0xFFFFFFFF.toInt(), 0.12f)
            fill.alpha = (255 * restDim).toInt()
            canvas.drawCircle(x, y, dotR * 0.8f * s, fill)
        }
        if (breathing) animating = true

        // Faint guide (Memory, Relaxed)
        guide?.let { g -> if (g.size >= 2) drawTrail(canvas, g.map { centersX[it] }.toFloatArray(), g.map { centersY[it] }.toFloatArray(), t.line, 0.12f, 0.8f, false) }

        // Lingering trails: Endless Flow tails and Constellation star lines
        val gi = ghosts.iterator()
        while (gi.hasNext()) {
            val g = gi.next()
            val age = (now - g.start) / g.life
            if (age >= 1f) { gi.remove(); continue }
            animating = true
            if (g.constellation) {
                val fade = if (age < 0.7f) 1f else 1f - (age - 0.7f) / 0.3f
                val ox = if (reduceMotion) 0f else 3f * density * sin((now / 3000f + g.seed).toDouble()).toFloat()
                val oy = if (reduceMotion) 0f else 3f * density * cos((now / 3700f + g.seed).toDouble()).toFloat()
                val xs = FloatArray(g.xs.size) { g.xs[it] + ox }
                val ys = FloatArray(g.ys.size) { g.ys[it] + oy }
                drawTrail(canvas, xs, ys, g.color, 0.5f * fade, 0.5f, false)
                for (k in xs.indices) {
                    val tw = if (reduceMotion) 1f else 0.6f + 0.4f * sin((now / 700f + k * 1.7f + g.seed).toDouble()).toFloat()
                    fill.color = t.active
                    fill.alpha = (255 * 0.8f * fade * tw * restDim).toInt()
                    canvas.drawCircle(xs[k], ys[k], dotR * 0.9f, fill)
                }
            } else {
                if (g.curve) drawCurve(canvas, g.xs, g.ys, g.xs.size, g.color, 0.7f * (1f - age), false)
                else drawTrail(canvas, g.xs, g.ys, g.color, 0.7f * (1f - age), 1f, false)
            }
        }

        // Ripples
        val ri = ripples.iterator()
        while (ri.hasNext()) {
            val r = ri.next()
            val p = (now - r.start) / r.dur
            if (p >= 1f) { ri.remove(); continue }
            animating = true
            if (p < 0f) continue
            val e = 1f - (1f - p).pow(3)
            stroke.color = t.ripple
            stroke.alpha = (255 * r.alpha * 0.75f * (1f - p) * restDim).toInt()
            stroke.strokeWidth = (if (t.glow == 0f) 1f else 1.6f) * density * (1.5f - p)
            canvas.drawCircle(r.x, r.y, dotR * 1.4f + (r.maxR - dotR) * e, stroke)
        }

        // Sparks (Ember): drift and fade, with a gentle twinkle.
        if (t.particles && !reduceMotion) {
            if (drawing && now - lastSpark > 70) { lastSpark = now; spawnSparks(tailX, tailY, 1) }
            for (k in 0 until sparkMax) {
                if (spLife[k] <= 0f) continue
                val age = (now - spStart[k]).toFloat()
                val p = age / spLife[k]
                if (p >= 1f) { spLife[k] = 0f; continue }
                animating = true
                val sec = age / 1000f
                val tw = 0.6f + 0.4f * sin((now / 180f + k * 1.3f).toDouble()).toFloat()
                val a = (1f - p) * tw * restDim
                val sx = spX[k] + spVX[k] * sec
                val sy = spY[k] + spVY[k] * sec
                drawGlow(canvas, sx, sy, spR[k] * 4f, t.glowColor, a * 0.5f)
                fill.color = Themes.mix(t.secondaryGlow, 0xFFFFFFFF.toInt(), 0.4f)
                fill.alpha = (255 * a).toInt()
                canvas.drawCircle(sx, sy, spR[k], fill)
            }
        }

        // The pattern itself
        if (selected.isNotEmpty()) {
            canvas.save()
            var alpha = patternAlpha * patternOpacity * restDim
            var lineColor = t.line
            var activeColor = t.active
            if (missStart > 0) {
                val mp = ((now - missStart) / (if (reduceMotion) 300f else 900f)).coerceIn(0f, 1f)
                lineColor = Themes.mix(t.line, t.error, min(1f, mp * 3f))
                activeColor = Themes.mix(t.active, t.error, min(1f, mp * 3f))
                missGlow = true
                alpha *= 1f - mp * 0.85f
                if (!reduceMotion) canvas.translate(0f, 8f * density * mp)
                animating = true
            }
            if (completing && !reduceMotion && completionShape != Shape.STRAIGHT) {
                val b = 1f + 0.04f * sin(PI * cp).toFloat()
                canvas.scale(b, b, gridCx, gridCy)
            }
            if (completing && reduceMotion) alpha = min(1f, alpha * (1f + 0.4f * sin(PI * cp).toFloat()))

            // Points (with the zigzag wave displacement during that completion)
            val m = selected.size
            ensurePts(m + 1)
            for (k in 0 until m) { ptsX[k] = centersX[selected[k]]; ptsY[k] = centersY[selected[k]] }
            if (completing && completionShape == Shape.ZIGZAG && !reduceMotion) displaceWave(m, cp)

            if (showLines && mode != Mode.RIPPLE) {
                if (curvedLines && cvN >= 1) drawCurve(canvas, cvX, cvY, cvN, lineColor, alpha, drawing)
                else drawTrail(canvas, ptsX, ptsY, lineColor, alpha, 1f, drawing, m)
                if (mode == Mode.MIRROR) for (mi in mirrorMaps()) {
                    val xs = FloatArray(m) { centersX[mi(selected[it])] }
                    val ys = FloatArray(m) { centersY[mi(selected[it])] }
                    drawTrail(canvas, xs, ys, lineColor, alpha * 0.6f, 1f, false, m)
                }
            }

            // Loop interior fill
            if (completing && completionShape == Shape.LOOP && !reduceMotion) {
                path.reset()
                for (k in 0 until m) if (k == 0) path.moveTo(ptsX[0], ptsY[0]) else path.lineTo(ptsX[k], ptsY[k])
                path.close()
                fill.color = activeColor
                fill.alpha = (255 * 0.14f * sin(PI * cp).toFloat() * alpha).toInt()
                canvas.drawPath(path, fill)
            }

            // Active dots: spring pop, slow glow pulse
            for (k in 0 until m) {
                val i = selected[k]
                drawActiveDot(canvas, ptsX[k], ptsY[k], springS[i], activeColor, alpha, now)
            }
            if (mode == Mode.MIRROR) for (mi in mirrorMaps()) for (i in selected) {
                val j = mi(i)
                if (!isSelected[j]) drawActiveDot(canvas, centersX[j], centersY[j], springS[i], activeColor, alpha * 0.6f, now)
            }
            if (selected.isNotEmpty()) animating = true

            // Travelling light
            if (completing && !reduceMotion && m >= 2 && !curvedLines) drawLight(canvas, m, cp, activeColor, alpha)
            canvas.restore()
        }

        if (animating) postInvalidateOnAnimation()
        else if (idleBreathing && !reduceMotion) postInvalidateDelayed(33)
    }

    /** A smooth radial glow: one cached unit gradient per colour, scaled to size, so there are no visible rings. */
    private fun drawGlow(canvas: Canvas, x: Float, y: Float, r: Float, color: Int, alpha: Float) {
        if (alpha <= 0.002f || r <= 0f) return
        val shader = glowShaders.getOrPut(color or 0xFF000000.toInt()) {
            if (glowShaders.size > 24) glowShaders.clear()
            val rgb = color and 0x00FFFFFF
            RadialGradient(0f, 0f, 1f,
                intArrayOf(rgb or (0xB0 shl 24), rgb or (0x60 shl 24), rgb or (0x22 shl 24), rgb or (0x08 shl 24), rgb),
                floatArrayOf(0f, 0.25f, 0.55f, 0.82f, 1f), Shader.TileMode.CLAMP)
        }
        glowPaint.shader = shader
        glowPaint.alpha = (255 * alpha).toInt().coerceIn(0, 255)
        canvas.save()
        canvas.translate(x, y)
        canvas.scale(r, r)
        canvas.drawCircle(0f, 0f, 1f, glowPaint)
        canvas.restore()
    }

    private fun drawActiveDot(canvas: Canvas, x: Float, y: Float, s: Float, color: Int, alpha: Float, now: Long) {
        val g = theme.glow
        val gc = if (missGlow) color else theme.glowColor
        if (g > 0f) {
            val pulse = if (reduceMotion) 0.5f else 0.5f + 0.5f * sin(2 * PI * (now % 1600) / 1600.0).toFloat()
            val lift = 1f + 0.5f * brighten
            val sc = glowScale * s
            drawGlow(canvas, x, y, dotR * 6.2f * sc, gc, alpha * g * (0.42f + 0.1f * pulse) * lift)
            drawGlow(canvas, x, y, dotR * 3.0f * sc, Themes.mix(gc, theme.secondaryGlow, 0.4f), alpha * g * 0.6f * lift)
        }
        fill.color = Themes.mix(color, 0xFFFFFFFF.toInt(), 0.3f + 0.35f * brighten)
        fill.alpha = (255 * alpha).toInt()
        canvas.drawCircle(x, y, dotR * 1.1f * s * (1f + 0.12f * brighten), fill)
        if (highContrast) {
            stroke.color = theme.text
            stroke.alpha = (255 * alpha).toInt()
            stroke.strokeWidth = 2f * density
            canvas.drawCircle(x, y, dotR * 2.0f * s, stroke)
        }
    }

    /**
     * A tapered line in three layers: a wide faint halo, a medium glow and a thin crisp core.
     * The glow passes are drawn as one path each so overlapping segments never double up.
     */
    private fun drawTrail(canvas: Canvas, xs: FloatArray, ys: FloatArray, color: Int, alpha: Float,
                          widthScale: Float, live: Boolean, size: Int = xs.size) {
        if (size < 1 || alpha <= 0f) return
        val segs = size - 1 + (if (live) 1 else 0)
        if (segs < 1) return
        val g = if (highContrast) 0f else theme.glow
        val glowColor = if (missGlow) color else theme.glowColor
        if (g > 0f) {
            path.reset()
            path.moveTo(xs[0], ys[0])
            for (k in 1 until size) path.lineTo(xs[k], ys[k])
            if (live) path.lineTo(tailX, tailY)
            val base = lineW * widthScale
            val lift = 1f + 0.5f * brighten
            stroke.color = glowColor
            stroke.strokeWidth = base * 6.5f * glowScale
            stroke.alpha = (255 * alpha * g * 0.07f * lift).toInt().coerceAtMost(255)
            canvas.drawPath(path, stroke)
            stroke.color = Themes.mix(glowColor, theme.secondaryGlow, 0.35f)
            stroke.strokeWidth = base * 3.0f * glowScale
            stroke.alpha = (255 * alpha * g * 0.17f * lift).toInt().coerceAtMost(255)
            canvas.drawPath(path, stroke)
        }
        for (k in 0 until segs) {
            val x0 = xs[k]; val y0 = ys[k]
            val x1 = if (k + 1 < size) xs[k + 1] else tailX
            val y1 = if (k + 1 < size) ys[k + 1] else tailY
            val frac = if (segs <= 1) 1f else k / (segs - 1f)
            stroke.color = Themes.mix(color, 0xFFFFFFFF.toInt(), 0.2f + 0.3f * brighten)
            stroke.strokeWidth = lineW * widthScale * (0.7f + 0.3f * frac)
            stroke.alpha = (255 * alpha).toInt()
            canvas.drawLine(x0, y0, x1, y1, stroke)
        }
    }

    /**
     * A smooth line through the given points (Catmull-Rom turned into cubic Béziers), ending at the finger when
     * [live]. Same three layers as [drawTrail]: wide halo, medium glow, crisp core.
     */
    private fun drawCurve(canvas: Canvas, xs: FloatArray, ys: FloatArray, n: Int, color: Int, alpha: Float, live: Boolean) {
        if (n < 1 || alpha <= 0f) return
        val total = n + if (live) 1 else 0
        if (total < 2) return
        fun px(i: Int) = if (i >= n) tailX else xs[i.coerceIn(0, n - 1)]
        fun py(i: Int) = if (i >= n) tailY else ys[i.coerceIn(0, n - 1)]
        path.reset()
        path.moveTo(px(0), py(0))
        for (i in 0 until total - 1) {
            val a = (i - 1).coerceAtLeast(0); val d = (i + 2).coerceAtMost(total - 1)
            path.cubicTo(
                px(i) + (px(i + 1) - px(a)) / 6f, py(i) + (py(i + 1) - py(a)) / 6f,
                px(i + 1) - (px(d) - px(i)) / 6f, py(i + 1) - (py(d) - py(i)) / 6f,
                px(i + 1), py(i + 1))
        }
        val g = if (highContrast) 0f else theme.glow
        val glowColor = if (missGlow) color else theme.glowColor
        if (g > 0f) {
            val lift = 1f + 0.5f * brighten
            stroke.color = glowColor
            stroke.strokeWidth = lineW * 6.5f * glowScale
            stroke.alpha = (255 * alpha * g * 0.07f * lift).toInt().coerceAtMost(255)
            canvas.drawPath(path, stroke)
            stroke.color = Themes.mix(glowColor, theme.secondaryGlow, 0.35f)
            stroke.strokeWidth = lineW * 3.0f * glowScale
            stroke.alpha = (255 * alpha * g * 0.17f * lift).toInt().coerceAtMost(255)
            canvas.drawPath(path, stroke)
        }
        stroke.color = Themes.mix(color, 0xFFFFFFFF.toInt(), 0.2f + 0.3f * brighten)
        stroke.strokeWidth = lineW
        stroke.alpha = (255 * alpha).toInt()
        canvas.drawPath(path, stroke)
    }

    private fun drawLight(canvas: Canvas, m: Int, cp: Float, color: Int, alpha: Float) {
        val closed = completionShape == Shape.LOOP
        val laps = if (closed) 2f else 1f
        when (completionShape) {
            Shape.SYMMETRIC -> {
                lightAt(canvas, m, cp * 0.5f, false, color, alpha, 1f)
                lightAt(canvas, m, 1f - cp * 0.5f, false, color, alpha, 1f)
            }
            Shape.SPIRAL -> lightAt(canvas, m, cp, false, color, alpha, 0.7f + cp)
            Shape.ALL_DOTS -> {}
            else -> lightAt(canvas, m, (cp * laps) % 1f, closed, color, alpha, 1f)
        }
    }

    private fun lightAt(canvas: Canvas, m: Int, f: Float, closed: Boolean, color: Int, alpha: Float, size: Float) {
        val segCount = if (closed) m else m - 1
        var total = 0f
        for (k in 0 until segCount) total += segLen(k, m)
        if (total <= 0f) return
        var target = f * total
        for (k in 0 until segCount) {
            val l = segLen(k, m)
            if (target <= l || k == segCount - 1) {
                val u = if (l == 0f) 0f else (target / l).coerceIn(0f, 1f)
                val a = k; val b = (k + 1) % m
                val x = ptsX[a] + (ptsX[b] - ptsX[a]) * u
                val y = ptsY[a] + (ptsY[b] - ptsY[a]) * u
                drawGlow(canvas, x, y, dotR * 4.5f * size, color, alpha * 0.8f)
                fill.color = Themes.mix(color, 0xFFFFFFFF.toInt(), 0.6f)
                fill.alpha = (255 * alpha).toInt()
                canvas.drawCircle(x, y, dotR * 1.3f * size, fill)
                return
            }
            target -= l
        }
    }

    private fun segLen(k: Int, m: Int): Float {
        val b = (k + 1) % m
        return hypot(ptsX[b] - ptsX[k], ptsY[b] - ptsY[k])
    }

    private fun displaceWave(m: Int, cp: Float) {
        val amp = spacing * 0.09f * (1f - cp)
        val ox = ptsX.copyOf(m); val oy = ptsY.copyOf(m)
        for (k in 0 until m) {
            val a = max(0, k - 1); val b = min(m - 1, k + 1)
            var nx = -(oy[b] - oy[a]); var ny = ox[b] - ox[a]
            val len = hypot(nx, ny).coerceAtLeast(1f)
            nx /= len; ny /= len
            val w = sin(2 * PI * (cp * 2 - k / m.toFloat())).toFloat() * amp
            ptsX[k] = ox[k] + nx * w
            ptsY[k] = oy[k] + ny * w
        }
    }

    private fun ensurePts(size: Int) {
        if (ptsX.size < size) { ptsX = FloatArray(size * 2); ptsY = FloatArray(size * 2) }
    }

    private fun mirrorMaps(): List<(Int) -> Int> {
        val n = gridSize
        val v: (Int) -> Int = { i -> (i / n) * n + (n - 1 - i % n) }
        if (!mirrorFourWay) return listOf(v)
        val h: (Int) -> Int = { i -> (n - 1 - i / n) * n + i % n }
        val p: (Int) -> Int = { i -> (n - 1 - i / n) * n + (n - 1 - i % n) }
        return listOf(v, h, p)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(longPress)
        cancelAnimations()
    }

    // ---------------------------------------------------------------- accessibility

    private var a11yFocused = Int.MIN_VALUE
    private var a11yHovered = Int.MIN_VALUE
    private val a11yRect = Rect()
    private val a11yManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

    private fun a11yLabel(id: Int): String {
        val n = gridSize
        if (id == n * n) return "Finish pattern"
        val base = "Row ${id / n + 1}, column ${id % n + 1}"
        val k = selected.indexOf(id)
        return if (k >= 0) "$base, connected, ${k + 1} of ${selected.size}" else base
    }

    private fun a11yBounds(id: Int, out: Rect) {
        val n = gridSize
        if (id == n * n) {
            out.set((gridCx - spacing).toInt(), (gridCy + spacing * n / 2f).toInt(),
                (gridCx + spacing).toInt(), (gridCy + spacing * n / 2f + spacing * 0.6f).toInt())
            return
        }
        val h = (spacing / 2).toInt()
        out.set(centersX[id].toInt() - h, centersY[id].toInt() - h, centersX[id].toInt() + h, centersY[id].toInt() + h)
    }

    private fun sendA11y(id: Int, type: Int) {
        if (a11yManager?.isEnabled != true) return
        @Suppress("DEPRECATION")
        val e = AccessibilityEvent.obtain(type)
        e.packageName = context.packageName
        e.className = "android.widget.Button"
        e.setSource(this, id)
        e.contentDescription = a11yLabel(id)
        parent?.requestSendAccessibilityEvent(this, e)
    }

    private fun a11yClick(id: Int): Boolean {
        val n = gridSize
        if (!interactive || isPlaying) return false
        if (id == n * n) {
            if (selected.size >= 1) {
                val size = selected.size
                drawing = false
                release(isTap = false)
                announceForAccessibility("Pattern finished, $size dots")
            }
            return true
        }
        if (mode == Mode.RIPPLE) { rippleDot(id); invalidate(); return true }
        if (completionStart > 0 || patternAlpha < 1f) { fadeAnimator?.cancel(); clearPattern() }
        if (!isSelected[id]) { addDot(id); speedDp = 500f }
        invalidate()
        return true
    }

    private val provider = object : AccessibilityNodeProvider() {
        override fun createAccessibilityNodeInfo(virtualViewId: Int): AccessibilityNodeInfo? {
            val n = gridSize
            if (virtualViewId == HOST_VIEW_ID) {
                @Suppress("DEPRECATION")
                val info = AccessibilityNodeInfo.obtain(this@PatternView)
                onInitializeAccessibilityNodeInfo(info)
                for (i in 0..n * n) info.addChild(this@PatternView, i)
                return info
            }
            if (virtualViewId < 0 || virtualViewId > n * n) return null
            @Suppress("DEPRECATION")
            val info = AccessibilityNodeInfo.obtain()
            info.setSource(this@PatternView, virtualViewId)
            info.setParent(this@PatternView)
            info.packageName = context.packageName
            info.className = "android.widget.Button"
            info.contentDescription = a11yLabel(virtualViewId)
            info.isEnabled = true
            info.isClickable = true
            info.isFocusable = true
            info.isVisibleToUser = true
            a11yBounds(virtualViewId, a11yRect)
            @Suppress("DEPRECATION")
            info.setBoundsInParent(a11yRect)
            val loc = IntArray(2)
            getLocationOnScreen(loc)
            a11yRect.offset(loc[0], loc[1])
            info.setBoundsInScreen(a11yRect)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
            if (a11yFocused == virtualViewId) {
                info.isAccessibilityFocused = true
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS)
            } else {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS)
            }
            return info
        }

        override fun performAction(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
            if (virtualViewId == HOST_VIEW_ID) return performAccessibilityAction(action, arguments)
            return when (action) {
                AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS -> {
                    a11yFocused = virtualViewId
                    invalidate()
                    sendA11y(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
                    true
                }
                AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS -> {
                    if (a11yFocused == virtualViewId) a11yFocused = Int.MIN_VALUE
                    sendA11y(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED)
                    true
                }
                AccessibilityNodeInfo.ACTION_CLICK -> {
                    val ok = a11yClick(virtualViewId)
                    if (ok) sendA11y(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
                    ok
                }
                else -> false
            }
        }
    }

    override fun getAccessibilityNodeProvider(): AccessibilityNodeProvider? =
        if (a11yManager?.isEnabled == true) provider else super.getAccessibilityNodeProvider()

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        val m = a11yManager
        if (m == null || !m.isEnabled || !m.isTouchExplorationEnabled) return super.dispatchHoverEvent(event)
        val n = gridSize
        var id = Int.MIN_VALUE
        for (i in 0..n * n) {
            a11yBounds(i, a11yRect)
            if (a11yRect.contains(event.x.toInt(), event.y.toInt())) { id = i; break }
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> if (id != a11yHovered) {
                if (id != Int.MIN_VALUE) sendA11y(id, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
                if (a11yHovered != Int.MIN_VALUE) sendA11y(a11yHovered, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT)
                a11yHovered = id
            }
            MotionEvent.ACTION_HOVER_EXIT -> {
                if (a11yHovered != Int.MIN_VALUE) sendA11y(a11yHovered, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT)
                a11yHovered = Int.MIN_VALUE
            }
        }
        return id != Int.MIN_VALUE || super.dispatchHoverEvent(event)
    }
}
