package com.fidget.patternlock.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.Theme
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Radii
import com.fidget.patternlock.ui.theme.Spacing

/** A wide theme card: the theme's own landscape, its name and a one-line description. */
@Composable
fun ThemePreviewCard(theme: Theme, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalFidget.current.accent
    val shape = RoundedCornerShape(Radii.r24)
    Box(
        modifier.fillMaxWidth().height(104.dp).clip(shape)
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else Color(theme.text).copy(alpha = 0.08f)), shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "${theme.name} theme. ${theme.blurb}"; this.selected = selected },
    ) {
        ThemeArt(theme, Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.CenterStart).padding(horizontal = Spacing.xl)) {
            FText(theme.name, style = FidgetType.screenTitle.copy(fontSize = androidx.compose.ui.unit.TextUnit(19f, androidx.compose.ui.unit.TextUnitType.Sp)), color = Color(theme.text))
            FText(theme.blurb, Modifier.padding(top = Spacing.xs), FidgetType.caption, Color(theme.muted))
        }
        if (selected) Box(Modifier.align(Alignment.CenterEnd).padding(end = Spacing.xl).size(28.dp).clip(CircleShape)
            .background(Color(theme.active).copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
            FidgetIcon(FidgetIconKind.CHECK, Color(theme.bgTop), size = 18.dp)
        }
    }
}

/** A compact theme tile for the Settings row: landscape with a play mark on the active theme, name below. */
@Composable
fun ThemeTile(theme: Theme, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalFidget.current.accent
    val shape = RoundedCornerShape(Radii.r16)
    Column(modifier.width(76.dp).clickable(role = Role.RadioButton, onClick = onClick)
        .semantics { contentDescription = "${theme.name} theme. ${theme.blurb}"; this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp).clip(shape)
            .border(BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else Color(theme.text).copy(alpha = 0.1f)), shape)) {
            ThemeArt(theme, Modifier.fillMaxSize())
            if (selected) Box(Modifier.align(Alignment.Center).size(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center) { FidgetIcon(FidgetIconKind.PLAY, Color.White, size = 20.dp) }
        }
        FText(theme.name, Modifier.padding(top = Spacing.xs), FidgetType.label,
            if (selected) LocalFidget.current.textPrimary else LocalFidget.current.textSecondary)
    }
}
