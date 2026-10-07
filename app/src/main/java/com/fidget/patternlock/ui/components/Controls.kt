package com.fidget.patternlock.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Motion
import com.fidget.patternlock.ui.theme.Radii
import com.fidget.patternlock.ui.theme.Spacing

@Composable
fun FText(text: String, modifier: Modifier = Modifier, style: TextStyle = FidgetType.body, color: Color = LocalFidget.current.textPrimary,
          align: TextAlign? = null, maxLines: Int = Int.MAX_VALUE) {
    Text(text, modifier, color = color, style = style, textAlign = align, maxLines = maxLines)
}

/** A 48dp round touch target holding an icon. */
@Composable
fun FidgetIconButton(kind: FidgetIconKind, description: String, onClick: () -> Unit, modifier: Modifier = Modifier,
                     tint: Color = LocalFidget.current.textPrimary) {
    Box(modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = description, onClick = onClick)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        FidgetIcon(kind, tint)
    }
}

/** Back arrow, centred title, optional subtitle and trailing action. */
@Composable
fun FidgetTopBar(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, subtitle: String? = null,
                 trailing: @Composable (() -> Unit)? = null) {
    val c = LocalFidget.current
    Column(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Spacing.sm, vertical = Spacing.xs)) {
        Box(Modifier.fillMaxWidth().heightIn(min = 56.dp), contentAlignment = Alignment.Center) {
            if (onBack != null) Box(Modifier.align(Alignment.CenterStart)) { FidgetIconButton(FidgetIconKind.BACK, "Back", onBack) }
            FText(title, style = FidgetType.screenTitle, color = c.textPrimary)
            if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
        }
        if (subtitle != null) FText(subtitle, Modifier.fillMaxWidth().padding(bottom = Spacing.sm), FidgetType.caption, c.textSecondary, TextAlign.Center)
    }
}

/** A calm rounded button. [primary] is the solid accent, otherwise a quiet translucent pill. */
@Composable
fun PillButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false,
               icon: FidgetIconKind? = null) {
    val c = LocalFidget.current
    val shape = RoundedCornerShape(Radii.r24)
    Row(
        modifier.heightIn(min = 52.dp).clip(shape)
            .background(if (primary) c.accent.copy(alpha = 0.92f) else c.surface, shape)
            .border(BorderStroke(1.dp, if (primary) Color.Transparent else c.border), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.xxl, vertical = Spacing.md),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { FidgetIcon(icon, if (primary) c.onAccent else c.textPrimary, size = 20.dp); Box(Modifier.size(Spacing.sm)) }
        FText(label, style = FidgetType.bodyMedium, color = if (primary) c.onAccent else c.textPrimary)
    }
}

/** Rounded track with one softly glowing option. */
@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalFidget.current
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(Radii.r24)).background(c.surface)
        .border(BorderStroke(1.dp, c.border), RoundedCornerShape(Radii.r24)).padding(Spacing.xs)) {
        options.forEachIndexed { k, label ->
            val on = k == selected
            val bg by animateColorAsState(if (on) c.accentSurface else Color.Transparent, Motion.fast(), label = "seg")
            val fg by animateColorAsState(if (on) c.textPrimary else c.textSecondary, Motion.fast(), label = "segText")
            Box(
                Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(Radii.r20)).background(bg)
                    .then(if (on) Modifier.border(BorderStroke(1.dp, c.accent.copy(alpha = 0.55f)), RoundedCornerShape(Radii.r20)) else Modifier)
                    .selectable(on, role = Role.RadioButton, onClick = { onSelect(k) }),
                contentAlignment = Alignment.Center,
            ) { FText(label, style = FidgetType.bodyMedium, color = fg) }
        }
    }
}

/** Title, optional subtitle and a trailing control. The whole row toggles when [checked] is given. */
@Composable
fun SettingRow(title: String, modifier: Modifier = Modifier, subtitle: String? = null, onClick: (() -> Unit)? = null,
               trailing: @Composable (RowScope.() -> Unit)? = null) {
    val c = LocalFidget.current
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp).then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            FText(title, style = FidgetType.body, color = c.textPrimary)
            if (subtitle != null) FText(subtitle, style = FidgetType.caption, color = c.textSecondary)
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    val c = LocalFidget.current
    SettingRow(title, modifier.toggleable(checked, role = Role.Switch, onValueChange = onChange), subtitle) {
        Switch(
            checked = checked, onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent, checkedTrackColor = c.accent, checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = c.textSecondary, uncheckedTrackColor = c.surface, uncheckedBorderColor = c.border,
            ),
        )
    }
}

@Composable
fun FidgetSlider(value: Float, onChange: (Float) -> Unit, description: String, modifier: Modifier = Modifier,
                 onFinished: (() -> Unit)? = null) {
    val c = LocalFidget.current
    Slider(
        value, onChange, modifier.semantics { contentDescription = description }, onValueChangeFinished = onFinished,
        colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.inactiveDot.copy(alpha = 0.55f)),
    )
}
