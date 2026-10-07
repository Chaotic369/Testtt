package com.example.edgering.data.model

/** Pure helpers for drag-to-rearrange lists. */
object Reorder {
    /** Returns a copy of [list] with the element at [from] moved to index [to]. */
    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        require(from in list.indices) { "from out of range: $from" }
        require(to in list.indices) { "to out of range: $to" }
        if (from == to) return list.toList()
        val copy = list.toMutableList()
        copy.add(to, copy.removeAt(from))
        return copy
    }

    /** True when [ordered] contains exactly the ids in [current], once each (order may differ). */
    fun isSamePermutation(current: List<Long>, ordered: List<Long>): Boolean =
        ordered.size == current.size && ordered.toSet().size == ordered.size && ordered.toSet() == current.toSet()
}
