package com.fidget.patternlock.ui.home

import com.fidget.patternlock.ui.theme.topInsets
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fidget.patternlock.domain.Constellation
import com.fidget.patternlock.domain.Constellations
import com.fidget.patternlock.domain.formatDotCount
import com.fidget.patternlock.domain.levels.LevelPacks
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.components.ConstellationArt
import com.fidget.patternlock.ui.components.FText
import com.fidget.patternlock.ui.components.FidgetBottomBar
import com.fidget.patternlock.ui.components.FidgetIcon
import com.fidget.patternlock.ui.components.FidgetIconKind
import com.fidget.patternlock.ui.components.FidgetLogo
import com.fidget.patternlock.ui.components.NavTab
import com.fidget.patternlock.ui.components.SkyBackdrop
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Spacing
import com.fidget.patternlock.ui.theme.bottomInsets
import com.fidget.patternlock.ui.theme.topInsets
import kotlinx.coroutines.delay
import java.util.Locale

private val Peach = Brush.horizontalGradient(listOf(Color(0xFFFBD9A6), Color(0xFFF0BE80)))
private val Cream = Color(0xFFEADFD0)
private val Amber = Color(0xFFF6C27C)
private val Dusky = Color(0xFFA9B0C8)

@Composable
fun HomeScreen(
    onTab: (NavTab) -> Unit, onSettings: () -> Unit, onConstellation: (String) -> Unit,
    /** Opens the Journey map, then the given level if there is one to continue. */
    onJourney: (String?) -> Unit,
) {
    val env = LocalEnv.current
    val settings = env.settings
    val c = LocalFidget.current

    Box(Modifier.fillMaxSize()) {
        if (!settings.amoled) SkyBackdrop()
        Column(Modifier.fillMaxSize()) {
            // Header: mark, wordmark and settings.
            Row(Modifier.fillMaxWidth().topInsets().padding(horizontal = Spacing.xl, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically) {
                FidgetLogo(size = 48.dp)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Pattern") }
                        withStyle(SpanStyle(fontWeight = FontWeight.Light)) { append(" Fidget") }
                    },
                    Modifier.weight(1f).padding(start = Spacing.md), color = c.textPrimary,
                    style = TextStyle(fontSize = 28.sp, letterSpacing = 0.2.sp),
                )
                Box(Modifier.size(48.dp).clip(CircleShape).background(c.surface).border(BorderStroke(1.dp, c.border), CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Settings", onClick = onSettings)
                    .semantics { contentDescription = "Settings" }, contentAlignment = Alignment.Center) {
                    FidgetIcon(FidgetIconKind.GEAR, c.textPrimary, size = 24.dp)
                }
            }

            // Headline.
            Column(Modifier.fillMaxWidth().padding(top = Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally) {
                FText("Draw patterns.", style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Medium), color = Cream, align = TextAlign.Center)
                FText("Find your calm.", style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Medium), color = Amber, align = TextAlign.Center)
                FText("CONNECT  •  BREATHE  •  RELAX", Modifier.padding(top = Spacing.md),
                    TextStyle(fontSize = 13.sp, letterSpacing = 3.sp), Dusky, TextAlign.Center)
            }

            // Constellations draw themselves here, one after another.
            ConstellationShowcase(Modifier.weight(1f).fillMaxWidth(), onConstellation)

            // Lifetime count.
            Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "${formatDotCount(settings.totalDots)} dots connected" },
                horizontalAlignment = Alignment.CenterHorizontally) {
                FText(formatDotCount(settings.totalDots, Locale.getDefault()), style = TextStyle(fontSize = 42.sp, fontWeight = FontWeight.Medium), color = Cream)
                FText("DOTS CONNECTED", Modifier.padding(top = 2.dp), TextStyle(fontSize = 12.sp, letterSpacing = 3.sp), Dusky)
            }

            JourneyCard(onJourney, Modifier.padding(horizontal = Spacing.xxl).padding(top = Spacing.lg))

            // The call to action sits at the bottom. Tapping it opens into the three tabs; back closes them again.
            var open by remember { mutableStateOf(false) }
            BackHandler(enabled = open) { open = false }
            AnimatedContent(
                open, Modifier.fillMaxWidth(), label = "startOrTabs",
                transitionSpec = {
                    (fadeIn(tween(420, delayMillis = 120)) + scaleIn(tween(420, delayMillis = 120), initialScale = 0.9f, transformOrigin = TransformOrigin(0.5f, 1f)))
                        .togetherWith(fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.9f, transformOrigin = TransformOrigin(0.5f, 1f)))
                        .using(SizeTransform(clip = false))
                },
            ) { isOpen ->
                if (isOpen) FidgetBottomBar(NavTab.DRAW, onSelect = onTab, detailed = true)
                else StartButton(onClick = { open = true }, Modifier.padding(horizontal = Spacing.xxl, vertical = Spacing.xl).bottomInsets())
            }
        }
    }
}

