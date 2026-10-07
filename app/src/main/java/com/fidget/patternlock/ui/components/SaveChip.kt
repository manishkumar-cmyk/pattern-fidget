package com.fidget.patternlock.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fidget.patternlock.interaction.SavePrompt
import com.fidget.patternlock.ui.theme.Motion

@Composable
fun BoxScope.SaveChip(prompt: SavePrompt, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        prompt.visible, modifier.align(Alignment.BottomCenter),
        enter = fadeIn(Motion.normal()), exit = fadeOut(Motion.normal()),
    ) {
        PillButton(prompt.label, onClick = { prompt.save() })
    }
}
