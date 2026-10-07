package com.example.edgering.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun emptyMapGivesDefaults() = assertEquals(AppSettings(), AppSettings.fromMap(emptyMap()))

    @Test
    fun masterSwitchIsOffByDefault() = assertFalse(AppSettings().serviceEnabled)

    @Test
    fun everyFieldRoundTripsThroughTheMap() {
        val custom = AppSettings(
            gridColumns = 6,
            gridDirection = GridDirection.BOTTOM_TO_TOP,
            gridPosition = GridPosition.BOTTOM,
            iconSize = IconSize.LARGE,
            labelPosition = LabelPosition.AUTO_HIDE_ABOVE,
            hideEmptyCells = true,
            launchMode = LaunchMode.TOUCH_THEN_TAP,
            serviceEnabled = true,
            disableInLandscape = true,
            hideServiceNotification = true,
            hideAzIndex = true,
            leftHandedLayout = true,
        )
        assertEquals(custom, AppSettings.fromMap(custom.toMap()))
    }

    @Test
    fun toMapContainsExactlyTheDocumentedKeys() {
        val documented = setOf(
            "grid_columns", "grid_direction", "grid_position", "icon_size", "label_position",
            "hide_empty_cells", "launch_mode", "service_enabled", "disable_in_landscape",
            "hide_service_notification", "hide_az_index", "left_handed_layout",
        )
        assertEquals(documented, AppSettings().toMap().keys)
    }

    @Test
    fun corruptValuesFallBackToDefaultsPerField() {
        val parsed = AppSettings.fromMap(
            mapOf(
                SettingKeys.GRID_COLUMNS to "banana",
                SettingKeys.GRID_DIRECTION to "SIDEWAYS",
                SettingKeys.ICON_SIZE to "LARGE",
                SettingKeys.HIDE_AZ_INDEX to "yes",
                SettingKeys.LEFT_HANDED_LAYOUT to "true",
            ),
        )
        assertEquals(AppSettings.DEFAULT_COLUMNS, parsed.gridColumns)
        assertEquals(GridDirection.TOP_TO_BOTTOM, parsed.gridDirection)
        assertEquals(IconSize.LARGE, parsed.iconSize)
        assertFalse(parsed.hideAzIndex)
        assertTrue(parsed.leftHandedLayout)
    }

    @Test
    fun outOfRangeColumnsUseTheDefault() {
        assertEquals(4, AppSettings.fromMap(mapOf(SettingKeys.GRID_COLUMNS to "0")).gridColumns)
        assertEquals(4, AppSettings.fromMap(mapOf(SettingKeys.GRID_COLUMNS to "9")).gridColumns)
        assertEquals(1, AppSettings.fromMap(mapOf(SettingKeys.GRID_COLUMNS to "1")).gridColumns)
        assertEquals(8, AppSettings.fromMap(mapOf(SettingKeys.GRID_COLUMNS to "8")).gridColumns)
    }

    @Test
    fun unknownKeysAreIgnored() {
        assertEquals(AppSettings(), AppSettings.fromMap(mapOf("from_the_future" to "1")))
    }

    @Test
    fun serviceEnabledIsTheOnlyKeyExcludedFromBackups() =
        assertEquals(setOf(SettingKeys.SERVICE_ENABLED), SettingKeys.EXCLUDED_FROM_BACKUP)
}
