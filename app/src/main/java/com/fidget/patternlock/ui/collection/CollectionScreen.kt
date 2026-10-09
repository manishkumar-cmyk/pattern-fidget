package com.fidget.patternlock.ui.collection

import com.fidget.patternlock.ui.theme.topInsets
import com.fidget.patternlock.ui.theme.bottomInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.fidget.patternlock.SavedPattern
import com.fidget.patternlock.Shapes
import com.fidget.patternlock.Themes
import com.fidget.patternlock.domain.patternMeta
import com.fidget.patternlock.ui.LocalEnv
import com.fidget.patternlock.ui.components.FText
import com.fidget.patternlock.ui.components.FidgetBottomBar
import com.fidget.patternlock.ui.components.FidgetIcon
import com.fidget.patternlock.ui.components.FidgetIconButton
import com.fidget.patternlock.ui.components.FidgetIconKind
import com.fidget.patternlock.ui.components.GlowCard
import com.fidget.patternlock.ui.components.NavTab
import com.fidget.patternlock.ui.components.PatternThumb
import com.fidget.patternlock.ui.components.PillButton
import com.fidget.patternlock.ui.theme.FidgetType
import com.fidget.patternlock.ui.theme.LocalFidget
import com.fidget.patternlock.ui.theme.Radii
import com.fidget.patternlock.ui.theme.Spacing
import java.text.DateFormat
import java.util.Date

internal fun patternTitle(p: SavedPattern) =
    p.name.ifBlank { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(p.created)) }

private val filters = listOf("All", "Favourites", "3×3", "4×4", "5×5")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(onTab: (NavTab) -> Unit, onOpen: (id: Long, loop: Boolean) -> Unit, onSettings: () -> Unit) {
    val env = LocalEnv.current
    val c = LocalFidget.current
    var all by remember { mutableStateOf(env.store.load()) }
    var filter by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf<String?>(null) }
    var filterMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf<SavedPattern?>(null) }
    var renaming by remember { mutableStateOf<SavedPattern?>(null) }
    fun refresh() { all = env.store.load() }

    val items = all.filter {
        when (filter) { 1 -> it.favorite; 2 -> it.n == 3; 3 -> it.n == 4; 4 -> it.n == 5; else -> true } &&
            (query.isNullOrBlank() || patternTitle(it).contains(query!!, ignoreCase = true))
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().topInsets().padding(start = Spacing.xxl, end = Spacing.sm, top = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically) {
            FText("Collection", Modifier.weight(1f), FidgetType.title.copy(fontSize = 26.sp))
            FidgetIconButton(FidgetIconKind.SEARCH, "Search", { query = if (query == null) "" else null }, tint = c.textSecondary)
            Box {
                FidgetIconButton(FidgetIconKind.TUNE, "Filter", { filterMenu = true }, tint = if (filter != 0) c.accent else c.textSecondary)
                DropdownMenu(filterMenu, { filterMenu = false }) {
                    filters.forEachIndexed { k, label ->
                        DropdownMenuItem(text = { FText(label, color = if (k == filter) c.accent else c.textPrimary) },
                            onClick = { filter = k; filterMenu = false })
                    }
                }
            }
            Box {
                FidgetIconButton(FidgetIconKind.MORE, "More", { moreMenu = true }, tint = c.textSecondary)
                DropdownMenu(moreMenu, { moreMenu = false }) {
                    DropdownMenuItem(text = { FText("Settings") }, onClick = { moreMenu = false; onSettings() })
                }
            }
        }
        query?.let { q ->
            TextField(
                q, { query = it }, Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.sm),
                placeholder = { FText("Search by name", color = c.textSecondary) }, singleLine = true,
                shape = RoundedCornerShape(Radii.r16),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = c.textPrimary, unfocusedTextColor = c.textPrimary, cursorColor = c.accent),
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (items.isEmpty()) {
                FText(if (all.isEmpty()) "Patterns you save will rest here." else "Nothing here yet.",
                    Modifier.align(Alignment.Center).padding(Spacing.xxxl), FidgetType.body, c.textSecondary, TextAlign.Center)
            } else {
                LazyVerticalGrid(
                    GridCells.Fixed(2), Modifier.fillMaxSize().widthIn(max = 640.dp).align(Alignment.TopCenter),
                    contentPadding = PaddingValues(start = Spacing.xl, end = Spacing.xl, top = Spacing.sm, bottom = Spacing.xl),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    itemsIndexed(items, key = { _, p -> p.id }) { i, p ->
                        PatternCard(
                            p, offsetMs = i * 450L,
                            onOpen = { onOpen(p.id, false) },
                            onFavorite = { env.store.update(p.copy(favorite = !p.favorite)); refresh() },
                            onOptions = { options = p },
                        )
                    }
                }
            }
        }
        FidgetBottomBar(NavTab.COLLECTION, onSelect = { if (it != NavTab.COLLECTION) onTab(it) })
    }

    options?.let { p ->
        PatternOptionsSheet(
            p, onDismiss = { options = null },
            onLoop = { options = null; onOpen(p.id, true) },
            onFavorite = { env.store.update(p.copy(favorite = !p.favorite)); options = null; refresh() },
            onRename = { options = null; renaming = p },
            onDelete = {
                env.store.remove(p.id)
                if (env.settings.homePattern == p.id) env.settings.homePattern = 0L
                options = null; refresh()
            },
        )
    }
    renaming?.let { p ->
        RenameDialog(p, onDismiss = { renaming = null }, onSave = { name ->
            env.store.update(p.copy(name = name.trim())); renaming = null; refresh()
        })
    }
}

