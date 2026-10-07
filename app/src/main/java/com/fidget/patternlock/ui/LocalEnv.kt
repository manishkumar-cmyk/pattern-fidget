package com.fidget.patternlock.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.fidget.patternlock.data.FidgetEnv

val LocalEnv = staticCompositionLocalOf<FidgetEnv> { error("FidgetEnv not provided") }
