package com.fidget.patternlock.ui.theme

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The app runs full screen with the system bars hidden. These keep content clear of the camera cut-out and of the
 * space the bars use when a swipe brings them back, so nothing jumps when they appear.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.topInsets(): Modifier =
    windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility.union(WindowInsets.displayCutout).only(WindowInsetsSides.Top))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.bottomInsets(): Modifier =
    windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility.only(WindowInsetsSides.Bottom))
