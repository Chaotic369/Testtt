package com.example.edgering.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReorderTest {
    @Test
    fun movesForwardAndBackward() {
        assertEquals(listOf(2L, 3L, 1L), Reorder.move(listOf(1L, 2L, 3L), 0, 2))
        assertEquals(listOf(3L, 1L, 2L), Reorder.move(listOf(1L, 2L, 3L), 2, 0))
    }

    @Test
    fun sameIndexReturnsEqualCopyAndDoesNotMutateInput() {
        val input = listOf(1L, 2L, 3L)
        assertEquals(input, Reorder.move(input, 1, 1))
        Reorder.move(input, 0, 2)
        assertEquals(listOf(1L, 2L, 3L), input)
    }

    @Test
    fun outOfRangeIndicesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { Reorder.move(listOf(1L, 2L), 2, 0) }
        assertThrows(IllegalArgumentException::class.java) { Reorder.move(listOf(1L, 2L), 0, -1) }
        assertThrows(IllegalArgumentException::class.java) { Reorder.move(emptyList<Long>(), 0, 0) }
    }

    @Test
    fun permutationCheckAcceptsAnyOrderOfTheSameIds() {
        assertTrue(Reorder.isSamePermutation(listOf(1L, 2L, 3L), listOf(3L, 1L, 2L)))
        assertTrue(Reorder.isSamePermutation(emptyList(), emptyList()))
    }

    @Test
    fun permutationCheckRejectsMissingExtraAndDuplicateIds() {
        assertFalse(Reorder.isSamePermutation(listOf(1L, 2L, 3L), listOf(1L, 2L)))
        assertFalse(Reorder.isSamePermutation(listOf(1L, 2L, 3L), listOf(1L, 2L, 4L)))
        assertFalse(Reorder.isSamePermutation(listOf(1L, 2L, 3L), listOf(1L, 1L, 2L)))
        assertFalse(Reorder.isSamePermutation(listOf(1L, 2L), listOf(1L, 2L, 2L)))
    }
}
