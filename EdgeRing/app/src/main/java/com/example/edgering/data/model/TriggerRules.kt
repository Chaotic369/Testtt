package com.example.edgering.data.model

import kotlin.math.abs

/** Strip geometry. [offsetFraction] shifts the strip's centre from the edge middle, as a fraction of the edge. */
data class TriggerGeometry(
    val lengthFraction: Float = DEFAULT_LENGTH,
    val thicknessDp: Int = DEFAULT_THICKNESS_DP,
    val offsetFraction: Float = 0f,
) {
    companion object {
        /** Same values as the M01 spike strip: 40 % of the edge, 24 dp, centred. */
        const val DEFAULT_LENGTH = 0.4f
        const val DEFAULT_THICKNESS_DP = 24
    }
}

/** Pure validation and clamping for triggers, shared by the repository and the backup validator. */
object TriggerRules {
    const val MIN_LENGTH = 0.05f
    const val MAX_LENGTH = 1f
    const val MIN_THICKNESS_DP = 4
    const val MAX_THICKNESS_DP = 64
    const val MAX_NAME_LENGTH = 48
    private const val EPSILON = 1e-4f

    /** The strip must stay entirely on its edge: |offset| <= (1 - length) / 2. */
    fun maxOffset(length: Float): Float = (1f - length) / 2f

    fun isValid(g: TriggerGeometry): Boolean =
        g.lengthFraction.isFinite() && g.offsetFraction.isFinite() &&
            g.lengthFraction in MIN_LENGTH..MAX_LENGTH &&
            g.thicknessDp in MIN_THICKNESS_DP..MAX_THICKNESS_DP &&
            abs(g.offsetFraction) <= maxOffset(g.lengthFraction) + EPSILON

    /** Returns the nearest valid geometry; non-finite numbers fall back to the defaults. */
    fun clamp(g: TriggerGeometry): TriggerGeometry {
        val length = if (g.lengthFraction.isFinite()) {
            g.lengthFraction.coerceIn(MIN_LENGTH, MAX_LENGTH)
        } else {
            TriggerGeometry.DEFAULT_LENGTH
        }
        val limit = maxOffset(length)
        val offset = if (g.offsetFraction.isFinite()) g.offsetFraction.coerceIn(-limit, limit) else 0f
        return TriggerGeometry(length, g.thicknessDp.coerceIn(MIN_THICKNESS_DP, MAX_THICKNESS_DP), offset)
    }
}
