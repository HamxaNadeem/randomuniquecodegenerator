package com.hamxanadeem.blockygems.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** The title: a wave of bobbing gems above "BLOCKY GEMS" in glossy lettering. */
class LogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val dp = resources.displayMetrics.density
    private val gems = GemRenderer()
    private val rect = RectF()
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val black = Typeface.create("sans-serif-black", Typeface.NORMAL)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = black
        textAlign = Paint.Align.CENTER
    }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = black
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        color = 0xFF2A0E4F.toInt()
    }
    private var topShader: Shader? = null
    private var bottomShader: Shader? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val textSize = min(w / 6.2f, h * 0.3f)
        fill.textSize = textSize
        stroke.textSize = textSize
        stroke.strokeWidth = textSize * 0.14f
        val line1 = h * 0.62f
        val line2 = h * 0.95f
        topShader = LinearGradient(0f, line1 - textSize, 0f, line1, 0xFFFFF59D.toInt(), 0xFFFF9800.toInt(), Shader.TileMode.CLAMP)
        bottomShader = LinearGradient(0f, line2 - textSize, 0f, line2, 0xFF84FFFF.toInt(), 0xFFB13DF2.toInt(), Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        val t = SystemClock.uptimeMillis() / 1000f
        val size = min(width / 9f, height * 0.22f)
        val bitmap = { color: Int -> gems.bitmap(color, size.roundToInt()) }
        val count = 7
        val startX = (width - count * size * 1.1f) / 2f
        for (i in 0 until count) {
            val bob = sin(t * 3f + i * 0.7f) * size * 0.18f
            val x = startX + i * size * 1.1f
            val y = height * 0.04f + bob
            rect.set(x, y, x + size, y + size)
            canvas.drawBitmap(bitmap(i), null, rect, bitmapPaint)
        }
        drawWord(canvas, "BLOCKY", height * 0.62f, topShader)
        drawWord(canvas, "GEMS", height * 0.95f, bottomShader)
        if (isShown) postInvalidateOnAnimation()
    }

    private fun drawWord(canvas: Canvas, word: String, baseline: Float, shader: Shader?) {
        canvas.drawText(word, width / 2f, baseline, stroke)
        fill.shader = shader
        canvas.drawText(word, width / 2f, baseline, fill)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) postInvalidateOnAnimation()
    }
}
