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
    /** Soft colour of the glow around dots and lines. */
    val glowColor: Int = active,
    /** Warmer or cooler accent used for the outer halo and the travelling light. */
    val secondaryGlow: Int = line,
    val ripple: Int = active,
    /** Muted warm red used for a missed Memory pattern. Never harsh. */
    val error: Int = 0xFFE58A9A.toInt(),
    /** Mid stop of the background gradient. */
    val bgMid: Int = bgBottom,
    /** Tiny drifting points of light around the pattern (Ember). */
    val particles: Boolean = false,
)

enum class HapticFlavor { ROUNDED, LIGHT, DRY, RISING, TICKS }

private fun c(hex: Long) = hex.toInt()

object Themes {
    val all = listOf(
        Theme("Dusk", "Calm purples and warm glow", c(0xFF060B18), c(0xFF171442), c(0xFF141C38),
            c(0xFF4A4A86), c(0xFFB3BEFF), c(0xFF9FB0FF), c(0xFFE8E8F8), c(0xFF9A9CC4),
            0.9f, 1.0f, false, SoundEnv.CHIME, 2, false, HapticFlavor.ROUNDED,
            glowColor = c(0xFF8F7BFF), secondaryGlow = c(0xFFE0A8FF), ripple = c(0xFFA58CFF), bgMid = c(0xFF0B1126)),
        Theme("Fog", "Minimal and serene", c(0xFF161C2C), c(0xFF4A5470), c(0xFF283044),
            c(0xFF66707F), c(0xFFE2E7EE), c(0xFFCDD5E0), c(0xFFE8ECF2), c(0xFFA0AAB8),
            0.45f, 1.5f, false, SoundEnv.RAIN, 0, false, HapticFlavor.LIGHT,
            glowColor = c(0xFFD5DCE8), secondaryGlow = c(0xFFFFFFFF), ripple = c(0xFFDDE3EC), bgMid = c(0xFF2A3347)),
        Theme("Sage", "Natural and soothing", c(0xFF07120C), c(0xFF2A5236), c(0xFF13261B),
            c(0xFF3F5E4A), c(0xFFBFE8C4), c(0xFFA6D8AE), c(0xFFE3EFE2), c(0xFF8FAA95),
            0.6f, 1.15f, false, SoundEnv.WOOD, -5, false, HapticFlavor.DRY,
            glowColor = c(0xFF78E0A6), secondaryGlow = c(0xFFC8F5D2), ripple = c(0xFF8FE8B5), bgMid = c(0xFF0F2318)),
        Theme("Tide", "Fluid and refreshing", c(0xFF030E18), c(0xFF0C4560), c(0xFF0A2535),
            c(0xFF2B5868), c(0xFF8AF3E6), c(0xFF6FE6D8), c(0xFFDDF4F2), c(0xFF86AEB0),
            0.85f, 1.0f, true, SoundEnv.GLASS, 4, false, HapticFlavor.RISING,
            glowColor = c(0xFF2EE6D6), secondaryGlow = c(0xFF7CC8FF), ripple = c(0xFF4FE0E8), bgMid = c(0xFF06283A)),
        Theme("Ink", "True black, minimal and focused", c(0xFF000000), c(0xFF000000), c(0xFF0F0F12),
            c(0xFF3A3A3D), c(0xFFFFFFFF), c(0xFFE6E6EA), c(0xFFF2F2F4), c(0xFF8E8E94),
            0f, 0.8f, false, SoundEnv.BELLS, -3, true, HapticFlavor.TICKS,
            glowColor = c(0xFFFFFFFF), secondaryGlow = c(0xFFC4CAD6), ripple = c(0xFFD8DCE6), bgMid = c(0xFF000000)),
        Theme("Ember", "Warm gold on a deep night", c(0xFF04060E), c(0xFF0C101E), c(0xFF141826),
            c(0xFFE0B672), c(0xFFFFDFA6), c(0xFFFFC970), c(0xFFF6EAD6), c(0xFFA99B85),
            1.0f, 1.15f, true, SoundEnv.MARIMBA, -2, false, HapticFlavor.ROUNDED,
            glowColor = c(0xFFFFA83A), secondaryGlow = c(0xFFFFD98A), ripple = c(0xFFFFB347), bgMid = c(0xFF080B16),
            particles = true),
    )

    /** Applies accessibility variants on top of a theme. */
    fun adjusted(t: Theme, amoled: Boolean, highContrast: Boolean): Theme {
        var r = t
        if (amoled) r = r.copy(bgTop = 0xFF000000.toInt(), bgBottom = 0xFF000000.toInt(), bgMid = 0xFF000000.toInt(),
            surface = mix(0xFF000000.toInt(), t.surface, 0.6f))
        if (highContrast) r = r.copy(
            bgBottom = r.bgTop, bgMid = r.bgTop,
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
