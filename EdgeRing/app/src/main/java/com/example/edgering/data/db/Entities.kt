package com.example.edgering.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.edgering.data.model.ItemType
import com.example.edgering.data.model.TriggerGeometry
import com.example.edgering.data.model.TriggerPosition

/** A category tab ("zone"). Deleting a zone deletes its items and un-targets its triggers. */
@Entity(tableName = "zones")
data class ZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val position: Int,
)

/** An edge touch strip. Exactly one row has [isDefault] = true; it cannot be deleted. */
@Entity(
    tableName = "triggers",
    foreignKeys = [
        ForeignKey(
            entity = ZoneEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetZoneId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("targetZoneId")],
)
data class TriggerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: TriggerPosition,
    val lengthFraction: Float = TriggerGeometry.DEFAULT_LENGTH,
    val thicknessDp: Int = TriggerGeometry.DEFAULT_THICKNESS_DP,
    val offsetFraction: Float = 0f,
    /** Zone opened first when this trigger is used; null means "the first zone". */
    val targetZoneId: Long? = null,
    val enabled: Boolean = true,
    val isDefault: Boolean = false,
)

/** Kept outside the entity so Room never mistakes it for a column. */
fun TriggerEntity.geometry(): TriggerGeometry = TriggerGeometry(lengthFraction, thicknessDp, offsetFraction)

/**
 * One cell in a zone's grid, or in a folder.
 *
 * - [parentItemId] null = top level of the zone; otherwise the id of a FOLDER item that holds it.
 *   A child always has the same [zoneId] as its parent (checked by the repository and validator).
 * - [target] by [type]: APP = "package/className"; INTENT_SHORTCUT = intent URI string;
 *   ACTION = action id; URL = the link; FOLDER = null; FS_FOLDER = folder URI or path.
 * - [options]: opaque JSON text for per-type settings (for example file-folder sorting, section 2.16).
 * - [customIcon]: name of an app-private icon file chosen by the user (M05); not part of backups yet.
 */
@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = ZoneEntity::class,
            parentColumns = ["id"],
            childColumns = ["zoneId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("zoneId"), Index("parentItemId")],
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val zoneId: Long,
    val parentItemId: Long? = null,
    val position: Int,
    val type: ItemType,
    val label: String,
    val target: String? = null,
    val customIcon: String? = null,
    val options: String? = null,
)

/** Cache of launchable activities, filled by the app index (M03). Not user data; never backed up. */
@Entity(tableName = "app_cache", primaryKeys = ["packageName", "className"])
data class AppCacheEntity(
    val packageName: String,
    val className: String,
    val label: String,
)
