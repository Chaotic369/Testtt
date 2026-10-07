package com.example.edgering.data.repo

import androidx.room.withTransaction
import com.example.edgering.data.db.AppCacheEntity
import com.example.edgering.data.db.EdgeRingDatabase
import com.example.edgering.data.db.ItemEntity
import com.example.edgering.data.db.TriggerEntity
import com.example.edgering.data.db.ZoneEntity
import com.example.edgering.data.model.ItemType
import com.example.edgering.data.model.Limits.MAX_LABEL_LENGTH
import com.example.edgering.data.model.Limits.MAX_NAME_LENGTH
import com.example.edgering.data.model.Reorder
import com.example.edgering.data.model.TriggerGeometry
import com.example.edgering.data.model.TriggerPosition
import com.example.edgering.data.model.TriggerRules
import kotlinx.coroutines.flow.Flow

private fun cleanName(raw: String, max: Int): String {
    val trimmed = raw.trim()
    require(trimmed.isNotEmpty()) { "name must not be blank" }
    return trimmed.take(max)
}

class ZoneRepository(private val db: EdgeRingDatabase) {
    private val dao = db.zoneDao()

    fun observeAll(): Flow<List<ZoneEntity>> = dao.observeAll()

    suspend fun add(name: String, colorArgb: Int): Long = db.withTransaction {
        dao.insert(ZoneEntity(name = cleanName(name, MAX_NAME_LENGTH), colorArgb = colorArgb, position = dao.nextPosition()))
    }

    /** Returns false when the zone no longer exists. */
    suspend fun edit(id: Long, name: String, colorArgb: Int): Boolean = db.withTransaction {
        val zone = dao.getById(id) ?: return@withTransaction false
        dao.update(zone.copy(name = cleanName(name, MAX_NAME_LENGTH), colorArgb = colorArgb)) > 0
    }

    /** [orderedIds] must be exactly the current zone ids; otherwise nothing changes and false is returned. */
    suspend fun reorder(orderedIds: List<Long>): Boolean = db.withTransaction {
        if (!Reorder.isSamePermutation(dao.orderedIds(), orderedIds)) return@withTransaction false
        orderedIds.forEachIndexed { index, id -> dao.setPosition(id, index) }
        true
    }

    /** Deletes the zone, its items (cascade) and clears it as target of any trigger (set null). */
    suspend fun delete(id: Long): Boolean = dao.deleteById(id) > 0
}

class TriggerRepository(private val db: EdgeRingDatabase) {
    private val dao = db.triggerDao()

    fun observeAll(): Flow<List<TriggerEntity>> = dao.observeAll()

    /** Creates the default trigger if none exists. [name] comes from a string resource. Returns its id. */
    suspend fun ensureDefault(name: String): Long = db.withTransaction {
        dao.getDefault()?.id ?: dao.insert(
            TriggerEntity(name = cleanName(name, TriggerRules.MAX_NAME_LENGTH), position = TriggerPosition.RIGHT, isDefault = true),
        )
    }

    suspend fun add(
        name: String,
        position: TriggerPosition,
        geometry: TriggerGeometry = TriggerGeometry(),
        targetZoneId: Long? = null,
    ): Long = db.withTransaction {
        val g = TriggerRules.clamp(geometry)
        dao.insert(
            TriggerEntity(
                name = cleanName(name, TriggerRules.MAX_NAME_LENGTH),
                position = position,
                lengthFraction = g.lengthFraction,
                thicknessDp = g.thicknessDp,
                offsetFraction = g.offsetFraction,
                targetZoneId = targetZoneId?.takeIf { db.zoneDao().getById(it) != null },
            ),
        )
    }

