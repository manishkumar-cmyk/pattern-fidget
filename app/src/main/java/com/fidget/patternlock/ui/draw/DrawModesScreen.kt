@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.fidget.patternlock.ui.draw

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.fillMaxSize
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
import com.fidget.patternlock.ui.components.ModeCardShell
import com.fidget.patternlock.ui.components.ConstellationArt
import com.fidget.patternlock.domain.Constellations
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.fidget.patternlock.ui.theme.Spacing

/** "Choose a mode": every drawing mode as a card in a calm two-column grid. */
@Composable
fun DrawModesScreen(onBack: () -> Unit, onMode: (Mode) -> Unit, onConstellations: () -> Unit) {
    val settings = LocalEnv.current.settings
    Column(Modifier.fillMaxSize()) {
        FidgetTopBar("Draw", onBack, subtitle = "Choose a mode")
        LazyVerticalGrid(
            GridCells.Fixed(2),
            Modifier.fillMaxSize().widthIn(max = 640.dp).align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(start = Spacing.xl, end = Spacing.xl, top = Spacing.sm,
                bottom = Spacing.xxxl + WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                ModeCard(Mode.FREE, selected = true, offsetMs = 0L, onClick = { settings.mode = Mode.FREE; onMode(Mode.FREE) })
            }
            item {
                ModeCardShell("Trace Constellations", "Connect the stars and learn their story.", false, onConstellations) {
                    ConstellationArt(Constellations.all[0], Modifier.fillMaxWidth().height(76.dp), pad = 6.dp, unit = 1.2.dp)
                }
            }
            itemsIndexed(listOf(Mode.ENDLESS, Mode.MIRROR, Mode.RIPPLE, Mode.CONSTELLATION)) { i, m ->
                ModeCard(m, selected = false, offsetMs = (i + 2) * 650L, onClick = { settings.mode = m; onMode(m) })
            }
        }
    }
}
