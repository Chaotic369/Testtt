package com.example.edgering.overlay

/** Screen edge that hosts the touch strip. M06 replaces this with fully editable triggers. */
enum class TriggerSide {
    LEFT,
    RIGHT,
    ;

    companion object {
        fun fromName(name: String?): TriggerSide = entries.firstOrNull { it.name == name } ?: RIGHT
    }
}
