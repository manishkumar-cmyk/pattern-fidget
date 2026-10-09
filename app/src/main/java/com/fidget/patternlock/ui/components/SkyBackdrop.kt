package com.fidget.patternlock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.fidget.patternlock.R

/** The night-sky photograph behind Home and the constellation screens, with a scrim so text and glow stay readable. */
@Composable
fun SkyBackdrop(top: Float = 0.25f, bottom: Float = 0.8f) {
    val navy = Color(0xFF04060E)
    Image(painterResource(R.drawable.bg_home), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
        0f to navy.copy(alpha = top), 0.55f to navy.copy(alpha = 0.05f), 0.8f to navy.copy(alpha = bottom * 0.6f), 1f to navy.copy(alpha = bottom))))
}
