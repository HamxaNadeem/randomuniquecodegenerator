package com.hamxanadeem.blockygems.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

/** A deep purple night sky with softly glowing orbs drifting upwards. */
class BackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private class Orb(var x: Float, var y: Float, val radius: Float, val speed: Float, val phase: Float, color: Int) {
        // Built once, centred on the origin; drawing translates the canvas instead.
        val shader = RadialGradient(
            0f, 0f, radius,
            GemPalette.withAlpha(color, 70), GemPalette.withAlpha(color, 0),
            Shader.TileMode.CLAMP,
        )
    }

    private val skyPaint = Paint()
    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val orbs = ArrayList<Orb>()
    private val random = Random(42)
    private var lastFrame = 0L

    /** Set false to save battery when the view is hidden. */
    var animating = true
        set(value) {
            field = value
            if (value) postInvalidateOnAnimation()
        }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        skyPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(0xFF2A1260.toInt(), 0xFF1B1A5C.toInt(), 0xFF0E2A5C.toInt()),
            null,
            Shader.TileMode.CLAMP,
        )
        orbs.clear()
        val density = resources.displayMetrics.density
        repeat(ORB_COUNT) {
            orbs += Orb(
                x = random.nextFloat() * w,
                y = random.nextFloat() * h,
                radius = (20 + random.nextFloat() * 70) * density,
                speed = (6 + random.nextFloat() * 14) * density,
                phase = random.nextFloat() * 6.28f,
                color = GemPalette[random.nextInt(GemPalette.gems.size)].base,
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), skyPaint)
        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1000f).coerceAtMost(0.05f)
        lastFrame = now
        val t = now / 1000f
        for (orb in orbs) {
            orb.y -= orb.speed * dt
            if (orb.y < -orb.radius) {
                orb.y = height + orb.radius
                orb.x = random.nextFloat() * width
            }
            val x = orb.x + sin(t * 0.5f + orb.phase) * orb.radius * 0.4f
            orbPaint.shader = orb.shader
            val save = canvas.save()
            canvas.translate(x, orb.y)
            canvas.drawCircle(0f, 0f, orb.radius, orbPaint)
            canvas.restoreToCount(save)
        }
        if (animating && isShown) postInvalidateOnAnimation()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            lastFrame = 0L
            postInvalidateOnAnimation()
        }
    }

    private companion object {
        const val ORB_COUNT = 14
    }
}
