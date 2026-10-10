@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.fidget.patternlock.ui.journey

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.Themes
import com.fidget.patternlock.data.ProgressStore
import com.fidget.patternlock.domain.levels.Level
import com.fidget.patternlock.domain.levels.LevelPacks
import com.fidget.patternlock.domain.levels.World
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.components.FText
import com.fidget.patternlock.ui.components.FidgetIcon
import com.fidget.patternlock.ui.components.FidgetIconKind
import com.fidget.patternlock.ui.components.FidgetTopBar
import com.fidget.patternlock.ui.components.GlowCard
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Radii
import com.fidget.patternlock.ui.theme.Spacing

/** Warm gold for earned stars and must dots, the same in every theme. */
internal val StarGold = Color(0xFFF6C27C)

/** Where each level sits across the path, repeating: centre, right, centre, left. */
private val Lanes = listOf(0.5f, 0.78f, 0.5f, 0.22f)
private val RowHeight = 112.dp
private val NodeSize = 60.dp

/** The Journey map: each world as a winding path of levels. The next level to play gently pulses. */
@Composable
fun JourneyScreen(onBack: () -> Unit, onLevel: (String) -> Unit) {
    val env = LocalEnv.current
    val progress = env.progress
    val hide = env.settings.hideScores
    val current = progress.nextLevel()
    val list = rememberLazyListState()
    LaunchedEffect(Unit) { list.scrollToItem(((current?.world ?: 1) - 1).coerceAtLeast(0)) }

    val cleared = LevelPacks.all.count { progress.isCleared(it) }
    Column(Modifier.fillMaxSize()) {
        FidgetTopBar("Journey", onBack,
            subtitle = if (hide) "$cleared of ${LevelPacks.all.size} levels" else "${progress.totalStars} of ${progress.maxStars} stars")
        LazyColumn(
            Modifier.fillMaxSize().widthIn(max = 560.dp).align(Alignment.CenterHorizontally), state = list,
            contentPadding = PaddingValues(start = Spacing.xl, end = Spacing.xl, top = Spacing.sm,
                bottom = Spacing.xxxl + WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            items(LevelPacks.worlds, key = { it.number }) { world ->
                WorldSection(world, progress, current, hide, onLevel)
            }
            item {
                FText("More worlds are on their way.", Modifier.fillMaxWidth().padding(vertical = Spacing.lg),
                    FidgetType.caption, LocalFidget.current.textSecondary, TextAlign.Center)
            }
        }
    }
}

@Composable
private fun WorldSection(world: World, progress: ProgressStore, current: Level?, hide: Boolean, onLevel: (String) -> Unit) {
    val c = LocalFidget.current
    val open = progress.isUnlocked(world)
    val look = Themes.all[world.themeIndex]
    Column {
        GlowCard(Modifier.fillMaxWidth(), radius = Radii.r24, tint = Color(look.bgMid).copy(alpha = 0.55f),
            description = "World ${world.number}, ${world.name}. ${if (open) "${progress.clearedIn(world)} of ${world.levels.size} levels cleared" else "Locked"}") {
            Row(Modifier.padding(Spacing.xl), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    FText("WORLD ${world.number}", style = FidgetType.label, color = Color(look.active))
                    FText(world.name, Modifier.padding(top = 2.dp), FidgetType.title)
                    FText(world.blurb, Modifier.padding(top = Spacing.xs), FidgetType.caption, c.textSecondary)
                    FText(
                        when {
                            !open -> "Clear the world before to open"
                            hide -> "${progress.clearedIn(world)} of ${world.levels.size} cleared"
                            else -> "${progress.clearedIn(world)} of ${world.levels.size} cleared  ·  ${progress.starsIn(world)} of ${world.levels.size * 3} stars"
                        },
                        Modifier.padding(top = Spacing.sm), FidgetType.label, c.textPrimary,
                    )
                }
                // A little swatch of the world's theme.
                Box(Modifier.size(52.dp).clip(CircleShape).background(Color(look.bgTop)).border(BorderStroke(1.dp, Color(look.active).copy(alpha = 0.5f)), CircleShape),
                    contentAlignment = Alignment.Center) {
                    if (open) Canvas(Modifier.size(30.dp)) {
                        val r = size.minDimension / 9f
                        for (k in 0 until 3) for (j in 0 until 3) {
                            drawCircle(Color(if ((k + j) % 2 == 0) look.active else look.dot), r, Offset(size.width * (j + 0.5f) / 3f, size.height * (k + 0.5f) / 3f))
                        }
                    } else FidgetIcon(FidgetIconKind.LOCK, c.textSecondary, size = 22.dp)
                }
            }
        }
        LevelPath(world.levels, progress, current, hide, onLevel)
    }
}

