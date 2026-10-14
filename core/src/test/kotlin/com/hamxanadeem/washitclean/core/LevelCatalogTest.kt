package com.hamxanadeem.washitclean.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LevelCatalogTest {

    @Test
    fun `levels are the same every time`() {
        assertEquals(LevelCatalog.level(17), LevelCatalog.level(17))
    }

    @Test
    fun `first level is easy`() {
        val level = LevelCatalog.level(1)
        assertEquals(1f, level.toughness)
        assertEquals(ObjectKind.CAR, level.kind)
        assertEquals("Dusty Car", level.name)
    }

    @Test
    fun `later levels are harder, pay more and stay within limits`() {
        for (n in 1 until 200) {
            val current = LevelCatalog.level(n)
            val next = LevelCatalog.level(n + 1)
            assertTrue(next.toughness >= current.toughness)
            assertTrue(next.toughness <= LevelCatalog.MAX_TOUGHNESS)
            assertTrue(next.reward > current.reward)
        }
    }

    @Test
    fun `every object kind appears`() {
        val kinds = (1..ObjectKind.entries.size).map { LevelCatalog.level(it).kind }.toSet()
        assertEquals(ObjectKind.entries.toSet(), kinds)
    }

    @Test
    fun `level numbers start at one`() {
        assertThrows<IllegalArgumentException> { LevelCatalog.level(0) }
    }

    @Test
    fun `hardest level can be finished with the starting nozzle in reasonable time`() {
        val level = LevelCatalog.level(500)
        val nozzle = NozzleCatalog.default
        val grid = DirtGrid(
            level.gridWidth,
            level.gridHeight,
            DirtGenerator.generate(level.gridWidth, level.gridHeight, level.seed, level.toughness) { _, _ -> true },
        )
        // Sweep the whole grid in rows, like a player would, counting passes.
        var passes = 0
        while (!grid.isClean() && passes < 100) {
            var y = 0f
            while (y <= level.gridHeight) {
                grid.sprayStroke(0f, y, level.gridWidth.toFloat(), y, nozzle.radius, nozzle.power)
                y += nozzle.radius
            }
            passes++
        }
        assertTrue(grid.isClean(), "level was not clean after $passes passes")
        assertTrue(passes <= 30, "level needed $passes full passes")
    }
}
