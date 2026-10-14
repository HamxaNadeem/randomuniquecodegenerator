package com.hamxanadeem.washitclean.core

import kotlin.math.min

/** The kinds of object a level asks the player to clean. */
enum class ObjectKind(val displayName: String) {
    CAR("Car"),
    SNEAKER("Sneaker"),
    PATIO("Patio"),
    HOUSE("House"),
    BOAT("Boat"),
}

data class Level(
    val number: Int,
    val kind: ObjectKind,
    val name: String,
    /** How thick the dirt is. 1.0 is the first level; higher takes more passes. */
    val toughness: Float,
    val reward: Int,
    val seed: Long,
    val gridWidth: Int = GRID_WIDTH,
    val gridHeight: Int = GRID_HEIGHT,
) {
    companion object {
        const val GRID_WIDTH = 96
        const val GRID_HEIGHT = 128
    }
}

/** Levels are generated from their number, so the game never runs out of them. */
object LevelCatalog {

    const val MAX_TOUGHNESS = 3.5f

    private val adjectives = listOf("Dusty", "Muddy", "Grimy", "Mossy", "Oily", "Filthy", "Swampy", "Sticky")

    fun level(number: Int): Level {
        require(number >= 1) { "Levels start at 1" }
        val kinds = ObjectKind.entries
        val kind = kinds[(number - 1) % kinds.size]
        val adjective = adjectives[((number - 1) / kinds.size) % adjectives.size]
        return Level(
            number = number,
            kind = kind,
            name = "$adjective ${kind.displayName}",
            toughness = min(MAX_TOUGHNESS, 1f + (number - 1) * 0.12f),
            reward = 30 + number * 10,
            seed = number * 7919L,
        )
    }
}
