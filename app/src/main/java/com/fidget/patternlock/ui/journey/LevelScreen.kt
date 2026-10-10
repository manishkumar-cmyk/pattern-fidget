package com.fidget.patternlock.ui.journey

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidget.patternlock.DotMark
import com.fidget.patternlock.Mode
import com.fidget.patternlock.domain.levels.Level
import com.fidget.patternlock.domain.levels.LevelEngine
import com.fidget.patternlock.domain.levels.LevelPacks
import com.fidget.patternlock.domain.levels.LevelType
import com.fidget.patternlock.interaction.GridFeedback
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.components.FText
import com.fidget.patternlock.ui.components.FidgetIcon
import com.fidget.patternlock.ui.components.FidgetIconKind
import com.fidget.patternlock.ui.components.FidgetTopBar
import com.fidget.patternlock.ui.components.GlowCard
import com.fidget.patternlock.ui.components.PatternGrid
import com.fidget.patternlock.ui.components.PatternGridHandle
import com.fidget.patternlock.ui.components.PillButton
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Motion
import com.fidget.patternlock.ui.theme.Radii
import com.fidget.patternlock.ui.theme.Spacing
import com.fidget.patternlock.ui.theme.bottomInsets
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private enum class Phase { SHOW, INPUT, MISSED, SOLVED }

/** Types that show a shape first and then hide it. */
private val LevelType.shows get() = this == LevelType.SILHOUETTE || this == LevelType.REVERSE || this == LevelType.MIRROR

