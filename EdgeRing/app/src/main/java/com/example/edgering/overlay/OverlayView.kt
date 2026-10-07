package com.example.edgering.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import com.example.edgering.overlay.geometry.CellGrid
import kotlin.math.min

/**
 * Full-screen, non-touchable overlay that draws the dimmed backdrop, the 3x4 grid, the highlighted
 * cell and the ring that follows the finger. Allocation-free in [onDraw].
 */
class OverlayView(context: Context) : View(context) {

    /** Called once on the first draw after a gesture starts (used to measure open latency). */
    var onFirstDraw: (() -> Unit)? = null

    var grid: CellGrid? = null
        private set

    private val density = resources.displayMetrics.density
    private val dimPaint = Paint().apply { color = Color.argb(176, 0, 0, 0) }
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 255, 255, 255) }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(150, 79, 195, 247) }
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, LABEL_SP, resources.displayMetrics)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = RING_STROKE_DP * density
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(79, 195, 247) }

    private val cellRect = RectF()
    private val location = IntArray(2)
    private val cellInset = CELL_GAP_DP * density / 2f
    private val cornerRadius = CORNER_DP * density

    private var items: List<OverlayItem> = emptyList()
    private var labels: Array<String> = emptyArray()
    private var selected = CellGrid.NO_CELL
    private var fingerX = 0f
    private var fingerY = 0f
    private var fingerVisible = false

    fun setItems(newItems: List<OverlayItem>) {
        items = newItems
        rebuildLabels()
        invalidate()
    }

    /** Converts raw screen coordinates to this view's coordinates. [out] receives x, y. */
    fun toLocal(rawX: Float, rawY: Float, out: FloatArray) {
        getLocationOnScreen(location)
        out[0] = rawX - location[0]
        out[1] = rawY - location[1]
    }

    fun beginGesture(x: Float, y: Float, selectedIndex: Int) {
        fingerX = x
        fingerY = y
        selected = selectedIndex
        fingerVisible = true
        visibility = VISIBLE
        invalidate()
    }

    fun updateGesture(x: Float, y: Float, selectedIndex: Int) {
        fingerX = x
        fingerY = y
        selected = selectedIndex
        invalidate()
    }

    fun endGesture() {
        fingerVisible = false
        selected = CellGrid.NO_CELL
        onFirstDraw = null
        visibility = INVISIBLE
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        grid = if (w > 0 && h > 0) {
            CellGrid(w * 0.08f, h * 0.14f, w * 0.84f, h * 0.72f, COLUMNS, ROWS)
        } else {
            null
        }
        rebuildLabels()
    }

    private fun rebuildLabels() {
        val g = grid
        labels = if (g == null) {
            emptyArray()
        } else {
            val maxWidth = g.cellWidth - 2f * cellInset - 8f * density
            Array(min(items.size, g.cellCount)) { i ->
                TextUtils.ellipsize(items[i].label, labelPaint, maxWidth, TextUtils.TruncateAt.END).toString()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        onFirstDraw?.let {
            onFirstDraw = null
            it()
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        val g = grid ?: return
        for (i in 0 until g.cellCount) {
            val left = g.cellLeft(i) + cellInset
            val top = g.cellTop(i) + cellInset
            cellRect.set(left, top, left + g.cellWidth - 2f * cellInset, top + g.cellHeight - 2f * cellInset)
            canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, if (i == selected) selectedPaint else cellPaint)
            if (i >= labels.size) continue
            val icon = items[i].icon
            if (icon != null) {
                val size = (min(cellRect.width(), cellRect.height()) * 0.5f).toInt()
                val cx = cellRect.centerX().toInt()
                val iconTop = (cellRect.top + cellRect.height() * 0.12f).toInt()
                icon.setBounds(cx - size / 2, iconTop, cx + size / 2, iconTop + size)
                icon.draw(canvas)
            }
            canvas.drawText(labels[i], cellRect.centerX(), cellRect.bottom - LABEL_BOTTOM_DP * density, labelPaint)
        }
        if (fingerVisible) {
            canvas.drawCircle(fingerX, fingerY, RING_RADIUS_DP * density, ringPaint)
            canvas.drawCircle(fingerX, fingerY, DOT_RADIUS_DP * density, dotPaint)
        }
    }

    companion object {
        const val COLUMNS = 3
        const val ROWS = 4
        private const val CELL_GAP_DP = 8f
        private const val CORNER_DP = 16f
        private const val LABEL_SP = 12f
        private const val LABEL_BOTTOM_DP = 10f
        private const val RING_RADIUS_DP = 34f
        private const val RING_STROKE_DP = 3f
        private const val DOT_RADIUS_DP = 6f
    }
}