    /** Edits everything except identity and the default flag. Returns false if the trigger is gone. */
    suspend fun edit(
        id: Long,
        name: String,
        position: TriggerPosition,
        geometry: TriggerGeometry,
        targetZoneId: Long?,
        enabled: Boolean,
    ): Boolean = db.withTransaction {
        val current = dao.getById(id) ?: return@withTransaction false
        val g = TriggerRules.clamp(geometry)
        val target = targetZoneId?.takeIf { db.zoneDao().getById(it) != null }
        dao.update(
            current.copy(
                name = cleanName(name, TriggerRules.MAX_NAME_LENGTH),
                position = position,
                lengthFraction = g.lengthFraction,
                thicknessDp = g.thicknessDp,
                offsetFraction = g.offsetFraction,
                targetZoneId = target,
                enabled = enabled,
            ),
        ) > 0
    }

    /** False for the default trigger and for unknown ids. */
    suspend fun delete(id: Long): Boolean = dao.deleteIfNotDefault(id) > 0
}

/** User-editable fields of an item; identity, zone, parent and position are managed by the repository. */
data class ItemDraft(
    val type: ItemType,
    val label: String,
    val target: String? = null,
    val customIcon: String? = null,
    val options: String? = null,
)

class ItemRepository(private val db: EdgeRingDatabase) {
    private val dao = db.itemDao()

    fun observeChildren(zoneId: Long, parentItemId: Long? = null): Flow<List<ItemEntity>> =
        dao.observeChildren(zoneId, parentItemId)

    /**
     * Appends an item to the zone (or folder). Returns null when the zone does not exist, or the
     * parent is missing, is not a FOLDER, or belongs to another zone.
     */
    suspend fun add(zoneId: Long, parentItemId: Long?, draft: ItemDraft): Long? = db.withTransaction {
        if (db.zoneDao().getById(zoneId) == null) return@withTransaction null
        if (parentItemId != null) {
            val parent = dao.getById(parentItemId)
            if (parent == null || parent.type != ItemType.FOLDER || parent.zoneId != zoneId) return@withTransaction null
        }
        dao.insert(
            ItemEntity(
                zoneId = zoneId,
                parentItemId = parentItemId,
                position = dao.nextPosition(zoneId, parentItemId),
                type = draft.type,
                label = cleanName(draft.label, MAX_LABEL_LENGTH),
                target = draft.target,
                customIcon = draft.customIcon,
                options = draft.options,
            ),
        )
    }

    /** Replaces the editable fields. Returns false if the item is gone. */
    suspend fun edit(id: Long, draft: ItemDraft): Boolean = db.withTransaction {
        val item = dao.getById(id) ?: return@withTransaction false
        dao.update(
            item.copy(
                type = draft.type,
                label = cleanName(draft.label, MAX_LABEL_LENGTH),
                target = draft.target,
                customIcon = draft.customIcon,
                options = draft.options,
            ),
        ) > 0
    }

    /** [orderedIds] must be exactly the current siblings; otherwise nothing changes and false is returned. */
    suspend fun reorder(zoneId: Long, parentItemId: Long?, orderedIds: List<Long>): Boolean = db.withTransaction {
        if (!Reorder.isSamePermutation(dao.siblingIds(zoneId, parentItemId), orderedIds)) return@withTransaction false
        orderedIds.forEachIndexed { index, id -> dao.setPosition(id, index) }
        true
    }

    /** Deleting a folder deletes everything inside it (cascade). */
    suspend fun delete(id: Long): Boolean = dao.deleteById(id) > 0
}

class AppCacheRepository(db: EdgeRingDatabase) {
    private val dao = db.appCacheDao()

    fun observeAll(): Flow<List<AppCacheEntity>> = dao.observeAll()
    suspend fun getAll(): List<AppCacheEntity> = dao.getAll()
    suspend fun upsertAll(apps: List<AppCacheEntity>) = dao.upsertAll(apps)
    suspend fun remove(packageName: String, className: String) = dao.delete(packageName, className)
    suspend fun removePackage(packageName: String) = dao.deletePackage(packageName)
    suspend fun clear() = dao.deleteAll()
}
