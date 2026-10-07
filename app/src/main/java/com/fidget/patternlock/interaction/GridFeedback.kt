package com.fidget.patternlock.interaction

import com.fidget.patternlock.Mode
import com.fidget.patternlock.PatternView
import com.fidget.patternlock.Shape
import com.fidget.patternlock.data.FidgetEnv

/**
 * Connects grid events to sound, haptics and the lifetime dot count, so every screen feels the same.
 * Screens add behaviour through the optional callbacks; the sensory part never lives in a composable.
 */
class GridFeedback(
    private val env: FidgetEnv,
    private val mode: () -> Mode,
    private val gridSize: () -> Int,
    /** Count dots towards the lifetime total (off in Memory watch playback, collection playback). */
    private val counts: Boolean = true,
    /** Playback taps are felt too (Memory demo, Collection playback) but not on the quiet home animation. */
    private val playbackHaptics: Boolean = false,
) : PatternView.Listener {

    var touchStart: (() -> Unit)? = null
    var touchEnd: (() -> Unit)? = null
    var released: ((List<Int>, Shape) -> Unit)? = null
    var longPressed: (() -> Unit)? = null
    var twoFingerTapped: (() -> Unit)? = null
    var tapped: (() -> Unit)? = null
    var loopSpeed: ((Float) -> Unit)? = null
    /** Should playback (the home idle animation, Memory's demo) be audible? */
    var playbackAudible: () -> Boolean = { true }
    /** Plays the standard completion sound and haptic on release. Memory turns this off to judge the result first. */
    var completionFeedback = true

    override fun onTouchStart() { touchStart?.invoke() }
    override fun onTouchEnd() { touchEnd?.invoke() }
    override fun onNear() = env.haptics.near()

    override fun onDot(index: Int, count: Int, dx: Int, dy: Int, speed: Float, fromPlayback: Boolean) {
        val n = gridSize()
        if (fromPlayback) {
            if (playbackAudible()) {
                env.sound.dot(count, index / n, n, dx, dy, 500f)
                if (playbackHaptics) env.haptics.dot(count, 500f)
            }
            return
        }
        if (mode() == Mode.RIPPLE) {
            env.sound.dotByPosition(index, n, if (speed > 1200f) 0.45f else 0.65f)
        } else {
            env.sound.dot(count, index / n, n, dx, dy, speed)
            if (mode() == Mode.MIRROR) env.sound.harmony(0.28f)
        }
        env.haptics.dot(count, speed)
        if (counts) env.settings.totalDots++
    }

    override fun onRelease(pattern: List<Int>, shape: Shape) {
        env.settings.persistTotal()
        if (completionFeedback) {
            if (shape == Shape.ALL_DOTS) env.haptics.allDots() else env.haptics.complete()
            env.sound.complete(shape == Shape.ALL_DOTS)
        }
        if (mode() == Mode.LOOP) loopSpeed?.invoke(1f)
        released?.invoke(pattern, shape)
    }

    override fun onCycle() { env.sound.breath(); env.haptics.breath() }
    override fun onLongPress() { env.haptics.complete(); longPressed?.invoke() }
    override fun onTwoFingerTap() { twoFingerTapped?.invoke() }
    override fun onTap() { tapped?.invoke() }
    override fun onLoopSpeed(speed: Float) { loopSpeed?.invoke(speed) }
}
