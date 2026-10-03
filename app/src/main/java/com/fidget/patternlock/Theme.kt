package com.fidget.patternlock

/** A calm, low-contrast palette. `surface` is used for buttons and thumbnails, `error` for a missed memory round. */
data class Theme(
    val name: String,
    val background: Int,
    val surface: Int,
    val dot: Int,
    val active: Int,
    val line: Int,
    val text: Int,
    val muted: Int,
    val error: Int,
    val isLight: Boolean,
)

private fun c(hex: Long) = hex.toInt()

object Themes {
    val all = listOf(
        Theme("Dusk", c(0xFF1B2027), c(0xFF262D36), c(0xFF3C4652), c(0xFFA9BFD2), c(0xFF8FA9BF),
            c(0xFFD7DEE5), c(0xFF8592A0), c(0xFFD99A9A), false),
        Theme("Fog", c(0xFFE7EBEE), c(0xFFD9E0E5), c(0xFFB3BEC7), c(0xFF48677F), c(0xFF5C7C94),
            c(0xFF26323C), c(0xFF6F7E8A), c(0xFFA8626A), true),
        Theme("Sage", c(0xFFE5EAE2), c(0xFFD6DED2), c(0xFFB4C0AF), c(0xFF557458), c(0xFF6A8A6C),
            c(0xFF28332A), c(0xFF6C7A6B), c(0xFFA86A5E), true),
        Theme("Tide", c(0xFF12201F), c(0xFF1C2E2D), c(0xFF304544), c(0xFF8CC2B8), c(0xFF77AFA5),
            c(0xFFD2E2DF), c(0xFF7F9996), c(0xFFD9A08F), false),
        Theme("Ink", c(0xFF1D1B24), c(0xFF292632), c(0xFF423D4F), c(0xFFC3B6DE), c(0xFFAFA1CE),
            c(0xFFE0DCE8), c(0xFF8E879D), c(0xFFDE9AA6), false),
    )
}
