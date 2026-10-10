package com.fidget.patternlock

import com.fidget.patternlock.domain.levels.Level
import com.fidget.patternlock.domain.levels.LevelEngine
import com.fidget.patternlock.domain.levels.LevelPacks
import com.fidget.patternlock.domain.levels.LevelRules
import com.fidget.patternlock.domain.levels.LevelType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelTest {

    /** A pattern a person can draw: no repeats, and every dot a line jumps over is already in it. */
    private fun drawable(n: Int, p: List<Int>) =
        p.size == p.toSet().size && p.all { it in 0 until n * n } &&
            (1 until p.size).all { k -> Patterns.between(n, p[k - 1], p[k]).all { it in p.subList(0, k) } }

    @Test fun everyLevelIsWellFormedAndSolvable() {
        assertEquals(LevelPacks.all.size, LevelPacks.all.map { it.id }.toSet().size)
        for (level in LevelPacks.all) {
            if (level.type == LevelType.PATH) {
                assertTrue(level.id, level.must.isNotEmpty() && level.must.none { it in level.avoid })
                assertTrue(level.id, LevelEngine(level).par > 0)
            } else {
                assertTrue(level.id, level.target.size >= 3 && drawable(level.n, level.target))
                assertTrue(level.id, drawable(level.n, LevelRules.answer(level)))
                assertTrue(level.id, LevelRules.problem(level, LevelRules.answer(level)) == null)
            }
        }
    }

    @Test fun traceAcceptsEitherDirectionAndExplainsMisses() {
        val level = Level(1, 1, "L", 3, LevelType.TRACE, listOf(0, 3, 6, 7, 8))
        assertEquals(null, LevelRules.problem(level, listOf(8, 7, 6, 3, 0)))
        assertEquals("Almost. Every line, in one stroke.", LevelRules.problem(level, listOf(0, 3, 6)))
        assertEquals("Stay on the outline.", LevelRules.problem(level, listOf(0, 4, 8)))
    }

    @Test fun pathNeedsEveryGoldDotAndNoCrossedOne() {
        val level = Level(1, 1, "P", 3, LevelType.PATH, must = setOf(1, 3, 5, 7), avoid = setOf(4))
        assertEquals(4, LevelEngine(level).par)
        assertTrue(LevelRules.problem(level, listOf(1, 4, 7, 3, 5))!!.contains("crossed"))
        assertTrue(LevelRules.problem(level, listOf(1, 3, 7))!!.contains("One gold dot"))
        assertEquals(null, LevelRules.problem(level, listOf(1, 3, 7, 5)))
    }

    @Test fun parIgnoresAwkwardKnightJumps() {
        // Around the centre on 4×4: the knight-jump shortcut 2 → 13 would brush the avoided centre.
        assertEquals(6, LevelRules.shortestPath(4, setOf(1, 2, 13, 14), setOf(5, 6, 9, 10)))
        assertEquals(3, LevelRules.shortestPath(3, setOf(0, 8), emptySet()))
        assertEquals(-1, LevelRules.shortestPath(3, setOf(0, 8), setOf(1, 3, 4)))
    }

    @Test fun reverseAndMirrorWantTheTransformedShape() {
        val reverse = Level(1, 1, "R", 3, LevelType.REVERSE, listOf(0, 1, 4, 7, 8))
        assertEquals(listOf(8, 7, 4, 1, 0), LevelRules.answer(reverse))
        assertTrue(LevelRules.problem(reverse, reverse.target)!!.contains("other end"))
        val mirror = Level(1, 1, "M", 3, LevelType.MIRROR, listOf(0, 3, 4, 7))
        assertEquals(listOf(2, 5, 4, 7), LevelRules.answer(mirror))
    }

    @Test fun starsRewardNeatnessNeverPunish() {
        val path = Level(1, 1, "P", 3, LevelType.PATH, must = setOf(0, 8))
        val engine = LevelEngine(path)
        assertFalse(engine.submit(listOf(0, 1)).solved)
        assertEquals(3, engine.submit(listOf(0, 4, 8)).stars)
        assertEquals(2, LevelEngine(path).submit(listOf(0, 1, 4, 8)).stars)

        val silhouette = Level(1, 1, "S", 3, LevelType.SILHOUETTE, listOf(6, 3, 1, 5, 8))
        assertEquals(3, LevelEngine(silhouette).submit(silhouette.target).stars)
        val slow = LevelEngine(silhouette)
        slow.submit(listOf(0, 1)); slow.peek()
        assertEquals(2, slow.submit(silhouette.target).stars)
        val slower = LevelEngine(silhouette)
        repeat(3) { slower.submit(listOf(0, 1)) }
        assertTrue(slower.canSkip)
        assertEquals(1, slower.submit(silhouette.target).stars)
    }
}
