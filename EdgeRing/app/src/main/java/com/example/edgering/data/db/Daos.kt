package com.example.edgering.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {
    @Query("SELECT * FROM zones ORDER BY position, id")
    fun observeAll(): Flow<List<ZoneEntity>>

    @Query("SELECT * FROM zones ORDER BY position, id")
    suspend fun getAll(): List<ZoneEntity>

    @Query("SELECT * FROM zones WHERE id = :id")
    suspend fun getById(id: Long): ZoneEntity?

    @Query("SELECT id FROM zones ORDER BY position, id")
    suspend fun orderedIds(): List<Long>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM zones")
    suspend fun nextPosition(): Int

    /** Returns the new row id. An explicit non-zero id is kept (used by backup import). */
    @Insert
    suspend fun insert(zone: ZoneEntity): Long

    @Insert
    suspend fun insertAll(zones: List<ZoneEntity>)

    @Update
    suspend fun update(zone: ZoneEntity): Int

    @Query("UPDATE zones SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)

    @Query("DELETE FROM zones WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM zones")
    suspend fun deleteAll()
}

@Dao
interface TriggerDao {
    @Query("SELECT * FROM triggers ORDER BY isDefault DESC, id")
    fun observeAll(): Flow<List<TriggerEntity>>

    @Query("SELECT * FROM triggers ORDER BY isDefault DESC, id")
    suspend fun getAll(): List<TriggerEntity>

    @Query("SELECT * FROM triggers WHERE id = :id")
    suspend fun getById(id: Long): TriggerEntity?

    @Query("SELECT * FROM triggers WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): TriggerEntity?

    @Insert
    suspend fun insert(trigger: TriggerEntity): Long

    @Insert
    suspend fun insertAll(triggers: List<TriggerEntity>)

    @Update
    suspend fun update(trigger: TriggerEntity): Int

    /** Database-level guard: the default trigger is never deleted. Returns rows deleted (0 or 1). */
    @Query("DELETE FROM triggers WHERE id = :id AND isDefault = 0")
    suspend fun deleteIfNotDefault(id: Long): Int

    @Query("DELETE FROM triggers")
    suspend fun deleteAll()
}

@Dao
interface ItemDao {
    /** `IS` (not `=`) so that a null [parentItemId] matches top-level items. */
    @Query(
        "SELECT * FROM items WHERE zoneId = :zoneId AND parentItemId IS :parentItemId " +
            "ORDER BY position, id",
    )
    fun observeChildren(zoneId: Long, parentItemId: Long?): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items ORDER BY zoneId, parentItemId, position, id")
    suspend fun getAll(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): ItemEntity?

    @Query(
        "SELECT id FROM items WHERE zoneId = :zoneId AND parentItemId IS :parentItemId " +
            "ORDER BY position, id",
    )
    suspend fun siblingIds(zoneId: Long, parentItemId: Long?): List<Long>

    @Query(
        "SELECT COALESCE(MAX(position) + 1, 0) FROM items " +
            "WHERE zoneId = :zoneId AND parentItemId IS :parentItemId",
    )
    suspend fun nextPosition(zoneId: Long, parentItemId: Long?): Int

    @Insert
    suspend fun insert(item: ItemEntity): Long

    /** Parents must come before their children in [items] (foreign keys are checked per row). */
    @Insert
    suspend fun insertAll(items: List<ItemEntity>)

    @Update
    suspend fun update(item: ItemEntity): Int

    @Query("UPDATE items SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM items")
    suspend fun deleteAll()
}

@Dao
interface AppCacheDao {
    @Query("SELECT * FROM app_cache ORDER BY label COLLATE NOCASE, packageName, className")
    fun observeAll(): Flow<List<AppCacheEntity>>

    @Query("SELECT * FROM app_cache")
    suspend fun getAll(): List<AppCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<AppCacheEntity>)

    @Query("DELETE FROM app_cache WHERE packageName = :packageName AND className = :className")
    suspend fun delete(packageName: String, className: String)

    @Query("DELETE FROM app_cache WHERE packageName = :packageName")
    suspend fun deletePackage(packageName: String)

    @Query("DELETE FROM app_cache")
    suspend fun deleteAll()
}
