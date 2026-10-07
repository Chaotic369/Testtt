package com.example.edgering.overlay

import android.content.ComponentName
import android.graphics.drawable.Drawable

/** One launchable cell in the overlay grid. */
data class OverlayItem(
    val label: String,
    val component: ComponentName,
    val icon: Drawable?,
)
