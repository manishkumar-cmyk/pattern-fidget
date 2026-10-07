package com.fidget.patternlock.ui.themes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.Themes
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.components.FidgetTopBar
import com.fidget.patternlock.ui.components.ThemePreviewCard
import com.fidget.patternlock.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** All five themes as large cards. Tapping one applies it straight away, so the whole app is the preview. */
@Composable
fun ThemesScreen(onBack: () -> Unit) {
    val env = LocalEnv.current
    val settings = env.settings
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        FidgetTopBar("Themes", onBack)
        Column(
            Modifier.weight(1f).widthIn(max = 640.dp).align(Alignment.CenterHorizontally).verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.xl, vertical = Spacing.sm).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Themes.all.forEachIndexed { i, base ->
                ThemePreviewCard(Themes.adjusted(base, settings.amoled, settings.highContrast), selected = i == settings.themeIndex, onClick = {
                    if (i != settings.themeIndex) {
                        settings.themeIndex = i
                        scope.launch { delay(450); env.sound.preview() }
                    }
                })
            }
        }
    }
}
