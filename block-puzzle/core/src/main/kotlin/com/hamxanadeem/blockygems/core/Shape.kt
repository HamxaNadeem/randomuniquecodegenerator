package com.hamxanadeem.blockygems.core

data class Cell(val row: Int, val col: Int)

/**
 * The geometry of a piece. [pattern] uses `X` for a block, `.` for a gap and `/`
 * between rows, e.g. `"X./XX"` is a small L. The pattern doubles as a stable id
 * for saving games.
 */
class Shape(val pattern: String, val weight: Int) {

    val cells: List<Cell>
    val rows: Int
    val cols: Int

    init {
        val lines = pattern.split('/')
        require(lines.isNotEmpty() && lines.all { it.length == lines[0].length }) { "Ragged pattern $pattern" }
        cells = lines.flatMapIndexed { r, line ->
            line.mapIndexedNotNull { c, ch ->
                when (ch) {
                    'X' -> Cell(r, c)
                    '.' -> null
                    else -> throw IllegalArgumentException("Bad character '$ch' in $pattern")
                }
            }
        }
        require(cells.isNotEmpty()) { "Empty pattern" }
        rows = lines.size
        cols = lines[0].length
    }

    val size: Int get() = cells.size

    override fun equals(other: Any?) = other is Shape && other.pattern == pattern
    override fun hashCode() = pattern.hashCode()
    override fun toString() = "Shape($pattern)"
}

/** A shape in a gem colour, ready to be placed. */
data class Piece(val shape: Shape, val color: Int)

object ShapeCatalog {

    /** Number of gem colours. */
    const val COLORS = 7

    // Weights make small, flexible shapes more common than awkward big ones.
    val all: List<Shape> = listOf(
        "X" to 2,
        "XX" to 6, "X/X" to 6,
        "XXX" to 6, "X/X/X" to 6,
        "XXXX" to 4, "X/X/X/X" to 4,
        "XXXXX" to 2, "X/X/X/X/X" to 2,
        "XX/XX" to 7,
        "XXX/XXX" to 3, "XX/XX/XX" to 3,
        "XXX/XXX/XXX" to 2,
        // Small corners.
        "X./XX" to 4, ".X/XX" to 4, "XX/X." to 4, "XX/.X" to 4,
        // L and J shapes.
        "X./X./XX" to 3, ".X/.X/XX" to 3, "XX/X./X." to 3, "XX/.X/.X" to 3,
        "XXX/X.." to 3, "XXX/..X" to 3, "X../XXX" to 3, "..X/XXX" to 3,
        // T shapes.
        "XXX/.X." to 3, ".X./XXX" to 3, "X./XX/X." to 3, ".X/XX/.X" to 3,
        // S and Z shapes.
        "XX./.XX" to 2, ".XX/XX." to 2, "X./XX/.X" to 2, ".X/XX/X." to 2,
        // Big corners.
        "X../X../XXX" to 1, "..X/..X/XXX" to 1, "XXX/X../X.." to 1, "XXX/..X/..X" to 1,
    ).map { (pattern, weight) -> Shape(pattern, weight) }

    private val byPattern = all.associateBy { it.pattern }

    fun byPattern(pattern: String): Shape? = byPattern[pattern]
}
