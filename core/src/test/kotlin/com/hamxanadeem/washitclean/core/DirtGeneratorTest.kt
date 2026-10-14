package com.hamxanadeem.washitclean.core

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DirtGeneratorTest {

    private val everywhere: (Int, Int) -> Boolean = { _, _ -> true }

    @Test
    fun `same seed gives the same dirt`() {
        val a = DirtGenerator.generate(40, 50, seed = 42, toughness = 1.5f, isSurface = everywhere)
        val b = DirtGenerator.generate(40, 50, seed = 42, toughness = 1.5f, isSurface = everywhere)
        assertArrayEquals(a, b)
    }

    @Test
    fun `different seeds give different dirt`() {
        val a = DirtGenerator.generate(40, 50, seed = 1, toughness = 1f, isSurface = everywhere)
        val b = DirtGenerator.generate(40, 50, seed = 2, toughness = 1f, isSurface = everywhere)
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun `values stay between half and full toughness`() {
        val toughness = 2f
        val dirt = DirtGenerator.generate(96, 128, seed = 7, toughness = toughness, isSurface = everywhere)
        assertTrue(dirt.all { it in (0.5f * toughness)..toughness })
    }

    @Test
    fun `background cells stay clean`() {
        val width = 30
        val dirt = DirtGenerator.generate(width, 30, seed = 3, toughness = 1f) { x, _ -> x < 10 }
        for (i in dirt.indices) {
            if (i % width < 10) assertTrue(dirt[i] > 0f) else assertEquals(0f, dirt[i])
        }
    }
}
