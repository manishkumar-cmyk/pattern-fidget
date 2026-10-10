package com.fidget.patternlock.domain.levels

/** The kinds of Journey puzzle. Each one asks for something different from the same grid. */
enum class LevelType(val label: String, val goal: String) {
    TRACE("Trace", "Trace the outline in one stroke"),
    PATH("Path", "Touch every gold dot. Never the crossed ones."),
    SILHOUETTE("Silhouette", "Remember the shape, then draw it"),
    REVERSE("Reverse", "Watch, then draw it backwards"),
    MIRROR("Mirror", "Watch, then draw its mirror image"),
}

/**
 * One handcrafted Journey level. [target] is the shape for every type except [LevelType.PATH], which uses
 * [must] and [avoid] instead. Targets are written so a person can draw them: no dot is jumped over unless it is
 * already part of the pattern.
 */
data class Level(
    val world: Int,
    val number: Int,
    val name: String,
    val n: Int,
    val type: LevelType,
    val target: List<Int> = emptyList(),
    val must: Set<Int> = emptySet(),
    val avoid: Set<Int> = emptySet(),
) {
    val id: String get() = "$world-$number"
}

/** A group of levels with its own look. Worlds borrow their names and colours from the app's themes. */
data class World(val number: Int, val name: String, val blurb: String, val themeIndex: Int, val levels: List<Level>)
