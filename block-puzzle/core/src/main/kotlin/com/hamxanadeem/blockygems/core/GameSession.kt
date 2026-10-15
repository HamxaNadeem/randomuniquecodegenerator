package com.hamxanadeem.blockygems.core

data class MoveResult(
    val placement: Placement,
    val points: Int,
    val combo: Int,
    val allClear: Boolean,
    val praise: String?,
    /** True if the tray was empty and three new pieces were dealt. */
    val trayRefilled: Boolean,
) {
    val lines: Int get() = placement.lines
}

/** One game: the board, the tray of pieces, the score and the combo. */
class GameSession private constructor(
    private val generator: PieceGenerator,
    val board: Board,
    private val tray: Array<Piece?>,
    score: Int,
    combo: Int,
    movesSinceClear: Int,
    revived: Boolean,
) {
    constructor(generator: PieceGenerator) : this(generator, Board(), arrayOfNulls(PieceGenerator.TRAY_SIZE), 0, 0, 0, false) {
        refillTray()
    }

    var score: Int = score
        private set
    var combo: Int = combo
        private set
    private var movesSinceClear: Int = movesSinceClear
    var revived: Boolean = revived
        private set

    fun trayPiece(slot: Int): Piece? = tray[slot]

    val trayPieces: List<Piece?> get() = tray.toList()

    /** The game is over when none of the remaining pieces fit. */
    val isGameOver: Boolean get() = tray.filterNotNull().none { board.fits(it.shape) }

    val canRevive: Boolean get() = isGameOver && !revived

    fun canPlace(slot: Int, row: Int, col: Int): Boolean {
        val piece = tray.getOrNull(slot) ?: return false
        return board.canPlace(piece.shape, row, col)
    }

    /** Places the piece from [slot] with its top-left at ([row], [col]); null if it can't go there. */
    fun place(slot: Int, row: Int, col: Int): MoveResult? {
        if (!canPlace(slot, row, col)) return null
        val piece = tray[slot]!!
        val placement = board.place(piece, row, col)
        tray[slot] = null

        var points = Scoring.placementPoints(piece.shape.size)
        if (placement.lines > 0) {
            combo += 1
            movesSinceClear = 0
            points += Scoring.clearPoints(placement.lines, combo)
        } else {
            movesSinceClear += 1
            if (movesSinceClear >= Scoring.COMBO_WINDOW) combo = 0
        }
        val allClear = placement.lines > 0 && board.isClear
        if (allClear) points += Scoring.ALL_CLEAR_BONUS
        score += points

        val refilled = tray.all { it == null }
        if (refilled) refillTray()
        return MoveResult(
            placement = placement,
            points = points,
            combo = combo,
            allClear = allClear,
            praise = if (placement.lines > 0) Scoring.praise(placement.lines, combo, allClear) else null,
            trayRefilled = refilled,
        )
    }

    /**
     * Second chance after game over (offered for watching a rewarded ad): empties
     * the fullest rows and deals a fresh tray. Allowed once per game.
     */
    fun revive(): List<Pair<Cell, Int>> {
        check(canRevive) { "Revive is only allowed once, after game over" }
        revived = true
        val fullest = (0 until board.size)
            .sortedByDescending { r -> (0 until board.size).count { c -> !board.isEmpty(r, c) } }
            .take(REVIVE_ROWS)
        val cleared = board.clearRows(fullest)
        combo = 0
        movesSinceClear = 0
        refillTray()
        return cleared
    }

    private fun refillTray() {
        generator.nextTray(board).forEachIndexed { i, piece -> tray[i] = piece }
    }

    /** Saves the game as text so it can be resumed after the app is closed. */
    fun encode(): String = listOf(
        FORMAT,
        score.toString(),
        combo.toString(),
        movesSinceClear.toString(),
        if (revived) "1" else "0",
        board.encode(),
        tray.joinToString(",") { p -> if (p == null) "-" else "${p.shape.pattern}@${p.color}" },
    ).joinToString(";")

    companion object {
        private const val FORMAT = "v1"
        const val REVIVE_ROWS = 3

        /** Restores a game saved with [encode], or returns null if the text is unusable. */
        fun decode(text: String, generator: PieceGenerator): GameSession? {
            val parts = text.split(';')
            if (parts.size != 7 || parts[0] != FORMAT) return null
            val score = parts[1].toIntOrNull()?.takeIf { it >= 0 } ?: return null
            val combo = parts[2].toIntOrNull()?.takeIf { it >= 0 } ?: return null
            val moves = parts[3].toIntOrNull()?.takeIf { it >= 0 } ?: return null
            val revived = when (parts[4]) { "1" -> true; "0" -> false; else -> return null }
            val board = Board.decode(parts[5]) ?: return null
            val slots = parts[6].split(',')
            if (slots.size != PieceGenerator.TRAY_SIZE) return null
            val tray = arrayOfNulls<Piece>(PieceGenerator.TRAY_SIZE)
            slots.forEachIndexed { i, slot ->
                if (slot != "-") {
                    val shape = ShapeCatalog.byPattern(slot.substringBefore('@')) ?: return null
                    val color = slot.substringAfter('@').toIntOrNull()?.takeIf { it in 0 until ShapeCatalog.COLORS } ?: return null
                    tray[i] = Piece(shape, color)
                }
            }
            val session = GameSession(generator, board, tray, score, combo, moves, revived)
            if (tray.all { it == null }) session.refillTray()
            return session
        }
    }
}
