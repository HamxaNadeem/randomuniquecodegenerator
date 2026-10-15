package com.hamxanadeem.blockygems.ui

import android.graphics.Color

/** The seven gem colours: a light, base and dark shade of each for shading. */
object GemPalette {

    class Gem(val light: Int, val base: Int, val dark: Int)

    val gems: List<Gem> = listOf(
        Gem(0xFFFF8A80.toInt(), 0xFFF2384A.toInt(), 0xFF9E0B24.toInt()), // ruby
        Gem(0xFFFFD180.toInt(), 0xFFFF9800.toInt(), 0xFFB34700.toInt()), // amber
        Gem(0xFFFFF59D.toInt(), 0xFFFFD600.toInt(), 0xFFB38F00.toInt()), // topaz
        Gem(0xFFB9F6CA.toInt(), 0xFF2BD96B.toInt(), 0xFF00803A.toInt()), // emerald
        Gem(0xFF84FFFF.toInt(), 0xFF1EC8F0.toInt(), 0xFF00739E.toInt()), // aquamarine
        Gem(0xFF8C9EFF.toInt(), 0xFF3D5AFE.toInt(), 0xFF1A237E.toInt()), // sapphire
        Gem(0xFFEA80FC.toInt(), 0xFFB13DF2.toInt(), 0xFF5E1490.toInt()), // amethyst
    )

    operator fun get(color: Int): Gem = gems[color.mod(gems.size)]

    fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}
