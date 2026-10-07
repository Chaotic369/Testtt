package com.example.edgering.overlay.geometry

import com.example.edgering.overlay.geometry.CellGrid.Companion.NO_CELL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CellGridTest {
    // 3 columns x 4 rows, each cell 100 x 50, grid origin (10, 20).
    private val grid = CellGrid(left = 10f, top = 20f, width = 300f, height = 200f, columns = 3, rows = 4)

    @Test
    fun cellCountIsColumnsTimesRows() = assertEquals(12, grid.cellCount)

    @Test
    fun centreOfEveryCellMapsToItsOwnIndex() {
        for (index in 0 until grid.cellCount) {
            val b = grid.boundsOf(index)
            val cx = (b.left + b.right) / 2f
            val cy = (b.top + b.bottom) / 2f
            assertEquals(index, grid.cellAt(cx, cy))
        }
    }

    @Test
    fun cornersMapToExpectedCells() {
        assertEquals(0, grid.cellAt(10f, 20f))
        assertEquals(2, grid.cellAt(309.9f, 20f))
        assertEquals(9, grid.cellAt(10f, 219.9f))
        assertEquals(11, grid.cellAt(309.9f, 219.9f))
    }

    @Test
    fun sharedEdgeBelongsToTheNextCell() {
        assertEquals(1, grid.cellAt(110f, 20f))
        assertEquals(3, grid.cellAt(10f, 70f))
    }

    @Test
    fun rightAndBottomEdgesAreOutside() {
        assertEquals(NO_CELL, grid.cellAt(310f, 100f))
        assertEquals(NO_CELL, grid.cellAt(100f, 220f))
    }

    @Test
    fun pointsOutsideTheGridAreNoCell() {
        assertEquals(NO_CELL, grid.cellAt(9.9f, 100f))
        assertEquals(NO_CELL, grid.cellAt(100f, 19.9f))
        assertEquals(NO_CELL, grid.cellAt(-5f, -5f))
        assertEquals(NO_CELL, grid.cellAt(5000f, 5000f))
    }

    @Test
    fun nanIsNoCell() {
        assertEquals(NO_CELL, grid.cellAt(Float.NaN, 100f))
        assertEquals(NO_CELL, grid.cellAt(100f, Float.NaN))
    }

    @Test
    fun boundsTileTheGridWithoutGaps() {
        val first = grid.boundsOf(0)
        val last = grid.boundsOf(11)
        assertEquals(10f, first.left, 0.001f)
        assertEquals(20f, first.top, 0.001f)
        assertEquals(310f, last.right, 0.001f)
        assertEquals(220f, last.bottom, 0.001f)
        assertEquals(grid.boundsOf(0).right, grid.boundsOf(1).left, 0.001f)
        assertEquals(grid.boundsOf(0).bottom, grid.boundsOf(3).top, 0.001f)
    }

    @Test
    fun boundsOfRejectsBadIndex() {
        assertThrows(IllegalArgumentException::class.java) { grid.boundsOf(-1) }
        assertThrows(IllegalArgumentException::class.java) { grid.boundsOf(12) }
    }

    @Test
    fun invalidGridsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { CellGrid(0f, 0f, 100f, 100f, 0, 1) }
        assertThrows(IllegalArgumentException::class.java) { CellGrid(0f, 0f, 100f, 100f, 1, 0) }
        assertThrows(IllegalArgumentException::class.java) { CellGrid(0f, 0f, 0f, 100f, 1, 1) }
        assertThrows(IllegalArgumentException::class.java) { CellGrid(0f, 0f, 100f, -1f, 1, 1) }
    }
}
