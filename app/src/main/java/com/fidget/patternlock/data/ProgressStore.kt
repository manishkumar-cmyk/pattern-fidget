package com.fidget.patternlock.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fidget.patternlock.Prefs
import com.fidget.patternlock.domain.levels.Level
import com.fidget.patternlock.domain.levels.LevelPacks
import com.fidget.patternlock.domain.levels.World
import kotlin.math.max

/** Journey progress: best stars per level. Compose observes it, so the map and Home update as soon as a level ends. */
class ProgressStore(private val prefs: Prefs) {

    private var best by mutableStateOf(parse(prefs.levels))

    /** Best stars for a cleared level (0 if it was skipped), or null if it has not been cleared. */
    fun stars(level: Level): Int? = best[level.id]

    fun isCleared(level: Level) = level.id in best

    /** Levels open one after another. The first is always open. */
    fun isUnlocked(level: Level): Boolean {
        val i = LevelPacks.all.indexOf(level)
        return i <= 0 || LevelPacks.all[i - 1].id in best
    }

    fun isUnlocked(world: World) = isUnlocked(world.levels.first())

    /** Keeps the better result, so replaying a level never costs anything. */
    fun record(level: Level, stars: Int) {
        best = best + (level.id to max(best[level.id] ?: 0, stars))
        prefs.levels = best.entries.joinToString(",") { "${it.key}:${it.value}" }
    }

    val totalStars: Int get() = best.values.sum()
    val maxStars: Int get() = LevelPacks.all.size * 3

    fun starsIn(world: World): Int = world.levels.sumOf { best[it.id] ?: 0 }
    fun clearedIn(world: World): Int = world.levels.count { it.id in best }

    /** The first level not yet cleared, or null once the whole Journey is done. */
    fun nextLevel(): Level? = LevelPacks.all.firstOrNull { it.id !in best }

    val hasStarted: Boolean get() = best.isNotEmpty()

    companion object {
        fun parse(raw: String): Map<String, Int> = raw.split(',').mapNotNull { entry ->
            val parts = entry.split(':')
            val stars = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            if (parts[0].isBlank()) null else parts[0] to stars.coerceIn(0, 3)
        }.toMap()
    }
}
