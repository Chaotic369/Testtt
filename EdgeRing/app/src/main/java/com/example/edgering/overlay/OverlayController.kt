package com.example.edgering.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import androidx.annotation.StringRes
import com.example.edgering.R
import com.example.edgering.overlay.geometry.CellGrid
import com.example.edgering.overlay.geometry.GestureTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Owns the two overlay windows: the edge trigger strip (always present while the service runs) and
 * the full-screen, non-touchable drawing overlay (pre-added, shown only during a gesture).
 *
 * Safety: the drawing overlay is FLAG_NOT_TOUCHABLE so it can never trap touches, [remove] always
 * detaches both windows, and an idle watchdog hides the overlay if a gesture stalls.
 */
class OverlayController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val side: TriggerSide,
    private val onLatency: (Long) -> Unit,
    private val onMessage: (Int) -> Unit,
) : TriggerView.Listener {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val tracker = GestureTracker()
    private val point = FloatArray(2)

    private var overlayView: OverlayView? = null
    private var triggerView: TriggerView? = null
    private var items: List<OverlayItem> = emptyList()
    private var watchdog: Job? = null
    private var lastEventAt = 0L

    /** Adds both windows. Returns false (and leaves nothing attached) if the system refuses. */
    fun install(): Boolean {
        val wm = windowManager ?: return false
        if (overlayView != null) return true
        return try {
            val overlay = OverlayView(context).apply {
                visibility = View.INVISIBLE
                setItems(items)
            }
            wm.addView(overlay, overlayParams())
            overlayView = overlay
            val trigger = TriggerView(context, this).apply { side = this@OverlayController.side }
            wm.addView(trigger, triggerParams())
            triggerView = trigger
            true
        } catch (e: RuntimeException) {
            // BadTokenException, SecurityException, IllegalStateException, ...
            remove()
            false
        }
    }

    /** Detaches every window this controller added. Safe to call repeatedly. */
    fun remove() {
        watchdog?.cancel()
        watchdog = null
        tracker.cancel()
        val wm = windowManager
        for (view in listOfNotNull(triggerView, overlayView)) {
            try {
                wm?.removeViewImmediate(view)
            } catch (e: RuntimeException) {
                // Already detached.
            }
        }
        triggerView = null
        overlayView = null
    }

    fun setItems(newItems: List<OverlayItem>) {
        items = newItems
        overlayView?.setItems(newItems)
    }

    /** Re-applies the trigger geometry, e.g. after a rotation. */
    fun refreshTriggerLayout() {
        val trigger = triggerView ?: return
        try {
            windowManager?.updateViewLayout(trigger, triggerParams())
        } catch (e: RuntimeException) {
            // View was removed concurrently.
        }
    }

    override fun onGestureDown(rawX: Float, rawY: Float, eventTime: Long) {
        val overlay = overlayView ?: return
        tracker.grid = overlay.grid
        tracker.selectableCount = items.size
        overlay.toLocal(rawX, rawY, point)
        val selected = tracker.start(point[0], point[1])
        overlay.onFirstDraw = { onLatency(SystemClock.uptimeMillis() - eventTime) }
        overlay.beginGesture(point[0], point[1], selected)
        triggerView?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        lastEventAt = SystemClock.uptimeMillis()
        armWatchdog()
    }

    override fun onGestureMove(rawX: Float, rawY: Float) {
        val overlay = overlayView ?: return
        if (!tracker.isActive) return
        overlay.toLocal(rawX, rawY, point)
        tracker.move(point[0], point[1])
        overlay.updateGesture(point[0], point[1], tracker.selected)
        lastEventAt = SystemClock.uptimeMillis()
    }

    override fun onGestureUp(rawX: Float, rawY: Float) {
        val overlay = overlayView ?: return
        if (!tracker.isActive) return
        overlay.toLocal(rawX, rawY, point)
        val cell = tracker.finish(point[0], point[1])
        // Launch before hiding: the visible overlay window is what lets the activity start from
        // a service on newer Android versions (to be confirmed on device).
        if (cell != CellGrid.NO_CELL) launch(items[cell])
        hide()
    }

    override fun onGestureCancel() = hide()

    private fun hide() {
        watchdog?.cancel()
        watchdog = null
        tracker.cancel()
        overlayView?.endGesture()
    }

    private fun launch(item: OverlayItem) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(item.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            context.startActivity(intent)
        } catch (e: RuntimeException) {
            // ActivityNotFoundException, SecurityException, ...
            showMessage(R.string.toast_launch_failed)
        }
    }

    private fun showMessage(@StringRes message: Int) = onMessage(message)

    private fun armWatchdog() {
        watchdog?.cancel()
        watchdog = scope.launch {
            while (true) {
                delay(WATCHDOG_CHECK_MS)
                if (SystemClock.uptimeMillis() - lastEventAt >= WATCHDOG_IDLE_MS) {
                    hide()
                    return@launch
                }
            }
        }
    }

    private fun triggerParams(): WindowManager.LayoutParams {
        val metrics = context.resources.displayMetrics
        val params = WindowManager.LayoutParams(
            (TRIGGER_THICKNESS_DP * metrics.density).toInt(),
            (metrics.heightPixels * TRIGGER_LENGTH_FRACTION).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        val horizontal = if (side == TriggerSide.LEFT) Gravity.START else Gravity.END
        params.gravity = horizontal or Gravity.CENTER_VERTICAL
        return params
    }

    private fun overlayParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        return params
    }

    companion object {
        private const val TRIGGER_THICKNESS_DP = 24f
        private const val TRIGGER_LENGTH_FRACTION = 0.4f
        private const val WATCHDOG_CHECK_MS = 1_000L
        private const val WATCHDOG_IDLE_MS = 20_000L
    }
}
