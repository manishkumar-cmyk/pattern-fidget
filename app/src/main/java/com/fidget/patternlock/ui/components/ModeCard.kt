package com.fidget.patternlock.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.Mode
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Radii
import com.fidget.patternlock.ui.theme.Spacing

private val samples = mapOf(
    Mode.FREE to listOf(6, 4, 2, 5, 8),
    Mode.ENDLESS to listOf(0, 1, 2, 5, 4, 3, 6, 7, 8),
    Mode.MIRROR to listOf(0, 3, 7),
    Mode.RIPPLE to listOf(4, 0, 8),
    Mode.CONSTELLATION to listOf(0, 4, 2, 7),
    Mode.ZEN to listOf(4),
    Mode.LOOP to listOf(0, 1, 4, 3),
)

private fun Mode.thumbStyle() = when (this) {
    Mode.RIPPLE -> ThumbStyle.RIPPLE
    Mode.MIRROR -> ThumbStyle.MIRROR
    Mode.CONSTELLATION -> ThumbStyle.CONSTELLATION
    Mode.ZEN -> ThumbStyle.ZEN
    else -> ThumbStyle.NORMAL
}

/** A drawing mode: a quietly animated mini preview, the name and a one-line description. */
@Composable
fun ModeCard(mode: Mode, selected: Boolean, offsetMs: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ModeCardShell(mode.label, mode.blurb, selected, onClick, modifier) {
        PatternThumb(3, samples.getValue(mode), Modifier.fillMaxWidth().height(76.dp), style = mode.thumbStyle(),
            offsetMs = offsetMs, sizeFraction = 0.95f)
    }
}

/** The card frame shared by every entry in the Draw list. */
@Composable
fun ModeCardShell(title: String, blurb: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
                  art: @Composable () -> Unit) {
    val c = LocalFidget.current
    GlowCard(modifier.fillMaxWidth().heightIn(min = 176.dp), selected = selected, radius = Radii.r24,
        onClick = onClick, description = "$title. $blurb") {
        Column(
            Modifier.fillMaxWidth().padding(Spacing.lg).align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            art()
            FText(title, Modifier.padding(top = Spacing.sm), FidgetType.bodyMedium, c.textPrimary, TextAlign.Center)
            FText(blurb, Modifier.padding(top = Spacing.xs), FidgetType.caption, c.textSecondary, TextAlign.Center)
        }
    }
}
