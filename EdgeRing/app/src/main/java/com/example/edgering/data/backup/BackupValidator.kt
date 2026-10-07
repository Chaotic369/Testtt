package com.example.edgering.data.backup

import com.example.edgering.data.model.ItemType
import com.example.edgering.data.model.Limits
import com.example.edgering.data.model.TriggerGeometry
import com.example.edgering.data.model.TriggerPosition
import com.example.edgering.data.model.TriggerRules

enum class BackupIssueCode {
    MALFORMED,
    TOO_LARGE,
    WRONG_FORMAT,
    UNSUPPORTED_VERSION,
    TOO_MANY_ENTRIES,
    DUPLICATE_ID,
    INVALID_ID,
    UNKNOWN_VALUE,
    BAD_TEXT,
    BAD_POSITION,
    MISSING_ZONE,
    MISSING_PARENT,
    PARENT_NOT_FOLDER,
    PARENT_ZONE_MISMATCH,
    PARENT_CYCLE,
    BAD_TRIGGER_GEOMETRY,
    DEFAULT_TRIGGER_COUNT,
}

/** [detail] only ever holds a location such as "items[3]", never user content (privacy). */
data class BackupIssue(val code: BackupIssueCode, val detail: String? = null)

/**
 * Checks a decoded [BackupFile] completely BEFORE anything is written, so a bad file can never
 * damage existing data. An empty result means the file is safe to import.
 */
object BackupValidator {
    const val MAX_REPORTED_ISSUES = 20

    fun validate(file: BackupFile): List<BackupIssue> {
        if (file.format != BackupFile.FORMAT) return listOf(BackupIssue(BackupIssueCode.WRONG_FORMAT))
        if (file.schemaVersion !in 1..BackupFile.CURRENT_VERSION) {
            return listOf(BackupIssue(BackupIssueCode.UNSUPPORTED_VERSION, "version ${file.schemaVersion}"))
        }
        if (file.zones.size > Limits.MAX_ZONES || file.triggers.size > Limits.MAX_TRIGGERS ||
            file.items.size > Limits.MAX_ITEMS || file.settings.size > Limits.MAX_SETTINGS
        ) {
            return listOf(BackupIssue(BackupIssueCode.TOO_MANY_ENTRIES))
        }

        val issues = ArrayList<BackupIssue>()
        fun report(code: BackupIssueCode, detail: String) {
            if (issues.size < MAX_REPORTED_ISSUES) issues.add(BackupIssue(code, detail))
        }
        fun badText(s: String, max: Int) = s.isBlank() || s.length > max

        val zoneIds = HashSet<Long>()
        file.zones.forEachIndexed { i, z ->
            if (z.id <= 0) report(BackupIssueCode.INVALID_ID, "zones[$i]")
            else if (!zoneIds.add(z.id)) report(BackupIssueCode.DUPLICATE_ID, "zones[$i]")
            if (badText(z.name, Limits.MAX_NAME_LENGTH)) report(BackupIssueCode.BAD_TEXT, "zones[$i]")
            if (z.position < 0) report(BackupIssueCode.BAD_POSITION, "zones[$i]")
        }

        val triggerIds = HashSet<Long>()
        file.triggers.forEachIndexed { i, t ->
            if (t.id <= 0) report(BackupIssueCode.INVALID_ID, "triggers[$i]")
            else if (!triggerIds.add(t.id)) report(BackupIssueCode.DUPLICATE_ID, "triggers[$i]")
            if (badText(t.name, TriggerRules.MAX_NAME_LENGTH)) report(BackupIssueCode.BAD_TEXT, "triggers[$i]")
            if (TriggerPosition.entries.none { it.name == t.position }) report(BackupIssueCode.UNKNOWN_VALUE, "triggers[$i]")
            if (!TriggerRules.isValid(TriggerGeometry(t.lengthFraction, t.thicknessDp, t.offsetFraction))) {
                report(BackupIssueCode.BAD_TRIGGER_GEOMETRY, "triggers[$i]")
            }
            if (t.targetZoneId != null && t.targetZoneId !in zoneIds) report(BackupIssueCode.MISSING_ZONE, "triggers[$i]")
        }
        if (file.triggers.count { it.isDefault } != 1) report(BackupIssueCode.DEFAULT_TRIGGER_COUNT, "triggers")

        val itemsById = HashMap<Long, BackupItem>()
        file.items.forEachIndexed { i, item ->
            if (item.id <= 0) report(BackupIssueCode.INVALID_ID, "items[$i]")
            else if (itemsById.put(item.id, item) != null) report(BackupIssueCode.DUPLICATE_ID, "items[$i]")
            if (item.zoneId !in zoneIds) report(BackupIssueCode.MISSING_ZONE, "items[$i]")
            if (ItemType.entries.none { it.name == item.type }) report(BackupIssueCode.UNKNOWN_VALUE, "items[$i]")
            if (badText(item.label, Limits.MAX_LABEL_LENGTH)) report(BackupIssueCode.BAD_TEXT, "items[$i]")
            if ((item.target?.length ?: 0) > Limits.MAX_TARGET_LENGTH || (item.options?.length ?: 0) > Limits.MAX_OPTIONS_LENGTH) {
                report(BackupIssueCode.BAD_TEXT, "items[$i]")
            }
            if (item.position < 0) report(BackupIssueCode.BAD_POSITION, "items[$i]")
        }
        // Parent links are checked after all ids are known (a parent may appear after its child).
        file.items.forEachIndexed { i, item ->
            val parentId = item.parentItemId ?: return@forEachIndexed
            val parent = itemsById[parentId]
            when {
                parent == null -> report(BackupIssueCode.MISSING_PARENT, "items[$i]")
                parent.type != ItemType.FOLDER.name -> report(BackupIssueCode.PARENT_NOT_FOLDER, "items[$i]")
                parent.zoneId != item.zoneId -> report(BackupIssueCode.PARENT_ZONE_MISMATCH, "items[$i]")
                hasCycle(item, itemsById) -> report(BackupIssueCode.PARENT_CYCLE, "items[$i]")
            }
        }
        return issues
    }

    /** Walks up the parent chain; a chain longer than the item count must contain a loop. */
    private fun hasCycle(start: BackupItem, byId: Map<Long, BackupItem>): Boolean {
        var current: BackupItem? = start
        var steps = 0
        while (current?.parentItemId != null) {
            if (++steps > byId.size) return true
            current = byId[current.parentItemId]
        }
        return false
    }
}
