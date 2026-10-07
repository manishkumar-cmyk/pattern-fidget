package com.fidget.patternlock.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.SoundEnv
import com.fidget.patternlock.Themes
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.components.FText
import com.fidget.patternlock.ui.components.FidgetIcon
import com.fidget.patternlock.ui.components.FidgetIconKind
import com.fidget.patternlock.ui.components.FidgetSlider
import com.fidget.patternlock.ui.components.FidgetTopBar
import com.fidget.patternlock.ui.components.GlowCard
import com.fidget.patternlock.ui.components.ThemeTile
import com.fidget.patternlock.ui.components.ToggleRow
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Radii
import com.fidget.patternlock.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun SoundEnv.icon() = when (this) {
    SoundEnv.CHIME, SoundEnv.BELLS -> FidgetIconKind.BELL
    SoundEnv.PIANO -> FidgetIconKind.PIANO
    SoundEnv.MARIMBA, SoundEnv.WOOD -> FidgetIconKind.MARIMBA
    SoundEnv.BUBBLES, SoundEnv.WATER, SoundEnv.RAIN -> FidgetIconKind.BUBBLES
    else -> FidgetIconKind.WAVE
}

private val featuredPacks = listOf(SoundEnv.CHIME, SoundEnv.PIANO, SoundEnv.MARIMBA, SoundEnv.BUBBLES)

@Composable
fun SettingsScreen(onBack: () -> Unit, onSeeThemes: () -> Unit) {
    val env = LocalEnv.current
    val s = env.settings
    val c = LocalFidget.current
    val scope = rememberCoroutineScope()
    var allPacks by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        FidgetTopBar("Settings", onBack)
        Column(
            Modifier.weight(1f).widthIn(max = 640.dp).align(Alignment.CenterHorizontally).verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl).navigationBarsPadding().padding(bottom = Spacing.xxxl),
        ) {
            // Themes: inline previews, "See all" opens the full screen.
            SectionHeader("Themes", "See all", onSeeThemes)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Themes.all.forEachIndexed { i, base ->
                    ThemeTile(Themes.adjusted(base, s.amoled, s.highContrast), i == s.themeIndex, {
                        if (i != s.themeIndex) { s.themeIndex = i; scope.launch { delay(450); env.sound.preview() } }
                    })
                }
            }

            // Sound packs.
            SectionHeader("Sound pack", if (allPacks) "Show less" else "See all") { allPacks = !allPacks }
            val packs: List<SoundEnv?> = if (allPacks) SoundEnv.values().toList() else featuredPacks
            packs.chunked(4).forEach { rowPacks ->
                Row(Modifier.fillMaxWidth().padding(bottom = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    rowPacks.forEach { pack ->
                        pack ?: return@forEach
                        PackTile(pack, pack == s.soundPack, Modifier.weight(1f)) {
                            s.soundEnv = pack.ordinal
                            scope.launch { delay(700); env.sound.preview() }
                        }
                    }
                    repeat(4 - rowPacks.size) { Box(Modifier.weight(1f)) }
                }
            }
            if (allPacks) ToggleRow("Match theme", s.soundEnv < 0, { s.soundEnv = if (it) -1 else s.soundPack.ordinal },
                subtitle = "Uses ${s.baseTheme.sound.label} with the ${s.baseTheme.name} theme")

            Spacer(Spacing.lg)
            // Sound
            SliderRow("Sound", s.volume, { s.volume = it; env.sound.volume = it }, "Volume", s.soundOn, { s.soundOn = it },
                onFinished = { env.sound.playDegree(4, 0.7f) })
            ToggleRow("Direction-aware notes", s.directional, { s.directional = it }, subtitle = "Upward strokes rise, downward strokes fall")

            // Touch
            ToggleRow("Vibration", s.hapticsOn, { s.hapticsOn = it; env.haptics.enabled = it; if (it) env.haptics.preview() })
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                FText("Haptic intensity", Modifier.weight(0.9f), FidgetType.body)
                Slider(
                    s.hapticLevel.toFloat(), { v -> val l = v.roundToInt(); if (l != s.hapticLevel) { s.hapticLevel = l; env.haptics.level = l; env.haptics.preview() } },
                    Modifier.weight(1.1f).semantics { contentDescription = "Haptic intensity" }, valueRange = 1f..3f, steps = 1,
                    colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.inactiveDot.copy(alpha = 0.55f),
                        activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent),
                )
            }

            // Comfort & accessibility
            ToggleRow("Reduce motion", s.reduceMotion, { s.reduceMotion = it }, subtitle = "Fades instead of springs and ripples")
            ToggleRow("Dark mode (AMOLED)", s.amoled, { s.amoled = it }, subtitle = "True black background in every theme")
            ToggleRow("High contrast", s.highContrast, { s.highContrast = it }, subtitle = "Brighter dots and lines, flat background")
            ToggleRow("Larger dots", s.largerDots, { s.largerDots = it }, subtitle = "Bigger dots and touch areas")
            ToggleRow("Show path lines", s.showLines, { s.showLines = it }, subtitle = "Off shows dots only")
            ToggleRow("Quiet completions", s.quietCompletion, { s.quietCompletion = it }, subtitle = "Patterns simply fade when you lift")

            Spacer(Spacing.xl)
            FText("Pattern Fidget 4.0", style = FidgetType.bodyMedium)
            FText("Nothing leaves your device. No accounts, ads or notifications.", style = FidgetType.caption, color = c.textSecondary)
        }
    }
}

@Composable
private fun Spacer(h: androidx.compose.ui.unit.Dp) = Box(Modifier.height(h))

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    val c = LocalFidget.current
    Row(Modifier.fillMaxWidth().padding(top = Spacing.xl, bottom = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        FText(title, Modifier.weight(1f), FidgetType.bodyMedium)
        FText(action, Modifier.clickable(onClick = onAction).padding(vertical = Spacing.sm, horizontal = Spacing.xs), FidgetType.caption, c.accent)
    }
}

@Composable
private fun PackTile(pack: SoundEnv, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalFidget.current
    GlowCard(modifier.height(80.dp), selected = selected, radius = Radii.r16, onClick = onClick, description = "${pack.label} sound pack") {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            FidgetIcon(pack.icon(), if (selected) c.accent else c.textSecondary, size = 26.dp)
            FText(pack.label, Modifier.padding(top = Spacing.xs), FidgetType.label, if (selected) c.textPrimary else c.textSecondary, maxLines = 1)
        }
    }
}

/** A label, a slider and an on/off switch in one row, as in the reference. */
@Composable
private fun SliderRow(
    label: String, value: Float, onValue: (Float) -> Unit, sliderLabel: String, on: Boolean, onToggle: (Boolean) -> Unit,
    onFinished: (() -> Unit)? = null,
) {
    val c = LocalFidget.current
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        FText(label, Modifier.weight(0.9f), FidgetType.body)
        FidgetSlider(value, onValue, sliderLabel, Modifier.weight(1.3f), onFinished)
        Switch(
            on, onToggle, Modifier.padding(start = Spacing.sm).semantics { contentDescription = label },
            colors = SwitchDefaults.colors(checkedThumbColor = c.onAccent, checkedTrackColor = c.accent, checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = c.textSecondary, uncheckedTrackColor = c.surface, uncheckedBorderColor = c.border),
        )
    }
}
