package com.hamxanadeem.blockygems.core

object Scoring {

    /** A combo ends after this many placements in a row that clear nothing. */
    const val COMBO_WINDOW = 3

    const val ALL_CLEAR_BONUS = 300

    /** One point per block placed. */
    fun placementPoints(blocks: Int): Int = blocks

    /** Clearing more lines at once is worth much more; combos multiply it. */
    fun clearPoints(lines: Int, combo: Int): Int =
        if (lines <= 0) 0 else 40 * lines * (lines + 1) * maxOf(1, combo)

    /** The cheer shown after a clear, or null for an ordinary one. */
    fun praise(lines: Int, combo: Int, allClear: Boolean): String? = when {
        allClear -> "All Clear!"
        lines >= 4 || combo >= 6 -> "Unbelievable!"
        lines == 3 || combo >= 4 -> "Amazing!"
        lines == 2 || combo >= 3 -> "Excellent!"
        combo == 2 -> "Great!"
        else -> null
    }
}
