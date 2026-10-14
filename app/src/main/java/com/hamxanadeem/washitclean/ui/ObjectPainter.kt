package com.hamxanadeem.washitclean.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.hamxanadeem.washitclean.core.ObjectKind

/**
 * Draws the clean version of each object with simple shapes, so the game needs no
 * image assets. Coordinates are fractions of the target rectangle, which lets the
 * same drawing make both the on-screen object and the low-resolution dirt mask.
 */
object ObjectPainter {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()

    fun draw(canvas: Canvas, kind: ObjectKind, bounds: RectF) {
        val painter = Scaled(canvas, bounds)
        when (kind) {
            ObjectKind.CAR -> painter.car()
            ObjectKind.SNEAKER -> painter.sneaker()
            ObjectKind.PATIO -> painter.patio()
            ObjectKind.HOUSE -> painter.house()
            ObjectKind.BOAT -> painter.boat()
        }
    }

    private class Scaled(val canvas: Canvas, val b: RectF) {
        fun x(f: Float) = b.left + f * b.width()
        fun y(f: Float) = b.top + f * b.height()

        fun box(l: Float, t: Float, r: Float, bt: Float, color: Int, corner: Float = 0f) {
            paint.style = Paint.Style.FILL
            paint.color = color
            rect.set(x(l), y(t), x(r), y(bt))
            val c = corner * b.width()
            canvas.drawRoundRect(rect, c, c, paint)
        }

        fun circle(cx: Float, cy: Float, radius: Float, color: Int) {
            paint.style = Paint.Style.FILL
            paint.color = color
            canvas.drawCircle(x(cx), y(cy), radius * b.width(), paint)
        }

        fun polygon(color: Int, vararg points: Float) {
            paint.style = Paint.Style.FILL
            paint.color = color
            path.reset()
            path.moveTo(x(points[0]), y(points[1]))
            var i = 2
            while (i < points.size) {
                path.lineTo(x(points[i]), y(points[i + 1]))
                i += 2
            }
            path.close()
            canvas.drawPath(path, paint)
        }

        fun line(x0: Float, y0: Float, x1: Float, y1: Float, width: Float, color: Int) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = width * b.width()
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = color
            canvas.drawLine(x(x0), y(y0), x(x1), y(y1), paint)
        }

        fun car() {
            polygon(0xFFD32F2F.toInt(), 0.22f, 0.45f, 0.32f, 0.30f, 0.68f, 0.30f, 0.80f, 0.45f)
            polygon(0xFF90CAF9.toInt(), 0.29f, 0.44f, 0.36f, 0.33f, 0.48f, 0.33f, 0.48f, 0.44f)
            polygon(0xFF90CAF9.toInt(), 0.52f, 0.44f, 0.52f, 0.33f, 0.65f, 0.33f, 0.73f, 0.44f)
            box(0.04f, 0.44f, 0.96f, 0.62f, 0xFFE53935.toInt(), corner = 0.05f)
            box(0.06f, 0.48f, 0.13f, 0.52f, 0xFFFFF176.toInt(), corner = 0.01f)
            box(0.88f, 0.48f, 0.94f, 0.52f, 0xFFFF8A65.toInt(), corner = 0.01f)
            line(0.48f, 0.50f, 0.54f, 0.50f, 0.012f, 0xFF8E0000.toInt())
            for (cx in floatArrayOf(0.25f, 0.75f)) {
                circle(cx, 0.62f, 0.11f, 0xFF212121.toInt())
                circle(cx, 0.62f, 0.055f, 0xFFBDBDBD.toInt())
            }
        }

        fun sneaker() {
            polygon(
                0xFF1E88E5.toInt(),
                0.10f, 0.62f, 0.12f, 0.38f, 0.30f, 0.30f, 0.45f, 0.42f, 0.70f, 0.50f, 0.90f, 0.55f, 0.92f, 0.62f,
            )
            polygon(0xFF1565C0.toInt(), 0.12f, 0.38f, 0.30f, 0.30f, 0.28f, 0.45f, 0.12f, 0.50f)
            for (i in 0 until 4) {
                val lx = 0.34f + i * 0.07f
                line(lx, 0.40f + i * 0.02f, lx + 0.05f, 0.46f + i * 0.02f, 0.015f, 0xFFFFFFFF.toInt())
            }
            box(0.08f, 0.60f, 0.94f, 0.70f, 0xFFFAFAFA.toInt(), corner = 0.04f)
            line(0.10f, 0.67f, 0.92f, 0.67f, 0.01f, 0xFFB0BEC5.toInt())
        }

        fun patio() {
            val tile = 0.84f / 4
            for (row in 0 until 5) {
                for (col in 0 until 4) {
                    val l = 0.08f + col * tile
                    val t = 0.10f + row * tile
                    val color = if ((row + col) % 2 == 0) 0xFFBCAAA4.toInt() else 0xFFD7CCC8.toInt()
                    box(l + 0.008f, t + 0.006f, l + tile - 0.008f, t + tile * 0.75f - 0.006f, color, corner = 0.01f)
                }
            }
        }

        fun house() {
            polygon(0xFF6D4C41.toInt(), 0.06f, 0.40f, 0.50f, 0.12f, 0.94f, 0.40f)
            box(0.12f, 0.38f, 0.88f, 0.82f, 0xFFFFE0B2.toInt())
            box(0.42f, 0.58f, 0.58f, 0.82f, 0xFF8D6E63.toInt(), corner = 0.01f)
            circle(0.55f, 0.70f, 0.012f, 0xFFFFD54F.toInt())
            for (wx in floatArrayOf(0.18f, 0.66f)) {
                box(wx, 0.46f, wx + 0.16f, 0.58f, 0xFF81D4FA.toInt())
                line(wx + 0.08f, 0.46f, wx + 0.08f, 0.58f, 0.01f, 0xFFFFFFFF.toInt())
                line(wx, 0.52f, wx + 0.16f, 0.52f, 0.01f, 0xFFFFFFFF.toInt())
            }
            box(0.70f, 0.14f, 0.78f, 0.28f, 0xFF8D6E63.toInt())
        }

        fun boat() {
            polygon(0xFFFFFFFF.toInt(), 0.52f, 0.12f, 0.52f, 0.50f, 0.86f, 0.50f)
            polygon(0xFFFFCC80.toInt(), 0.48f, 0.18f, 0.48f, 0.50f, 0.22f, 0.50f)
            line(0.50f, 0.10f, 0.50f, 0.56f, 0.015f, 0xFF5D4037.toInt())
            box(0.30f, 0.50f, 0.66f, 0.58f, 0xFFECEFF1.toInt(), corner = 0.01f)
            polygon(0xFF0277BD.toInt(), 0.04f, 0.56f, 0.96f, 0.56f, 0.82f, 0.74f, 0.18f, 0.74f)
            line(0.10f, 0.62f, 0.92f, 0.62f, 0.015f, 0xFFFFFFFF.toInt())
            for (px in floatArrayOf(0.30f, 0.50f, 0.70f)) circle(px, 0.67f, 0.025f, 0xFFE1F5FE.toInt())
        }
    }
}
