package com.example.edgering.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TriggerRulesTest {
    @Test
    fun defaultGeometryIsValidAndMatchesTheM01Spike() {
        val g = TriggerGeometry()
        assertTrue(TriggerRules.isValid(g))
        assertEquals(0.4f, g.lengthFraction, 0f)
        assertEquals(24, g.thicknessDp)
        assertEquals(0f, g.offsetFraction, 0f)
    }

    @Test
    fun offsetMustKeepTheStripOnTheEdge() {
        // length 0.4 -> max offset 0.3
        assertTrue(TriggerRules.isValid(TriggerGeometry(0.4f, 24, 0.3f)))
        assertTrue(TriggerRules.isValid(TriggerGeometry(0.4f, 24, -0.3f)))
        assertFalse(TriggerRules.isValid(TriggerGeometry(0.4f, 24, 0.35f)))
        // full-length strip cannot be shifted at all
        assertTrue(TriggerRules.isValid(TriggerGeometry(1f, 24, 0f)))
        assertFalse(TriggerRules.isValid(TriggerGeometry(1f, 24, 0.1f)))
    }

    @Test
    fun rangesAreEnforcedAtBothEnds() {
        assertFalse(TriggerRules.isValid(TriggerGeometry(0.04f, 24, 0f)))
        assertFalse(TriggerRules.isValid(TriggerGeometry(1.01f, 24, 0f)))
        assertFalse(TriggerRules.isValid(TriggerGeometry(0.4f, 3, 0f)))
        assertFalse(TriggerRules.isValid(TriggerGeometry(0.4f, 65, 0f)))
        assertTrue(TriggerRules.isValid(TriggerGeometry(0.05f, 4, 0f)))
        assertTrue(TriggerRules.isValid(TriggerGeometry(1f, 64, 0f)))
    }

    @Test
    fun nonFiniteNumbersAreInvalid() {
        assertFalse(TriggerRules.isValid(TriggerGeometry(Float.NaN, 24, 0f)))
        assertFalse(TriggerRules.isValid(TriggerGeometry(0.4f, 24, Float.POSITIVE_INFINITY)))
    }

    @Test
    fun clampAlwaysProducesAValidGeometry() {
        val inputs = listOf(
            TriggerGeometry(-5f, -1, 9f),
            TriggerGeometry(5f, 500, -9f),
            TriggerGeometry(0.9f, 24, 0.5f),
            TriggerGeometry(Float.NaN, 10, Float.NaN),
            TriggerGeometry(0.4f, 24, Float.NEGATIVE_INFINITY),
        )
        for (input in inputs) assertTrue("clamp($input)", TriggerRules.isValid(TriggerRules.clamp(input)))
    }

    @Test
    fun clampKeepsValidGeometryUnchanged() {
        val g = TriggerGeometry(0.5f, 30, -0.2f)
        assertEquals(g, TriggerRules.clamp(g))
    }

    @Test
    fun clampPullsOffsetBackOntoTheEdge() {
        val c = TriggerRules.clamp(TriggerGeometry(0.8f, 24, 0.5f))
        assertEquals(0.1f, c.offsetFraction, 1e-6f)
    }
}
