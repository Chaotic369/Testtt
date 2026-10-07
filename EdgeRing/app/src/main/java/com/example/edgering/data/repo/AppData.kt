package com.example.edgering.data.repo

import android.content.Context
import com.example.edgering.data.backup.BackupRepository
import com.example.edgering.data.db.EdgeRingDatabase

/** Process-wide holder for the data layer. Nothing is opened until first use. */
class AppData private constructor(context: Context) {
    val database: EdgeRingDatabase by lazy { EdgeRingDatabase.create(context) }
    val settings: SettingsRepository by lazy { SettingsRepository(context) }
    val zones: ZoneRepository by lazy { ZoneRepository(database) }
    val triggers: TriggerRepository by lazy { TriggerRepository(database) }
    val items: ItemRepository by lazy { ItemRepository(database) }
    val appCache: AppCacheRepository by lazy { AppCacheRepository(database) }
    val backup: BackupRepository by lazy { BackupRepository(database, settings) }

    companion object {
        @Volatile
        private var instance: AppData? = null

        fun get(context: Context): AppData =
            instance ?: synchronized(this) {
                instance ?: AppData(context.applicationContext).also { instance = it }
            }
    }
}
