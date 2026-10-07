package com.fidget.patternlock.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Radii
import com.fidget.patternlock.ui.theme.Spacing

enum class NavTab(val label: String, val icon: FidgetIconKind) {
    DRAW("Draw", FidgetIconKind.DRAW), MEMORY("Memory", FidgetIconKind.MEMORY), COLLECTION("Collection", FidgetIconKind.COLLECTION)
}

/** Three rounded translucent tiles. The current one is brighter, with an accent icon and a soft glow. */
@Composable
fun FidgetBottomBar(active: NavTab, onSelect: (NavTab) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalFidget.current
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        NavTab.values().forEach { tab ->
            val on = tab == active
            GlowCard(Modifier.weight(1f).height(96.dp), selected = on, radius = Radii.r24,
                onClick = { onSelect(tab) }, description = tab.label) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    FidgetIcon(tab.icon, if (on) c.accent else c.textSecondary, size = 34.dp)
                    FText(tab.label, Modifier.padding(top = Spacing.sm), FidgetType.bodyMedium, if (on) c.textPrimary else c.textSecondary)
                }
            }
        }
    }
}
