package com.fidget.patternlock.ui.draw

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.fidget.patternlock.Mode
import com.fidget.patternlock.interaction.GridFeedback
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.components.FidgetIconButton
import com.fidget.patternlock.ui.components.FidgetIconKind
import com.fidget.patternlock.ui.components.PatternGrid
import com.fidget.patternlock.ui.components.SegmentedControl
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Motion
import com.fidget.patternlock.ui.theme.Spacing
import kotlinx.coroutines.delay

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Zen: only the background, the dots, the lines and their glow. A single tap reveals the controls, which fade
 * again after a few quiet seconds. A two-finger tap or the back gesture leaves.
 */
@Composable
fun ZenScreen(onExit: () -> Unit) {
    val env = LocalEnv.current
    val settings = env.settings
    val c = LocalFidget.current
    val view = LocalView.current

    DisposableEffect(Unit) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    // First visits show the controls briefly, as a hint that a tap brings them back.
    val showHint = remember { env.prefs.hintCount("zen2") < 2 }
    var visible by remember { mutableStateOf(showHint) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { if (showHint) env.prefs.bumpHint("zen2") }
    LaunchedEffect(visible, tick) {
        if (visible) { delay(3200); visible = false }
    }

    val feedback = remember {
        GridFeedback(env, mode = { Mode.ENDLESS }, gridSize = { settings.grid }).apply {
            touchStart = { visible = false }
            tapped = { visible = true; tick++ }
            twoFingerTapped = onExit
        }
    }

    Box(Modifier.fillMaxSize()) {
        PatternGrid(
            Modifier.fillMaxSize(), gridSize = settings.grid, mode = Mode.ENDLESS, gridFill = 0.86f, listener = feedback,
        )
        AnimatedVisibility(visible, Modifier.align(Alignment.TopStart), enter = fadeIn(Motion.slow()), exit = fadeOut(Motion.slow())) {
            FidgetIconButton(FidgetIconKind.BACK, "Leave Zen", onExit, Modifier.statusBarsPadding().padding(Spacing.sm), tint = c.textSecondary)
        }
        AnimatedVisibility(visible, Modifier.align(Alignment.BottomCenter), enter = fadeIn(Motion.slow()), exit = fadeOut(Motion.slow())) {
            SegmentedControl(
                listOf("3×3", "4×4", "5×5"), settings.grid - 3, { settings.grid = it + 3; tick++ },
                Modifier.widthIn(max = 380.dp).padding(horizontal = Spacing.xxl, vertical = Spacing.xxl).navigationBarsPadding(),
            )
        }
    }
}
