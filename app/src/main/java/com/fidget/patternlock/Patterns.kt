package com.fidget.patternlock

import android.content.SharedPreferences
import kotlin.math.abs
import kotlin.random.Random

data class SavedPattern(val n: Int, val dots: List<Int>)

object Patterns {

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    /** Grid points a straight line from a to b passes over (a lock screen auto-adds these). */
    fun between(n: Int, a: Int, b: Int): List<Int> {
        val r1 = a / n; val c1 = a % n
        val dr = b / n - r1; val dc = b % n - c1
        val g = gcd(abs(dr), abs(dc))
        return (1 until g).map { k -> (r1 + dr / g * k) * n + (c1 + dc / g * k) }
    }

    /** A random pattern a person could actually draw: never jumps over an unused dot. */
    fun random(n: Int, length: Int, rnd: Random = Random.Default): List<Int> {
        val total = n * n
        val target = length.coerceIn(2, total)
        var best = emptyList<Int>()
        repeat(40) {
            val used = BooleanArray(total)
            val sel = ArrayList<Int>()
            val start = rnd.nextInt(total)
            sel.add(start); used[start] = true
            while (sel.size < target) {
                val last = sel.last()
                val options = (0 until total).filter { j -> !used[j] && between(n, last, j).all { used[it] } }
                if (options.isEmpty()) break
                val next = options[rnd.nextInt(options.size)]
                sel.add(next); used[next] = true
            }
            if (sel.size == target) return sel
            if (sel.size > best.size) best = sel
        }
        return best
    }
}

/** Saved patterns live in SharedPreferences as "3:0,1,2|4:5,6,7", newest first. */
class PatternStore(private val prefs: SharedPreferences) {

    fun load(): MutableList<SavedPattern> {
        val raw = prefs.getString("saved", "") ?: ""
        if (raw.isBlank()) return mutableListOf()
        return raw.split("|").mapNotNull { entry ->
            val parts = entry.split(":")
            val n = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val dots = parts.getOrNull(1)?.split(",")?.mapNotNull { it.toIntOrNull() } ?: return@mapNotNull null
            if (n !in 3..5 || dots.size < 2 || dots.any { it !in 0 until n * n }) null else SavedPattern(n, dots)
        }.toMutableList()
    }

    private fun write(list: List<SavedPattern>) {
        prefs.edit().putString("saved", list.joinToString("|") { "${it.n}:${it.dots.joinToString(",")}" }).apply()
    }

    fun add(p: SavedPattern) {
        val list = load()
        if (list.firstOrNull() == p) return
        list.add(0, p)
        write(list.take(60))
    }

    fun removeAt(index: Int) {
        val list = load()
        if (index in list.indices) { list.removeAt(index); write(list) }
    }
}
