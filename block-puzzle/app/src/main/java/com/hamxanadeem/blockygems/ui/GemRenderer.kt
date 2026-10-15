package com.hamxanadeem.blockygems.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.createBitmap

/**
 * Pre-renders a glossy, bevelled gem block for every colour at a given size.
 * Drawing a cached bitmap is much faster than shading every block every frame.
 */
class GemRenderer {

    private var cellSize = 0
    private var bitmaps: List<Bitmap> = emptyList()

    fun bitmap(color: Int, size: Int): Bitmap {
        if (size != cellSize || bitmaps.isEmpty()) {
            bitmaps.forEach { it.recycle() }
            cellSize = size
            bitmaps = GemPalette.gems.map { render(it, maxOf(size, 4)) }
        }
        return bitmaps[color.mod(bitmaps.size)]
    }

    private fun render(gem: GemPalette.Gem, size: Int): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val s = size.toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val inset = s * 0.04f
        val corner = s * 0.2f
        val outer = RectF(inset, inset, s - inset, s - inset)

        // Body: light at the top, deep at the bottom.
        paint.shader = LinearGradient(0f, inset, 0f, s - inset, gem.light, gem.dark, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(outer, corner, corner, paint)

        // Raised face, which makes the bevel around it.
        val face = RectF(s * 0.16f, s * 0.14f, s * 0.84f, s * 0.8f)
        paint.shader = LinearGradient(0f, face.top, 0f, face.bottom, gem.base, gem.light, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(face, corner * 0.6f, corner * 0.6f, paint)

        // Gloss across the upper half.
        paint.shader = LinearGradient(0f, inset, 0f, s * 0.5f, 0x88FFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(inset * 2, inset * 2, s - inset * 2, s * 0.5f), corner, corner, paint)

        // A small sparkle in the corner.
        val sparkleX = s * 0.3f
        val sparkleY = s * 0.28f
        paint.shader = RadialGradient(sparkleX, sparkleY, s * 0.12f, 0xEEFFFFFF.toInt(), 0x00FFFFFF, Shader.TileMode.CLAMP)
        canvas.drawCircle(sparkleX, sparkleY, s * 0.12f, paint)

        // Thin dark rim to separate neighbouring blocks.
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, s * 0.03f)
        paint.color = gem.dark
        canvas.drawRoundRect(outer, corner, corner, paint)
        return bitmap
    }
}
