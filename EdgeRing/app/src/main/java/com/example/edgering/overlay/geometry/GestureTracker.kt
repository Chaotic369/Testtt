package com.example.edgering.overlay.geometry

import com.example.edgering.overlay.geometry.CellGrid.Companion.NO_CELL

/**
 * Tracks one press-drag-release gesture over a [CellGrid]. Pure logic (no Android types).
 *
 * Only cells below [selectableCount] can be selected, so empty grid slots never launch anything.
 * A cancelled gesture, or one released outside every selectable cell, yields [NO_CELL].
 */
class GestureTracker {
    var grid: CellGrid? = null
    var selectableCount: Int = 0

    var isActive: Boolean = false
        private set

    var selected: Int = NO_CELL
        private set

    /** Begins a gesture at ([x], [y]); returns the selected cell or [NO_CELL]. */
    fun start(x: Float, y: Float): Int {
        isActive = true
        selected = resolve(x, y)
        return selected
    }

    /** Returns true if the selection changed. Ignored when no gesture is active. */
    fun move(x: Float, y: Float): Boolean {
        if (!isActive) return false
        val next = resolve(x, y)
        val changed = next != selected
        selected = next
        return changed
    }

    /** Ends the gesture at ([x], [y]); returns the cell to launch or [NO_CELL]. */
    fun finish(x: Float, y: Float): Int {
        if (!isActive) return NO_CELL
        val result = resolve(x, y)
        reset()
        return result
    }

    fun cancel() = reset()

    private fun resolve(x: Float, y: Float): Int {
        val g = grid ?: return NO_CELL
        val cell = g.cellAt(x, y)
        return if (cell in 0 until selectableCount) cell else NO_CELL
    }

    private fun reset() {
        isActive = false
        selected = NO_CELL
    }
}
