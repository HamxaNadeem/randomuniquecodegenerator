package com.hamxanadeem.washitclean.core

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * The layer of dirt covering an object, stored as one value per grid cell.
 * A cell value of 0 is clean; larger values need more spraying to remove.
 */
class DirtGrid(val width: Int, val height: Int, initial: FloatArray) {

    private val dirt: FloatArray = initial.copyOf()
    private val initialTotal: Double
    private var remainingTotal: Double

    init {
        require(width > 0 && height > 0) { "Grid must not be empty" }
        require(initial.size == width * height) { "Expected ${width * height} cells, got ${initial.size}" }
        require(initial.all { it >= 0f }) { "Dirt values must not be negative" }
        initialTotal = initial.sumOf { it.toDouble() }
        remainingTotal = initialTotal
    }

    fun dirtAt(x: Int, y: Int): Float = dirt[y * width + x]

    /** Fraction of the original dirt removed so far, from 0 to 1. */
    val cleanFraction: Float
        get() = if (initialTotal <= 0.0) 1f else (1.0 - remainingTotal / initialTotal).toFloat().coerceIn(0f, 1f)

    fun isClean(threshold: Float = COMPLETE_THRESHOLD): Boolean = cleanFraction >= threshold

    /**
     * Sprays water centred on ([cx], [cy]) in cell coordinates. The spray is strongest
     * at the centre and fades to nothing at [radius]. Returns the amount of dirt removed.
     * [onCellChanged] is called for every cell whose value changed.
     */
    fun spray(
        cx: Float,
        cy: Float,
        radius: Float,
        power: Float,
        onCellChanged: ((x: Int, y: Int) -> Unit)? = null,
    ): Float {
        if (radius <= 0f || power <= 0f) return 0f
        val minX = max(0, floor(cx - radius).toInt())
        val maxX = min(width - 1, ceil(cx + radius).toInt())
        val minY = max(0, floor(cy - radius).toInt())
        val maxY = min(height - 1, ceil(cy + radius).toInt())
        var removed = 0f
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                // Distance from the spray centre to the centre of the cell.
                val d = hypot(x + 0.5f - cx, y + 0.5f - cy) / radius
                if (d >= 1f) continue
                val index = y * width + x
                val before = dirt[index]
                if (before <= 0f) continue
                val after = max(0f, before - power * (1f - d * d))
                dirt[index] = after
                removed += before - after
                onCellChanged?.invoke(x, y)
            }
        }
        remainingTotal = max(0.0, remainingTotal - removed)
        return removed
    }

    /**
     * Sprays along the segment from ([x0], [y0]) to ([x1], [y1]) so a fast swipe leaves
     * no gaps. The segment's end point is sprayed; its start point is not, because it
     * was the end point of the previous stroke.
     */
    fun sprayStroke(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        radius: Float,
        power: Float,
        onCellChanged: ((x: Int, y: Int) -> Unit)? = null,
    ): Float {
        val length = hypot(x1 - x0, y1 - y0)
        val step = max(radius / 2f, 0.5f)
        val steps = max(1, ceil(length / step).toInt())
        var removed = 0f
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            removed += spray(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, radius, power, onCellChanged)
        }
        return removed
    }

    /** Removes every remaining speck, used to finish a level once it is nearly clean. */
    fun clearAll(onCellChanged: ((x: Int, y: Int) -> Unit)? = null) {
        for (i in dirt.indices) {
            if (dirt[i] > 0f) {
                dirt[i] = 0f
                onCellChanged?.invoke(i % width, i / width)
            }
        }
        remainingTotal = 0.0
    }

    companion object {
        /** A level counts as clean once this fraction of the dirt is gone. */
        const val COMPLETE_THRESHOLD = 0.97f
    }
}
