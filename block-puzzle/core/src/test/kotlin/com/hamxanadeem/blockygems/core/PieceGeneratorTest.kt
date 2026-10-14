package com.hamxanadeem.blockygems.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PieceGeneratorTest {

    @Test
    fun `deals three pieces in valid colours`() {
        val tray = PieceGenerator(Random(1)).nextTray(Board())
        assertEquals(3, tray.size)
        assertTrue(tray.all { it.color in 0 until ShapeCatalog.COLORS })
    }

    @Test
    fun `only deals pieces that fit when some do`() {
        val board = Board()
        // Checkerboard: only single blocks fit.
        for (r in 0 until 8) for (c in 0 until 8) {
            if ((r + c) % 2 == 0) board.place(Piece(ShapeCatalog.byPattern("X")!!, 0), r, c)
        }
        val generator = PieceGenerator(Random(5))
        repeat(50) {
            assertTrue(generator.nextTray(board).all { board.fits(it.shape) })
        }
    }

    @Test
    fun `often deals a piece that completes a line`() {
        val board = Board()
        for (c in 0 until 7) board.place(Piece(ShapeCatalog.byPattern("X")!!, 0), 0, c)
        val generator = PieceGenerator(Random(9))
        val helpfulTrays = (1..200).count {
            generator.nextTray(board).any { piece ->
                (0..8 - piece.shape.rows).any { r ->
                    (0..8 - piece.shape.cols).any { c -> board.linesCompletedBy(piece.shape, r, c).first.isNotEmpty() }
                }
            }
        }
        assertTrue(helpfulTrays > 80, "only $helpfulTrays of 200 trays could complete the row")
    }

    @Test
    fun `same seed deals the same pieces`() {
        val a = PieceGenerator(Random(3)).nextTray(Board())
        val b = PieceGenerator(Random(3)).nextTray(Board())
        assertEquals(a, b)
    }
}
