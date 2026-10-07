package com.example.edgering.data.model

/*
 * Enums that are persisted BY NAME (Room TEXT columns, DataStore strings, backup JSON).
 * Never rename or remove a constant without a migration and a CHANGELOG entry; adding is safe.
 */

/** What an item does when released on. `target` meaning per type is documented on ItemEntity. */
enum class ItemType { APP, INTENT_SHORTCUT, ACTION, URL, FOLDER, FS_FOLDER }

/**
 * Edge a trigger sits on. Corner/centre variants of the reference app are expressed through
 * length + offset instead; M06 may add constants if device testing shows they are needed.
 */
enum class TriggerPosition { LEFT, RIGHT, TOP, BOTTOM }

enum class GridDirection { TOP_TO_BOTTOM, BOTTOM_TO_TOP }

enum class GridPosition { TOP, CENTER, BOTTOM }

enum class IconSize { SMALL, MEDIUM, LARGE }

enum class LabelPosition { ABOVE, BELOW, HIDDEN, AUTO_HIDE_ABOVE, AUTO_HIDE_BELOW }

enum class LaunchMode { HYBRID, SINGLE_TOUCH, TOUCH_THEN_TAP }
