package com.hamxanadeem.washitclean.core

/** A spray nozzle. [radius] is in grid cells; [power] is dirt removed per spray at the centre. */
data class Nozzle(
    val id: String,
    val name: String,
    val price: Int,
    val radius: Float,
    val power: Float,
)

object NozzleCatalog {
    val all: List<Nozzle> = listOf(
        Nozzle("hose", "Garden Hose", price = 0, radius = 6f, power = 0.10f),
        Nozzle("jet", "Jet Nozzle", price = 150, radius = 7f, power = 0.18f),
        Nozzle("turbo", "Turbo Spinner", price = 500, radius = 9f, power = 0.26f),
        Nozzle("fan", "Wide Fan", price = 1200, radius = 13f, power = 0.30f),
        Nozzle("blaster", "Pro Blaster", price = 3000, radius = 15f, power = 0.45f),
    )

    val default: Nozzle = all.first()

    fun byId(id: String): Nozzle = all.firstOrNull { it.id == id } ?: default
}
