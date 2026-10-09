package com.fidget.patternlock.domain

/**
 * A constellation to trace: real star positions (projected from right ascension and declination, north up, east
 * left, as on a star chart, fitted to the screen when drawn) and the lines between them, plus a short story shown once it has been drawn.
 */
data class Constellation(
    val id: String,
    val name: String,
    val keyword: String,
    val description: String,
    val stars: List<Pair<Float, Float>>,
    val edges: List<Pair<Int, Int>>,
) {
    /** Index of the edge joining [a] and [b] in either direction, or -1. */
    fun edgeBetween(a: Int, b: Int): Int = edges.indexOfFirst { (x, y) -> (x == a && y == b) || (x == b && y == a) }
}

private fun c(id: String, name: String, keyword: String, description: String, stars: List<Pair<Float, Float>>, edges: List<Pair<Int, Int>>) =
    Constellation(id, name, keyword, description, stars, edges)

private fun chain(vararg i: Int) = (0 until i.size - 1).map { i[it] to i[it + 1] }

object Constellations {
    val all: List<Constellation> = listOf(
        c("orion", "Orion", "Focus",
            "The Hunter. His three-star belt is one of the easiest patterns to find in the winter sky, and the red star Betelgeuse marks his shoulder.",
            listOf(-84f to -150f, 47f to -131f, 93f to 126f, -51f to 151f, -1f to 2f),
            listOf(0 to 1, 0 to 4, 1 to 4, 2 to 4, 3 to 4)),
        c("ursa-major", "Ursa Major", "Stability",
            "The Great Bear. Seven of its stars form the Big Dipper, and the two at the end of the bowl point the way to Polaris, the North Star.",
            listOf(11f to -25f, 158f to -132f, 189f to -41f, 68f to 30f, -83f to -12f, -160f to -7f, -251f to 73f),
            listOf(0 to 1, 0 to 3, 0 to 4, 1 to 2, 2 to 3, 4 to 5, 5 to 6)),
        c("cassiopeia", "Cassiopeia", "Balance",
            "A queen of Greek myth, drawn as a bright W that circles the north celestial pole and stays above the horizon all night for much of the northern world.",
            listOf(-103f to -74f, -53f to -5f, 10f to -12f, 50f to 60f, 116f to 4f),
            listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4)),
        c("cygnus", "Cygnus", "Clarity",
            "The Swan, flying along the Milky Way. Its brightest stars form the Northern Cross, with Deneb at the tail.",
            listOf(-191f to 111f, -84f to 53f, 3f to -54f, 119f to -148f, -57f to -145f, 207f to 150f),
            listOf(0 to 1, 1 to 2, 2 to 3, 2 to 4, 2 to 5)),
        c("scorpius", "Scorpius", "Transformation",
            "The Scorpion, a long curve of stars with red Antares as its heart. It crawls low across the summer sky.",
            listOf(201f to -88f, 203f to -151f, 186f to -204f, 79f to -91f, 52f to -60f, -3f to 45f, -8f to 111f, -157f to 211f, -183f to 142f, -155f to 104f),
            listOf(0 to 1, 1 to 2, 1 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7, 7 to 8, 8 to 9)),
        c("leo", "Leo", "Confidence",
            "The Lion. A backwards question mark of stars called the Sickle forms his head and mane, with bright Regulus at its base.",
            listOf(123f to 109f, 124f to 24f, 69f to -28f, -154f to -43f, -315f to 52f, -159f to 46f, 81f to -91f, 207f to -105f),
            listOf(0 to 1, 0 to 5, 1 to 2, 2 to 3, 2 to 6, 3 to 4, 4 to 5, 6 to 7)),
        c("taurus", "Taurus", "Patience",
            "The Bull. The V-shaped Hyades make its face, glowing red Aldebaran is its eye, and the Pleiades cluster rides on its shoulder.",
            listOf(-271f to -81f, -13f to 14f, 17f to -33f, -212f to -212f, 138f to 82f, 304f to 138f),
            listOf(0 to 1, 1 to 2, 1 to 4, 2 to 3, 4 to 5)),
        c("aries", "Aries", "New Beginnings",
            "The Ram. A small, quiet arc of three stars that marked the start of spring in ancient calendars, a symbol of fresh starts.",
            listOf(-152f to -86f, 16f to -13f, 68f to 32f, 73f to 59f),
            listOf(0 to 1, 1 to 2, 2 to 3)),
        c("gemini", "Gemini", "Connection",
            "The Twins, Castor and Pollux. Two bright stars mark their heads, with lines of stars trailing down to their feet.",
            listOf(165f to -3f, 132f to -1f, 46f to -44f, -147f to -169f, -195f to -104f, -100f to 10f, 75f to 109f, 44f to 172f),
            listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6, 6 to 7)),
        c("cancer", "Cancer", "Nurture",
            "The Crab. Faint, but famous for the Beehive Cluster at its centre, which shows as a soft patch of light on a dark night.",
            listOf(-73f to 89f, -13f to -20f, -20f to -209f, 109f to 136f),
            listOf(0 to 1, 1 to 2, 1 to 3)),
        c("virgo", "Virgo", "Mindfulness",
            "The Maiden, the second largest constellation. Bright Spica marks the grain she holds, and the sky around her is rich in galaxies.",
            listOf(299f to -39f, 68f to 19f, -55f to 91f, -122f to 192f, -22f to -200f, 7f to -66f, -165f to 4f),
            listOf(0 to 1, 1 to 2, 1 to 5, 2 to 3, 2 to 6, 4 to 5)),
        c("libra", "Libra", "Harmony",
            "The Scales, said to weigh balance and justice. It is the only zodiac constellation named for an object rather than a creature.",
            listOf(65f to 83f, 125f to -77f, 15f to -198f, -64f to -101f, -64f to 134f),
            listOf(0 to 1, 1 to 2, 1 to 3, 2 to 3, 3 to 4)),
        c("sagittarius", "Sagittarius", "Exploration",
            "The Archer. Its brightest stars outline a teapot, and just above the spout lies the centre of our Milky Way galaxy.",
            listOf(67f to 138f, 45f to 95f, 59f to 15f, 34f to -62f, -99f to 18f, -35f to -35f, -73f to -46f, 117f to 29f, -137f to -137f),
            listOf(0 to 1, 1 to 2, 1 to 4, 1 to 7, 2 to 3, 2 to 5, 2 to 7, 3 to 5, 4 to 5, 4 to 6, 5 to 6, 6 to 8)),
        c("capricornus", "Capricornus", "Discipline",
            "The Sea-Goat, a faint triangle of stars. It is one of the oldest named constellations, known since Babylonian times.",
            listOf(249f to -39f, -32f to 86f, -119f to -22f),
            listOf(0 to 1, 0 to 2, 1 to 2)),
        c("aquarius", "Aquarius", "Flow",
            "The Water Bearer, pouring out a stream of stars. Its zigzags of faint stars flow across the sky like water.",
            listOf(454f to 37f, 245f to -45f, 93f to -140f, -9f to -146f, -112f to -12f, -224f to 19f, -180f to 234f),
            listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 4 to 5, 5 to 6)),
        c("pisces", "Pisces", "Compassion",
            "The Fishes, two rings of stars joined by a long cord. A gentle, faint constellation of the autumn sky.",
            listOf(-157f to -410f, -251f to -135f, -320f to -26f, -408f to 91f, -128f to 4f, 149f to 21f, 237f to 42f, 346f to 82f),
            listOf(0 to 1, 1 to 2, 2 to 3, 2 to 4, 4 to 5, 5 to 6, 6 to 7)),
        c("lyra", "Lyra", "Creativity",
            "The Lyre of Orpheus. Vega, one of the brightest stars in the sky, sits at one corner of a small parallelogram.",
            listOf(12f to -19f, 13f to -55f, 39f to -41f, -22f to -7f, -39f to 66f, -7f to 55f),
            listOf(0 to 1, 0 to 2, 0 to 3, 0 to 5, 1 to 2, 3 to 4, 4 to 5)),
        c("draco", "Draco", "Perspective",
            "The Dragon, winding between the Big and Little Dippers. Thousands of years ago its star Thuban was the North Star.",
            listOf(-160f to 106f, -194f to 199f, -119f to 198f, -234f to -135f, -39f to -31f, 48f to 42f, 184f to 60f, 295f to -104f, 339f to -298f, 398f to -400f, -121f to -178f, -253f to -209f),
            listOf(0 to 1, 0 to 2, 0 to 3, 1 to 2, 3 to 10, 3 to 11, 4 to 5, 4 to 10, 5 to 6, 6 to 7, 7 to 8, 8 to 9)),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** The constellation after [id] that hasn't been discovered yet, or simply the next one when all are. */
    fun next(id: String, discovered: Set<String>): Constellation {
        val i = all.indexOfFirst { it.id == id }
        val order = all.indices.map { all[(i + 1 + it) % all.size] }
        return order.firstOrNull { it.id !in discovered } ?: order.first()
    }
}
