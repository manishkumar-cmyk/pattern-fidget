package com.fidget.patternlock.ui.components

import com.fidget.patternlock.ui.theme.bottomInsets
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Radii
import com.fidget.patternlock.ui.theme.Spacing

enum class NavTab(val label: String, val hint: String, val icon: FidgetIconKind, val iconColor: Color?) {
    DRAW("Draw", "Create calming patterns", FidgetIconKind.DRAW, null),
    MEMORY("Memory", "Remember and revisit", FidgetIconKind.MEMORY, Color(0xFF8FA8FF)),
    COLLECTION("Collection", "Explore patterns", FidgetIconKind.COLLECTION, Color(0xFFA08CFF)),
}

/**
 * Three rounded translucent tiles. The current one is brighter, with an accent border and soft glow.
 * [detailed] adds a one-line description under each name, as on Home.
 */
@Composable
fun FidgetBottomBar(active: NavTab, onSelect: (NavTab) -> Unit, modifier: Modifier = Modifier, detailed: Boolean = false) {
    val c = LocalFidget.current
    Row(
        modifier.fillMaxWidth().bottomInsets().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        NavTab.values().forEach { tab ->
            val on = tab == active
            val iconTint = if (on) c.accent else (if (detailed) tab.iconColor else null) ?: c.textSecondary
            GlowCard(Modifier.weight(1f).height(if (detailed) 124.dp else 96.dp), selected = on, radius = Radii.r24,
                onClick = { onSelect(tab) }, description = "${tab.label}. ${tab.hint}") {
                Column(Modifier.align(Alignment.Center).padding(horizontal = Spacing.sm), horizontalAlignment = Alignment.CenterHorizontally) {
                    FidgetIcon(tab.icon, iconTint, size = 34.dp)
                    FText(tab.label, Modifier.padding(top = Spacing.sm), FidgetType.bodyMedium, c.textPrimary)
                    if (detailed) FText(tab.hint, Modifier.padding(top = 2.dp), FidgetType.label.copy(fontSize = androidx.compose.ui.unit.TextUnit(11.5f, androidx.compose.ui.unit.TextUnitType.Sp)),
                        c.textSecondary, TextAlign.Center)
                }
            }
        }
    }
}
