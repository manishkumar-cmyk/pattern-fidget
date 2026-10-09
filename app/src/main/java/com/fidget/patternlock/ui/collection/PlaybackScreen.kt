package com.fidget.patternlock.ui.collection

import com.fidget.patternlock.ui.theme.bottomInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.Mode
import com.fidget.patternlock.Shape
import com.fidget.patternlock.Shapes
import com.fidget.patternlock.domain.patternMeta
import com.fidget.patternlock.interaction.GridFeedback
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.components.FText
import com.fidget.patternlock.ui.components.FidgetIconButton
import com.fidget.patternlock.ui.components.FidgetIconKind
import com.fidget.patternlock.ui.components.FidgetSlider
import com.fidget.patternlock.ui.components.FidgetTopBar
import com.fidget.patternlock.ui.components.PatternGrid
import com.fidget.patternlock.ui.components.PatternGridHandle
import com.fidget.patternlock.ui.components.PillButton
import com.fidget.patternlock.ui.components.ToggleRow
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Spacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Plays a saved pattern back on the shared grid, with speed and loop. */
@Composable
fun PlaybackScreen(id: Long, startLooping: Boolean, onBack: () -> Unit) {
    val env = LocalEnv.current
    val c = LocalFidget.current
    val scope = rememberCoroutineScope()
    var pattern by remember { mutableStateOf(env.store.get(id)) }
    val p = pattern
    if (p == null) { LaunchedEffect(Unit) { onBack() }; return }

    val handle = remember { PatternGridHandle() }
    var speed by remember { mutableFloatStateOf(1f) }
    var loop by remember { mutableStateOf(startLooping) }
    var again by remember { mutableStateOf<Job?>(null) }

    fun play() {
        again?.cancel()
        handle.play(p.dots, (420 / speed).toLong()) {
            val shape = Shapes.detect(p.n, p.dots)
            env.sound.complete(shape == Shape.ALL_DOTS)
            if (shape == Shape.ALL_DOTS) env.haptics.allDots() else env.haptics.complete()
            handle.celebrate(true)
            if (loop) again = scope.launch { delay((2200 / speed).toLong()); play() }
        }
    }

    val feedback = remember(p.n) { GridFeedback(env, { Mode.FREE }, { p.n }, counts = false, playbackHaptics = true) }
    LaunchedEffect(Unit) { delay(400); play() }
    DisposableEffect(Unit) { onDispose { again?.cancel() } }

    Column(Modifier.fillMaxSize()) {
        FidgetTopBar(patternTitle(p), onBack, subtitle = patternMeta(p.n, p.dots.size), trailing = {
            FidgetIconButton(if (p.favorite) FidgetIconKind.HEART else FidgetIconKind.HEART_OUTLINE,
                if (p.favorite) "Remove from favourites" else "Add to favourites",
                {
                    val updated = p.copy(favorite = !p.favorite)
                    env.store.update(updated); pattern = updated; env.haptics.dot(1, 300f)
                }, tint = if (p.favorite) c.error else c.textSecondary)
        })
        Box(Modifier.weight(1f).fillMaxWidth()) {
            PatternGrid(Modifier.align(Alignment.Center).fillMaxSize().widthIn(max = 620.dp), gridSize = p.n, handle = handle,
                interactive = false, autoFade = false, listener = feedback)
        }
        Column(Modifier.widthIn(max = 560.dp).align(Alignment.CenterHorizontally).padding(horizontal = Spacing.xl).bottomInsets()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FText("Speed", style = FidgetType.body)
                FidgetSlider((speed - 0.5f) / 1.5f, { speed = 0.5f + it * 1.5f }, "Playback speed", Modifier.weight(1f).padding(horizontal = Spacing.sm))
                FText("${"%.1f".format(speed)}×", Modifier.widthIn(min = 44.dp), FidgetType.caption, c.textSecondary, TextAlign.End)
            }
            ToggleRow("Loop", loop, { loop = it; if (it && !handle.isPlaying) play() })
            PillButton("Play again", { play() }, Modifier.align(Alignment.CenterHorizontally).padding(top = Spacing.sm, bottom = Spacing.xl), primary = true)
        }
    }
}