@Composable
private fun LevelPath(levels: List<Level>, progress: ProgressStore, current: Level?, hide: Boolean, onLevel: (String) -> Unit) {
    val c = LocalFidget.current
    BoxWithConstraints(Modifier.fillMaxWidth().height(RowHeight * levels.size).padding(top = Spacing.sm)) {
        val w = maxWidth
        val trail = c.textSecondary.copy(alpha = 0.22f)
        val walked = c.accent.copy(alpha = 0.55f)
        Canvas(Modifier.fillMaxSize()) {
            val rowPx = RowHeight.toPx()
            val nodeTop = NodeSize.toPx() / 2f
            fun at(k: Int) = Offset(size.width * Lanes[k % Lanes.size], rowPx * k + nodeTop)
            for (k in 1 until levels.size) {
                val a = at(k - 1)
                val b = at(k)
                val mid = (a.y + b.y) / 2f
                val path = Path().apply { moveTo(a.x, a.y); cubicTo(a.x, mid, b.x, mid, b.x, b.y) }
                drawPath(path, if (progress.isUnlocked(levels[k])) walked else trail,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        levels.forEachIndexed { k, level ->
            val x = w * Lanes[k % Lanes.size]
            LevelNode(
                level, progress.isUnlocked(level), progress.stars(level), level == current, hide,
                onClick = { onLevel(level.id) },
                modifier = Modifier.offset(x = x - 48.dp, y = RowHeight * k),
            )
        }
    }
}

@Composable
private fun LevelNode(level: Level, open: Boolean, stars: Int?, isCurrent: Boolean, hide: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val env = LocalEnv.current
    val c = LocalFidget.current
    val pulse = if (isCurrent && !env.settings.reduceMotion) {
        val t by rememberInfiniteTransition(label = "pulse").animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Restart), label = "ring")
        t
    } else 0f
    val cleared = stars != null
    val state = when {
        !open -> "locked"
        cleared && hide -> "cleared"
        cleared -> "$stars of 3 stars"
        else -> "not played yet"
    }
    Column(modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(NodeSize)
                .drawBehind {
                    if (isCurrent) {
                        val r = size.minDimension / 2f
                        drawCircle(c.accent.copy(alpha = 0.35f * (1f - pulse)), r * (1f + 0.45f * pulse))
                        drawCircle(c.glow.copy(alpha = 0.18f), r * 1.25f)
                    }
                }
                .clip(CircleShape)
                .background(when {
                    isCurrent -> c.accent.copy(alpha = 0.92f)
                    cleared -> c.accentSurface
                    else -> c.surface
                })
                .border(BorderStroke(if (cleared) 1.4.dp else 1.dp, if (cleared) c.accent.copy(alpha = 0.7f) else c.border), CircleShape)
                .clickable(enabled = open, role = Role.Button, onClickLabel = "Play", onClick = onClick)
                .semantics { contentDescription = "Level ${level.number}, ${level.name}, ${level.type.label}, $state" },
            contentAlignment = Alignment.Center,
        ) {
            if (open) FText("${level.number}", style = FidgetType.title, color = if (isCurrent) c.onAccent else c.textPrimary)
            else FidgetIcon(FidgetIconKind.LOCK, c.textSecondary.copy(alpha = 0.7f), size = 20.dp)
        }
        if (cleared && !hide) {
            Row(Modifier.padding(top = Spacing.xs), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (k in 0 until 3) FidgetIcon(if (k < (stars ?: 0)) FidgetIconKind.STAR else FidgetIconKind.STAR_OUTLINE,
                    if (k < (stars ?: 0)) StarGold else c.textSecondary.copy(alpha = 0.4f), size = 13.dp)
            }
        }
        FText(level.name, Modifier.padding(top = if (cleared && !hide) 2.dp else Spacing.xs), FidgetType.label,
            if (open) c.textPrimary else c.textSecondary.copy(alpha = 0.6f), TextAlign.Center, maxLines = 1)
    }
}
