package com.example.edgering.data.backup

import androidx.room.withTransaction
import com.example.edgering.data.db.EdgeRingDatabase
import com.example.edgering.data.model.AppSettings
import com.example.edgering.data.repo.SettingsRepository
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

sealed interface ImportResult {
    /** [settingsApplied] is false only if the settings store failed AFTER the database import succeeded. */
    data class Success(val zones: Int, val triggers: Int, val items: Int, val settingsApplied: Boolean) : ImportResult

    /** The file was not accepted; nothing was changed. */
    data class Rejected(val issues: List<BackupIssue>) : ImportResult

    /** A storage error occurred; the database transaction was rolled back and existing data is unchanged. */
    data object StorageError : ImportResult
}

class BackupRepository(
    private val db: EdgeRingDatabase,
    private val settings: SettingsRepository,
    /** Seam for tests: runs the import writes atomically. Production uses a Room transaction. */
    private val inTransaction: suspend (suspend () -> Unit) -> Unit = { block -> db.withTransaction { block() } },
) {
    /** Returns the backup as JSON text. Reads all tables in one transaction so the snapshot is consistent. */
    suspend fun export(): String {
        val snapshot = db.withTransaction {
            Triple(db.zoneDao().getAll(), db.triggerDao().getAll(), db.itemDao().getAll())
        }
        val file = BackupMapper.toFile(snapshot.first, snapshot.second, snapshot.third, settings.current())
        return BackupCodec.encode(file)
    }

    /**
     * Order matters for safety: decode, validate everything, then replace the data in ONE
     * transaction. Any failure before or inside the transaction leaves existing data untouched.
     * The device-specific master switch (service_enabled) is never imported.
     */
    suspend fun import(text: String): ImportResult {
        val file = when (val decoded = BackupCodec.decode(text)) {
            is DecodeResult.Failed -> return ImportResult.Rejected(listOf(decoded.issue))
            is DecodeResult.Ok -> decoded.file
        }
        val issues = BackupValidator.validate(file)
        if (issues.isNotEmpty()) return ImportResult.Rejected(issues)

        val entities = BackupMapper.toEntities(file)
        try {
            inTransaction {
                // Deleting zones cascades to items and clears trigger targets.
                db.zoneDao().deleteAll()
                db.triggerDao().deleteAll()
                db.zoneDao().insertAll(entities.zones)
                db.triggerDao().insertAll(entities.triggers)
                db.itemDao().insertAll(entities.items)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return ImportResult.StorageError
        }

        val settingsApplied = try {
            val keepEnabled = settings.current().serviceEnabled
            settings.replace(AppSettings.fromMap(file.settings).copy(serviceEnabled = keepEnabled))
            true
        } catch (e: IOException) {
            false
        }
        return ImportResult.Success(entities.zones.size, entities.triggers.size, entities.items.size, settingsApplied)
    }
}
