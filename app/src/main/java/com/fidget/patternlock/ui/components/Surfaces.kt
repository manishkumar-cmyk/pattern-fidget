package com.fidget.patternlock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Radii
import kotlin.math.abs

/**
 * The calm backdrop: a vertical gradient, a soft glow behind the centre and a few barely-there stars.
 * Drawn once per size, no animation.
 */
@Composable
fun FidgetBackground(modifier: Modifier = Modifier, stars: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val colors = LocalFidget.current
    Box(modifier.fillMaxSize().background(colors.background).drawBehind {
        val glowStrength = colors.theme.glow
        if (glowStrength > 0f) {
            drawRect(Brush.radialGradient(
                listOf(colors.glow.copy(alpha = 0.16f * glowStrength), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.42f), radius = size.width * 0.95f))
        }
        if (stars && glowStrength > 0f) {
            for (k in 0 until 26) {
                // Deterministic scatter, so the stars never jump between recompositions.
                val fx = abs(((k * 7919) % 1000) / 1000f)
                val fy = abs(((k * 104729) % 1000) / 1000f)
                val r = (0.6f + (k % 3) * 0.45f) * density
                drawCircle(colors.textPrimary.copy(alpha = 0.05f + (k % 4) * 0.018f), r, Offset(fx * size.width, fy * size.height))
            }
        }
    }, content = content)
}

/**
 * A translucent elevated surface. [selected] adds a soft accent border and glow. Pass [onClick] to make it tappable.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    radius: Dp = Radii.r20,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    description: String? = null,
    tint: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalFidget.current
    val shape = RoundedCornerShape(radius)
    val base = tint ?: colors.surface
    var m = modifier
        .drawBehind {
            if (selected && colors.theme.glow > 0f) {
                drawRoundRect(
                    Brush.radialGradient(listOf(colors.glow.copy(alpha = 0.22f * colors.theme.glow), Color.Transparent),
                        center = center, radius = size.maxDimension * 0.85f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()))
            }
        }
        .clip(shape)
        .background(if (selected) Brush.verticalGradient(listOf(colors.accentSurface, base)) else Brush.verticalGradient(listOf(base, base)))
        .border(BorderStroke(if (selected) 1.2.dp else 1.dp, if (selected) colors.accent.copy(alpha = 0.7f) else colors.border), shape)
    if (description != null) m = m.semantics { contentDescription = description; this.selected = selected }
    if (onClick != null) {
        m = m.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = LocalIndication.current,
            role = Role.Button,
            onLongClick = onLongClick,
            onClick = onClick,
        )
    }
    Box(m, content = content)
}
