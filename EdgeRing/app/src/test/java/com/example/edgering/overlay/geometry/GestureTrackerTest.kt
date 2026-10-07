package com.example.edgering.overlay.geometry

import com.example.edgering.overlay.geometry.CellGrid.Companion.NO_CELL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureTrackerTest {
    // 3 x 4 grid of 100 x 100 cells at the origin.
    private fun tracker(selectable: Int = 12) = GestureTracker().apply {
        grid = CellGrid(0f, 0f, 300f, 400f, columns = 3, rows = 4)
        selectableCount = selectable
    }

    @Test
    fun startInsideGridSelectsThatCell() {
        val t = tracker()
        assertEquals(4, t.start(150f, 150f))
        assertTrue(t.isActive)
        assertEquals(4, t.selected)
    }

    @Test
    fun startOutsideGridSelectsNothingButGestureIsActive() {
        val t = tracker()
        assertEquals(NO_CELL, t.start(-20f, 150f))
        assertTrue(t.isActive)
    }

    @Test
    fun moveReportsWhetherSelectionChanged() {
        val t = tracker()
        t.start(50f, 50f)
        assertFalse(t.move(60f, 60f))
        assertTrue(t.move(250f, 50f))
        assertEquals(2, t.selected)
        assertTrue(t.move(-10f, 50f))
        assertEquals(NO_CELL, t.selected)
    }

    @Test
    fun finishReturnsCellUnderReleasePointAndEndsGesture() {
        val t = tracker()
        t.start(50f, 50f)
        t.move(250f, 350f)
        assertEquals(11, t.finish(250f, 350f))
        assertFalse(t.isActive)
        assertEquals(NO_CELL, t.selected)
    }

    @Test
    fun releaseOutsideGridLaunchesNothing() {
        val t = tracker()
        t.start(50f, 50f)
        assertEquals(NO_CELL, t.finish(500f, 50f))
    }

    @Test
    fun cancelledGestureLaunchesNothing() {
        val t = tracker()
        t.start(50f, 50f)
        t.cancel()
        assertFalse(t.isActive)
        assertEquals(NO_CELL, t.finish(50f, 50f))
    }

    @Test
    fun moveAndFinishAreIgnoredWithoutAnActiveGesture() {
        val t = tracker()
        assertFalse(t.move(50f, 50f))
        assertEquals(NO_CELL, t.finish(50f, 50f))
        assertEquals(NO_CELL, t.selected)
    }

    @Test
    fun emptySlotsBeyondSelectableCountCannotBeSelected() {
        val t = tracker(selectable = 5)
        assertEquals(4, t.start(150f, 150f))
        assertTrue(t.move(250f, 150f))
        assertEquals(NO_CELL, t.selected)
        assertEquals(NO_CELL, t.finish(250f, 350f))
    }

    @Test
    fun missingGridSelectsNothing() {
        val t = GestureTracker().apply { selectableCount = 12 }
        assertEquals(NO_CELL, t.start(10f, 10f))
        assertEquals(NO_CELL, t.finish(10f, 10f))
    }

    @Test
    fun trackerCanBeReusedAfterFinish() {
        val t = tracker()
        t.start(50f, 50f)
        t.finish(50f, 50f)
        assertEquals(1, t.start(150f, 50f))
        assertEquals(1, t.finish(150f, 50f))
    }
}
