package com.hamxanadeem.blockygems.core

import kotlin.random.Random

/**
 * Deals the three pieces in the tray. It plays fair: every piece dealt fits on the
 * board when possible, and it often slips in a piece that completes a line, which
 * is what makes block puzzles feel rewarding.
 */
class PieceGenerator(private val random: Random = Random.Default) {

    private val shapes = ShapeCatalog.all
    private val totalWeight = shapes.sumOf { it.weight }

    fun nextTray(board: Board): List<Piece> {
        val fitting = shapes.filter { board.fits(it) }
        if (fitting.isEmpty()) return List(TRAY_SIZE) { randomPiece(shapes) }

        val tray = MutableList(TRAY_SIZE) { randomPiece(fitting) }
        if (random.nextFloat() < HELPFUL_CHANCE) {
            val helpful = fitting.filter { completesALine(board, it) }
            if (helpful.isNotEmpty()) tray[random.nextInt(TRAY_SIZE)] = randomPiece(helpful)
        }
        return tray
    }

    private fun completesALine(board: Board, shape: Shape): Boolean {
        for (r in 0..board.size - shape.rows) for (c in 0..board.size - shape.cols) {
            val (rows, cols) = board.linesCompletedBy(shape, r, c)
            if (rows.isNotEmpty() || cols.isNotEmpty()) return true
        }
        return false
    }

    private fun randomPiece(from: List<Shape>): Piece {
        val total = if (from === shapes) totalWeight else from.sumOf { it.weight }
        var pick = random.nextInt(total)
        val shape = from.first { pick -= it.weight; pick < 0 }
        return Piece(shape, random.nextInt(ShapeCatalog.COLORS))
    }

    companion object {
        const val TRAY_SIZE = 3
        const val HELPFUL_CHANCE = 0.45f
    }
}
