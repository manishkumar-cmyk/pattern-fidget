package com.fidget.patternlock.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fidget.patternlock.Difficulty
import com.fidget.patternlock.Haptics
import com.fidget.patternlock.Mode
import com.fidget.patternlock.PatternStore
import com.fidget.patternlock.Prefs
import com.fidget.patternlock.SoundEngine
import com.fidget.patternlock.SoundEnv
import com.fidget.patternlock.Theme
import com.fidget.patternlock.Themes
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** A property that Compose observes and that writes straight through to storage. */
private class Persisted<T>(initial: T, private val write: (T) -> Unit) : ReadWriteProperty<Any?, T> {
    private var state by mutableStateOf(initial)
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = state
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        state = value
        write(value)
    }
}

/**
 * Observable settings: the ThemeManager and preferences in one place. Every screen reads from here, so a theme
 * or accessibility change propagates immediately.
 */
class FidgetSettings(private val prefs: Prefs) {
    var themeIndex by Persisted(prefs.themeIndex) { prefs.themeIndex = it }
    var amoled by Persisted(prefs.amoled) { prefs.amoled = it }
    var highContrast by Persisted(prefs.highContrast) { prefs.highContrast = it }
    var reduceMotion by Persisted(prefs.reduceMotion) { prefs.reduceMotion = it }
    var largerDots by Persisted(prefs.largerDots) { prefs.largerDots = it }
    var showLines by Persisted(prefs.showLines) { prefs.showLines = it }
    var quietCompletion by Persisted(prefs.quietCompletion) { prefs.quietCompletion = it }

    var soundOn by Persisted(prefs.soundOn) { prefs.soundOn = it }
    var volume by Persisted(prefs.volume) { prefs.volume = it }
    /** Ordinal into [SoundEnv], or -1 to follow the theme's preferred pack. */
    var soundEnv by Persisted(prefs.soundEnv) { prefs.soundEnv = it }
    var directional by Persisted(prefs.directional) { prefs.directional = it }
    var hapticsOn by Persisted(prefs.hapticsOn) { prefs.hapticsOn = it }
    /** 1 = Light, 2 = Medium, 3 = Strong. */
    var hapticLevel by Persisted(prefs.hapticLevel) { prefs.hapticLevel = it }

    var mode by Persisted(prefs.mode) { prefs.mode = it }
    var grid by Persisted(prefs.grid) { prefs.grid = it }
    var mirrorFourWay by Persisted(prefs.mirrorFourWay) { prefs.mirrorFourWay = it }
    var difficulty by Persisted(prefs.difficulty) { prefs.difficulty = it }
    var homePattern by Persisted(prefs.homePattern) { prefs.homePattern = it }
    /** Lifetime connected dots. Held in memory while drawing and written out on release and pause. */
    var totalDots by mutableLongStateOf(prefs.totalDots)
    fun persistTotal() { prefs.totalDots = totalDots }

    val baseTheme: Theme get() = Themes.all[themeIndex]
    val theme: Theme get() = Themes.adjusted(baseTheme, amoled, highContrast)
    val soundPack: SoundEnv get() = SoundEnv.values().getOrNull(soundEnv) ?: baseTheme.sound
}

/** Everything the screens share: settings, storage, sound and haptics. Lives as long as the process. */
class FidgetEnv(context: Context) {
    val prefs = Prefs(context)
    val settings = FidgetSettings(prefs)
    val store = PatternStore(prefs.raw)
    val sound = SoundEngine(context)
    val haptics = Haptics(context)

    init { applySenses() }

    /** Pushes the current settings into the sound and haptic engines. */
    fun applySenses() {
        val s = settings
        val t = s.theme
        sound.enabled = s.soundOn
        sound.volume = s.volume
        sound.directional = s.directional
        sound.configure(s.soundPack, t.keyRoot, t.minorKey)
        haptics.enabled = s.hapticsOn
        haptics.level = s.hapticLevel
        haptics.flavor = t.haptic
    }

    fun mode(): Mode = settings.mode
    fun difficulty(): Difficulty = settings.difficulty
}
