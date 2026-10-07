package com.fidget.patternlock.ui.home

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.Mode
import com.fidget.patternlock.Patterns
import com.fidget.patternlock.domain.formatDotCount
import com.fidget.patternlock.interaction.GridFeedback
import com.fidget.patternlock.interaction.SavePrompt
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.components.FText
import com.fidget.patternlock.ui.components.FidgetBottomBar
import com.fidget.patternlock.ui.components.FidgetIconButton
import com.fidget.patternlock.ui.components.FidgetIconKind
import com.fidget.patternlock.ui.components.NavTab
import com.fidget.patternlock.ui.components.PatternGrid
import com.fidget.patternlock.ui.components.PatternGridHandle
import com.fidget.patternlock.ui.components.SaveChip
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Motion
import com.fidget.patternlock.ui.theme.Spacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import java.util.Locale

private const val GRID = 3

@Composable
fun HomeScreen(onTab: (NavTab) -> Unit, onSettings: () -> Unit, onZen: () -> Unit) {
    val env = LocalEnv.current
    val settings = env.settings
    val c = LocalFidget.current
    val scope = rememberCoroutineScope()
    val handle = remember { PatternGridHandle() }
    val save = remember { SavePrompt(env, scope) }
    var chromeHidden by remember { mutableStateOf(false) }
    var restore by remember { mutableStateOf<Job?>(null) }
    var lastTouch by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    val chromeAlpha by animateFloatAsState(if (chromeHidden) 0f else 1f, Motion.normal(), label = "chrome")

    val feedback = remember {
        GridFeedback(env, mode = { Mode.FREE }, gridSize = { GRID }).apply {
            // The ambient animation is silent; only a deliberate replay is heard.
            playbackAudible = { handle.patternOpacity >= 1f }
            touchStart = {
                lastTouch = SystemClock.uptimeMillis()
                handle.patternOpacity = 1f
                restore?.cancel()
                chromeHidden = true
                save.hide()
            }
            touchEnd = {
                lastTouch = SystemClock.uptimeMillis()
                restore?.cancel()
                restore = scope.launch { delay(1500); chromeHidden = false }
            }
            released = { pattern, _ -> save.offer(GRID, pattern) }
            longPressed = { onZen() }
        }
    }

    // Ambient idle: after a quiet moment the grid slowly traces a pattern by itself, then fades and begins another.
    LaunchedEffect(Unit) {
        delay(1400)
        while (true) {
            val quiet = SystemClock.uptimeMillis() - lastTouch > 2500 && handle.currentPattern.isEmpty() &&
                !handle.isPlaying && !settings.reduceMotion
            if (quiet) {
                handle.patternOpacity = 0.5f
                handle.play(idlePattern(env), 480L) {
                    handle.celebrate(false)
                    handle.fadeOut(900L, 1500L) { handle.patternOpacity = 1f }
                }
                delay(9000)
            } else delay(1500)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().alpha(chromeAlpha).padding(start = Spacing.xxl, end = Spacing.sm, top = Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f).padding(top = Spacing.sm)) {
                FText("Pattern Fidget", style = FidgetType.title)
                FText("A calmer kind of play", Modifier.padding(top = Spacing.xs), FidgetType.body, c.textSecondary)
            }
            FidgetIconButton(FidgetIconKind.GEAR, "Settings", onSettings, tint = c.textSecondary)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            PatternGrid(
                Modifier.align(Alignment.Center).fillMaxSize().widthIn(max = 560.dp),
                gridSize = GRID, mode = Mode.FREE, handle = handle, longPress = true, interruptible = true,
                gridFill = 0.8f, listener = feedback,
            )
            SaveChip(save, Modifier.padding(bottom = Spacing.sm))
        }

        Column(
            Modifier.fillMaxWidth().alpha(chromeAlpha).padding(bottom = Spacing.xs)
                .semantics(mergeDescendants = true) { contentDescription = "${formatDotCount(settings.totalDots)} dots connected" },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FText(formatDotCount(settings.totalDots, Locale.getDefault()), style = FidgetType.hero, align = TextAlign.Center)
            FText("dots connected", style = FidgetType.caption, color = c.textSecondary)
        }
        Box(Modifier.alpha(chromeAlpha)) { FidgetBottomBar(NavTab.DRAW, onSelect = onTab) }
    }
}

private fun idlePattern(env: com.fidget.patternlock.data.FidgetEnv): List<Int> {
    val fav = env.settings.homePattern.takeIf { it != 0L }?.let { env.store.get(it) }
    return if (fav != null && fav.n == GRID) fav.dots else Patterns.random(GRID, Random.nextInt(4, 8))
}
