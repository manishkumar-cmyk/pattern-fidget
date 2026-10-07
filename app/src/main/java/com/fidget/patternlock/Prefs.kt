package com.fidget.patternlock

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

enum class Mode(val label: String, val blurb: String) {
    FREE("Free Draw", "Draw anything. Just enjoy."),
    ENDLESS("Endless Flow", "Keep drawing without stopping."),
    CONSTELLATION("Constellation", "Lines rest and drift."),
    RIPPLE("Ripple", "Touch dots to make ripples."),
    MIRROR("Mirror", "Draw on one side, see the other."),
    LOOP("Pattern Loop", "Draw once, watch it repeat."),
    ZEN("Zen", "No interface. Just the grid."),
}

enum class Difficulty(val label: String) { RELAXED("Relaxed"), CLASSIC("Classic"), FOCUS("Focus") }

/** Every setting in one place, backed by SharedPreferences. */
class Prefs(context: Context) {
    val raw: SharedPreferences = context.getSharedPreferences("fidget", Context.MODE_PRIVATE)

    private val systemReduceMotion = try {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    } catch (e: Exception) { false }

    private fun b(k: String, d: Boolean) = raw.getBoolean(k, d)
    private fun i(k: String, d: Int) = raw.getInt(k, d)
    private fun f(k: String, d: Float) = raw.getFloat(k, d)
    private fun put(block: SharedPreferences.Editor.() -> Unit) = raw.edit().apply(block).apply()

    var themeIndex: Int
        get() = i("theme", 0).coerceIn(0, Themes.all.lastIndex)
        set(v) = put { putInt("theme", v) }
    var grid: Int
        get() = i("grid", 3).coerceIn(3, 5)
        set(v) = put { putInt("grid", v) }
    var mode: Mode
        get() = Mode.values().getOrElse(i("mode3", 0)) { Mode.FREE }.let { if (it == Mode.ZEN) Mode.FREE else it }
        set(v) = put { putInt("mode3", v.ordinal) }
    var mirrorFourWay: Boolean
        get() = b("mirror4", false)
        set(v) = put { putBoolean("mirror4", v) }

    var soundOn: Boolean
        get() = b("sound", true)
        set(v) = put { putBoolean("sound", v) }
    var volume: Float
        get() = f("volume", 0.8f)
        set(v) = put { putFloat("volume", v) }
    /** -1 means "match theme". */
    var soundEnv: Int
        get() = i("env", -1)
        set(v) = put { putInt("env", v) }
    var directional: Boolean
        get() = b("directional", true)
        set(v) = put { putBoolean("directional", v) }

    var hapticsOn: Boolean
        get() = b("haptics", true)
        set(v) = put { putBoolean("haptics", v) }
    /** 1 = Light, 2 = Medium, 3 = Strong. */
    var hapticLevel: Int
        get() = i("hapticLevel", 2).coerceIn(1, 3)
        set(v) = put { putInt("hapticLevel", v) }
    var showLines: Boolean
        get() = b("lines", true)
        set(v) = put { putBoolean("lines", v) }

    var reduceMotion: Boolean
        get() = b("reduceMotion", systemReduceMotion)
        set(v) = put { putBoolean("reduceMotion", v) }
    var highContrast: Boolean
        get() = b("highContrast", false)
        set(v) = put { putBoolean("highContrast", v) }
    var amoled: Boolean
        get() = b("amoled", false)
        set(v) = put { putBoolean("amoled", v) }
    var largerDots: Boolean
        get() = b("largerDots", false)
        set(v) = put { putBoolean("largerDots", v) }
    var quietCompletion: Boolean
        get() = b("quietCompletion", false)
        set(v) = put { putBoolean("quietCompletion", v) }
    var showCount: Boolean
        get() = b("showCount", false)
        set(v) = put { putBoolean("showCount", v) }

    var difficulty: Difficulty
        get() = Difficulty.values().getOrElse(i("difficulty", 1)) { Difficulty.CLASSIC }
        set(v) = put { putInt("difficulty", v.ordinal) }
    fun best(d: Difficulty, n: Int) = i("best_${d.name}_$n", 0)
    fun setBest(d: Difficulty, n: Int, v: Int) = put { putInt("best_${d.name}_$n", v) }

    var totalDots: Long
        get() = raw.getLong("totalDots", 0L)
        set(v) = put { putLong("totalDots", v) }
    /** Id of the saved pattern used as the home idle animation, or 0. */
    var homePattern: Long
        get() = raw.getLong("homePattern", 0L)
        set(v) = put { putLong("homePattern", v) }

    /** One-time hints: each key counts how often it has been shown. */
    fun hintCount(key: String) = i("hint_$key", 0)
    fun bumpHint(key: String) = put { putInt("hint_$key", hintCount(key) + 1) }
}
