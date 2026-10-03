package com.fidget.patternlock

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

enum class SoundPack(val label: String) {
    CHIME("Chime"), PIANO("Piano"), MARIMBA("Marimba"), BUBBLES("Bubbles")
}

/** Haptic ticks and synthesized tones. Every sound is generated on the device, so the app ships no audio files. */
class Feedback(private val context: Context, initialPack: SoundPack) {

    var soundOn = true
    var hapticsOn = true

    var pack: SoundPack = initialPack
        set(value) {
            if (field == value) return
            field = value
            loadPack()
        }

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val handler = Handler(Looper.getMainLooper())

    // C major pentatonic, rising: each new dot plays the next note up.
    private val notes = doubleArrayOf(
        523.25, 587.33, 659.25, 783.99, 880.0,
        1046.5, 1174.66, 1318.51, 1567.98, 1760.0, 2093.0, 2349.32
    )
    private val noteIds = IntArray(notes.size)
    private var loadedCount = 0
    private var previewPending = false
    private val failId: Int

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && noteIds.contains(sampleId)) {
                loadedCount++
                if (loadedCount == notes.size && previewPending) {
                    previewPending = false
                    playPreview()
                }
            }
        }
        val failFile = File(context.cacheDir, "fail_v2.wav")
        if (!failFile.exists()) writeWav(failFile, renderFail())
        failId = soundPool.load(failFile.path, 1)
        loadPack()
    }

    private fun loadPack() {
        for (id in noteIds) if (id != 0) soundPool.unload(id)
        loadedCount = 0
        val p = pack
        notes.forEachIndexed { i, f ->
            val file = File(context.cacheDir, "${p.name.lowercase()}_v2_$i.wav")
            if (!file.exists()) writeWav(file, render(p, f))
            noteIds[i] = soundPool.load(file.path, 1)
        }
    }

    private fun note(idx: Int, volume: Float) {
        soundPool.play(noteIds[idx.coerceIn(0, notes.lastIndex)], volume, volume, 1, 0, 1f)
    }

    fun onDot(countInPattern: Int) {
        if (soundOn) note(min(countInPattern - 1, notes.lastIndex), 0.7f)
        tick(14, (60 + countInPattern * 16).coerceAtMost(230))
    }

    fun onRelease(length: Int) {
        if (length < 2) return
        if (soundOn) for (idx in intArrayOf(0, 2, 4)) note(idx, 0.3f)
        wave(longArrayOf(0, 18, 60, 28), intArrayOf(0, 100, 0, 200))
    }

    fun onSuccess() {
        if (soundOn) {
            note(2, 0.35f)
            handler.postDelayed({ note(4, 0.35f) }, 90)
            handler.postDelayed({ note(7, 0.35f) }, 180)
        }
        wave(longArrayOf(0, 16, 70, 16, 70, 24), intArrayOf(0, 90, 0, 140, 0, 200))
    }

    fun onFail() {
        if (soundOn) soundPool.play(failId, 0.6f, 0.6f, 1, 0, 1f)
        wave(longArrayOf(0, 40, 90, 60), intArrayOf(0, 120, 0, 90))
    }

    /** Plays a short rising run so you can hear a pack right after choosing it. */
    fun preview() {
        if (!soundOn) return
        if (loadedCount < notes.size) { previewPending = true; return }
        playPreview()
    }

    private fun playPreview() {
        intArrayOf(0, 2, 4, 5).forEachIndexed { k, idx -> handler.postDelayed({ note(idx, 0.6f) }, k * 130L) }
    }

    private fun tick(ms: Long, amplitude: Int) {
        if (!hapticsOn) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(
            if (v.hasAmplitudeControl()) VibrationEffect.createOneShot(ms, amplitude)
            else VibrationEffect.createOneShot(ms - 2, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    private fun wave(timings: LongArray, amplitudes: IntArray) {
        if (!hapticsOn) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(
            if (v.hasAmplitudeControl()) VibrationEffect.createWaveform(timings, amplitudes, -1)
            else VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    fun shutdown() {
        handler.removeCallbacksAndMessages(null)
        soundPool.release()
    }

    // ---- Synthesis ----

    private val rate = 44100

    private fun render(pack: SoundPack, freq: Double): FloatArray {
        val seconds = when (pack) {
            SoundPack.CHIME -> 0.22
            SoundPack.PIANO -> 0.7
            SoundPack.MARIMBA -> 0.45
            SoundPack.BUBBLES -> 0.15
        }
        val n = (rate * seconds).toInt()
        val out = FloatArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            val s = when (pack) {
                SoundPack.CHIME -> {
                    val env = min(1.0, t / 0.004) * exp(-t * 22.0)
                    (sin(2 * PI * freq * t) + 0.3 * sin(4 * PI * freq * t)) * env
                }
                SoundPack.PIANO -> {
                    // Octave lower, a few harmonics that each die away faster than the last.
                    val f = freq / 2
                    min(1.0, t / 0.003) * (
                        sin(2 * PI * f * t) * exp(-t * 5.0) +
                        0.5 * sin(4 * PI * f * t) * exp(-t * 8.0) +
                        0.25 * sin(6 * PI * f * t) * exp(-t * 11.0) +
                        0.12 * sin(8 * PI * f * t) * exp(-t * 15.0))
                }
                SoundPack.MARIMBA -> {
                    // Wooden bar: strong fundamental plus the bar's inharmonic overtones, quickly damped.
                    val f = freq / 2
                    min(1.0, t / 0.002) * (
                        sin(2 * PI * f * t) * exp(-t * 9.0) +
                        0.35 * sin(2 * PI * f * 3.93 * t) * exp(-t * 38.0) +
                        0.12 * sin(2 * PI * f * 9.2 * t) * exp(-t * 80.0))
                }
                SoundPack.BUBBLES -> {
                    // A quick upward pitch sweep, like a bubble popping at the surface.
                    val f = freq * 0.6 * (1 + 0.9 * (1 - exp(-t * 45.0)))
                    phase += 2 * PI * f / rate
                    min(1.0, t / 0.006) * exp(-t * 28.0) * sin(phase)
                }
            }
            out[i] = s.toFloat()
        }
        return finish(out, 0.55f)
    }

    private fun renderFail(): FloatArray {
        val n = (rate * 0.5).toInt()
        val out = FloatArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            val f = 196.0 * (1 - 0.06 * min(1.0, t / 0.3))
            phase += 2 * PI * f / rate
            out[i] = ((sin(phase) + 0.2 * sin(2 * phase)) * min(1.0, t / 0.01) * exp(-t * 7.0)).toFloat()
        }
        return finish(out, 0.45f)
    }

    /** Normalizes to a calm peak level and fades the tail so nothing clicks. */
    private fun finish(s: FloatArray, peak: Float): FloatArray {
        val max = s.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val fade = (rate * 0.005).toInt().coerceAtMost(s.size)
        for (i in s.indices) {
            var v = s[i] / max * peak
            val fromEnd = s.size - 1 - i
            if (fromEnd < fade) v *= fromEnd.toFloat() / fade
            s[i] = v
        }
        return s
    }

    private fun writeWav(file: File, samples: FloatArray) {
        val n = samples.size
        val buf = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray()); buf.putInt(36 + n * 2); buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray()); buf.putInt(16)
        buf.putShort(1.toShort())          // PCM
        buf.putShort(1.toShort())          // mono
        buf.putInt(rate); buf.putInt(rate * 2)
        buf.putShort(2.toShort())          // block align
        buf.putShort(16.toShort())         // bits per sample
        buf.put("data".toByteArray()); buf.putInt(n * 2)
        for (v in samples) buf.putShort((v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
        file.writeBytes(buf.array())
    }
}
