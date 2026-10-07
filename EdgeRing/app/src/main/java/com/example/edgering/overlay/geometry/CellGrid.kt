package com.example.edgering.overlay.geometry

/** Axis-aligned rectangle of one grid cell. Plain floats so the geometry stays JVM-testable. */
data class CellBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

/**
 * Uniform [columns] x [rows] grid over a rectangle.
 *
 * Cells tile the whole rectangle with no dead zones (half-open intervals: a point exactly on a
 * shared edge belongs to the next cell). Index = row * columns + column.
 */
class CellGrid(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val columns: Int,
    val rows: Int,
) {
    init {
        require(columns > 0 && rows > 0) { "grid needs at least one column and one row" }
        require(width > 0f && height > 0f) { "grid needs a positive size" }
    }

    val cellCount: Int = columns * rows
    val cellWidth: Float = width / columns
    val cellHeight: Float = height / rows

    private val right: Float = left + width
    private val bottom: Float = top + height

    /** Index of the cell under ([x], [y]), or [NO_CELL] if the point is outside the grid. */
    fun cellAt(x: Float, y: Float): Int {
        if (x.isNaN() || y.isNaN()) return NO_CELL
        if (x < left || y < top || x >= right || y >= bottom) return NO_CELL
        val column = ((x - left) / cellWidth).toInt().coerceIn(0, columns - 1)
        val row = ((y - top) / cellHeight).toInt().coerceIn(0, rows - 1)
        return row * columns + column
    }

    /** Allocation-free accessors for drawing code. [index] must be in 0 until [cellCount]. */
    fun cellLeft(index: Int): Float = left + (index % columns) * cellWidth

    fun cellTop(index: Int): Float = top + (index / columns) * cellHeight

    fun boundsOf(index: Int): CellBounds {
        require(index in 0 until cellCount) { "cell index out of range: $index" }
        val l = cellLeft(index)
        val t = cellTop(index)
        return CellBounds(l, t, l + cellWidth, t + cellHeight)
    }

    companion object {
        const val NO_CELL = -1
    }
}
