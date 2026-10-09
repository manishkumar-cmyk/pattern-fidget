@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.fidget.patternlock.ui.constellation

import com.fidget.patternlock.ui.theme.bottomInsets
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidget.patternlock.Themes
import com.fidget.patternlock.domain.Constellation
import com.fidget.patternlock.domain.Constellations
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.components.ConstellationArt
import com.fidget.patternlock.ui.components.FText
import com.fidget.patternlock.ui.components.FidgetIcon
import com.fidget.patternlock.ui.components.FidgetIconButton
import com.fidget.patternlock.ui.components.FidgetIconKind
import com.fidget.patternlock.ui.components.FidgetTopBar
import com.fidget.patternlock.ui.components.GlowCard
import com.fidget.patternlock.ui.components.PillButton
import com.fidget.patternlock.ui.components.SkyBackdrop
import com.fidget.patternlock.ui.components.drawConstellation
import com.fidget.patternlock.ui.components.layoutStars
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Radii
import com.fidget.patternlock.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.sign

/** Every constellation as a card. Ones you have traced are brighter and carry a small mark. */
@Composable
fun ConstellationListScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val settings = LocalEnv.current.settings
    val c = LocalFidget.current
    val done = settings.discovered
    Box(Modifier.fillMaxSize()) {
        SkyBackdrop(top = 0.55f, bottom = 0.7f)
        Column(Modifier.fillMaxSize()) {
            FidgetTopBar("Constellations", onBack, subtitle = "${done.size} of ${Constellations.all.size} traced")
            LazyVerticalGrid(
                GridCells.Fixed(2), Modifier.fillMaxSize().widthIn(max = 640.dp).align(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(start = Spacing.xl, end = Spacing.xl, top = Spacing.sm,
                    bottom = Spacing.xxxl + WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(Constellations.all, key = { it.id }) { cons ->
                    val traced = cons.id in done
                    GlowCard(Modifier.fillMaxWidth(), selected = traced, radius = Radii.r20, onClick = { onOpen(cons.id) },
                        description = "${cons.name}, ${cons.keyword}${if (traced) ", traced" else ""}") {
                        Column(Modifier.padding(Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
                            ConstellationArt(cons, Modifier.fillMaxWidth().height(120.dp), pad = 14.dp, dim = if (traced) 0f else 0.15f)
                            FText(cons.name, Modifier.padding(top = Spacing.sm), FidgetType.bodyMedium, c.textPrimary, TextAlign.Center)
                            FText(cons.keyword, style = FidgetType.caption, color = c.textSecondary, align = TextAlign.Center)
                        }
                        if (traced) Box(Modifier.align(Alignment.TopEnd).padding(Spacing.sm)) { FidgetIcon(FidgetIconKind.CHECK, c.accent, size = 18.dp) }
                    }
                }
            }
        }
    }
}

private class Pulse(val star: Int, val start: Long)

/**
 * Trace a constellation by dragging through its stars. Each line lights up when you travel along it; pass over
 * a star that isn't joined to the one you are on and nothing happens. Lift and start again whenever you like.
 * When every line is drawn, the stars glow and a panel tells their story.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun ConstellationPlayScreen(id: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val cons = Constellations.byId(id)
    if (cons == null) { LaunchedEffect(Unit) { onBack() }; return }
    val env = LocalEnv.current
    val colors = LocalFidget.current
    val scope = rememberCoroutineScope()

    var done by remember(id) { mutableStateOf(setOf<Int>()) }
    var current by remember(id) { mutableStateOf<Int?>(null) }
    var finger by remember(id) { mutableStateOf<Offset?>(null) }
    var guide by remember { mutableStateOf(true) }
    var finished by remember(id) { mutableStateOf(false) }
    var showInfo by remember(id) { mutableStateOf(false) }
    val pulses = remember(id) { mutableStateListOf<Pulse>() }
    var clock by remember { mutableLongStateOf(0L) }

    fun now() = System.nanoTime() / 1_000_000

    LaunchedEffect(pulses.size) {
        while (pulses.isNotEmpty()) {
            withFrameMillis { }
            clock = now()
            pulses.removeAll { clock - it.start > 1500 }
        }
    }

    fun finish() {
        finished = true
        env.sound.complete(false)
        env.haptics.complete()
        val t = now()
        cons.stars.indices.forEach { pulses.add(Pulse(it, t + it * 90L)) }
        env.settings.discovered = env.settings.discovered + cons.id
        env.settings.persistDiscovered()
        scope.launch { delay(1100); showInfo = true }
    }

    fun reset() {
        done = emptySet(); current = null; finger = null; finished = false; showInfo = false; pulses.clear()
    }

    Box(Modifier.fillMaxSize()) {
        SkyBackdrop(top = 0.5f, bottom = 0.75f)
        Column(Modifier.fillMaxSize()) {
            FidgetTopBar(
                title = if (finished) cons.name else "Trace the stars", onBack = onBack,
                subtitle = if (finished) cons.keyword else "${done.size} of ${cons.edges.size} lines",
                trailing = {
                    Row {
                        FidgetIconButton(FidgetIconKind.EYE, if (guide) "Hide guide lines" else "Show guide lines", { guide = !guide },
                            tint = if (guide) colors.accent else colors.textSecondary)
                        FidgetIconButton(FidgetIconKind.RETRY, "Start over", { reset() }, tint = colors.textSecondary)
                    }
                },
            )
            Canvas(
                Modifier.weight(1f).fillMaxWidth().padding(bottom = Spacing.xl).pointerInput(cons) {
                    val pad = 56.dp.toPx()
                    val pts = layoutStars(cons, size.width.toFloat(), size.height.toFloat(), pad)
                    // Wide enough for a thumb, but never so wide that two close stars are confused.
                    var nearest = Float.MAX_VALUE
                    for (i in pts.indices) for (j in i + 1 until pts.size) nearest = minOf(nearest, hypot(pts[i].x - pts[j].x, pts[i].y - pts[j].y))
                    val hit = (nearest * 0.42f).coerceIn(14.dp.toPx(), 30.dp.toPx())

                    fun visit(s: Int) {
                        val cur = current
                        if (cur == null) { current = s; env.haptics.dot(1, 300f); return }
                        if (cur == s) return
                        val e = cons.edgeBetween(cur, s)
                        if (e < 0) return
                        if (e !in done) {
                            done = done + e
                            val dx = sign(pts[s].x - pts[cur].x).toInt(); val dy = sign(pts[s].y - pts[cur].y).toInt()
                            val row = (pts[s].y / size.height * 5f).toInt().coerceIn(0, 4)
                            env.sound.dot(done.size, row, 5, dx, dy, 400f)
                            env.haptics.dot(done.size, 400f)
                            pulses.add(Pulse(s, now()))
                            if (done.size == cons.edges.size) finish()
                        }
                        current = s
                    }

                    fun sweep(a: Offset, b: Offset) {
                        if (finished) return
                        val d = b - a
                        val len2 = d.x * d.x + d.y * d.y
                        val hits = pts.indices.mapNotNull { i ->
                            val t = if (len2 == 0f) 0f else (((pts[i].x - a.x) * d.x + (pts[i].y - a.y) * d.y) / len2).coerceIn(0f, 1f)
                            val q = Offset(a.x + d.x * t, a.y + d.y * t)
                            if (hypot(pts[i].x - q.x, pts[i].y - q.y) <= hit) t to i else null
                        }.sortedBy { it.first }
                        hits.forEach { visit(it.second) }
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var prev = down.position
                        finger = prev
                        sweep(prev, prev)
                        do {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull() ?: break
                            for (h in ch.historical) { sweep(prev, h.position); prev = h.position }
                            sweep(prev, ch.position); prev = ch.position
                            finger = ch.position
                            ch.consume()
                        } while (ev.changes.any { it.pressed })
                        current = null
                        finger = null
                    }
                },
            ) {
                val pts = layoutStars(cons, size.width, size.height, 56.dp.toPx())
                val touched = BooleanArray(pts.size)
                cons.edges.forEachIndexed { i, (a, b) -> if (i in done) { touched[a] = true; touched[b] = true } }
                current?.let { touched[it] = true }
                val celebrate = if (finished) 1f else 0f
                drawConstellation(cons, pts, colors, 1.9.dp.toPx(), { if (it in done) 1f else 0f }, { if (touched[it] || finished) 1f else 0f },
                    guide = if (guide && !finished) 0.2f else 0f, boost = celebrate)
                val cur = current; val f = finger
                if (cur != null && f != null) {
                    drawLine(Color(colors.theme.line).copy(alpha = 0.7f), pts[cur], f, strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    drawCircle(Color(colors.theme.glowColor).copy(alpha = 0.3f), 14.dp.toPx(), f)
                }
                for (p in pulses) {
                    val t = ((clock - p.start) / 1500f)
                    if (t < 0f || t >= 1f) continue
                    drawCircle(colors.ripple.copy(alpha = 0.5f * (1f - t)), 10.dp.toPx() + 48.dp.toPx() * t, pts[p.star], style = Stroke(1.5.dp.toPx()))
                }
            }
        }

        // The story, rising in once the constellation is complete.
        AnimatedVisibility(
            showInfo, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(),
        ) {
            StoryCard(cons, onNext = { onOpen(Constellations.next(cons.id, env.settings.discovered).id) }, onAll = onBack)
        }
    }
}

@Composable
private fun StoryCard(cons: Constellation, onNext: () -> Unit, onAll: () -> Unit) {
    val c = LocalFidget.current
    GlowCard(Modifier.fillMaxWidth().widthIn(max = 640.dp).padding(horizontal = Spacing.lg, vertical = Spacing.lg).bottomInsets(),
        radius = Radii.r28, tint = Color(Themes.mix(c.theme.bgTop, c.theme.surface, 0.7f)).copy(alpha = 0.94f)) {
        Column(Modifier.padding(Spacing.xl)) {
            FText(cons.keyword.uppercase(), style = TextStyle(fontSize = 12.sp, letterSpacing = 3.sp), color = c.accent)
            FText(cons.name, Modifier.padding(top = Spacing.xs), TextStyle(fontSize = 28.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
            FText(cons.description, Modifier.padding(top = Spacing.md, bottom = Spacing.xl), FidgetType.body, c.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                PillButton("Next star", onNext, Modifier.weight(1f), primary = true)
                PillButton("All stars", onAll, Modifier.weight(1f))
            }
        }
    }
}
