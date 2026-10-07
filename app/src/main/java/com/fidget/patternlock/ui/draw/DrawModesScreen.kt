package com.fidget.patternlock.ui.draw

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.Mode
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.components.FidgetTopBar
import com.fidget.patternlock.ui.components.ModeCard
import com.fidget.patternlock.ui.theme.Spacing

/** "Choose a mode": every drawing mode as a card in a calm two-column grid. */
@Composable
fun DrawModesScreen(onBack: () -> Unit, onMode: (Mode) -> Unit) {
    val settings = LocalEnv.current.settings
    Column(Modifier.fillMaxSize()) {
        FidgetTopBar("Draw", onBack, subtitle = "Choose a mode")
        LazyVerticalGrid(
            GridCells.Fixed(2),
            Modifier.fillMaxSize().widthIn(max = 640.dp).align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(start = Spacing.xl, end = Spacing.xl, top = Spacing.sm, bottom = Spacing.xxxl),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            itemsIndexed(Mode.values().toList()) { i, m ->
                ModeCard(m, selected = m == settings.mode, offsetMs = i * 650L, onClick = {
                    if (m != Mode.ZEN) settings.mode = m
                    onMode(m)
                })
            }
        }
    }
}
