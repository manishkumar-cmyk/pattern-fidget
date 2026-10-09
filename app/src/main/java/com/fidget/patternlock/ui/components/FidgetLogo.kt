package com.fidget.patternlock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.ui.theme.LocalFidget
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The app mark: a glowing centre inside two thin orbit rings with small stars on them. */
@Composable
fun FidgetLogo(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val c = LocalFidget.current
    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val ctr = Offset(r, r)
        val gold = Color(c.theme.secondaryGlow)
        drawCircle(gold.copy(alpha = 0.7f), r * 0.9f, ctr, style = Stroke(1.2f * density))
        drawCircle(gold.copy(alpha = 0.55f), r * 0.55f, ctr, style = Stroke(1f * density))
        drawCircle(c.glow.copy(alpha = 0.35f), r * 0.42f, ctr)
        drawCircle(Color(c.theme.active), r * 0.26f, ctr)
        for (k in listOf(20f, 110f, 200f, 290f)) {
            val a = k * PI / 180
            drawCircle(gold, r * 0.07f, Offset(ctr.x + (cos(a) * r * 0.9f).toFloat(), ctr.y + (sin(a) * r * 0.9f).toFloat()))
        }
        drawCircle(gold, r * 0.05f, Offset(ctr.x + (cos(PI * 0.9) * r * 0.55f).toFloat(), ctr.y + (sin(PI * 0.9) * r * 0.55f).toFloat()))
    }
}
