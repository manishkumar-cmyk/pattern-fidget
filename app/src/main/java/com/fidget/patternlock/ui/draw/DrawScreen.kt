package com.fidget.patternlock.ui.draw

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.Mode
import com.fidget.patternlock.interaction.GridFeedback
import com.fidget.patternlock.interaction.SavePrompt
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.components.FText
import com.fidget.patternlock.ui.components.FidgetIconButton
import com.fidget.patternlock.ui.components.FidgetIconKind
import com.fidget.patternlock.ui.components.FidgetTopBar
import com.fidget.patternlock.ui.components.PatternGrid
import com.fidget.patternlock.ui.components.PatternGridHandle
import com.fidget.patternlock.ui.components.SaveChip
import com.fidget.patternlock.ui.components.SegmentedControl
import com.fidget.patternlock.ui.components.ToggleRow
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Motion
import com.fidget.patternlock.ui.theme.Spacing
import com.fidget.patternlock.ui.theme.bottomInsets

/**
 * One screen for every drawing mode. The header and the grid-size selector are there when you arrive; the moment
 * you start drawing they slide away and leave only the grid. The system back gesture brings them back, and a
 * second one leaves. In Draw the line is a smooth curve that follows your finger.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawScreen(mode: Mode, onBack: () -> Unit) {
    val env = LocalEnv.current
    val settings = env.settings
    val c = LocalFidget.current
    val scope = rememberCoroutineScope()
    val handle = remember { PatternGridHandle() }
    val save = remember { SavePrompt(env, scope) }
    var controls by remember { mutableStateOf(false) }
    var chrome by remember { mutableStateOf(true) }
    var loopText by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = !chrome) { chrome = true }

    val feedback = remember(mode) {
        GridFeedback(env, mode = { mode }, gridSize = { settings.grid }).apply {
            touchStart = { save.hide(); chrome = false }
            released = { pattern, _ ->
                if (mode != Mode.LOOP) save.offer(settings.grid, pattern)
            }
            loopSpeed = { v -> loopText = "${"%.1f".format(v)}×  ·  drag up or down for speed  ·  tap to clear" }
            touchEnd = { if (!handle.isLooping) loopText = null }
        }
    }

    Box(Modifier.fillMaxSize()) {
        // The grid never moves: the controls float over it and leave when you start to draw.
        PatternGrid(
            Modifier.align(Alignment.Center).fillMaxSize().widthIn(max = 620.dp),
            gridSize = settings.grid, mode = mode, handle = handle, mirrorFourWay = settings.mirrorFourWay,
            curved = mode == Mode.FREE, gridFill = 0.88f, listener = feedback,
        )

        AnimatedVisibility(
            chrome, Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(Motion.normal()) { -it } + fadeIn(Motion.normal()),
            exit = slideOutVertically(Motion.normal()) { -it } + fadeOut(Motion.fast()),
        ) {
            FidgetTopBar(mode.label, onBack, trailing = {
                FidgetIconButton(FidgetIconKind.TUNE, "Controls", { controls = true }, tint = c.textSecondary)
            })
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().padding(bottom = Spacing.sm)) {
                SaveChip(save)
                loopText?.let {
                    FText(it, Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.md), FidgetType.caption, c.textSecondary)
                }
            }
            AnimatedVisibility(
                chrome,
                enter = slideInVertically(Motion.normal()) { it } + fadeIn(Motion.normal()),
                exit = slideOutVertically(Motion.normal()) { it } + fadeOut(Motion.fast()),
            ) {
                SegmentedControl(
                    listOf("3×3", "4×4", "5×5"), settings.grid - 3, { settings.grid = it + 3 },
                    Modifier.widthIn(max = 420.dp).padding(horizontal = Spacing.xxl, vertical = Spacing.lg).bottomInsets(),
                )
            }
        }
    }

    if (controls) {
        ModalBottomSheet(
            onDismissRequest = { controls = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.theme.let { androidx.compose.ui.graphics.Color(com.fidget.patternlock.Themes.mix(it.bgTop, it.surface, 0.8f)) },
        ) {
            Column(Modifier.padding(horizontal = Spacing.xl).padding(bottom = Spacing.xxxl).bottomInsets()) {
                FText("Controls", style = FidgetType.screenTitle, modifier = Modifier.padding(bottom = Spacing.sm))
                ToggleRow("Sound", settings.soundOn, { settings.soundOn = it })
                ToggleRow("Vibration", settings.hapticsOn, { settings.hapticsOn = it })
                ToggleRow("Show path lines", settings.showLines, { settings.showLines = it }, subtitle = "Off shows dots only")
                if (mode == Mode.MIRROR) {
                    FText("Mirror axis", Modifier.padding(top = Spacing.lg, bottom = Spacing.sm), FidgetType.caption, c.textSecondary)
                    SegmentedControl(listOf("Vertical", "Four-way"), if (settings.mirrorFourWay) 1 else 0, { settings.mirrorFourWay = it == 1 })
                }
            }
        }
    }
}
