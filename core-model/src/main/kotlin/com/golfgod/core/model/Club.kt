package com.golfgod.core.model

enum class ClubCategory {
    DRIVER,
    WOOD,
    HYBRID,
    IRON,
    WEDGE,
    PUTTER
}

data class Club(
    val id: String,
    val name: String,
    val code: String,
    val category: ClubCategory,
    val loftDegrees: Float,
    val shaftLengthInches: Float,
    val standardSmashFactor: Float,
    val defaultTargetTempo: Float = 3.0f,
    val typicalCarryYards: Int,
    val emoji: String
) {
    val shaftLengthMeters: Float
        get() = shaftLengthInches * 0.0254f
}

object StandardBag {
    val CLUBS: List<Club> = listOf(
        Club("dr", "Driver", "DR", ClubCategory.DRIVER, 10.5f, 45.5f, 1.48f, 3.1f, 240, "🚀"),
        Club("3w", "3-Wood", "3W", ClubCategory.WOOD, 15.0f, 43.0f, 1.45f, 3.1f, 215, "🪵"),
        Club("5w", "5-Wood", "5W", ClubCategory.WOOD, 18.0f, 42.0f, 1.42f, 3.0f, 200, "🪵"),
        Club("3h", "3-Hybrid", "3H", ClubCategory.HYBRID, 19.0f, 40.5f, 1.40f, 3.0f, 190, "⚡"),
        Club("4i", "4-Iron", "4I", ClubCategory.IRON, 21.0f, 38.5f, 1.38f, 3.0f, 180, "🎯"),
        Club("5i", "5-Iron", "5I", ClubCategory.IRON, 24.0f, 38.0f, 1.37f, 3.0f, 170, "🎯"),
        Club("6i", "6-Iron", "6I", ClubCategory.IRON, 27.0f, 37.5f, 1.36f, 3.0f, 160, "🎯"),
        Club("7i", "7-Iron", "7I", ClubCategory.IRON, 31.0f, 37.0f, 1.35f, 3.0f, 150, "⭐"),
        Club("8i", "8-Iron", "8I", ClubCategory.IRON, 35.0f, 36.5f, 1.32f, 3.0f, 140, "🎯"),
        Club("9i", "9-Iron", "9I", ClubCategory.IRON, 40.0f, 36.0f, 1.30f, 3.0f, 130, "🎯"),
        Club("pw", "Pitching Wedge", "PW", ClubCategory.WEDGE, 45.0f, 35.5f, 1.26f, 2.9f, 120, "⛳"),
        Club("gw", "Gap Wedge", "GW", ClubCategory.WEDGE, 50.0f, 35.25f, 1.23f, 2.8f, 105, "⛳"),
        Club("sw", "Sand Wedge", "SW", ClubCategory.WEDGE, 54.0f, 35.0f, 1.20f, 2.8f, 90, "🏖️"),
        Club("lw", "Lob Wedge", "LW", ClubCategory.WEDGE, 58.0f, 34.75f, 1.18f, 2.8f, 75, "🏖️"),
        Club("pt", "Putter", "PT", ClubCategory.PUTTER, 3.0f, 34.0f, 1.00f, 2.0f, 15, "⛳")
    )

    fun getByCode(code: String): Club {
        return CLUBS.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: CLUBS[0]
    }
}
