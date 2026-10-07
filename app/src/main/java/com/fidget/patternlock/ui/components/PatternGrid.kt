package com.fidget.patternlock.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.fidget.patternlock.Mode
import com.fidget.patternlock.PatternView
import com.fidget.patternlock.Shape
import com.fidget.patternlock.ui.LocalEnv

/** Imperative handle for the grid: playback, fades and completion effects. */
class PatternGridHandle {
    internal var view: PatternView? = null
    val isPlaying get() = view?.isPlaying == true
    val isLooping get() = view?.isLooping == true
    val currentPattern: List<Int> get() = view?.currentPattern ?: emptyList()

    var patternOpacity: Float
        get() = view?.patternOpacity ?: 1f
        set(v) { view?.patternOpacity = v }

    fun play(pattern: List<Int>, stepMs: Long = 450L, onDone: (() -> Unit)? = null) { view?.play(pattern, stepMs, onDone) }
    fun fadeOut(delayMs: Long = 400L, durationMs: Long = 1200L, then: (() -> Unit)? = null) { view?.fadeOut(delayMs, durationMs, then) }
    fun clear() { view?.clearPattern() }
    fun cancel() { view?.cancelAnimations() }
    fun celebrate(fadeAfter: Boolean) { view?.celebrate(fadeAfter) }
    fun miss(then: (() -> Unit)? = null) { view?.miss(then) }
    fun stopLoop() { view?.stopLoop() }
}

/** Forwards to whichever listener is current, so the view is created once and never rebuilt. */
private class ForwardingListener(var target: () -> PatternView.Listener?) : PatternView.Listener {
    override fun onTouchStart() { target()?.onTouchStart() }
    override fun onTouchEnd() { target()?.onTouchEnd() }
    override fun onNear() { target()?.onNear() }
    override fun onDot(index: Int, count: Int, dx: Int, dy: Int, speed: Float, fromPlayback: Boolean) { target()?.onDot(index, count, dx, dy, speed, fromPlayback) }
    override fun onRelease(pattern: List<Int>, shape: Shape) { target()?.onRelease(pattern, shape) }
    override fun onCycle() { target()?.onCycle() }
    override fun onLongPress() { target()?.onLongPress() }
    override fun onTwoFingerTap() { target()?.onTwoFingerTap() }
    override fun onTap() { target()?.onTap() }
    override fun onLoopSpeed(speed: Float) { target()?.onLoopSpeed(speed) }
}

/**
 * The shared grid. Free Draw, Memory, Zen, Mirror, Ripple, Constellation, Endless Flow and Collection playback
 * all use this one component, so gesture rules and rendering never diverge between screens.
 */
@Composable
fun PatternGrid(
    modifier: Modifier = Modifier,
    gridSize: Int = 3,
    mode: Mode = Mode.FREE,
    handle: PatternGridHandle = remember { PatternGridHandle() },
    interactive: Boolean = true,
    autoFade: Boolean = true,
    guide: List<Int>? = null,
    longPress: Boolean = false,
    interruptible: Boolean = false,
    idleBreathing: Boolean = true,
    mirrorFourWay: Boolean = false,
    curved: Boolean = false,
    gridFill: Float = 0.84f,
    listener: PatternView.Listener? = null,
) {
    val settings = LocalEnv.current.settings
    val current = rememberUpdatedState(listener)
    val forwarding = remember { ForwardingListener { current.value } }
    DisposableEffect(handle) { onDispose { handle.view?.cancelAnimations(); handle.view = null } }
    AndroidView(
        modifier = modifier,
        factory = { ctx -> PatternView(ctx).also { it.listener = forwarding; handle.view = it } },
        update = { pv ->
            handle.view = pv
            val th = settings.theme
            if (pv.theme != th) pv.theme = th
            pv.reduceMotion = settings.reduceMotion
            if (pv.largerDots != settings.largerDots) pv.largerDots = settings.largerDots
            pv.showLines = settings.showLines
            pv.quietCompletion = settings.quietCompletion
            pv.highContrast = settings.highContrast
            if (pv.gridFill != gridFill) pv.gridFill = gridFill
            pv.gridSize = gridSize
            pv.mode = mode
            pv.mirrorFourWay = mirrorFourWay
            pv.curvedLines = curved
            pv.interactive = interactive
            pv.autoFade = autoFade
            pv.guide = guide
            pv.longPressEnabled = longPress
            pv.interruptible = interruptible
            pv.idleBreathing = idleBreathing
        },
    )
}
