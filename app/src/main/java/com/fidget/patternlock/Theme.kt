package com.fidget.patternlock

/**
 * A theme is a full sensory preset: palette, line character, motion pace, sound environment,
 * musical key and haptic flavour. Users may override the sound environment; the rest travels together.
 */
data class Theme(
    val name: String,
    val blurb: String,
    val bgTop: Int,
    val bgBottom: Int,
    val surface: Int,
    val dot: Int,
    val active: Int,
    val line: Int,
    val text: Int,
    val muted: Int,
    /** Strength of the soft glow around lines and dots, 0..1. Ink has none. */
    val glow: Float,
    /** Multiplies every slow duration (fades, ripples, breathing). Fog is the slowest. */
    val pace: Float,
    /** Tide draws ripples as two interfering rings. */
    val doubleRipple: Boolean,
    val sound: SoundEnv,
    /** Root of the theme's pentatonic key, in semitones from middle C. */
    val keyRoot: Int,
    val minorKey: Boolean,
    val haptic: HapticFlavor,
)

enum class HapticFlavor { ROUNDED, LIGHT, DRY, RISING, TICKS }

private fun c(hex: Long) = hex.toInt()

object Themes {
    val all = listOf(
        Theme("Dusk", "Warm violet, glowing trails", c(0xFF14112B), c(0xFF2E2156), c(0xFF231D45),
            c(0xFF4A4378), c(0xFFA9BCFF), c(0xFF8FA8FF), c(0xFFE6E4F5), c(0xFF9A96BD),
            0.9f, 1.0f, false, SoundEnv.CHIME, 2, false, HapticFlavor.ROUNDED),
        Theme("Fog", "Soft grey, slow mist", c(0xFF22272E), c(0xFF414852), c(0xFF2E343C),
            c(0xFF5D6670), c(0xFFDDE3EA), c(0xFFC9D1DA), c(0xFFE8ECF0), c(0xFF9AA4AE),
            0.45f, 1.5f, false, SoundEnv.RAIN, 0, false, HapticFlavor.LIGHT),
        Theme("Sage", "Moss green, wooden notes", c(0xFF111D17), c(0xFF283A2D), c(0xFF1C2B21),
            c(0xFF41594A), c(0xFFB8D8B2), c(0xFFA0C79A), c(0xFFE3EDE0), c(0xFF8EA690),
            0.6f, 1.15f, false, SoundEnv.WOOD, -5, false, HapticFlavor.DRY),
        Theme("Tide", "Deep teal, glass and water", c(0xFF071A21), c(0xFF0F3A44), c(0xFF0E2A32),
            c(0xFF2B5560), c(0xFF7FF0DF), c(0xFF6FE3D2), c(0xFFDDF3F0), c(0xFF83AAA7),
            0.85f, 1.0f, true, SoundEnv.GLASS, 4, false, HapticFlavor.RISING),
        Theme("Ink", "True black, minimal", c(0xFF000000), c(0xFF000000), c(0xFF111111),
            c(0xFF3A3A3A), c(0xFFFFFFFF), c(0xFFE6E6E6), c(0xFFF2F2F2), c(0xFF8C8C8C),
            0f, 0.8f, false, SoundEnv.BELLS, -3, true, HapticFlavor.TICKS),
    )

    /** Applies accessibility variants on top of a theme. */
    fun adjusted(t: Theme, amoled: Boolean, highContrast: Boolean): Theme {
        var r = t
        if (amoled) r = r.copy(bgTop = 0xFF000000.toInt(), bgBottom = 0xFF000000.toInt(),
            surface = mix(0xFF000000.toInt(), t.surface, 0.6f))
        if (highContrast) r = r.copy(
            bgBottom = r.bgTop,
            dot = mix(r.dot, r.text, 0.55f),
            line = r.active,
            muted = mix(r.muted, r.text, 0.5f),
        )
        return r
    }

    fun mix(a: Int, b: Int, f: Float): Int {
        fun ch(s: Int) = (((a shr s) and 0xFF) * (1 - f) + ((b shr s) and 0xFF) * f).toInt() shl s
        return ch(24) or ch(16) or ch(8) or ch(0)
    }

    fun alpha(color: Int, a: Float): Int = (color and 0x00FFFFFF) or ((a.coerceIn(0f, 1f) * 255).toInt() shl 24)
}