/** Plays one Journey level. Misses fade away gently and the grid is ready again at once. */
@Composable
fun LevelScreen(id: String, onBack: () -> Unit, onNext: (String) -> Unit) {
    val level = LevelPacks.byId(id)
    if (level == null) { LaunchedEffect(Unit) { onBack() }; return }
    val env = LocalEnv.current
    val settings = env.settings
    val c = LocalFidget.current
    val scope = rememberCoroutineScope()
    val handle = remember(id) { PatternGridHandle() }
    val engine = remember(id) { LevelEngine(level) }
    val world = LevelPacks.world(level)
    val next = LevelPacks.next(level)

    var phase by remember(id) { mutableStateOf(if (level.type.shows) Phase.SHOW else Phase.INPUT) }
    var note by remember(id) { mutableStateOf<String?>(null) }
    var stars by remember(id) { mutableIntStateOf(0) }
    var job by remember(id) { mutableStateOf<Job?>(null) }

    /** Plays the shape, then fades it away and hands the grid over. */
    fun show() {
        job?.cancel()
        job = scope.launch {
            phase = Phase.SHOW
            handle.clear()
            delay(450)
            suspendCancellableCoroutine<Unit> { cont ->
                cont.invokeOnCancellation { handle.cancel() }
                handle.play(level.target, 430L) { if (cont.isActive) cont.resume(Unit) }
            }
            delay(if (level.type == LevelType.SILHOUETTE) 700L else 1000L)
            suspendCancellableCoroutine<Unit> { cont ->
                cont.invokeOnCancellation { handle.cancel() }
                handle.fadeOut(0L, 350L) { if (cont.isActive) cont.resume(Unit) }
            }
            phase = Phase.INPUT
        }
    }

    fun showAgain() { engine.peek(); note = null; show() }

    fun skip() {
        if (!env.progress.isCleared(level)) env.progress.record(level, 0)
        if (next != null) onNext(next.id) else onBack()
    }

    val feedback = remember(id) {
        GridFeedback(env, mode = { Mode.FREE }, gridSize = { level.n }).apply {
            completionFeedback = false
            released = { drawn, _ ->
                if (phase == Phase.INPUT) {
                    val outcome = engine.submit(drawn)
                    if (outcome.solved) {
                        phase = Phase.SOLVED
                        note = null
                        stars = outcome.stars
                        env.progress.record(level, outcome.stars)
                        env.sound.complete(outcome.stars == 3)
                        if (outcome.stars == 3) env.haptics.allDots() else env.haptics.complete()
                        handle.celebrate(false)
                    } else {
                        phase = Phase.MISSED
                        note = outcome.note
                        env.sound.miss()
                        env.haptics.miss()
                        handle.miss { if (phase == Phase.MISSED) phase = Phase.INPUT }
                    }
                }
            }
        }
    }

    LaunchedEffect(id) { if (level.type.shows) show() }
    DisposableEffect(id) { onDispose { job?.cancel() } }

    val marks: Map<Int, DotMark> = remember(id) {
        when (level.type) {
            LevelType.PATH -> level.must.associateWith { DotMark.MUST } + level.avoid.associateWith { DotMark.AVOID }
            LevelType.SILHOUETTE -> mapOf(level.target.first() to DotMark.ANCHOR, level.target.last() to DotMark.ANCHOR)
            else -> emptyMap()
        }
    }
    val showMarks = level.type == LevelType.PATH || (level.type == LevelType.SILHOUETTE && phase != Phase.SHOW)

    Column(Modifier.fillMaxSize()) {
        FidgetTopBar(
            title = "${world.name} · ${level.number}", onBack = onBack,
            subtitle = level.name,
        )

        // The goal, always visible, so nobody has to remember the rules.
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
            FText(level.type.label.uppercase(), style = FidgetType.label.copy(letterSpacing = 2.5.sp), color = c.accent)
            FText(goalText(level, engine, settings.hideScores), Modifier.padding(top = Spacing.xs), FidgetType.body, c.textSecondary, TextAlign.Center)
        }

        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.sm), contentAlignment = Alignment.Center) {
            GlowCard(Modifier.fillMaxSize().widthIn(max = 560.dp), radius = Radii.r28) {}
            PatternGrid(
                Modifier.fillMaxSize().widthIn(max = 560.dp), gridSize = level.n, mode = Mode.FREE, handle = handle,
                interactive = phase == Phase.INPUT, autoFade = false, listener = feedback, gridFill = 0.78f,
                outline = if (level.type == LevelType.TRACE && phase != Phase.SOLVED) level.target else null,
                marks = if (showMarks) marks else emptyMap(),
            )
            if (phase == Phase.SHOW) {
                FText("Watch", Modifier.align(Alignment.TopCenter).padding(top = Spacing.xxl), FidgetType.screenTitle, c.textPrimary)
            }
        }

        Box(Modifier.fillMaxWidth().padding(horizontal = Spacing.xl).padding(top = Spacing.sm, bottom = Spacing.xl).bottomInsets()) {
            AnimatedContent(phase == Phase.SOLVED, Modifier.align(Alignment.Center).widthIn(max = 560.dp), label = "levelBottom",
                transitionSpec = { fadeIn(Motion.normal()) togetherWith fadeOut(Motion.fast()) }) { solved ->
                if (solved) Solved(
                    stars = stars, hideScores = settings.hideScores, hasNext = next != null,
                    onNext = { if (next != null) onNext(next.id) else onBack() },
                    onReplay = { onNext(level.id) },
                    onMap = onBack,
                ) else Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FText(note ?: " ", Modifier.fillMaxWidth().padding(bottom = Spacing.md), FidgetType.body,
                        if (note != null) c.textPrimary else c.textSecondary, TextAlign.Center)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        if (level.type.shows) {
                            PillButton("Show again", { showAgain() }, Modifier.weight(1f).alpha(if (phase == Phase.SHOW) 0.4f else 1f), icon = FidgetIconKind.EYE)
                        }
                        if (engine.canSkip) PillButton(if (next != null) "Skip for now" else "Finish", { skip() }, Modifier.weight(1f))
                        if (!level.type.shows && !engine.canSkip) {
                            FText(if (engine.attempts == 0) "Take your time." else "Try as often as you like.",
                                Modifier.fillMaxWidth().height(52.dp).padding(top = Spacing.md), FidgetType.caption, c.textSecondary, TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

private fun goalText(level: Level, engine: LevelEngine, hideScores: Boolean): String =
    if (level.type == LevelType.PATH && !hideScores) "${level.type.goal}\nThe neatest path uses ${engine.par} dots."
    else level.type.goal

@Composable
private fun Solved(stars: Int, hideScores: Boolean, hasNext: Boolean, onNext: () -> Unit, onReplay: () -> Unit, onMap: () -> Unit) {
    val c = LocalFidget.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (hideScores) {
            FidgetIcon(FidgetIconKind.CHECK, c.accent, size = 40.dp)
        } else {
            Row(Modifier.semantics(mergeDescendants = true) { contentDescription = "$stars of 3 stars" },
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.Bottom) {
                for (k in 0 until 3) {
                    val pop = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { delay(180L * k); pop.animateTo(1f, tween(360, easing = Motion.Gentle)) }
                    val earned = k < stars
                    FidgetIcon(if (earned) FidgetIconKind.STAR else FidgetIconKind.STAR_OUTLINE,
                        if (earned) StarGold else c.textSecondary.copy(alpha = 0.5f),
                        Modifier.scale(0.6f + 0.4f * pop.value).alpha(pop.value), size = if (k == 1) 48.dp else 40.dp)
                }
            }
        }
        FText(when {
            hideScores -> "Solved"
            stars == 3 -> "Beautifully done"
            stars == 2 -> "Lovely"
            else -> "Solved"
        }, Modifier.padding(top = Spacing.sm), FidgetType.screenTitle.copy(fontSize = 22.sp), align = TextAlign.Center)
        if (!hideScores && stars < 3) {
            FText("Replay any time for more stars.", Modifier.padding(top = Spacing.xs), FidgetType.caption, c.textSecondary, TextAlign.Center)
        }
        PillButton(if (hasNext) "Next level" else "Back to the map", onNext, Modifier.fillMaxWidth().padding(top = Spacing.lg), primary = true)
        Row(Modifier.fillMaxWidth().padding(top = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            PillButton("Replay", onReplay, Modifier.weight(1f), icon = FidgetIconKind.RETRY)
            PillButton("Map", onMap, Modifier.weight(1f), icon = FidgetIconKind.JOURNEY)
        }
    }
}
