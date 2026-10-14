package com.hamxanadeem.blockygems.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BoardTest {

    private fun shape(p: String) = ShapeCatalog.byPattern(p)!!
    private fun piece(p: String, color: Int = 0) = Piece(shape(p), color)

    /** Fills row [r] except column [gap]. */
    private fun Board.fillRowExcept(r: Int, gap: Int) {
        for (c in 0 until size) if (c != gap) place(piece("X", 1), r, c)
    }

    @Test
    fun `shapes parse into cells`() {
        val l = shape("X./X./XX")
        assertEquals(3, l.rows)
        assertEquals(2, l.cols)
        assertEquals(listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0), Cell(2, 1)), l.cells)
    }

    @Test
    fun `catalog patterns are unique and valid`() {
        val patterns = ShapeCatalog.all.map { it.pattern }
        assertEquals(patterns.size, patterns.toSet().size)
        assertTrue(ShapeCatalog.all.all { it.weight > 0 && it.rows <= 5 && it.cols <= 5 })
        assertThrows<IllegalArgumentException> { Shape("XX/X", 1) }
        assertThrows<IllegalArgumentException> { Shape("X?", 1) }
    }

    @Test
    fun `pieces cannot overlap or leave the board`() {
        val board = Board()
        assertTrue(board.canPlace(shape("XXX"), 0, 5))
        assertFalse(board.canPlace(shape("XXX"), 0, 6))
        assertFalse(board.canPlace(shape("X"), -1, 0))
        board.place(piece("XX/XX"), 3, 3)
        assertFalse(board.canPlace(shape("X"), 4, 4))
        assertThrows<IllegalArgumentException> { board.place(piece("X"), 4, 4) }
    }

    @Test
    fun `completing a row clears it`() {
        val board = Board()
        board.fillRowExcept(2, gap = 5)
        val placement = board.place(piece("X/X"), 1, 5)
        assertEquals(listOf(2), placement.clearedRows)
        assertEquals(emptyList<Int>(), placement.clearedCols)
        assertEquals(8, placement.clearedCells.size)
        assertTrue((0 until 8).all { board.isEmpty(2, it) })
        // The part of the piece outside the row stays.
        assertFalse(board.isEmpty(1, 5))
    }

    @Test
    fun `a row and a column cleared together share their crossing cell`() {
        val board = Board()
        board.fillRowExcept(7, gap = 0)
        for (r in 0 until 7) board.place(piece("X"), r, 0) // fill column 0 except row 7
        val placement = board.place(piece("X"), 7, 0)
        assertEquals(2, placement.lines)
        assertEquals(15, placement.clearedCells.size)
        assertTrue(board.isClear)
    }

    @Test
    fun `preview of completed lines does not change the board`() {
        val board = Board()
        board.fillRowExcept(4, gap = 2)
        val before = board.encode()
        assertEquals(listOf(4) to emptyList<Int>(), board.linesCompletedBy(shape("X"), 4, 2))
        assertEquals(emptyList<Int>() to emptyList<Int>(), board.linesCompletedBy(shape("X"), 3, 2))
        assertEquals(before, board.encode())
    }

    @Test
    fun `fits checks the whole board`() {
        val board = Board()
        for (r in 0 until 8) for (c in 0 until 8) if ((r + c) % 2 == 0) board.place(piece("X"), r, c)
        assertTrue(board.fits(shape("X")))
        assertFalse(board.fits(shape("XX")))
    }

    @Test
    fun `encode and decode round trip`() {
        val board = Board()
        board.place(piece("XX/XX", 3), 2, 2)
        val copy = Board.decode(board.encode())!!
        assertEquals(board.encode(), copy.encode())
        assertEquals(3, copy[3, 3])
        assertNull(Board.decode("short"))
        assertNull(Board.decode("9".repeat(64)))
    }
}
