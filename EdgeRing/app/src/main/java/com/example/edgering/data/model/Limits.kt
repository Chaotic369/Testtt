package com.example.edgering.data.model

/** Size limits shared by the repositories and the backup validator. */
object Limits {
    const val MAX_NAME_LENGTH = 64
    const val MAX_LABEL_LENGTH = 128
    const val MAX_TARGET_LENGTH = 4096
    const val MAX_OPTIONS_LENGTH = 8192
    const val MAX_ZONES = 200
    const val MAX_TRIGGERS = 50
    const val MAX_ITEMS = 20_000
    const val MAX_SETTINGS = 100
    const val MAX_BACKUP_CHARS = 8_000_000
}
