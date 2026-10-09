package com.fidget.patternlock.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fidget.patternlock.Mode
import com.fidget.patternlock.data.FidgetEnv
import com.fidget.patternlock.ui.collection.CollectionScreen
import com.fidget.patternlock.ui.collection.PlaybackScreen
import com.fidget.patternlock.ui.components.FidgetBackground
import com.fidget.patternlock.ui.constellation.ConstellationListScreen
import com.fidget.patternlock.ui.constellation.ConstellationPlayScreen
import com.fidget.patternlock.ui.components.NavTab
import com.fidget.patternlock.ui.draw.DrawModesScreen
import com.fidget.patternlock.ui.draw.DrawScreen
import com.fidget.patternlock.ui.draw.ZenScreen
import com.fidget.patternlock.ui.home.HomeScreen
import com.fidget.patternlock.ui.memory.MemoryScreen
import com.fidget.patternlock.ui.settings.SettingsScreen
import com.fidget.patternlock.ui.theme.FidgetTheme
import com.fidget.patternlock.ui.theme.Motion
import com.fidget.patternlock.ui.themes.ThemesScreen

object Routes {
    const val HOME = "home"
    const val DRAW = "draw"
    const val DRAW_MODE = "draw/{mode}"
    const val ZEN = "zen"
    const val MEMORY = "memory"
    const val COLLECTION = "collection"
    const val PLAYBACK = "playback/{id}/{loop}"
    const val SETTINGS = "settings"
    const val THEMES = "themes"
    const val CONSTELLATIONS = "constellations"
    const val CONSTELLATION = "constellation/{id}"
}

/** The values that decide how the sound and haptic engines are configured. A change re-applies them. */
private data class SensesKey(
    val theme: Int, val amoled: Boolean, val contrast: Boolean, val pack: Int, val soundOn: Boolean, val volume: Float,
    val directional: Boolean, val hapticsOn: Boolean, val hapticLevel: Int, val musicOn: Boolean, val musicVolume: Float,
)

@Composable
fun FidgetApp(env: FidgetEnv) {
    val s = env.settings
    val key = SensesKey(s.themeIndex, s.amoled, s.highContrast, s.soundEnv, s.soundOn, s.volume, s.directional, s.hapticsOn, s.hapticLevel, s.musicOn, s.musicVolume)
    LaunchedEffect(key) { env.applySenses() }
    FidgetTheme(s.theme) {
        FidgetBackground { AppNav() }
    }
}

@Composable
private fun AppNav() {
    val nav = rememberNavController()
    fun toTab(tab: NavTab) {
        when (tab) {
            NavTab.DRAW -> nav.navigate(Routes.DRAW) { popUpTo(Routes.HOME) }
            NavTab.MEMORY -> nav.navigate(Routes.MEMORY) { popUpTo(Routes.HOME) }
            NavTab.COLLECTION -> nav.navigate(Routes.COLLECTION) { popUpTo(Routes.HOME) }
        }
    }
    NavHost(
        nav, startDestination = Routes.HOME,
        enterTransition = { fadeIn(tween(Motion.Normal, easing = Motion.Easing)) },
        exitTransition = { fadeOut(tween(Motion.Fast, easing = Motion.Easing)) },
        popEnterTransition = { fadeIn(tween(Motion.Normal, easing = Motion.Easing)) },
        popExitTransition = { fadeOut(tween(Motion.Fast, easing = Motion.Easing)) },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onTab = ::toTab, onSettings = { nav.navigate(Routes.SETTINGS) },
                onStart = { nav.navigate("draw/${Mode.FREE.name}") },
                onConstellation = { nav.navigate("constellation/$it") },
            )
        }
        composable(Routes.DRAW) {
            DrawModesScreen(
                onBack = { nav.popBackStack() },
                onConstellations = { nav.navigate(Routes.CONSTELLATIONS) },
                onMode = { m ->
                    if (m == Mode.ZEN) nav.navigate(Routes.ZEN) else nav.navigate("draw/${m.name}")
                },
            )
        }
        composable(Routes.DRAW_MODE, listOf(navArgument("mode") { type = NavType.StringType })) { entry ->
            val mode = Mode.values().firstOrNull { it.name == entry.arguments?.getString("mode") } ?: Mode.FREE
            DrawScreen(mode, onBack = { nav.popBackStack() })
        }
        composable(Routes.ZEN) { ZenScreen(onExit = { nav.popBackStack() }) }
        composable(Routes.MEMORY) { MemoryScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.COLLECTION) {
            CollectionScreen(
                onTab = ::toTab,
                onOpen = { id, loop -> nav.navigate("playback/$id/$loop") },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            Routes.PLAYBACK,
            listOf(navArgument("id") { type = NavType.LongType }, navArgument("loop") { type = NavType.BoolType }),
        ) { entry ->
            PlaybackScreen(
                id = entry.arguments?.getLong("id") ?: 0L,
                startLooping = entry.arguments?.getBoolean("loop") ?: false,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() }, onSeeThemes = { nav.navigate(Routes.THEMES) })
        }
        composable(Routes.CONSTELLATIONS) {
            ConstellationListScreen(onBack = { nav.popBackStack() }, onOpen = { nav.navigate("constellation/$it") })
        }
        composable(Routes.CONSTELLATION, listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            ConstellationPlayScreen(
                id = entry.arguments?.getString("id") ?: "",
                onBack = { nav.popBackStack() },
                onOpen = { next -> nav.navigate("constellation/$next") { popUpTo(Routes.CONSTELLATIONS) } },
            )
        }
        composable(Routes.THEMES) { ThemesScreen(onBack = { nav.popBackStack() }) }
    }
}