@Composable
private fun PatternCard(p: SavedPattern, offsetMs: Long, onOpen: () -> Unit, onFavorite: () -> Unit, onOptions: () -> Unit) {
    val c = LocalFidget.current
    val title = patternTitle(p)
    GlowCard(Modifier.fillMaxWidth(), radius = Radii.r24, onClick = onOpen, onLongClick = onOptions,
        description = "$title, ${patternMeta(p.n, p.dots.size)}${if (p.favorite) ", favourite" else ""}. Double tap to play, long press for options.") {
        Column(Modifier.padding(Spacing.md)) {
            Box(Modifier.fillMaxWidth().height(124.dp)) {
                PatternThumb(p.n, p.dots, Modifier.fillMaxSize(), offsetMs = offsetMs, sizeFraction = 0.82f)
                Row(Modifier.align(Alignment.TopEnd)) {
                    SmallAction(if (p.favorite) FidgetIconKind.HEART else FidgetIconKind.HEART_OUTLINE,
                        if (p.favorite) "Remove from favourites" else "Add to favourites",
                        if (p.favorite) c.error else c.textSecondary, onFavorite)
                    SmallAction(FidgetIconKind.MORE, "Options for $title", c.textSecondary, onOptions)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    FText(title, style = FidgetType.bodyMedium, maxLines = 1)
                    FText(patternMeta(p.n, p.dots.size), style = FidgetType.caption, color = c.textSecondary)
                }
                Box(Modifier.size(40.dp).clip(CircleShape).background(c.elevatedSurface).clickable(role = Role.Button, onClick = onOpen),
                    contentAlignment = Alignment.Center) {
                    FidgetIcon(FidgetIconKind.PLAY, c.textPrimary, size = 20.dp)
                }
            }
        }
    }
}

@Composable
private fun SmallAction(icon: FidgetIconKind, description: String, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center) { FidgetIcon(icon, tint, size = 20.dp) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatternOptionsSheet(
    p: SavedPattern, onDismiss: () -> Unit, onLoop: () -> Unit, onFavorite: () -> Unit, onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val env = LocalEnv.current
    val c = LocalFidget.current
    var armed by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(Themes.mix(c.theme.bgTop, c.theme.surface, 0.8f)),
    ) {
        Column(Modifier.padding(horizontal = Spacing.xl).padding(bottom = Spacing.xxl).bottomInsets()) {
            FText(patternTitle(p), Modifier.padding(bottom = Spacing.sm), FidgetType.screenTitle)
            OptionRow("Play on loop", null, onLoop)
            OptionRow(if (p.favorite) "Remove from favourites" else "Add to favourites", null, onFavorite)
            OptionRow("Rename", "Up to 24 characters", onRename)
            OptionRow(if (armed) "Tap again to delete" else "Delete", null, { if (armed) onDelete() else armed = true },
                color = if (armed) c.error else c.textPrimary)
        }
    }
}

@Composable
private fun OptionRow(title: String, subtitle: String?, onClick: () -> Unit, color: Color = LocalFidget.current.textPrimary) {
    Column(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick).padding(vertical = Spacing.md, horizontal = Spacing.xs),
        verticalArrangement = Arrangement.Center) {
        FText(title, style = FidgetType.body, color = color)
        if (subtitle != null) FText(subtitle, style = FidgetType.caption, color = LocalFidget.current.textSecondary)
    }
}

@Composable
private fun RenameDialog(p: SavedPattern, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val c = LocalFidget.current
    var text by remember { mutableStateOf(p.name) }
    val suggestions = (listOf(Shapes.detect(p.n, p.dots).suggestion) + listOf("Wave", "Orbit", "Drift", "Spiral")).distinct().take(4)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(Themes.mix(c.theme.bgTop, c.theme.surface, 0.8f)),
        title = { FText("Name", style = FidgetType.screenTitle) },
        text = {
            Column {
                TextField(
                    text, { if (it.length <= 24) text = it }, Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { FText("Name this pattern", color = c.textSecondary) },
                    shape = RoundedCornerShape(Radii.r16),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = c.surface, unfocusedContainerColor = c.surface,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = c.textPrimary, unfocusedTextColor = c.textPrimary, cursorColor = c.accent),
                )
                Row(Modifier.padding(top = Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    suggestions.forEach { s ->
                        Box(Modifier.clip(RoundedCornerShape(Radii.r20)).background(c.surface).clickable { text = s }
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm)) { FText(s, style = FidgetType.caption) }
                    }
                }
            }
        },
        confirmButton = { PillButton("Save", { onSave(text) }, primary = true) },
        dismissButton = { PillButton("Cancel", onDismiss) },
    )
}
