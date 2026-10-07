package com.fidget.patternlock

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.random.Random

data class SavedPattern(
    val id: Long,
    val n: Int,
    val dots: List<Int>,
    val created: Long,
    val name: String = "",
    val favorite: Boolean = false,
)

object Patterns {

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    /** Grid points a straight line from a to b passes over (a lock screen auto-adds these). */
    fun between(n: Int, a: Int, b: Int): List<Int> {
        val r1 = a / n; val c1 = a % n
        val dr = b / n - r1; val dc = b % n - c1
        val g = gcd(abs(dr), abs(dc))
        return (1 until g).map { k -> (r1 + dr / g * k) * n + (c1 + dc / g * k) }
    }

    /** A random pattern a person could actually draw: never jumps over an unused dot, never one straight line. */
    fun random(n: Int, length: Int, rnd: Random = Random.Default): List<Int> {
        val total = n * n
        val target = length.coerceIn(2, total)
        var best = emptyList<Int>()
        repeat(60) {
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
            if (sel.size == target && (target < 3 || Shapes.detect(n, sel) != Shape.STRAIGHT)) return sel
            if (sel.size > best.size) best = sel
        }
        return best
    }
}

/** Saved patterns, newest first, stored as JSON. Reads the older "3:0,1,2|4:..." format once and converts it. */
class PatternStore(private val prefs: SharedPreferences) {

    fun load(): MutableList<SavedPattern> {
        val json = prefs.getString("collection", null)
        if (json == null) return migrate()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { k ->
                val o = arr.getJSONObject(k)
                val n = o.getInt("n")
                val d = o.getJSONArray("d")
                val dots = (0 until d.length()).map { d.getInt(it) }
                if (n !in 3..5 || dots.size < 2 || dots.any { it !in 0 until n * n }) null
                else SavedPattern(o.getLong("id"), n, dots, o.optLong("t", 0L), o.optString("name", ""), o.optBoolean("fav", false))
            }.toMutableList()
        } catch (e: Exception) { mutableListOf() }
    }

    private fun migrate(): MutableList<SavedPattern> {
        val raw = prefs.getString("saved", "") ?: ""
        val now = System.currentTimeMillis()
        val list = if (raw.isBlank()) mutableListOf() else raw.split("|").mapIndexedNotNull { k, entry ->
            val parts = entry.split(":")
            val n = parts.getOrNull(0)?.toIntOrNull() ?: return@mapIndexedNotNull null
            val dots = parts.getOrNull(1)?.split(",")?.mapNotNull { it.toIntOrNull() } ?: return@mapIndexedNotNull null
            if (n !in 3..5 || dots.size < 2 || dots.any { it !in 0 until n * n }) null
            else SavedPattern(now - k, n, dots, now - k * 60_000L)
        }.toMutableList()
        write(list)
        return list
    }

    private fun write(list: List<SavedPattern>) {
        val arr = JSONArray()
        for (p in list.take(200)) arr.put(JSONObject().apply {
            put("id", p.id); put("n", p.n); put("d", JSONArray(p.dots)); put("t", p.created)
            if (p.name.isNotBlank()) put("name", p.name)
            if (p.favorite) put("fav", true)
        })
        prefs.edit().putString("collection", arr.toString()).apply()
    }

    fun add(n: Int, dots: List<Int>): SavedPattern? {
        val list = load()
        val first = list.firstOrNull()
        if (first != null && first.n == n && first.dots == dots) return null
        val now = System.currentTimeMillis()
        val p = SavedPattern(now, n, dots, now)
        list.add(0, p)
        write(list)
        return p
    }

    fun get(id: Long) = load().firstOrNull { it.id == id }

    fun update(p: SavedPattern) {
        val list = load()
        val i = list.indexOfFirst { it.id == p.id }
        if (i >= 0) { list[i] = p; write(list) }
    }

    fun remove(id: Long) {
        val list = load()
        if (list.removeAll { it.id == id }) write(list)
    }
}
