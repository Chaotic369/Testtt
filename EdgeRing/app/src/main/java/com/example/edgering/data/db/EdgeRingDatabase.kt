package com.example.edgering.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [ZoneEntity::class, TriggerEntity::class, ItemEntity::class, AppCacheEntity::class],
    version = EdgeRingDatabase.VERSION,
    exportSchema = true,
)
abstract class EdgeRingDatabase : RoomDatabase() {
    abstract fun zoneDao(): ZoneDao
    abstract fun triggerDao(): TriggerDao
    abstract fun itemDao(): ItemDao
    abstract fun appCacheDao(): AppCacheDao

    companion object {
        const val NAME = "edgering.db"

        /** Bump together with a new entry in [MIGRATIONS]; the exported schema is committed under app/schemas. */
        const val VERSION = 1

        /**
         * Every schema change adds a [Migration] here AND a test in MigrationTest. There is
         * deliberately no destructive-migration fallback: user data is never dropped silently.
         */
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun create(context: Context): EdgeRingDatabase =
            Room.databaseBuilder(context.applicationContext, EdgeRingDatabase::class.java, NAME)
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
