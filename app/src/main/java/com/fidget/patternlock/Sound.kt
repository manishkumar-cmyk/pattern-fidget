package com.fidget.patternlock

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

enum class SoundEnv(val label: String, val octave: Double) {
    CHIME("Chime", 2.0), SOFT_SYNTH("Soft Synth", 1.0), GLASS("Glass", 2.0), WATER("Water", 2.0),
    WOOD("Wood", 1.0), RAIN("Rain", 2.0), BELLS("Bells", 1.0), PIANO("Piano", 1.0), BUBBLES("Bubbles", 2.0),
    /** Appended last so saved sound-pack choices keep their ordinals. */
    MARIMBA("Marimba", 1.0)
}

/**
 * Gentle generative music. Notes come from a two-octave pentatonic scale in the theme's key, and each
 * stroke's direction moves the melody: up rises, down falls, sideways shifts voice, diagonals add a harmony.
 * Every sample is synthesized on the device the first time it's needed, so the app ships no audio files.
 */
class SoundEngine(private val context: Context) {

    var enabled = true
    var volume = 0.8f
    var directional = true

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(12)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build())
        .build()
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()

    @Volatile private var noteIds = IntArray(0)
    @Volatile private var padId = 0
    @Volatile private var rootId = 0
    @Volatile private var missId = 0
    private var configKey = ""
    private var generation = 0

    // Melody state
    private var degree = 0
    private var sideToggle = false
    private val recent = ArrayList<Int>()

    fun configure(env: SoundEnv, keyRoot: Int, minor: Boolean) {
        val key = "${env.name}_${keyRoot}_$minor"
        if (key == configKey) return
        configKey = key
        val gen = ++generation
        worker.execute {
            val intervals = if (minor) intArrayOf(0, 3, 5, 7, 10) else intArrayOf(0, 2, 4, 7, 9)
            val root = 261.63 * 2.0.pow(keyRoot / 12.0) * env.octave
            val ids = IntArray(NOTES) { i ->
                val semis = intervals[i % 5] + 12 * (i / 5)
                load("${key}_$i", gen) { render(env, root * 2.0.pow(semis / 12.0)) }
            }
            val pad = load("${key}_pad", gen) { renderPad(root / env.octave, intervals) }
            val low = load("${key}_root", gen) { renderRoot(root / env.octave / 2) }
            val miss = if (missId != 0) missId else load("miss", gen) { renderMiss() }
            if (gen != generation) return@execute
            val old = noteIds + intArrayOf(padId, rootId)
            noteIds = ids; padId = pad; rootId = low; missId = miss
            for (id in old) if (id != 0 && id != miss) pool.unload(id)
        }
    }

    private fun load(name: String, gen: Int, synth: () -> FloatArray): Int {
        if (gen != generation) return 0
        val file = File(context.cacheDir, "v3_$name.wav")
        if (!file.exists()) writeWav(file, synth())
        return pool.load(file.path, 1)
    }

    private fun play(id: Int, vol: Float, rate: Float = 1f) {
        if (!enabled || id == 0) return
        val v = (vol * volume).coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, rate)
    }

    fun playDegree(d: Int, vol: Float) {
        val ids = noteIds
        if (ids.isEmpty()) return
        play(ids[fold(d)], vol)
    }

    private fun fold(d: Int): Int {
        var x = d
        while (x > NOTES - 1) x -= 5
        while (x < 0) x += 5
        return x
    }

    /**
     * A dot was entered. [dx]/[dy] are the stroke in grid cells (dy < 0 is upward), [speed] in dp per second.
     * Returns the scale degree that was played.
     */
    fun dot(count: Int, row: Int, n: Int, dx: Int, dy: Int, speed: Float): Int {
        if (count <= 1) {
            recent.clear()
            sideToggle = false
            degree = ((n - 1 - row) * 4) / (n - 1).coerceAtLeast(1)
        } else if (!directional) {
            degree = min(count - 1, NOTES - 1)
        } else {
            when {
                dy < 0 -> degree += if (dy <= -2) 2 else 1
                dy > 0 -> degree -= if (dy >= 2) 2 else 1
                else -> { sideToggle = !sideToggle; degree += if (sideToggle) (if (degree >= 5) -5 else 5) else 0 }
            }
            degree = fold(degree)
        }
        // Quick strokes play shorter and quieter, so a fast scribble never gets loud.
        val vol = when {
            speed > 1200f -> 0.5f
            speed < 300f -> 0.8f
            else -> 0.68f
        }
        playDegree(degree, vol)
        if (directional && count > 1 && dx != 0 && dy != 0) playDegree(degree + 2, vol * 0.35f)
        if (count == 4) play(padId, 0.16f)
        recent.add(degree)
        return degree
    }

    /** A plain note for a dot by position: used by Ripple mode and pattern playback. */
    fun dotByPosition(index: Int, n: Int, vol: Float = 0.6f) {
        val r = index / n
        val c = index % n
        playDegree(((n - 1 - r) * 4) / (n - 1) + c * 5 / n, vol)
    }

    fun harmony(vol: Float = 0.3f) = playDegree(degree + 3, vol)

    /** A resolving chord built from the last notes. */
    fun complete(allDots: Boolean) {
        if (recent.isEmpty()) return
        val chord = recent.takeLast(3).distinct()
        chord.forEachIndexed { k, d -> main.postDelayed({ playDegree(d, 0.32f) }, k * 70L) }
        if (allDots) {
            play(rootId, 0.45f)
            play(padId, 0.3f)
        }
        recent.clear()
    }

    /** Endless Flow finished a full cycle: a soft breath. */
    fun breath() {
        play(padId, 0.2f)
        playDegree(0, 0.25f)
    }

    fun miss() = play(missId, 0.4f)

    fun preview() {
        intArrayOf(0, 2, 4, 5, 7).forEachIndexed { k, d -> main.postDelayed({ playDegree(d, 0.6f) }, 120L + k * 140L) }
    }

    fun shutdown() {
        main.removeCallbacksAndMessages(null)
        worker.shutdownNow()
        pool.release()
    }

    // ---------------------------------------------------------------- synthesis

    private fun render(env: SoundEnv, f: Double): FloatArray {
        val seconds = when (env) {
            SoundEnv.CHIME -> 1.2; SoundEnv.SOFT_SYNTH -> 1.6; SoundEnv.GLASS -> 1.4; SoundEnv.WATER -> 0.35
            SoundEnv.WOOD -> 0.5; SoundEnv.RAIN -> 0.45; SoundEnv.BELLS -> 2.4; SoundEnv.PIANO -> 1.0
            SoundEnv.BUBBLES -> 0.16; SoundEnv.MARIMBA -> 0.8
        }
        val n = (RATE * seconds).toInt()
        val out = FloatArray(n)
        val rnd = Random(7)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            val s = when (env) {
                SoundEnv.CHIME -> att(t, 0.003) * (sin(2 * PI * f * t) * exp(-t * 4.5) +
                    0.35 * sin(2 * PI * f * 2.76 * t) * exp(-t * 9.0) +
                    0.15 * sin(2 * PI * f * 5.4 * t) * exp(-t * 14.0))
                SoundEnv.SOFT_SYNTH -> {
                    var v = 0.0
                    for (k in 1..4) {
                        val a = 1.0 / k.toDouble().pow(1.6)
                        v += a * (sin(2 * PI * f * k * 1.003 * t) + sin(2 * PI * f * k * 0.997 * t)) / 2
                    }
                    att(t, 0.04) * exp(-t * 2.2) * v
                }
                SoundEnv.GLASS -> att(t, 0.002) * (sin(2 * PI * f * t) * exp(-t * 3.2) +
                    0.5 * sin(2 * PI * f * 2.32 * t) * exp(-t * 6.0) +
                    0.25 * sin(2 * PI * f * 4.25 * t) * exp(-t * 10.0))
                SoundEnv.WATER -> {
                    val g = f * (0.75 + 0.35 * (1 - exp(-t * 30.0)))
                    phase += 2 * PI * g / RATE
                    att(t, 0.004) * exp(-t * 14.0) * sin(phase)
                }
                SoundEnv.WOOD -> att(t, 0.002) * (sin(2 * PI * f * t) * exp(-t * 9.0) +
                    0.35 * sin(2 * PI * f * 3.93 * t) * exp(-t * 38.0) +
                    0.12 * sin(2 * PI * f * 9.2 * t) * exp(-t * 80.0))
                SoundEnv.RAIN -> att(t, 0.002) * (sin(2 * PI * f * t) * exp(-t * 16.0) +
                    0.25 * (rnd.nextDouble() * 2 - 1) * exp(-t * 140.0))
                SoundEnv.BELLS -> {
                    var v = 0.0
                    for (k in BELL_RATIO.indices) v += BELL_AMP[k] * sin(2 * PI * f * BELL_RATIO[k] * t) * exp(-t * BELL_DECAY[k])
                    att(t, 0.002) * v
                }
                SoundEnv.PIANO -> att(t, 0.003) * (sin(2 * PI * f * t) * exp(-t * 4.0) +
                    0.5 * sin(4 * PI * f * t) * exp(-t * 7.0) +
                    0.25 * sin(6 * PI * f * t) * exp(-t * 10.0) +
                    0.12 * sin(8 * PI * f * t) * exp(-t * 14.0))
                SoundEnv.MARIMBA -> att(t, 0.002) * (sin(2 * PI * f * t) * exp(-t * 6.5) +
                    0.45 * sin(2 * PI * f * 4.0 * t) * exp(-t * 28.0) +
                    0.12 * sin(2 * PI * f * 9.9 * t) * exp(-t * 60.0))
                SoundEnv.BUBBLES -> {
                    val g = f * 0.6 * (1 + 0.9 * (1 - exp(-t * 45.0)))
                    phase += 2 * PI * g / RATE
                    att(t, 0.006) * exp(-t * 28.0) * sin(phase)
                }
            }
            out[i] = s.toFloat()
        }
        return finish(out, 0.55f)
    }

    private fun renderPad(root: Double, intervals: IntArray): FloatArray {
        val n = (RATE * 2.6).toInt()
        val out = FloatArray(n)
        val f1 = root / 2
        val f2 = f1 * 2.0.pow(7 / 12.0)
        val f3 = f1 * 2.0.pow(intervals[2] / 12.0) * 2
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            val env = min(1.0, t / 0.45) * exp(-t * 1.1)
            out[i] = (env * (sin(2 * PI * f1 * t) + 0.6 * sin(2 * PI * f2 * t) + 0.3 * sin(2 * PI * f3 * t))).toFloat()
        }
        return finish(out, 0.4f)
    }

    private fun renderRoot(f: Double): FloatArray {
        val n = (RATE * 2.0).toInt()
        val out = FloatArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            out[i] = (att(t, 0.01) * exp(-t * 1.6) * (sin(2 * PI * f * t) + 0.3 * sin(4 * PI * f * t))).toFloat()
        }
        return finish(out, 0.5f)
    }

    /** A muted, low, short sound for a missed Memory pattern: never a buzzer. */
    private fun renderMiss(): FloatArray {
        val n = (RATE * 0.5).toInt()
        val out = FloatArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / RATE
            val f = 220.0 * (1 - 0.05 * min(1.0, t / 0.3))
            phase += 2 * PI * f / RATE
            out[i] = ((sin(phase) + 0.15 * sin(2 * phase)) * min(1.0, t / 0.015) * exp(-t * 7.0)).toFloat()
        }
        return finish(out, 0.4f)
    }

    private fun att(t: Double, a: Double) = min(1.0, t / a)

    private fun finish(s: FloatArray, peak: Float): FloatArray {
        if (s.isEmpty()) return s
        val max = s.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val fade = (RATE * 0.01).toInt().coerceAtMost(s.size)
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
        buf.putShort(1.toShort()); buf.putShort(1.toShort())
        buf.putInt(RATE); buf.putInt(RATE * 2)
        buf.putShort(2.toShort()); buf.putShort(16.toShort())
        buf.put("data".toByteArray()); buf.putInt(n * 2)
        for (v in samples) buf.putShort((v.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort())
        val tmp = File(file.path + ".tmp")
        tmp.writeBytes(buf.array())
        tmp.renameTo(file)
    }

    private companion object {
        const val RATE = 44100
        const val NOTES = 10
        val BELL_RATIO = doubleArrayOf(0.5, 1.0, 1.19, 1.56, 2.0, 2.51, 3.0)
        val BELL_AMP = doubleArrayOf(0.6, 1.0, 0.5, 0.4, 0.35, 0.2, 0.12)
        val BELL_DECAY = doubleArrayOf(1.2, 1.6, 2.2, 2.6, 3.2, 4.0, 5.0)
    }
}
