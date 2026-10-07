package com.fidget.patternlock

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * A small haptic vocabulary: near, enter, connect, complete, all-dots and miss.
 * Uses Android's composed primitives where the phone supports them, and soft one-shots elsewhere.
 */
class Haptics(context: Context) {

    var enabled = true
    /** 1 = Light, 2 = Medium, 3 = Strong. */
    var level = 2
    var flavor = HapticFlavor.ROUNDED

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)

    private val hasPrimitives: Boolean = Build.VERSION.SDK_INT >= 31 && vibrator?.let {
        it.areAllPrimitivesSupported(
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            VibrationEffect.Composition.PRIMITIVE_CLICK,
            VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
            VibrationEffect.Composition.PRIMITIVE_QUICK_FALL)
    } == true

    private var last = 0L

    private val scale get() = when (level) { 1 -> 0.5f; 3 -> 1.0f; else -> 0.8f }

    private fun ready(force: Boolean = false): Vibrator? {
        if (!enabled) return null
        val v = vibrator ?: return null
        if (!v.hasVibrator()) return null
        val now = SystemClock.uptimeMillis()
        if (!force && now - last < 30) return null
        last = now
        return v
    }

    /** Finger approaching a dot. Only at Medium or Strong. */
    fun near() {
        if (level < 2 || flavor == HapticFlavor.TICKS) return
        val v = ready() ?: return
        if (hasPrimitives) compose(v, intArrayOf(LOW_TICK), floatArrayOf(0.15f), intArrayOf(0))
        else oneShot(v, 6, 0.12f)
    }

    /** A dot joined the pattern. Grows slightly stronger as the pattern grows; fast strokes feel crisper. */
    fun dot(count: Int, speed: Float) {
        val v = ready() ?: return
        val base = if (count <= 1) 0.35f else (0.35f + 0.05f * (count - 1)).coerceAtMost(0.7f)
        if (hasPrimitives) {
            val prim = when {
                flavor == HapticFlavor.DRY || flavor == HapticFlavor.TICKS -> TICK
                count <= 1 -> TICK
                speed < 300f -> LOW_TICK
                speed > 1200f -> TICK
                else -> CLICK
            }
            compose(v, intArrayOf(prim), floatArrayOf(base), intArrayOf(0))
        } else {
            oneShot(v, if (speed > 1200f || flavor == HapticFlavor.DRY) 8 else 14, base)
        }
    }

    fun complete() {
        val v = ready(true) ?: return
        if (hasPrimitives) {
            if (flavor == HapticFlavor.RISING)
                compose(v, intArrayOf(CLICK, SLOW_RISE), floatArrayOf(0.5f, 0.25f), intArrayOf(0, 60))
            else
                compose(v, intArrayOf(CLICK, CLICK), floatArrayOf(0.5f, 0.35f), intArrayOf(0, 90))
        } else wave(v, longArrayOf(0, 14, 90, 14), floatArrayOf(0f, 0.5f, 0f, 0.35f))
    }

    fun allDots() {
        val v = ready(true) ?: return
        if (flavor == HapticFlavor.TICKS) { complete(); return }
        if (hasPrimitives) compose(v, intArrayOf(SLOW_RISE, QUICK_FALL), floatArrayOf(0.8f, 0.5f), intArrayOf(0, 0))
        else wave(v, longArrayOf(0, 60, 40, 120), floatArrayOf(0f, 0.4f, 0.6f, 0.8f))
    }

    /** Endless Flow cycle: a single soft swell. */
    fun breath() {
        val v = ready(true) ?: return
        if (hasPrimitives) compose(v, intArrayOf(SLOW_RISE), floatArrayOf(0.3f), intArrayOf(0))
        else oneShot(v, 40, 0.25f)
    }

    fun miss() {
        val v = ready(true) ?: return
        if (hasPrimitives) compose(v, intArrayOf(LOW_TICK, LOW_TICK), floatArrayOf(0.3f, 0.3f), intArrayOf(0, 140))
        else wave(v, longArrayOf(0, 10, 140, 10), floatArrayOf(0f, 0.3f, 0f, 0.3f))
    }

    /** A sample tick for the settings slider. */
    fun preview() = dot(3, 600f)

    private fun compose(v: Vibrator, prims: IntArray, scales: FloatArray, delays: IntArray) {
        if (Build.VERSION.SDK_INT < 30) return
        val c = VibrationEffect.startComposition()
        for (k in prims.indices) c.addPrimitive(prims[k], (scales[k] * scale).coerceIn(0f, 1f), delays[k])
        v.vibrate(c.compose())
    }

    private fun oneShot(v: Vibrator, ms: Long, strength: Float) {
        val amp = (strength * scale * 255).toInt().coerceIn(1, 255)
        v.vibrate(if (v.hasAmplitudeControl()) VibrationEffect.createOneShot(ms, amp)
        else VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun oneShot(v: Vibrator, ms: Int, strength: Float) = oneShot(v, ms.toLong(), strength)

    private fun wave(v: Vibrator, timings: LongArray, strengths: FloatArray) {
        if (!v.hasAmplitudeControl()) { v.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)); return }
        val amps = IntArray(strengths.size) { (strengths[it] * scale * 255).toInt().coerceIn(0, 255) }
        v.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
    }

    private companion object {
        const val TICK = VibrationEffect.Composition.PRIMITIVE_TICK
        const val LOW_TICK = VibrationEffect.Composition.PRIMITIVE_LOW_TICK
        const val CLICK = VibrationEffect.Composition.PRIMITIVE_CLICK
        const val SLOW_RISE = VibrationEffect.Composition.PRIMITIVE_SLOW_RISE
        const val QUICK_FALL = VibrationEffect.Composition.PRIMITIVE_QUICK_FALL
    }
}
