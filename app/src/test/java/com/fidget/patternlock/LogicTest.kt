package com.fidget.patternlock

import com.fidget.patternlock.domain.Constellations
import com.fidget.patternlock.domain.MemoryGameEngine
import com.fidget.patternlock.domain.formatDotCount
import com.fidget.patternlock.domain.patternMeta
import java.util.Locale
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {

    @Test fun linesAutoIncludeTheDotsTheyCross() {
        assertEquals(listOf(1), Patterns.between(3, 0, 2))
        assertEquals(listOf(4), Patterns.between(3, 0, 8))
        assertEquals(emptyList<Int>(), Patterns.between(3, 0, 5))
        assertEquals(listOf(1, 2), Patterns.between(4, 0, 3))
    }

    @Test fun randomPatternsAreDrawableAndNeverStraight() {
        val rnd = Random(42)
        repeat(100) {
            val p = Patterns.random(3, 5, rnd)
            assertEquals(5, p.size)
            assertEquals(p.size, p.toSet().size)
            for (k in 1 until p.size) {
                // never jumps over a dot that is not already used
                assertTrue(Patterns.between(3, p[k - 1], p[k]).all { it in p.subList(0, k) })
            }
            assertFalse(Shapes.detect(3, p) == Shape.STRAIGHT)
        }
    }

    @Test fun shapesAreRecognised() {
        assertEquals(Shape.ALL_DOTS, Shapes.detect(3, listOf(0, 1, 2, 5, 4, 3, 6, 7, 8)))
        assertEquals(Shape.STRAIGHT, Shapes.detect(3, listOf(0, 1, 2)))
        assertEquals(Shape.LOOP, Shapes.detect(3, listOf(0, 1, 4, 3)))
    }

    @Test fun memoryLevelsGrowByOneDot() {
        val e = MemoryGameEngine(3, Random(1))
        assertEquals(1, e.level)
        assertEquals(3, e.newRound().size)
        assertTrue(e.matches(e.target))
        assertFalse(e.matches(e.target.reversed().takeIf { it != e.target } ?: listOf(99)))
        e.advance()
        assertEquals(2, e.level)
        assertEquals(4, e.newRound().size)
        e.reset()
        assertEquals(1, e.level)
    }

    @Test fun memoryNeverAsksForMoreThanTheGrid() {
        val e = MemoryGameEngine(3, Random(2))
        repeat(12) { e.newRound(); e.advance() }
        assertTrue(e.newRound().size <= 9)
    }

    @Test fun dotCountIsGrouped() {
        assertEquals("12,458", formatDotCount(12458, Locale.US))
        assertEquals("0", formatDotCount(-3, Locale.US))
        assertEquals("3×3 • 6 dots", patternMeta(3, 6))
        assertEquals("4×4 • 1 dot", patternMeta(4, 1))
    }

    @Test fun everyThemeHasUsableColours() {
        for (t in Themes.all) {
            assertTrue(t.name, t.blurb.isNotBlank())
            assertTrue(t.name, (t.error ushr 24) == 0xFF)
        }
        assertEquals(listOf("Dusk", "Fog", "Sage", "Tide", "Ink", "Ember"), Themes.all.map { it.name })
        assertEquals("Ember", Themes.all.last().name)
    }

    @Test fun marimbaWasAppendedWithoutShiftingSavedChoices() {
        assertEquals(0, SoundEnv.CHIME.ordinal)
        assertEquals(8, SoundEnv.BUBBLES.ordinal)
        assertEquals(9, SoundEnv.MARIMBA.ordinal)
    }

    @Test fun constellationsAreWellFormed() {
        assertEquals(18, Constellations.all.size)
        assertEquals(18, Constellations.all.map { it.id }.toSet().size)
        for (c in Constellations.all) {
            assertTrue(c.name, c.description.length > 40 && c.keyword.isNotBlank())
            assertTrue(c.name, c.edges.all { (a, b) -> a != b && a in c.stars.indices && b in c.stars.indices })
            // every star is part of the drawing, and no line is listed twice
            assertEquals(c.name, c.stars.indices.toSet(), c.edges.flatMap { listOf(it.first, it.second) }.toSet())
            assertEquals(c.name, c.edges.size, c.edges.map { setOf(it.first, it.second) }.toSet().size)
        }
    }

    @Test fun constellationEdgesAreFoundInEitherDirection() {
        val orion = Constellations.byId("orion")!!
        assertTrue(orion.edgeBetween(0, 1) >= 0)
        assertEquals(orion.edgeBetween(0, 1), orion.edgeBetween(1, 0))
        assertEquals(-1, orion.edgeBetween(0, 5))
    }

    @Test fun nextConstellationSkipsTracedOnes() {
        val all = Constellations.all
        val traced = setOf(all[1].id, all[2].id)
        assertEquals(all[3].id, Constellations.next(all[0].id, traced).id)
        assertEquals(all[1].id, Constellations.next(all[0].id, all.map { it.id }.toSet()).id)
    }
}
