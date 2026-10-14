package com.hamxanadeem.blockygems.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.hamxanadeem.blockygems.core.Board
import com.hamxanadeem.blockygems.core.Cell
import com.hamxanadeem.blockygems.core.GameSession
import com.hamxanadeem.blockygems.core.MoveResult
import com.hamxanadeem.blockygems.core.Piece
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * The whole play screen: score, board, the tray of three pieces, drag and drop,
 * and every effect. Everything is drawn on one canvas with cached gem bitmaps and
 * allocation-free particles so it stays at the display's full frame rate.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    interface Listener {
        fun onPickUp()
        fun onMove(result: MoveResult)
        fun onInvalidDrop()
        fun onGameOver()
    }

    var listener: Listener? = null
    var bestScore: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    private var session: GameSession? = null
    private val dp = resources.displayMetrics.density
    private val random = Random(SystemClock.uptimeMillis())

    // ---- Layout ----
    private var cell = 0f
    private var boardLeft = 0f
    private var boardTop = 0f
    private var boardSide = 0f
    private var trayTop = 0f
    private var trayCell = 0f
    private var trayCenterY = 0f
    private var hudHeight = 0f

    // ---- Drawing helpers ----
    private val boardGems = GemRenderer()
    private val trayGems = GemRenderer()
    private val rect = RectF()
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x8C0B0830.toInt() }
    private val panelStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x40FFFFFF
    }
    private val slotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1AFFFFFF }
    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val crownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFC107.toInt() }
    private val crownPath = Path()
    private val black = Typeface.create("sans-serif-black", Typeface.NORMAL)
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        typeface = black
        textAlign = Paint.Align.CENTER
    }
    private val bestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFE082.toInt()
        typeface = black
    }
    private val textFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = black
        textAlign = Paint.Align.CENTER
    }
    private val textStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = black
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        color = 0xFF2A0E4F.toInt()
    }

    // ---- Dragging ----
    private var dragSlot = -1
    private var dragX = 0f
    private var dragY = 0f
    private var targetRow = 0
    private var targetCol = 0
    private var targetValid = false
    private var previewRows: List<Int> = emptyList()
    private var previewCols: List<Int> = emptyList()

    private var returnSlot = -1
    private var returnFromX = 0f
    private var returnFromY = 0f
    private var returnStart = 0L

    // ---- Effects ----
    private class Fading(val row: Int, val col: Int, val color: Int, val start: Long)
    private class FloatingText(val text: String, val x: Float, val y: Float, val big: Boolean, val start: Long, val shader: Shader?)

    private val pops = ArrayList<Fading>()
    private val clearing = ArrayList<Fading>()
    private var flashRows: List<Int> = emptyList()
    private var flashCols: List<Int> = emptyList()
    private var flashStart = 0L
    private val texts = ArrayList<FloatingText>()
    private var shakeStart = 0L
    private var shakeStrength = 0f
    private var trayAppearAt = 0L
    private var displayedScore = 0f
    private var lastFrame = 0L

    // Particles stored as parallel arrays so nothing is allocated per frame.
    private val px = FloatArray(MAX_PARTICLES)
    private val py = FloatArray(MAX_PARTICLES)
    private val pvx = FloatArray(MAX_PARTICLES)
    private val pvy = FloatArray(MAX_PARTICLES)
    private val pLife = FloatArray(MAX_PARTICLES)
    private val pSize = FloatArray(MAX_PARTICLES)
    private val pColor = IntArray(MAX_PARTICLES)
    private var particleCount = 0

    fun setSession(newSession: GameSession) {
        session = newSession
        dragSlot = -1
        returnSlot = -1
        pops.clear()
        clearing.clear()
        texts.clear()
        particleCount = 0
        flashRows = emptyList()
        flashCols = emptyList()
        displayedScore = newSession.score.toFloat()
        trayAppearAt = SystemClock.uptimeMillis()
        if (newSession.isGameOver) post { listener?.onGameOver() }
        postInvalidateOnAnimation()
    }

    /** Plays the revive effect: cleared rows burst like a line clear, then a fresh tray drops in. */
    fun showRevive(cleared: List<Pair<Cell, Int>>) {
        val now = SystemClock.uptimeMillis()
        cleared.forEach { (c, color) ->
            clearing += Fading(c.row, c.col, color, now + c.col * 25L)
            burst(cellCenterX(c.col), cellCenterY(c.row), color, 6)
        }
        trayAppearAt = now
        addText("Second chance!", width / 2f, boardTop + boardSide / 2f, big = true)
        postInvalidateOnAnimation()
    }

    // ---------------------------------------------------------------- layout

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val pad = 16 * dp
        hudHeight = 104 * dp
        val available = h - hudHeight - pad * 2
        boardSide = min(w - pad * 2, available / 1.45f)
        cell = boardSide / Board.SIZE
        boardLeft = (w - boardSide) / 2f
        boardTop = hudHeight + pad
        trayTop = boardTop + boardSide + 20 * dp
        val trayHeight = h - trayTop - pad
        trayCell = min(min(cell * 0.6f, trayHeight / 5.6f), w / 3f / 5.6f)
        trayCenterY = trayTop + min(trayHeight, trayCell * 6f) / 2f
        panelStroke.strokeWidth = 1.5f * dp
        scorePaint.textSize = 52 * dp
        scorePaint.setShadowLayer(8 * dp, 0f, 3 * dp, 0x80000000.toInt())
        bestPaint.textSize = 20 * dp
    }

    private fun cellCenterX(col: Int) = boardLeft + (col + 0.5f) * cell
    private fun cellCenterY(row: Int) = boardTop + (row + 0.5f) * cell
    private fun slotCenterX(slot: Int) = width * (slot * 2 + 1) / 6f

    // ---------------------------------------------------------------- drawing

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val session = session ?: return
        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrame == 0L) 0.016f else ((now - lastFrame) / 1000f).coerceIn(0f, 0.05f)
        lastFrame = now
        var animating = false

        val save = canvas.save()
        val shakeT = (now - shakeStart) / SHAKE_MILLIS.toFloat()
        if (shakeT < 1f) {
            val amount = shakeStrength * (1f - shakeT)
            canvas.translate((random.nextFloat() - 0.5f) * amount, (random.nextFloat() - 0.5f) * amount)
            animating = true
        }

        animating = drawHud(canvas, session, dt) || animating
        drawBoard(canvas, session, now)
        animating = drawClearing(canvas, now) || animating
        animating = drawFlashes(canvas, now) || animating
        animating = drawTray(canvas, session, now) || animating
        animating = drawParticles(canvas, dt) || animating
        animating = drawDragged(canvas, session, now) || animating
        animating = drawTexts(canvas, now) || animating
        canvas.restoreToCount(save)

        if (animating || pops.isNotEmpty()) postInvalidateOnAnimation() else lastFrame = 0L
    }

    private fun drawHud(canvas: Canvas, session: GameSession, dt: Float): Boolean {
        val target = session.score.toFloat()
        val counting = displayedScore < target
        if (counting) {
            displayedScore += maxOf(1f, (target - displayedScore) * min(1f, dt * 8f))
            if (displayedScore > target) displayedScore = target
        } else {
            displayedScore = target
        }
        canvas.drawText(displayedScore.roundToInt().toString(), width / 2f, hudHeight * 0.82f, scorePaint)

        // Crown and best score in the top-left corner.
        val x = 20 * dp
        val y = 30 * dp
        val s = 22 * dp
        crownPath.reset()
        crownPath.moveTo(x, y + s * 0.75f)
        crownPath.lineTo(x, y + s * 0.15f)
        crownPath.lineTo(x + s * 0.3f, y + s * 0.45f)
        crownPath.lineTo(x + s * 0.5f, y)
        crownPath.lineTo(x + s * 0.7f, y + s * 0.45f)
        crownPath.lineTo(x + s, y + s * 0.15f)
        crownPath.lineTo(x + s, y + s * 0.75f)
        crownPath.close()
        canvas.drawPath(crownPath, crownPaint)
        canvas.drawText(maxOf(bestScore, session.score).toString(), x + s + 8 * dp, y + s * 0.72f, bestPaint)
        return counting
    }

    private fun drawBoard(canvas: Canvas, session: GameSession, now: Long) {
        val board = session.board
        val pad = 6 * dp
        rect.set(boardLeft - pad, boardTop - pad, boardLeft + boardSide + pad, boardTop + boardSide + pad)
        canvas.drawRoundRect(rect, 14 * dp, 14 * dp, panelPaint)
        canvas.drawRoundRect(rect, 14 * dp, 14 * dp, panelStroke)

        val size = cell.roundToInt()
        val dragging = dragSlot >= 0 && targetValid
        val dragColor = if (dragging) session.trayPiece(dragSlot)?.color else null
        val inset = cell * 0.05f
        val corner = cell * 0.18f

        pops.removeAll { now - it.start > POP_MILLIS }
        for (r in 0 until board.size) {
            for (c in 0 until board.size) {
                val left = boardLeft + c * cell
                val top = boardTop + r * cell
                val color = board[r, c]
                if (color == Board.EMPTY) {
                    rect.set(left + inset, top + inset, left + cell - inset, top + cell - inset)
                    canvas.drawRoundRect(rect, corner, corner, slotPaint)
                    continue
                }
                // Lines about to clear light up in the colour of the dragged piece.
                val previewed = dragColor != null && (r in previewRows || c in previewCols)
                val drawColor = if (previewed) dragColor!! else color
                var scale = 1f
                for (p in pops) {
                    if (p.row == r && p.col == c) {
                        val t = (now - p.start) / POP_MILLIS.toFloat()
                        scale = 1f + 0.18f * sin(t * Math.PI.toFloat())
                    }
                }
                drawGem(canvas, boardGems.bitmap(drawColor, size), left, top, cell, scale, 255)
            }
        }

        // Ghost of the dragged piece where it would land.
        if (dragging) {
            val piece = session.trayPiece(dragSlot) ?: return
            val ghostAlpha = if (previewRows.isNotEmpty() || previewCols.isNotEmpty()) 255 else 110
            for (pc in piece.shape.cells) {
                val left = boardLeft + (targetCol + pc.col) * cell
                val top = boardTop + (targetRow + pc.row) * cell
                drawGem(canvas, boardGems.bitmap(piece.color, size), left, top, cell, 1f, ghostAlpha)
            }
        }
    }

    private fun drawGem(canvas: Canvas, bitmap: Bitmap, left: Float, top: Float, size: Float, scale: Float, alpha: Int) {
        val half = size * scale / 2f
        val cx = left + size / 2f
        val cy = top + size / 2f
        rect.set(cx - half, cy - half, cx + half, cy + half)
        bitmapPaint.alpha = alpha
        canvas.drawBitmap(bitmap, null, rect, bitmapPaint)
    }

    private fun drawClearing(canvas: Canvas, now: Long): Boolean {
        if (clearing.isEmpty()) return false
        val size = cell.roundToInt()
        val iterator = clearing.iterator()
        while (iterator.hasNext()) {
            val f = iterator.next()
            val t = (now - f.start) / CLEAR_MILLIS.toFloat()
            if (t >= 1f) {
                iterator.remove()
                continue
            }
            if (t < 0f) {
                // Waiting for the wave to reach this cell: show it still in place.
                drawGem(canvas, boardGems.bitmap(f.color, size), boardLeft + f.col * cell, boardTop + f.row * cell, cell, 1f, 255)
                continue
            }
            val scale = if (t < 0.25f) 1f + t * 0.8f else 1.2f * (1f - (t - 0.25f) / 0.75f)
            drawGem(canvas, boardGems.bitmap(f.color, size), boardLeft + f.col * cell, boardTop + f.row * cell, cell, scale, (255 * (1f - t)).toInt())
        }
        return true
    }

    private fun drawFlashes(canvas: Canvas, now: Long): Boolean {
        val t = (now - flashStart) / FLASH_MILLIS.toFloat()
        if (t >= 1f || (flashRows.isEmpty() && flashCols.isEmpty())) return false
        flashPaint.color = GemPalette.withAlpha(0xFFFFFFFF.toInt(), (200 * (1f - t)).toInt())
        val grow = cell * 0.15f * t
        for (r in flashRows) {
            rect.set(boardLeft - grow, boardTop + r * cell - grow, boardLeft + boardSide + grow, boardTop + (r + 1) * cell + grow)
            canvas.drawRoundRect(rect, cell * 0.3f, cell * 0.3f, flashPaint)
        }
        for (c in flashCols) {
            rect.set(boardLeft + c * cell - grow, boardTop - grow, boardLeft + (c + 1) * cell + grow, boardTop + boardSide + grow)
            canvas.drawRoundRect(rect, cell * 0.3f, cell * 0.3f, flashPaint)
        }
        return true
    }

    private fun drawTray(canvas: Canvas, session: GameSession, now: Long): Boolean {
        val appearT = (now - trayAppearAt) / APPEAR_MILLIS.toFloat()
        val appearing = appearT < 1f
        // Overshooting ease so new pieces "pop" into the tray.
        val appearScale = if (appearing) easeOutBack(appearT.coerceAtLeast(0f)) else 1f
        val size = trayCell.roundToInt()
        for (slot in 0 until 3) {
            if (slot == dragSlot || slot == returnSlot) continue
            val piece = session.trayPiece(slot) ?: continue
            val fits = session.board.fits(piece.shape)
            drawPieceCentered(canvas, piece, slotCenterX(slot), trayCenterY, trayCell * appearScale, size, if (fits) 255 else 80)
        }
        return appearing
    }

    private fun drawPieceCentered(canvas: Canvas, piece: Piece, cx: Float, cy: Float, cellSize: Float, bitmapSize: Int, alpha: Int) {
        if (cellSize <= 0f) return
        val left = cx - piece.shape.cols * cellSize / 2f
        val top = cy - piece.shape.rows * cellSize / 2f
        val renderer = if (bitmapSize == trayCell.roundToInt()) trayGems else boardGems
        val bitmap = renderer.bitmap(piece.color, bitmapSize)
        for (pc in piece.shape.cells) {
            drawGem(canvas, bitmap, left + pc.col * cellSize, top + pc.row * cellSize, cellSize, 1f, alpha)
        }
    }

    private fun drawDragged(canvas: Canvas, session: GameSession, now: Long): Boolean {
        if (dragSlot >= 0) {
            val piece = session.trayPiece(dragSlot) ?: return false
            val (cx, cy) = draggedCenter(piece)
            drawPieceCentered(canvas, piece, cx, cy, cell, cell.roundToInt(), 255)
            return false
        }
        if (returnSlot >= 0) {
            val piece = session.trayPiece(returnSlot)
            val t = (now - returnStart) / RETURN_MILLIS.toFloat()
            if (piece == null || t >= 1f) {
                returnSlot = -1
                return false
            }
            val e = 1f - (1f - t) * (1f - t)
            val cx = returnFromX + (slotCenterX(returnSlot) - returnFromX) * e
            val cy = returnFromY + (trayCenterY - returnFromY) * e
            val size = cell + (trayCell - cell) * e
            drawPieceCentered(canvas, piece, cx, cy, size, cell.roundToInt(), 255)
            return true
        }
        return false
    }

    private fun drawParticles(canvas: Canvas, dt: Float): Boolean {
        var i = 0
        val gravity = 900 * dp
        while (i < particleCount) {
            pLife[i] -= dt * 1.6f
            if (pLife[i] <= 0f) {
                // Swap-remove keeps the arrays packed.
                particleCount--
                px[i] = px[particleCount]; py[i] = py[particleCount]
                pvx[i] = pvx[particleCount]; pvy[i] = pvy[particleCount]
                pLife[i] = pLife[particleCount]; pSize[i] = pSize[particleCount]; pColor[i] = pColor[particleCount]
                continue
            }
            pvy[i] += gravity * dt
            px[i] += pvx[i] * dt
            py[i] += pvy[i] * dt
            particlePaint.color = GemPalette.withAlpha(pColor[i], (255 * min(1f, pLife[i] * 1.5f)).toInt())
            canvas.drawCircle(px[i], py[i], pSize[i] * pLife[i], particlePaint)
            i++
        }
        return particleCount > 0
    }

    private fun drawTexts(canvas: Canvas, now: Long): Boolean {
        if (texts.isEmpty()) return false
        val iterator = texts.iterator()
        while (iterator.hasNext()) {
            val text = iterator.next()
            val duration = if (text.big) BIG_TEXT_MILLIS else SMALL_TEXT_MILLIS
            val t = (now - text.start) / duration.toFloat()
            if (t >= 1f) {
                iterator.remove()
                continue
            }
            val alpha = (255 * min(1f, (1f - t) * 3f)).toInt()
            val save = canvas.save()
            if (text.big) {
                val scale = if (t < 0.25f) easeOutBack(t / 0.25f) else 1f
                canvas.scale(scale, scale, text.x, text.y)
                textFill.textSize = 40 * dp
                textFill.shader = text.shader
                textStroke.textSize = 40 * dp
                textStroke.strokeWidth = 8 * dp
                textStroke.alpha = alpha
                textFill.alpha = alpha
                canvas.drawText(text.text, text.x, text.y, textStroke)
                canvas.drawText(text.text, text.x, text.y, textFill)
            } else {
                val y = text.y - 50 * dp * t
                textFill.shader = null
                textFill.color = 0xFFFFFFFF.toInt()
                textFill.alpha = alpha
                textFill.textSize = 24 * dp
                textStroke.textSize = 24 * dp
                textStroke.strokeWidth = 5 * dp
                textStroke.alpha = alpha
                canvas.drawText(text.text, text.x, y, textStroke)
                canvas.drawText(text.text, text.x, y, textFill)
            }
            canvas.restoreToCount(save)
        }
        return true
    }

    // ---------------------------------------------------------------- touch

    private fun draggedCenter(piece: Piece): Pair<Float, Float> {
        // Hold the piece above the finger so the player can see where it goes.
        val lift = cell * 1.3f + piece.shape.rows * cell / 2f
        return dragX to dragY - lift
    }

    private fun updateTarget(session: GameSession) {
        val piece = session.trayPiece(dragSlot) ?: return
        val (cx, cy) = draggedCenter(piece)
        val left = cx - piece.shape.cols * cell / 2f
        val top = cy - piece.shape.rows * cell / 2f
        val col = ((left - boardLeft) / cell).roundToInt()
        val row = ((top - boardTop) / cell).roundToInt()
        val valid = session.board.canPlace(piece.shape, row, col)
        if (row != targetRow || col != targetCol || valid != targetValid) {
            targetRow = row
            targetCol = col
            targetValid = valid
            val (rows, cols) = if (valid) session.board.linesCompletedBy(piece.shape, row, col) else emptyList<Int>() to emptyList()
            previewRows = rows
            previewCols = cols
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val session = session ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (session.isGameOver || event.y < boardTop + boardSide) return false
                val slot = (event.x / (width / 3f)).toInt().coerceIn(0, 2)
                if (session.trayPiece(slot) == null) return false
                if (returnSlot == slot) returnSlot = -1
                dragSlot = slot
                dragX = event.x
                dragY = event.y
                targetValid = false
                targetRow = Int.MIN_VALUE
                updateTarget(session)
                parent?.requestDisallowInterceptTouchEvent(true)
                listener?.onPickUp()
                postInvalidateOnAnimation()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragSlot < 0) return false
                dragX = event.x
                dragY = event.y
                updateTarget(session)
                postInvalidateOnAnimation()
            }
            MotionEvent.ACTION_UP -> {
                if (dragSlot < 0) return false
                performClick()
                drop(session)
            }
            MotionEvent.ACTION_CANCEL -> {
                if (dragSlot >= 0) returnToTray(session)
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    private fun drop(session: GameSession) {
        val slot = dragSlot
        val result = if (targetValid) session.place(slot, targetRow, targetCol) else null
        if (result == null) {
            returnToTray(session)
            listener?.onInvalidDrop()
            return
        }
        dragSlot = -1
        previewRows = emptyList()
        previewCols = emptyList()
        playMoveEffects(result)
        listener?.onMove(result)
        if (session.isGameOver) postDelayed({ if (this.session === session) listener?.onGameOver() }, GAME_OVER_DELAY_MILLIS)
        postInvalidateOnAnimation()
    }

    private fun returnToTray(session: GameSession) {
        val piece = session.trayPiece(dragSlot)
        if (piece != null) {
            val (cx, cy) = draggedCenter(piece)
            returnFromX = cx
            returnFromY = cy
            returnSlot = dragSlot
            returnStart = SystemClock.uptimeMillis()
        }
        dragSlot = -1
        previewRows = emptyList()
        previewCols = emptyList()
        postInvalidateOnAnimation()
    }

    private fun playMoveEffects(result: MoveResult) {
        val now = SystemClock.uptimeMillis()
        val placement = result.placement
        val clearedSet = placement.clearedCells.mapTo(HashSet()) { it.first }
        placement.placed.filter { it !in clearedSet }.forEach { pops += Fading(it.row, it.col, 0, now) }

        val centerRow = placement.placed.map { it.row }.average().toFloat()
        val centerCol = placement.placed.map { it.col }.average().toFloat()
        val centerX = boardLeft + (centerCol + 0.5f) * cell
        val centerY = boardTop + (centerRow + 0.5f) * cell

        if (placement.lines > 0) {
            // Cells vanish in a wave spreading out from where the piece landed.
            for ((c, color) in placement.clearedCells) {
                val distance = abs(c.row - centerRow) + abs(c.col - centerCol)
                val start = now + (distance * WAVE_STEP_MILLIS).toLong()
                clearing += Fading(c.row, c.col, color, start)
                burst(cellCenterX(c.col), cellCenterY(c.row), color, if (placement.lines > 1) 7 else 5)
            }
            flashRows = placement.clearedRows
            flashCols = placement.clearedCols
            flashStart = now
            if (placement.lines >= 2 || result.combo >= 3) {
                shakeStart = now
                shakeStrength = (6 + 4 * placement.lines) * dp
            }
        }
        addText("+${result.points}", centerX, centerY - cell * 0.5f, big = false)
        result.praise?.let { addText(it, width / 2f, boardTop + boardSide * 0.42f, big = true) }
        if (result.combo >= 2 && placement.lines > 0) {
            addText("Combo x${result.combo}", width / 2f, boardTop + boardSide * 0.42f + 44 * dp, big = false)
        }
        if (result.trayRefilled) trayAppearAt = now + 120
        if (result.allClear) {
            for (i in 0 until 40) burst(width * random.nextFloat(), boardTop + boardSide * random.nextFloat(), random.nextInt(7), 1)
        }
    }

    private fun burst(x: Float, y: Float, color: Int, count: Int) {
        val gem = GemPalette[color]
        repeat(count) {
            if (particleCount >= MAX_PARTICLES) return
            val i = particleCount++
            val angle = random.nextFloat() * 6.2832f
            val speed = (120 + random.nextFloat() * 380) * dp
            px[i] = x
            py[i] = y
            pvx[i] = cos(angle) * speed
            pvy[i] = sin(angle) * speed - 250 * dp
            pLife[i] = 0.7f + random.nextFloat() * 0.5f
            pSize[i] = (3 + random.nextFloat() * 5) * dp
            pColor[i] = if (random.nextInt(4) == 0) 0xFFFFFFFF.toInt() else if (random.nextBoolean()) gem.light else gem.base
        }
    }

    private fun addText(text: String, x: Float, y: Float, big: Boolean) {
        if (big) texts.removeAll { it.big }
        val shader = if (big) {
            LinearGradient(0f, y - 40 * dp, 0f, y, 0xFFFFF59D.toInt(), 0xFFFF9800.toInt(), Shader.TileMode.CLAMP)
        } else {
            null
        }
        texts += FloatingText(text, x, y, big, SystemClock.uptimeMillis(), shader)
    }

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val x = t - 1f
        return 1f + c3 * x * x * x + c1 * x * x
    }

    private companion object {
        const val MAX_PARTICLES = 700
        const val POP_MILLIS = 180L
        const val CLEAR_MILLIS = 380L
        const val WAVE_STEP_MILLIS = 28f
        const val FLASH_MILLIS = 320L
        const val SHAKE_MILLIS = 260L
        const val APPEAR_MILLIS = 320L
        const val RETURN_MILLIS = 170L
        const val SMALL_TEXT_MILLIS = 800L
        const val BIG_TEXT_MILLIS = 1100L
        const val GAME_OVER_DELAY_MILLIS = 700L
    }
}
