package com.example.edgering.data.model

/** Persistent setting names (DataStore keys and backup keys). Values are stored as strings. */
object SettingKeys {
    const val GRID_COLUMNS = "grid_columns"
    const val GRID_DIRECTION = "grid_direction"
    const val GRID_POSITION = "grid_position"
    const val ICON_SIZE = "icon_size"
    const val LABEL_POSITION = "label_position"
    const val HIDE_EMPTY_CELLS = "hide_empty_cells"
    const val LAUNCH_MODE = "launch_mode"
    const val SERVICE_ENABLED = "service_enabled"
    const val DISABLE_IN_LANDSCAPE = "disable_in_landscape"
    const val HIDE_SERVICE_NOTIFICATION = "hide_service_notification"
    const val HIDE_AZ_INDEX = "hide_az_index"
    const val LEFT_HANDED_LAYOUT = "left_handed_layout"

    /** Keys that describe this device's runtime state and are never exported or imported. */
    val EXCLUDED_FROM_BACKUP: Set<String> = setOf(SERVICE_ENABLED)
}

/**
 * All user settings with their defaults. Stored as plain strings so that a corrupt, missing or
 * unknown value can never crash the app: every field falls back to its default in [fromMap].
 * Adding a field later is migration-free (old stores simply lack the key).
 */
data class AppSettings(
    val gridColumns: Int = DEFAULT_COLUMNS,
    val gridDirection: GridDirection = GridDirection.TOP_TO_BOTTOM,
    val gridPosition: GridPosition = GridPosition.CENTER,
    val iconSize: IconSize = IconSize.MEDIUM,
    val labelPosition: LabelPosition = LabelPosition.BELOW,
    val hideEmptyCells: Boolean = false,
    val launchMode: LaunchMode = LaunchMode.HYBRID,
    /** Master switch. Off by default so the overlay never starts before the user opts in. */
    val serviceEnabled: Boolean = false,
    val disableInLandscape: Boolean = false,
    val hideServiceNotification: Boolean = false,
    val hideAzIndex: Boolean = false,
    val leftHandedLayout: Boolean = false,
) {
    fun toMap(): Map<String, String> = mapOf(
        SettingKeys.GRID_COLUMNS to gridColumns.toString(),
        SettingKeys.GRID_DIRECTION to gridDirection.name,
        SettingKeys.GRID_POSITION to gridPosition.name,
        SettingKeys.ICON_SIZE to iconSize.name,
        SettingKeys.LABEL_POSITION to labelPosition.name,
        SettingKeys.HIDE_EMPTY_CELLS to hideEmptyCells.toString(),
        SettingKeys.LAUNCH_MODE to launchMode.name,
        SettingKeys.SERVICE_ENABLED to serviceEnabled.toString(),
        SettingKeys.DISABLE_IN_LANDSCAPE to disableInLandscape.toString(),
        SettingKeys.HIDE_SERVICE_NOTIFICATION to hideServiceNotification.toString(),
        SettingKeys.HIDE_AZ_INDEX to hideAzIndex.toString(),
        SettingKeys.LEFT_HANDED_LAYOUT to leftHandedLayout.toString(),
    )

    companion object {
        const val DEFAULT_COLUMNS = 4
        val COLUMNS_RANGE: IntRange = 1..8

        /** Builds settings from stored strings; unknown keys are ignored, bad values use defaults. */
        fun fromMap(map: Map<String, String>): AppSettings {
            val d = AppSettings()
            return AppSettings(
                gridColumns = map[SettingKeys.GRID_COLUMNS]?.toIntOrNull()
                    ?.takeIf { it in COLUMNS_RANGE } ?: d.gridColumns,
                gridDirection = parseEnum(map[SettingKeys.GRID_DIRECTION], d.gridDirection),
                gridPosition = parseEnum(map[SettingKeys.GRID_POSITION], d.gridPosition),
                iconSize = parseEnum(map[SettingKeys.ICON_SIZE], d.iconSize),
                labelPosition = parseEnum(map[SettingKeys.LABEL_POSITION], d.labelPosition),
                hideEmptyCells = parseBool(map[SettingKeys.HIDE_EMPTY_CELLS], d.hideEmptyCells),
                launchMode = parseEnum(map[SettingKeys.LAUNCH_MODE], d.launchMode),
                serviceEnabled = parseBool(map[SettingKeys.SERVICE_ENABLED], d.serviceEnabled),
                disableInLandscape = parseBool(map[SettingKeys.DISABLE_IN_LANDSCAPE], d.disableInLandscape),
                hideServiceNotification = parseBool(map[SettingKeys.HIDE_SERVICE_NOTIFICATION], d.hideServiceNotification),
                hideAzIndex = parseBool(map[SettingKeys.HIDE_AZ_INDEX], d.hideAzIndex),
                leftHandedLayout = parseBool(map[SettingKeys.LEFT_HANDED_LAYOUT], d.leftHandedLayout),
            )
        }

        private inline fun <reified E : Enum<E>> parseEnum(value: String?, default: E): E =
            enumValues<E>().firstOrNull { it.name == value } ?: default

        private fun parseBool(value: String?, default: Boolean): Boolean = when (value) {
            "true" -> true
            "false" -> false
            else -> default
        }
    }
}
