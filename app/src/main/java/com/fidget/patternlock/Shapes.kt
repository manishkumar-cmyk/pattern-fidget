package com.fidget.patternlock

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max

enum class Shape(val suggestion: String) {
    ALL_DOTS("Everything"), LOOP("Orbit"), STRAIGHT("Line"), SPIRAL("Spiral"),
    SYMMETRIC("Mirror"), ZIGZAG("Wave"), OTHER("Drift")
}

/** Reads a pattern's geometry so each kind of shape can finish with its own animation. */
object Shapes {

    fun detect(n: Int, dots: List<Int>): Shape {
        if (dots.size < 2) return Shape.OTHER
        if (dots.size == n * n) return Shape.ALL_DOTS
        val xs = dots.map { it % n }
        val ys = dots.map { it / n }
        if (collinear(xs, ys)) return Shape.STRAIGHT
        if (dots.size >= 4 && max(abs(xs.first() - xs.last()), abs(ys.first() - ys.last())) <= 1) return Shape.LOOP

        val turns = ArrayList<Double>()
        for (k in 1 until dots.size - 1) {
            val a1 = atan2((ys[k] - ys[k - 1]).toDouble(), (xs[k] - xs[k - 1]).toDouble())
            val a2 = atan2((ys[k + 1] - ys[k]).toDouble(), (xs[k + 1] - xs[k]).toDouble())
            var d = a2 - a1
            while (d > PI) d -= 2 * PI
            while (d < -PI) d += 2 * PI
            if (abs(d) > 1e-6) turns.add(d)
        }
        if (dots.size >= 5 && turns.isNotEmpty() && (turns.all { it > 0 } || turns.all { it < 0 }) &&
            abs(turns.sum()) >= 1.5 * PI) return Shape.SPIRAL
        if (symmetric(n, dots)) return Shape.SYMMETRIC
        var flips = 0
        for (k in 1 until turns.size) if (turns[k] * turns[k - 1] < 0) flips++
        if (dots.size >= 4 && flips >= 2) return Shape.ZIGZAG
        return Shape.OTHER
    }

    private fun collinear(xs: List<Int>, ys: List<Int>): Boolean {
        for (k in 2 until xs.size) {
            val cross = (xs[1] - xs[0]) * (ys[k] - ys[0]) - (ys[1] - ys[0]) * (xs[k] - xs[0])
            if (cross != 0) return false
        }
        return true
    }

    private fun symmetric(n: Int, dots: List<Int>): Boolean {
        if (dots.size < 3) return false
        val set = dots.toSet()
        fun test(f: (Int, Int) -> Int) = set.all { f(it % n, it / n) in set }
        return test { c, r -> r * n + (n - 1 - c) } ||
            test { c, r -> (n - 1 - r) * n + c } ||
            test { c, r -> (n - 1 - r) * n + (n - 1 - c) }
    }
}
