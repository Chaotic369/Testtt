package com.example.edgering.data.backup

import kotlinx.serialization.Serializable

/**
 * Backup file, format version 1. Enum-like fields are plain strings so that an unknown value is
 * reported by [BackupValidator] instead of failing JSON decoding. Custom icons and the app cache
 * are not part of version 1. Add fields only with defaults; bump [CURRENT_VERSION] for any change
 * that older readers could misinterpret.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val schemaVersion: Int = CURRENT_VERSION,
    val zones: List<BackupZone> = emptyList(),
    val triggers: List<BackupTrigger> = emptyList(),
    val items: List<BackupItem> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
) {
    companion object {
        const val FORMAT = "edgering-backup"
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class BackupZone(val id: Long, val name: String, val colorArgb: Int, val position: Int)

@Serializable
data class BackupTrigger(
    val id: Long,
    val name: String,
    val position: String,
    val lengthFraction: Float,
    val thicknessDp: Int,
    val offsetFraction: Float,
    val targetZoneId: Long? = null,
    val enabled: Boolean = true,
    val isDefault: Boolean = false,
)

@Serializable
data class BackupItem(
    val id: Long,
    val zoneId: Long,
    val parentItemId: Long? = null,
    val position: Int,
    val type: String,
    val label: String,
    val target: String? = null,
    val options: String? = null,
)
