package com.fidget.patternlock.ui.memory

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidget.patternlock.Difficulty
import com.fidget.patternlock.Mode
import com.fidget.patternlock.domain.MemoryGameEngine
import com.fidget.patternlock.interaction.GridFeedback
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.components.FText
import com.fidget.patternlock.ui.components.FidgetIcon
import com.fidget.patternlock.ui.components.FidgetIconButton
import com.fidget.patternlock.ui.components.FidgetIconKind
import com.fidget.patternlock.ui.components.FidgetTopBar
import com.fidget.patternlock.ui.components.GlowCard
import com.fidget.patternlock.ui.components.PatternGrid
import com.fidget.patternlock.ui.components.PatternGridHandle
import com.fidget.patternlock.ui.components.PillButton
import com.fidget.patternlock.ui.components.SegmentedControl
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Motion
import com.fidget.patternlock.ui.theme.Radii
import com.fidget.patternlock.ui.theme.Spacing
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private enum class Phase { WATCH, INPUT, MATCHED, MISSED }

@Composable
fun MemoryScreen(onBack: () -> Unit) {
    val env = LocalEnv.current
    val settings = env.settings
    val c = LocalFidget.current
    val n = settings.grid
    val scope = rememberCoroutineScope()
    val handle = remember { PatternGridHandle() }
    val engine = remember(n) { MemoryGameEngine(n) }

    var phase by remember { mutableStateOf(Phase.WATCH) }
    var level by remember { mutableIntStateOf(1) }
    var guide by remember { mutableStateOf<List<Int>?>(null) }
    var showBest by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }

    fun bestText(): String {
        val b = env.prefs.best(settings.difficulty, n)
        return if (b > 0) "Longest remembered: $b dots" else "Nothing remembered yet. No rush."
    }

    /** Plays the target, then hides it (or leaves a faint guide in Relaxed) and hands over to the player. */
    fun watch() {
        job?.cancel()
        job = scope.launch {
            phase = Phase.WATCH
            guide = null
            handle.clear()
            delay(500)
            val step = if (settings.difficulty == Difficulty.FOCUS) 300L else 450L
            suspendCancellableCoroutine<Unit> { cont ->
                cont.invokeOnCancellation { handle.cancel() }
                handle.play(engine.target, step) { if (cont.isActive) cont.resume(Unit) }
            }
            delay(1000)
            if (settings.difficulty == Difficulty.RELAXED) {
                guide = engine.target
                handle.clear()
            } else {
                suspendCancellableCoroutine<Unit> { cont ->
                    cont.invokeOnCancellation { handle.cancel() }
                    handle.fadeOut(0L, 350L) { if (cont.isActive) cont.resume(Unit) }
                }
            }
            phase = Phase.INPUT
        }
    }

    fun startRound() {
        engine.newRound()
        level = engine.level
        watch()
    }

    fun startFresh() { engine.reset(); startRound() }

    fun tryAgain() {
        job?.cancel()
        handle.cancel()
        handle.clear()
        phase = Phase.INPUT
    }

    val feedback = remember(n) {
        GridFeedback(env, mode = { Mode.FREE }, gridSize = { n }, playbackHaptics = true).apply {
            completionFeedback = false
            released = { drawn, _ ->
                if (phase == Phase.INPUT) {
                    if (engine.matches(drawn)) {
                        phase = Phase.MATCHED
                        env.sound.complete(false)
                        env.haptics.complete()
                        guide = null
                        handle.celebrate(true)
                        if (engine.target.size > env.prefs.best(settings.difficulty, n)) {
                            env.prefs.setBest(settings.difficulty, n, engine.target.size)
                        }
                        engine.advance()
                        job = scope.launch { delay(1700); startRound() }
                    } else {
                        phase = Phase.MISSED
                        env.sound.miss()
                        env.haptics.miss()
                        handle.miss()
                    }
                }
            }
        }
    }

    LaunchedEffect(n) { startRound() }
    DisposableEffect(Unit) { onDispose { job?.cancel() } }

    val missed = phase == Phase.MISSED
    Column(Modifier.fillMaxSize()) {
        FidgetTopBar(
            title = if (phase == Phase.INPUT) "Your turn" else "Memory", onBack = onBack,
            subtitle = when {
                showBest -> bestText()
                phase == Phase.INPUT -> "${engine.target.size} dots"
                phase == Phase.MATCHED -> "Well remembered"
                else -> null
            },
            trailing = {
                FidgetIconButton(FidgetIconKind.CHART, "Longest remembered", { showBest = !showBest }, tint = c.textSecondary)
            },
        )

        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.sm), contentAlignment = Alignment.Center) {
            GlowCard(Modifier.fillMaxSize().widthIn(max = 560.dp).alpha(if (missed) 0f else 1f), radius = Radii.r28) {}
            PatternGrid(
                Modifier.fillMaxSize().widthIn(max = 560.dp), gridSize = n, mode = Mode.FREE, handle = handle,
                interactive = phase == Phase.INPUT, autoFade = false, guide = guide, listener = feedback, gridFill = 0.78f,
            )
            AnimatedContent(phase == Phase.WATCH, Modifier.align(Alignment.TopCenter).padding(top = Spacing.xxl), label = "levelTitle",
                transitionSpec = { fadeIn(Motion.normal()) togetherWith fadeOut(Motion.fast()) }) { watching ->
                if (watching) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FText("Level $level", style = FidgetType.screenTitle)
                    FText("Watch the pattern", Modifier.padding(top = Spacing.xs), FidgetType.caption, c.textSecondary)
                } else Box(Modifier.height(1.dp))
            }
        }

        Box(Modifier.fillMaxWidth().padding(horizontal = Spacing.xl).padding(top = Spacing.sm, bottom = Spacing.xl).navigationBarsPadding()
            .align(Alignment.CenterHorizontally)) {
            AnimatedContent(phase, Modifier.align(Alignment.Center).widthIn(max = 560.dp), label = "bottom",
                transitionSpec = { fadeIn(Motion.normal()) togetherWith fadeOut(Motion.fast()) }) { p ->
                when (p) {
                    Phase.WATCH, Phase.MATCHED -> SegmentedControl(
                        Difficulty.values().map { it.label }, settings.difficulty.ordinal,
                        { settings.difficulty = Difficulty.values()[it]; if (phase == Phase.WATCH) watch() },
                    )
                    Phase.INPUT -> Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        ActionTile("Show me", FidgetIconKind.EYE, settings.difficulty != Difficulty.FOCUS, Modifier.weight(1f)) { watch() }
                        ActionTile("Try again", FidgetIconKind.RETRY, true, Modifier.weight(1f)) { tryAgain() }
                        ActionTile("Start fresh", FidgetIconKind.HOME, true, Modifier.weight(1f)) { startFresh() }
                    }
                    Phase.MISSED -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FText("Pattern missed", style = FidgetType.screenTitle.copy(fontSize = 22.sp), align = TextAlign.Center)
                        FText("Take your time. You can try again.", Modifier.padding(top = Spacing.sm, bottom = Spacing.xl), FidgetType.body, c.textSecondary, TextAlign.Center)
                        PillButton("Try again", { tryAgain() }, Modifier.fillMaxWidth(), primary = true)
                        if (settings.difficulty != Difficulty.FOCUS) {
                            PillButton("Show pattern", { watch() }, Modifier.fillMaxWidth().padding(top = Spacing.md))
                        }
                        PillButton("Start fresh", { startFresh() }, Modifier.fillMaxWidth().padding(top = Spacing.md))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionTile(label: String, icon: FidgetIconKind, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = LocalFidget.current
    GlowCard(modifier.height(76.dp).alpha(if (enabled) 1f else 0.4f), radius = Radii.r20,
        onClick = if (enabled) onClick else null, description = label) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            FidgetIcon(icon, c.textPrimary, size = 24.dp)
            FText(label, Modifier.padding(top = Spacing.xs), FidgetType.label, c.textPrimary)
        }
    }
}