/** Continue the Journey from wherever the player left it. */
@Composable
private fun JourneyCard(onJourney: (String?) -> Unit, modifier: Modifier = Modifier) {
    val env = LocalEnv.current
    val progress = env.progress
    val c = LocalFidget.current
    val next = progress.nextLevel()
    val title = when {
        !progress.hasStarted -> "Begin the Journey"
        next == null -> "Journey complete"
        else -> "Continue the Journey"
    }
    val detail = when {
        next == null -> if (env.settings.hideScores) "Every level cleared" else "${progress.totalStars} of ${progress.maxStars} stars"
        !progress.hasStarted -> "Puzzles to solve, one calm level at a time"
        else -> "${LevelPacks.world(next).name} ${next.number}  ·  ${next.name}"
    }
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier.fillMaxWidth().height(64.dp).clip(shape).background(c.surface)
            .border(BorderStroke(1.dp, Amber.copy(alpha = 0.35f)), shape)
            .clickable(role = Role.Button, onClickLabel = title) { onJourney(if (progress.hasStarted) next?.id else null) }
            .semantics(mergeDescendants = true) { contentDescription = "$title. $detail" }
            .padding(horizontal = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FidgetIcon(FidgetIconKind.JOURNEY, Amber, size = 28.dp)
        Column(Modifier.weight(1f).padding(horizontal = Spacing.md)) {
            FText(title, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium), color = Cream, maxLines = 1)
            FText(detail, style = TextStyle(fontSize = 12.5.sp), color = Dusky, maxLines = 1)
        }
        FidgetIcon(FidgetIconKind.CHEVRON, Cream, size = 20.dp)
    }
}

@Composable
private fun StartButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(36.dp)
    Row(
        modifier.fillMaxWidth().height(68.dp)
            .shadow(20.dp, shape, ambientColor = Amber, spotColor = Amber).clip(shape).background(Peach)
            .clickable(role = Role.Button, onClickLabel = "Open Draw, Memory and Collection", onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).padding(start = Spacing.xxl)) { FidgetIcon(FidgetIconKind.PLAY, Color(0xFF14110C), size = 28.dp) }
        FText("Start Drawing", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium), color = Color(0xFF14110C))
        Box(Modifier.weight(1f).padding(end = Spacing.xxl), contentAlignment = Alignment.CenterEnd) {
            FidgetIcon(FidgetIconKind.CHEVRON, Color(0xFF14110C), size = 24.dp)
        }
    }
}

/**
 * Shows the constellations one after another in random order: stars appear, lines draw themselves, the name
 * fades in, and everything dissolves before the next one begins. Tap to trace the one on screen.
 */
@Composable
private fun ConstellationShowcase(modifier: Modifier, onOpen: (String) -> Unit) {
    val env = LocalEnv.current
    val reduce = env.settings.reduceMotion
    val c = LocalFidget.current
    var current by remember { mutableStateOf<Constellation?>(null) }
    val progress = remember { Animatable(0f) }
    val fade = remember { Animatable(0f) }

    LaunchedEffect(reduce) {
        val bag = ArrayDeque<Constellation>()
        var last: Constellation? = null
        while (true) {
            if (bag.isEmpty()) {
                val shuffled = Constellations.all.shuffled()
                bag.addAll(if (shuffled.first() == last) shuffled.drop(1) + shuffled.first() else shuffled)
            }
            val next = bag.removeFirst()
            last = next
            current = next
            progress.snapTo(if (reduce) 1f else 0f)
            fade.snapTo(0f)
            fade.animateTo(1f, tween(800))
            if (!reduce) progress.animateTo(1f, tween(next.edges.size * 480 + 500, easing = LinearEasing))
            delay(3400)
            fade.animateTo(0f, tween(900))
            delay(250)
        }
    }

    val cons = current
    Box(modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = cons != null) {
        cons?.let { onOpen(it.id) }
    }.semantics { contentDescription = cons?.let { "${it.name} constellation. Tap to trace it." } ?: "Constellations" }) {
        if (cons != null) {
            ConstellationArt(cons, Modifier.fillMaxSize(), progress = progress.value, alpha = fade.value, decor = true, pad = 44.dp, unit = 1.9.dp)
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = Spacing.sm).alpha(fade.value), horizontalAlignment = Alignment.CenterHorizontally) {
                FText(cons.name, style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium), color = Cream)
                FText(cons.keyword.uppercase(), style = TextStyle(fontSize = 11.sp, letterSpacing = 2.5.sp), color = Dusky)
            }
        }
    }
}
