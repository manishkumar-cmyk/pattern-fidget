package com.fidget.patternlock.domain.levels

import com.fidget.patternlock.domain.levels.LevelType.MIRROR
import com.fidget.patternlock.domain.levels.LevelType.PATH
import com.fidget.patternlock.domain.levels.LevelType.REVERSE
import com.fidget.patternlock.domain.levels.LevelType.SILHOUETTE
import com.fidget.patternlock.domain.levels.LevelType.TRACE

/**
 * The Journey: handcrafted levels in order. Dots are numbered row by row from the top left, so on 3×3
 *
 *     0 1 2
 *     3 4 5
 *     6 7 8
 */
object LevelPacks {

    private fun dusk(k: Int, name: String, type: LevelType, target: List<Int> = emptyList(), must: Set<Int> = emptySet(), avoid: Set<Int> = emptySet()) =
        Level(1, k, name, 3, type, target, must, avoid)

    private fun tide(k: Int, name: String, type: LevelType, target: List<Int> = emptyList(), must: Set<Int> = emptySet(), avoid: Set<Int> = emptySet()) =
        Level(2, k, name, 4, type, target, must, avoid)

    val worlds = listOf(
        World(1, "Dusk", "Nine dots and a quiet evening. Learn the ways to play.", 0, listOf(
            dusk(1, "First light", TRACE, listOf(0, 3, 6, 7, 8)),
            dusk(2, "Two stars", PATH, must = setOf(0, 8)),
            dusk(3, "Arch", SILHOUETTE, listOf(6, 3, 1, 5, 8)),
            dusk(4, "Turn back", REVERSE, listOf(0, 1, 4, 7, 8)),
            dusk(5, "Zigzag", TRACE, listOf(0, 1, 2, 4, 6, 7, 8)),
            dusk(6, "Around", PATH, must = setOf(1, 3, 5, 7), avoid = setOf(4)),
            dusk(7, "Reflection", MIRROR, listOf(0, 3, 4, 7)),
            dusk(8, "Serpent", SILHOUETTE, listOf(2, 1, 0, 3, 4, 5, 8, 7, 6)),
            dusk(9, "Corners", PATH, must = setOf(0, 2, 6, 8), avoid = setOf(4)),
            dusk(10, "Echo", REVERSE, listOf(3, 0, 1, 5, 8, 7)),
            dusk(11, "Bolt", TRACE, listOf(1, 3, 4, 5, 7)),
            dusk(12, "Twin wings", MIRROR, listOf(0, 4, 6, 7, 5)),
        )),
        World(2, "Tide", "Sixteen dots. Longer shapes, deeper water.", 3, listOf(
            tide(1, "Wave", TRACE, listOf(12, 8, 5, 10, 7, 3)),
            tide(2, "Tide pools", PATH, must = setOf(0, 3, 15), avoid = setOf(5, 10)),
            tide(3, "Cup", SILHOUETTE, listOf(0, 4, 8, 13, 14, 11, 7, 3)),
            tide(4, "Undertow", REVERSE, listOf(1, 5, 9, 14, 11, 7)),
            tide(5, "Spiral", TRACE, listOf(0, 1, 2, 3, 7, 11, 15, 14, 13, 12, 8, 4, 5, 6, 10, 9)),
            tide(6, "Reef", PATH, must = setOf(1, 2, 13, 14), avoid = setOf(5, 6, 9, 10)),
            tide(7, "Shell", MIRROR, listOf(0, 5, 9, 13, 14)),
            tide(8, "Saw", SILHOUETTE, listOf(0, 4, 1, 5, 2, 6, 3, 7)),
            tide(9, "Lighthouse", PATH, must = setOf(0, 3, 12, 15), avoid = setOf(6, 9)),
            tide(10, "Current", REVERSE, listOf(8, 12, 13, 9, 6, 2, 3)),
            tide(11, "Drift", TRACE, listOf(0, 1, 2, 3, 6, 9, 12, 13, 14, 15)),
            tide(12, "Moonrise", MIRROR, listOf(12, 9, 5, 2, 3, 7)),
        )),
    )

    val all: List<Level> = worlds.flatMap { it.levels }

    fun byId(id: String): Level? = all.firstOrNull { it.id == id }

    fun world(level: Level): World = worlds.first { it.number == level.world }

    /** The level after [level] in Journey order, or null at the end. */
    fun next(level: Level): Level? = all.getOrNull(all.indexOf(level) + 1)
}
