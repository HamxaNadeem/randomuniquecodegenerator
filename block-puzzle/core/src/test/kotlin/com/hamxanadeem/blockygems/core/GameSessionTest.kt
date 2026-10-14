package com.hamxanadeem.blockygems.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

class GameSessionTest {

    private fun shape(p: String) = ShapeCatalog.byPattern(p)!!

    /** A session whose board and tray we control, built through the save format. */
    private fun session(board: String = ".".repeat(64), tray: String = "X@0,X@1,X@2", score: Int = 0, combo: Int = 0): GameSession =
        GameSession.decode("v1;$score;$combo;0;0;$board;$tray", PieceGenerator(Random(1)))!!

    private fun boardWithRowAlmostFull(row: Int, gap: Int): String =
        (0 until 64).joinToString("") { i -> if (i / 8 == row && i % 8 != gap) "4" else "." }

    @Test
    fun `new game starts with a full tray and no score`() {
        val game = GameSession(PieceGenerator(Random(2)))
        assertEquals(0, game.score)
        assertTrue(game.trayPieces.all { it != null })
        assertFalse(game.isGameOver)
    }

    @Test
    fun `placing scores one point per block and empties the slot`() {
        val game = session(tray = "XX/XX@0,X@1,X@2")
        val result = game.place(0, 0, 0)!!
        assertEquals(4, result.points)
        assertEquals(4, game.score)
        assertNull(game.trayPiece(0))
    }

    @Test
    fun `invalid moves are rejected`() {
        val game = session(tray = "XXX@0,-,X@2")
        assertNull(game.place(0, 0, 6))
        assertNull(game.place(1, 0, 0))
        assertNull(game.place(5, 0, 0))
        assertEquals(0, game.score)
    }

    @Test
    fun `clearing a line scores and starts a combo`() {
        val game = session(board = boardWithRowAlmostFull(3, 6))
        val result = game.place(0, 3, 6)!!
        assertEquals(1, result.lines)
        assertEquals(1, result.combo)
        assertEquals(1 + Scoring.clearPoints(1, 1) + Scoring.ALL_CLEAR_BONUS, result.points)
        assertTrue(result.allClear)
        assertEquals("All Clear!", result.praise)
    }

    @Test
    fun `combo grows with consecutive clears and ends after three dry moves`() {
        val game = session(board = boardWithRowAlmostFull(0, 0), tray = "X@0,X@1,X@2", combo = 2)
        val clear = game.place(0, 0, 0)!!
        assertEquals(3, clear.combo)
        game.place(1, 5, 5)
        assertEquals(3, game.combo)
        game.place(2, 6, 6)
        // Tray refilled; one more dry move ends the combo.
        val slot = (0 until 3).first { game.trayPiece(it) != null }
        val piece = game.trayPiece(slot)!!
        val spot = (0..8 - piece.shape.rows).flatMap { r -> (0..8 - piece.shape.cols).map { c -> r to c } }
            .first { (r, c) -> game.board.canPlace(piece.shape, r, c) && game.board.linesCompletedBy(piece.shape, r, c).let { it.first.isEmpty() && it.second.isEmpty() } }
        game.place(slot, spot.first, spot.second)
        assertEquals(0, game.combo)
    }

    @Test
    fun `tray refills after the third piece`() {
        val game = session()
        assertFalse(game.place(0, 0, 0)!!.trayRefilled)
        assertFalse(game.place(1, 2, 2)!!.trayRefilled)
        assertTrue(game.place(2, 4, 4)!!.trayRefilled)
        assertTrue(game.trayPieces.all { it != null })
    }

    @Test
    fun `game over when nothing fits, and revive works once`() {
        val full = (0 until 64).joinToString("") { i -> if ((i / 8 + i % 8) % 2 == 0) "2" else "." }
        val game = session(board = full, tray = "XX@0,X/X@1,XXX@2")
        assertTrue(game.isGameOver)
        assertTrue(game.canRevive)
        val cleared = game.revive()
        assertEquals(GameSession.REVIVE_ROWS * 4, cleared.size)
        assertFalse(game.isGameOver)
        assertTrue(game.revived)
        assertFalse(game.canRevive)
        assertThrows<IllegalStateException> { game.revive() }
    }

    @Test
    fun `cannot revive a game that is not over`() {
        assertThrows<IllegalStateException> { session().revive() }
    }

    @Test
    fun `save and resume keeps everything`() {
        val game = GameSession(PieceGenerator(Random(4)))
        val slot = (0 until 3).first { game.trayPiece(it) != null }
        val piece = game.trayPiece(slot)!!
        game.place(slot, 0, 0)
        val restored = GameSession.decode(game.encode(), PieceGenerator(Random(99)))!!
        assertEquals(game.encode(), restored.encode())
        assertEquals(game.score, restored.score)
        assertEquals(piece.color, restored.board[piece.shape.cells[0].row, piece.shape.cells[0].col])
    }

    @Test
    fun `corrupt saves are rejected`() {
        val generator = PieceGenerator(Random(1))
        for (bad in listOf("", "v0;0;0;0;0;${".".repeat(64)};-,-,-", "v1;-5;0;0;0;${".".repeat(64)};-,-,-",
            "v1;0;0;0;0;${".".repeat(64)};nope@1,-,-", "v1;0;0;0;0;${".".repeat(64)};X@9,-,-", "v1;0;0;0;2;${".".repeat(64)};-,-,-")) {
            assertNull(GameSession.decode(bad, generator), bad)
        }
        assertNotNull(GameSession.decode("v1;0;0;0;0;${".".repeat(64)};-,-,-", generator))
    }

    @Test
    fun `a long random game always ends and never breaks the rules`() {
        val random = Random(7)
        repeat(20) {
            val game = GameSession(PieceGenerator(Random(random.nextLong())))
            var moves = 0
            while (!game.isGameOver && moves < 2000) {
                val options = (0 until 3).flatMap { slot ->
                    val p = game.trayPiece(slot) ?: return@flatMap emptyList()
                    (0..8 - p.shape.rows).flatMap { r -> (0..8 - p.shape.cols).map { c -> Triple(slot, r, c) } }
                        .filter { (s, r, c) -> game.canPlace(s, r, c) }
                }
                val (s, r, c) = options[random.nextInt(options.size)]
                val before = game.score
                assertNotNull(game.place(s, r, c))
                assertTrue(game.score > before)
                moves++
            }
            assertTrue(moves > 3)
        }
    }

    @Test
    fun `scoring rewards bigger clears and combos`() {
        assertEquals(0, Scoring.clearPoints(0, 3))
        assertTrue(Scoring.clearPoints(2, 1) > 2 * Scoring.clearPoints(1, 1))
        assertEquals(3 * Scoring.clearPoints(1, 1), Scoring.clearPoints(1, 3))
        assertNull(Scoring.praise(1, 1, false))
        assertEquals("Excellent!", Scoring.praise(2, 1, false))
    }
}
