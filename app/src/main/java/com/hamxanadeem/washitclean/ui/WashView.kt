package com.hamxanadeem.washitclean.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.createBitmap
import com.hamxanadeem.washitclean.core.DirtGenerator
import com.hamxanadeem.washitclean.core.DirtGrid
import com.hamxanadeem.washitclean.core.Level
import com.hamxanadeem.washitclean.core.Nozzle
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * The play area: draws a dirty object and lets the player wash it by dragging a finger.
 * The water hits a little above the finger so the finger doesn't hide the dirt.
 */
class WashView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    interface Listener {
        fun onProgress(cleanFraction: Float)
        fun onCleaned()
    }

    var listener: Listener? = null

    private var level: Level? = null
    private var nozzle: Nozzle? = null
    private var grid: DirtGrid? = null
    private var dirtBitmap: Bitmap? = null
    private var dirtPixels = IntArray(0)
    private var finished = false

    private val objectRect = RectF()
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 120, 200, 255)
        strokeCap = Paint.Cap.ROUND
    }
    private val dropPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    private val sprayOffset = 56f * resources.displayMetrics.density
    private var spraying = false
    private var sprayX = 0f
    private var sprayY = 0f
    private var lastGridX = 0f
    private var lastGridY = 0f
    private var lastSprayTime = 0L
    private var lastHapticTime = 0L
    private var finishedAt = 0L

    private val drops = ArrayList<Drop>()
    private val random = Random(SystemClock.uptimeMillis())

    private class Drop(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float)

    fun start(level: Level, nozzle: Nozzle) {
        this.level = level
        this.nozzle = nozzle
        finished = false
        drops.clear()

        // Paint the object at grid resolution and treat its opaque pixels as the surface
        // that gets dirty, so the background never needs cleaning.
        val w = level.gridWidth
        val h = level.gridHeight
        val mask = createBitmap(w, h)
        ObjectPainter.draw(Canvas(mask), level.kind, RectF(0f, 0f, w.toFloat(), h.toFloat()))
        val maskPixels = IntArray(w * h)
        mask.getPixels(maskPixels, 0, w, 0, 0, w, h)
        mask.recycle()

        val dirt = DirtGenerator.generate(w, h, level.seed, level.toughness) { x, y ->
            Color.alpha(maskPixels[y * w + x]) > 40
        }
        grid = DirtGrid(w, h, dirt)
        dirtPixels = IntArray(w * h)
        dirtBitmap?.recycle()
        dirtBitmap = createBitmap(w, h)
        for (y in 0 until h) for (x in 0 until w) updatePixel(x, y)
        dirtBitmap?.setPixels(dirtPixels, 0, w, 0, 0, w, h)
        listener?.onProgress(0f)
        invalidate()
    }

    fun setNozzle(nozzle: Nozzle) {
        this.nozzle = nozzle
    }

    private fun updatePixel(x: Int, y: Int) {
        val grid = grid ?: return
        val level = level ?: return
        val amount = (grid.dirtAt(x, y) / level.toughness).coerceIn(0f, 1f)
        dirtPixels[y * grid.width + x] = if (amount <= 0f) {
            Color.TRANSPARENT
        } else {
            // Thick dirt is dark mud; thin leftovers are light and see-through.
            val alpha = (60 + 195 * amount).toInt()
            val red = (150 - 60 * amount).toInt()
            val green = (125 - 55 * amount).toInt()
            val blue = (90 - 45 * amount).toInt()
            Color.argb(alpha, red, green, blue)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val level = level
        val gridW = (level?.gridWidth ?: Level.GRID_WIDTH).toFloat()
        val gridH = (level?.gridHeight ?: Level.GRID_HEIGHT).toFloat()
        val scale = min(w / gridW, h / gridH)
        val left = (w - gridW * scale) / 2f
        val top = (h - gridH * scale) / 2f
        objectRect.set(left, top, left + gridW * scale, top + gridH * scale)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val level = level ?: return
        ObjectPainter.draw(canvas, level.kind, objectRect)
        dirtBitmap?.let { canvas.drawBitmap(it, null, objectRect, bitmapPaint) }

        if (spraying && !finished) {
            waterPaint.strokeWidth = 10f * resources.displayMetrics.density
            canvas.drawLine(width / 2f, height.toFloat(), sprayX, sprayY, waterPaint)
        }
        val alive = updateAndDrawDrops(canvas)
        val sparkling = finished && drawSparkles(canvas)
        if (alive || sparkling || spraying) postInvalidateOnAnimation()
    }

    private fun updateAndDrawDrops(canvas: Canvas): Boolean {
        val iterator = drops.iterator()
        while (iterator.hasNext()) {
            val d = iterator.next()
            d.x += d.vx
            d.y += d.vy
            d.vy += 0.6f
            d.life -= 0.05f
            if (d.life <= 0f) {
                iterator.remove()
                continue
            }
            dropPaint.color = Color.argb((200 * d.life).toInt(), 180, 225, 255)
            canvas.drawCircle(d.x, d.y, 3f * resources.displayMetrics.density * d.life + 1f, dropPaint)
        }
        return drops.isNotEmpty()
    }

    private fun drawSparkles(canvas: Canvas): Boolean {
        val elapsed = SystemClock.uptimeMillis() - finishedAt
        if (elapsed > SPARKLE_MILLIS) return false
        val progress = elapsed / SPARKLE_MILLIS.toFloat()
        sparklePaint.alpha = (255 * (1f - progress)).toInt()
        val sparkleRandom = Random(level?.seed ?: 0L)
        repeat(14) {
            val sx = objectRect.left + sparkleRandom.nextFloat() * objectRect.width()
            val sy = objectRect.top + sparkleRandom.nextFloat() * objectRect.height()
            val size = (6f + 10f * sin(progress * Math.PI.toFloat() * 2f + it)) * resources.displayMetrics.density
            sparklePaint.strokeWidth = size / 4f
            canvas.drawLine(sx - size, sy, sx + size, sy, sparklePaint)
            canvas.drawLine(sx, sy - size, sx, sy + size, sparklePaint)
        }
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (grid == null || finished) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                spraying = true
                parent?.requestDisallowInterceptTouchEvent(true)
                moveSprayTo(event.x, event.y)
                lastGridX = toGridX(sprayX)
                lastGridY = toGridY(sprayY)
                lastSprayTime = SystemClock.uptimeMillis()
                sprayTo(lastGridX, lastGridY)
                postInvalidateOnAnimation()
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.historySize) {
                    moveSprayTo(event.getHistoricalX(i), event.getHistoricalY(i))
                    sprayTo(toGridX(sprayX), toGridY(sprayY))
                }
                moveSprayTo(event.x, event.y)
                sprayTo(toGridX(sprayX), toGridY(sprayY))
            }
            MotionEvent.ACTION_UP -> {
                spraying = false
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> spraying = false
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    private fun moveSprayTo(x: Float, y: Float) {
        sprayX = x
        sprayY = y - sprayOffset
    }

    private fun toGridX(x: Float) = (x - objectRect.left) / objectRect.width() * (grid?.width ?: 1)
    private fun toGridY(y: Float) = (y - objectRect.top) / objectRect.height() * (grid?.height ?: 1)

    private fun sprayTo(gx: Float, gy: Float) {
        val grid = grid ?: return
        val nozzle = nozzle ?: return
        val now = SystemClock.uptimeMillis()
        // Scale by elapsed time so cleaning speed doesn't depend on the touch sample rate.
        val timeScale = ((now - lastSprayTime) / FRAME_MILLIS).coerceIn(0.25f, 3f)
        lastSprayTime = now

        val removed = grid.sprayStroke(lastGridX, lastGridY, gx, gy, nozzle.radius, nozzle.power * timeScale) { x, y ->
            updatePixel(x, y)
        }
        lastGridX = gx
        lastGridY = gy
        if (removed <= 0f) return

        dirtBitmap?.setPixels(dirtPixels, 0, grid.width, 0, 0, grid.width, grid.height)
        addSplash()
        if (now - lastHapticTime > HAPTIC_INTERVAL_MILLIS) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            lastHapticTime = now
        }
        if (grid.isClean()) {
            finish()
        } else {
            listener?.onProgress(grid.cleanFraction)
        }
    }

    private fun addSplash() {
        if (drops.size > MAX_DROPS) return
        repeat(3) {
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 3f + random.nextFloat() * 6f
            drops += Drop(sprayX, sprayY, cos(angle) * speed, sin(angle) * speed - 4f, 1f)
        }
    }

    private fun finish() {
        val grid = grid ?: return
        grid.clearAll { x, y -> updatePixel(x, y) }
        dirtBitmap?.setPixels(dirtPixels, 0, grid.width, 0, 0, grid.width, grid.height)
        finished = true
        spraying = false
        finishedAt = SystemClock.uptimeMillis()
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        listener?.onProgress(1f)
        listener?.onCleaned()
        postInvalidateOnAnimation()
    }

    private companion object {
        const val FRAME_MILLIS = 1000f / 60f
        const val HAPTIC_INTERVAL_MILLIS = 90L
        const val SPARKLE_MILLIS = 1400L
        const val MAX_DROPS = 160
    }
}
