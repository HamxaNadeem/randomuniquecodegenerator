package com.hamxanadeem.blockygems.core

/** Result of placing a piece: what was filled and which lines were cleared. */
data class Placement(
    val placed: List<Cell>,
    val clearedRows: List<Int>,
    val clearedCols: List<Int>,
    /** Every cleared cell with the colour it had, for the clear animation. */
    val clearedCells: List<Pair<Cell, Int>>,
) {
    val lines: Int get() = clearedRows.size + clearedCols.size
}

/** The square playing field. Each cell is [EMPTY] or a gem colour. */
class Board(val size: Int = SIZE) {

    private val cells = IntArray(size * size) { EMPTY }

    operator fun get(row: Int, col: Int): Int = cells[row * size + col]

    fun isEmpty(row: Int, col: Int): Boolean = get(row, col) == EMPTY

    val filledCount: Int get() = cells.count { it != EMPTY }

    val isClear: Boolean get() = cells.all { it == EMPTY }

    /** True if [shape]'s top-left corner can go at ([row], [col]) without overlap. */
    fun canPlace(shape: Shape, row: Int, col: Int): Boolean {
        if (row < 0 || col < 0 || row + shape.rows > size || col + shape.cols > size) return false
        return shape.cells.all { isEmpty(row + it.row, col + it.col) }
    }

    /** True if [shape] fits anywhere on the board. */
    fun fits(shape: Shape): Boolean {
        for (r in 0..size - shape.rows) for (c in 0..size - shape.cols) {
            if (canPlace(shape, r, c)) return true
        }
        return false
    }

    /**
     * The rows and columns that placing [shape] at ([row], [col]) would complete,
     * without changing the board. Used to highlight lines while dragging.
     */
    fun linesCompletedBy(shape: Shape, row: Int, col: Int): Pair<List<Int>, List<Int>> {
        if (!canPlace(shape, row, col)) return emptyList<Int>() to emptyList()
        val covered = shape.cells.mapTo(HashSet()) { Cell(row + it.row, col + it.col) }
        fun filled(r: Int, c: Int) = !isEmpty(r, c) || Cell(r, c) in covered
        val rows = (row until row + shape.rows).filter { r -> (0 until size).all { c -> filled(r, c) } }
        val cols = (col until col + shape.cols).filter { c -> (0 until size).all { r -> filled(r, c) } }
        return rows to cols
    }

    /** Places [piece] and clears any completed rows and columns. Throws if it doesn't fit. */
    fun place(piece: Piece, row: Int, col: Int): Placement {
        require(canPlace(piece.shape, row, col)) { "${piece.shape} does not fit at $row,$col" }
        val placed = piece.shape.cells.map { Cell(row + it.row, col + it.col) }
        placed.forEach { cells[it.row * size + it.col] = piece.color }

        val fullRows = (0 until size).filter { r -> (0 until size).none { c -> isEmpty(r, c) } }
        val fullCols = (0 until size).filter { c -> (0 until size).none { r -> isEmpty(r, c) } }
        val cleared = LinkedHashMap<Cell, Int>()
        for (r in fullRows) for (c in 0 until size) cleared[Cell(r, c)] = get(r, c)
        for (c in fullCols) for (r in 0 until size) cleared[Cell(r, c)] = get(r, c)
        cleared.keys.forEach { cells[it.row * size + it.col] = EMPTY }
        return Placement(placed, fullRows, fullCols, cleared.map { it.key to it.value })
    }

    /** Empties the given rows. Used by the revive power-up. */
    fun clearRows(rows: Collection<Int>): List<Pair<Cell, Int>> {
        val cleared = ArrayList<Pair<Cell, Int>>()
        for (r in rows) for (c in 0 until size) {
            if (!isEmpty(r, c)) cleared += Cell(r, c) to get(r, c)
            cells[r * size + c] = EMPTY
        }
        return cleared
    }

    /** One character per cell: `.` for empty, the colour digit otherwise. */
    fun encode(): String = cells.joinToString("") { if (it == EMPTY) "." else it.toString() }

    companion object {
        const val SIZE = 8
        const val EMPTY = -1

        fun decode(text: String, size: Int = SIZE): Board? {
            if (text.length != size * size) return null
            val board = Board(size)
            for (i in text.indices) {
                val ch = text[i]
                board.cells[i] = when {
                    ch == '.' -> EMPTY
                    ch.isDigit() && ch.digitToInt() < ShapeCatalog.COLORS -> ch.digitToInt()
                    else -> return null
                }
            }
            return board
        }
    }
}
