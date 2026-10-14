package com.hamxanadeem.washitclean.core

import kotlin.random.Random

/** Creates natural-looking, blotchy dirt that is the same every time for a given seed. */
object DirtGenerator {

    private const val LATTICE_SPACING = 12

    /**
     * Returns `width * height` dirt values. Cells where [isSurface] is false (the
     * background around the object) stay clean. Dirty cells get values between
     * 50% and 100% of [toughness].
     */
    fun generate(
        width: Int,
        height: Int,
        seed: Long,
        toughness: Float,
        isSurface: (x: Int, y: Int) -> Boolean,
    ): FloatArray {
        require(toughness > 0f) { "Toughness must be positive" }
        val random = Random(seed)
        val cols = width / LATTICE_SPACING + 2
        val rows = height / LATTICE_SPACING + 2
        val lattice = FloatArray(cols * rows) { random.nextFloat() }
        val result = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (!isSurface(x, y)) continue
                val noise = 0.8f * smoothNoise(lattice, cols, x, y) + 0.2f * random.nextFloat()
                result[y * width + x] = toughness * (0.5f + 0.5f * noise)
            }
        }
        return result
    }

    private fun smoothNoise(lattice: FloatArray, cols: Int, x: Int, y: Int): Float {
        val gx = x.toFloat() / LATTICE_SPACING
        val gy = y.toFloat() / LATTICE_SPACING
        val x0 = gx.toInt()
        val y0 = gy.toInt()
        val tx = smoothStep(gx - x0)
        val ty = smoothStep(gy - y0)
        fun at(cx: Int, cy: Int) = lattice[cy * cols + cx]
        val top = lerp(at(x0, y0), at(x0 + 1, y0), tx)
        val bottom = lerp(at(x0, y0 + 1), at(x0 + 1, y0 + 1), tx)
        return lerp(top, bottom, ty)
    }

    private fun smoothStep(t: Float) = t * t * (3f - 2f * t)

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
