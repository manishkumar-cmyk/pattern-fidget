package com.fidget.patternlock.domain.levels

import com.fidget.patternlock.Patterns
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** How one attempt went. [note] is a gentle line for the player when it did not work out. */
data class Outcome(val solved: Boolean, val stars: Int, val note: String)

/**
 * The rules of one Journey level, with no UI in it. Nothing is ever lost: every attempt can be retried, and stars
 * only reward doing it neatly. Path levels give stars for using few dots; the rest for needing few attempts.
 */
class LevelEngine(val level: Level) {

    var attempts = 0
        private set
    /** Times the player asked to see the shape again. Counted like a miss for stars, nothing more. */
    var peeks = 0
        private set

    /** The fewest dots that solve a Path level. */
    val par: Int by lazy { if (level.type == LevelType.PATH) LevelRules.shortestPath(level.n, level.must, level.avoid) else 0 }

    /** What the player has to draw, for the types with one exact answer. */
    val answer: List<Int> get() = LevelRules.answer(level)

    /** After a few tries the player may move on without stars. */
    val canSkip: Boolean get() = attempts >= 3

    fun peek() { peeks++ }

    fun submit(drawn: List<Int>): Outcome {
        attempts++
        val note = LevelRules.problem(level, drawn)
        if (note != null) return Outcome(false, 0, note)
        val stars = when (level.type) {
            LevelType.PATH -> when {
                drawn.size <= par -> 3
                drawn.size <= par + 2 -> 2
                else -> 1
            }
            else -> {
                val slips = attempts - 1 + peeks
                when {
                    slips == 0 -> 3
                    slips <= 2 -> 2
                    else -> 1
                }
            }
        }
        return Outcome(true, stars, "")
    }
}

object LevelRules {

    /** Mirror a dot left to right. */
    fun mirror(n: Int, i: Int) = (i / n) * n + (n - 1 - i % n)

    fun answer(level: Level): List<Int> = when (level.type) {
        LevelType.REVERSE -> level.target.reversed()
        LevelType.MIRROR -> level.target.map { mirror(level.n, it) }
        else -> level.target
    }

    /** The undirected lines a pattern draws, so a shape counts whichever end it starts from. */
    fun edges(p: List<Int>): Set<Pair<Int, Int>> = (1 until p.size).map { min(p[it - 1], p[it]) to max(p[it - 1], p[it]) }.toSet()

    /** Null when [drawn] solves [level], otherwise a calm sentence about what to change. */
    fun problem(level: Level, drawn: List<Int>): String? = when (level.type) {
        LevelType.TRACE, LevelType.SILHOUETTE -> {
            val want = edges(level.target)
            val got = edges(drawn)
            when {
                got == want -> null
                !want.containsAll(got) -> if (level.type == LevelType.TRACE) "Stay on the outline." else "Not quite that shape. Have another look."
                else -> "Almost. Every line, in one stroke."
            }
        }
        LevelType.PATH -> {
            val hit = drawn.count { it in level.avoid }
            val missing = level.must.count { it !in drawn }
            when {
                hit > 0 -> "A crossed dot was touched. Find a way around it."
                missing == 1 -> "One gold dot is still waiting."
                missing > 1 -> "$missing gold dots are still waiting."
                else -> null
            }
        }
        LevelType.REVERSE -> when {
            drawn == answer(level) -> null
            drawn == level.target -> "That is the right shape, the right way round. Start from the other end."
            else -> "Not quite. Watch it once more if you like."
        }
        LevelType.MIRROR -> when {
            drawn == answer(level) -> null
            drawn == level.target -> "That is the shape itself. Draw its reflection."
            else -> "Not quite. Picture it flipped left to right."
        }
    }

    /**
     * The fewest dots in a drawable pattern that touches every [must] dot and no [avoid] dot, or -1 if there is none.
     * Only comfortable strokes count: to a neighbour, or straight along a row, column or diagonal (which picks up
     * the dots in between, as on a lock screen). Long knight-like jumps brush past other dots on a real finger, so a
     * par built on them would be unfair.
     */
    fun shortestPath(n: Int, must: Set<Int>, avoid: Set<Int>): Int {
        val total = n * n
        val goal = must.fold(0) { m, i -> m or (1 shl i) }
        // States grouped by how many dots they use, so the first goal reached is the shortest.
        val layers = Array(total + 1) { HashSet<Long>() }
        for (s in 0 until total) if (s !in avoid) layers[1].add(key(1 shl s, s))
        val seen = HashSet<Long>()
        for (k in 1..total) {
            for (st in layers[k]) {
                if (!seen.add(st)) continue
                val mask = (st shr 8).toInt()
                val last = (st and 0xFF).toInt()
                if (mask and goal == goal) return k
                for (j in 0 until total) {
                    if (mask and (1 shl j) != 0 || !comfortable(n, last, j)) continue
                    val added = Patterns.between(n, last, j).filter { mask and (1 shl it) == 0 } + j
                    if (added.any { it in avoid }) continue
                    val next = added.fold(mask) { m, i -> m or (1 shl i) }
                    if (k + added.size <= total) layers[k + added.size].add(key(next, j))
                }
            }
        }
        return -1
    }

    private fun key(mask: Int, last: Int) = (mask.toLong() shl 8) or last.toLong()

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    private fun comfortable(n: Int, a: Int, b: Int): Boolean {
        val dr = b / n - a / n
        val dc = b % n - a % n
        val g = gcd(abs(dr), abs(dc))
        return g > 0 && abs(dr / g) <= 1 && abs(dc / g) <= 1
    }
}
