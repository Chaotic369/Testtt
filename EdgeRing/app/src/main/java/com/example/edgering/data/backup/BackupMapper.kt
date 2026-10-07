package com.example.edgering.data.backup

import com.example.edgering.data.db.ItemEntity
import com.example.edgering.data.db.TriggerEntity
import com.example.edgering.data.db.ZoneEntity
import com.example.edgering.data.model.AppSettings
import com.example.edgering.data.model.ItemType
import com.example.edgering.data.model.SettingKeys
import com.example.edgering.data.model.TriggerPosition

data class BackupEntities(
    val zones: List<ZoneEntity>,
    val triggers: List<TriggerEntity>,
    /** Parents come before their children, so rows can be inserted in this order. */
    val items: List<ItemEntity>,
)

/** Entity <-> DTO conversion. [toEntities] must only be called on a file that passed [BackupValidator]. */
object BackupMapper {
    fun toFile(
        zones: List<ZoneEntity>,
        triggers: List<TriggerEntity>,
        items: List<ItemEntity>,
        settings: AppSettings,
    ): BackupFile = BackupFile(
        zones = zones.map { BackupZone(it.id, it.name, it.colorArgb, it.position) },
        triggers = triggers.map {
            BackupTrigger(
                it.id, it.name, it.position.name, it.lengthFraction, it.thicknessDp,
                it.offsetFraction, it.targetZoneId, it.enabled, it.isDefault,
            )
        },
        items = items.map {
            BackupItem(it.id, it.zoneId, it.parentItemId, it.position, it.type.name, it.label, it.target, it.options)
        },
        settings = settings.toMap() - SettingKeys.EXCLUDED_FROM_BACKUP,
    )

    fun toEntities(file: BackupFile): BackupEntities = BackupEntities(
        zones = file.zones.map { ZoneEntity(it.id, it.name.trim(), it.colorArgb, it.position) },
        triggers = file.triggers.map {
            TriggerEntity(
                id = it.id,
                name = it.name.trim(),
                position = TriggerPosition.valueOf(it.position),
                lengthFraction = it.lengthFraction,
                thicknessDp = it.thicknessDp,
                offsetFraction = it.offsetFraction,
                targetZoneId = it.targetZoneId,
                enabled = it.enabled,
                isDefault = it.isDefault,
            )
        },
        items = parentsFirst(file.items).map {
            ItemEntity(
                id = it.id,
                zoneId = it.zoneId,
                parentItemId = it.parentItemId,
                position = it.position,
                type = ItemType.valueOf(it.type),
                label = it.label.trim(),
                target = it.target,
                options = it.options,
            )
        },
    )

    /** Stable sort by nesting depth. Assumes a valid (acyclic, complete) parent graph. */
    internal fun parentsFirst(items: List<BackupItem>): List<BackupItem> {
        val byId = items.associateBy { it.id }
        val depth = HashMap<Long, Int>()
        fun depthOf(item: BackupItem): Int {
            depth[item.id]?.let { return it }
            var d = 0
            var current = item
            while (true) {
                val parent = current.parentItemId?.let { byId[it] } ?: break
                d++
                current = parent
            }
            depth[item.id] = d
            return d
        }
        return items.sortedBy { depthOf(it) }
    }
}
