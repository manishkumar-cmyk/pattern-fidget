package com.fidget.patternlock.domain

import java.text.NumberFormat
import java.util.Locale

/** "12,458": the calm lifetime dot count, formatted for the device's locale. */
fun formatDotCount(count: Long, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getIntegerInstance(locale).format(count.coerceAtLeast(0L))

/** "3×3 • 6 dots" */
fun patternMeta(n: Int, dots: Int): String = "$n×$n • $dots ${if (dots == 1) "dot" else "dots"}"
