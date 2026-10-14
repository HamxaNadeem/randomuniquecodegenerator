package com.hamxanadeem.washitclean.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DirtGridTest {

    private fun uniformGrid(width: Int = 20, height: Int = 20, value: Float = 1f) =
        DirtGrid(width, height, FloatArray(width * height) { value })

    @Test
    fun `new grid starts completely dirty`() {
        val grid = uniformGrid()
        assertEquals(0f, grid.cleanFraction)
        assertEquals(false, grid.isClean())
    }

    @Test
    fun `grid with no dirt counts as clean`() {
        val grid = DirtGrid(4, 4, FloatArray(16))
        assertEquals(1f, grid.cleanFraction)
        assertTrue(grid.isClean())
    }

    @Test
    fun `spray removes dirt near the centre but not outside the radius`() {
        val grid = uniformGrid()
        val removed = grid.spray(10f, 10f, radius = 3f, power = 0.5f)
        assertTrue(removed > 0f)
        assertTrue(grid.dirtAt(10, 10) < 1f)
        assertEquals(1f, grid.dirtAt(0, 0))
        assertEquals(1f, grid.dirtAt(19, 19))
    }

    @Test
    fun `dirt never goes below zero`() {
        val grid = uniformGrid()
        repeat(50) { grid.spray(10f, 10f, radius = 4f, power = 5f) }
        for (y in 0 until grid.height) for (x in 0 until grid.width) {
            assertTrue(grid.dirtAt(x, y) >= 0f)
        }
    }

    @Test
    fun `clean fraction matches the dirt actually removed`() {
        val grid = uniformGrid(value = 2f)
        val removed = grid.spray(5f, 5f, radius = 4f, power = 0.7f) + grid.spray(12f, 9f, radius = 4f, power = 0.7f)
        assertEquals(removed / (20 * 20 * 2f), grid.cleanFraction, 1e-4f)
    }

    @Test
    fun `spraying far outside the grid does nothing`() {
        val grid = uniformGrid()
        assertEquals(0f, grid.spray(-100f, -100f, radius = 5f, power = 1f))
        assertEquals(0f, grid.cleanFraction)
    }

    @Test
    fun `changed cells are reported`() {
        val grid = uniformGrid()
        val changed = mutableSetOf<Pair<Int, Int>>()
        grid.spray(10f, 10f, radius = 2f, power = 0.5f) { x, y -> changed += x to y }
        assertTrue((10 to 10) in changed)
        assertTrue(changed.all { (x, y) -> grid.dirtAt(x, y) < 1f })
    }

    @Test
    fun `a fast stroke leaves no gaps`() {
        val grid = uniformGrid(width = 60, height = 10)
        grid.sprayStroke(2f, 5f, 58f, 5f, radius = 3f, power = 0.3f)
        for (x in 2..57) assertTrue(grid.dirtAt(x, 5) < 1f, "cell $x was skipped")
    }

    @Test
    fun `clearAll finishes the level`() {
        val grid = uniformGrid()
        var reported = 0
        grid.clearAll { _, _ -> reported++ }
        assertEquals(400, reported)
        assertEquals(1f, grid.cleanFraction)
    }

    @Test
    fun `rejects mismatched sizes and negative dirt`() {
        assertThrows<IllegalArgumentException> { DirtGrid(3, 3, FloatArray(8)) }
        assertThrows<IllegalArgumentException> { DirtGrid(1, 1, floatArrayOf(-1f)) }
    }
}
