package com.fidget.patternlock.domain

import com.fidget.patternlock.Patterns
import kotlin.math.min
import kotlin.random.Random

/**
 * The rules of Memory, with no UI in it. A pattern is shown, the player draws it back, and a match makes the
 * next pattern one dot longer. There are no timers, lives or scores.
 */
class MemoryGameEngine(private val n: Int, private val random: Random = Random.Default) {

    val startLength = if (n == 3) 3 else 4
    private var length = startLength

    var target: List<Int> = emptyList()
        private set

    /** 1 for the first pattern, 2 for the next, and so on. */
    val level: Int get() = length - startLength + 1

    fun newRound(): List<Int> {
        target = Patterns.random(n, min(length, n * n), random)
        return target
    }

    fun matches(drawn: List<Int>) = drawn == target

    /** Call after a match: the next pattern is one dot longer, up to the whole grid. */
    fun advance() { length = min(target.size + 1, n * n) }

    fun reset() { length = startLength }
}
