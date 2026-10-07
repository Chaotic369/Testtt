package com.example.edgering.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/**
 * The thin touch strip on a screen edge. It receives the whole gesture (down, moves, up) because
 * the view that gets ACTION_DOWN keeps receiving the stream, and forwards raw screen coordinates.
 */
class TriggerView(
    context: Context,
    private val listener: Listener,
) : View(context) {

    interface Listener {
        fun onGestureDown(rawX: Float, rawY: Float, eventTime: Long)

        fun onGestureMove(rawX: Float, rawY: Float)

        fun onGestureUp(rawX: Float, rawY: Float)

        fun onGestureCancel()
    }

    var side: TriggerSide = TriggerSide.RIGHT
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 255, 255, 255)
    }
    private val lineWidth = LINE_WIDTH_DP * density
    private var tracking = false

    override fun onDraw(canvas: Canvas) {
        // Faint visible line so the strip can be found while testing (becomes optional in M06).
        val w = width.toFloat()
        val h = height.toFloat()
        val inset = h * 0.05f
        val left = if (side == TriggerSide.LEFT) 0f else w - lineWidth
        canvas.drawRoundRect(left, inset, left + lineWidth, h - inset, lineWidth / 2f, lineWidth / 2f, linePaint)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Ask the system not to treat this strip as the back gesture. Android caps the total
            // exclusion height per edge at 200dp, so only the first 200dp are requested.
            val height = min(bottom - top, (MAX_EXCLUSION_DP * density).toInt())
            systemGestureExclusionRects = listOf(Rect(0, 0, right - left, height))
        }
    }

    // The strip is a touch-only gesture surface; a TalkBack alternative is planned for M19.
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                tracking = true
                listener.onGestureDown(event.rawX, event.rawY, event.eventTime)
            }
            MotionEvent.ACTION_MOVE -> if (tracking) listener.onGestureMove(event.rawX, event.rawY)
            MotionEvent.ACTION_UP -> if (tracking) {
                tracking = false
                listener.onGestureUp(event.rawX, event.rawY)
            }
            MotionEvent.ACTION_CANCEL -> if (tracking) {
                tracking = false
                listener.onGestureCancel()
            }
            else -> Unit // Secondary pointers are ignored.
        }
        return true
    }

    companion object {
        private const val LINE_WIDTH_DP = 3f
        private const val MAX_EXCLUSION_DP = 200f
    }
}
