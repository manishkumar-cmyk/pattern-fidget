package com.fidget.patternlock.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp

/** Spacing scale. Generous by design: calm screens need room. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

object Radii {
    val r12 = 12.dp
    val r16 = 16.dp
    val r20 = 20.dp
    val r24 = 24.dp
    val r28 = 28.dp
}

/** Motion constants. Gentle deceleration, no bounce. */
object Motion {
    const val Fast = 160
    const val Normal = 280
    const val Slow = 560
    const val Ambient = 2400

    val Easing = FastOutSlowInEasing
    val Gentle = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> fast() = tween<T>(Fast, easing = Easing)
    fun <T> normal() = tween<T>(Normal, easing = Easing)
    fun <T> slow() = tween<T>(Slow, easing = Gentle)
}
