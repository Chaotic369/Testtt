package com.example.edgering.data.backup

/** Shared fixture: a small, valid backup with a nested folder (child listed before its parent on purpose). */
object TestBackups {
    fun valid(): BackupFile = BackupFile(
        zones = listOf(
            BackupZone(id = 1, name = "Social", colorArgb = 0xFF2196F3.toInt(), position = 0),
            BackupZone(id = 2, name = "Tools", colorArgb = 0xFF4CAF50.toInt(), position = 1),
        ),
        triggers = listOf(
            BackupTrigger(1, "Default", "RIGHT", 0.4f, 24, 0f, targetZoneId = null, enabled = true, isDefault = true),
            BackupTrigger(2, "Left tools", "LEFT", 0.3f, 16, 0.1f, targetZoneId = 2, enabled = false, isDefault = false),
        ),
        items = listOf(
            BackupItem(2, 1, parentItemId = 1, position = 0, type = "APP", label = "Mail", target = "com.mail/.Main"),
            BackupItem(1, 1, parentItemId = null, position = 0, type = "FOLDER", label = "Messaging"),
            BackupItem(3, 2, parentItemId = null, position = 0, type = "URL", label = "Docs", target = "https://example.org", options = "{\"k\":1}"),
        ),
        settings = mapOf("grid_columns" to "5", "launch_mode" to "SINGLE_TOUCH"),
    )
}
