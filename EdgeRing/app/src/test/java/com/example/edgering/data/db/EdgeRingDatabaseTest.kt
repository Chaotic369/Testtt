package com.example.edgering.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.edgering.data.model.ItemType
import com.example.edgering.data.model.TriggerPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Runs on the JVM through Robolectric (executed by `./gradlew test` in CI). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class EdgeRingDatabaseTest {
    private lateinit var db: EdgeRingDatabase

    @Before
    fun setUp() {
        db = TestDb.create()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun zone(name: String = "Z") = db.zoneDao().insert(ZoneEntity(name = name, colorArgb = 0, position = 0))

    private suspend fun item(zoneId: Long, parent: Long? = null, type: ItemType = ItemType.APP, label: String = "i") =
        db.itemDao().insert(ItemEntity(zoneId = zoneId, parentItemId = parent, position = 0, type = type, label = label))

    @Test
    fun foreignKeysAreEnforced() = runBlocking {
        assertThrows(SQLiteConstraintException::class.java) { runBlocking { item(zoneId = 999) } }
        Unit
    }

    @Test
    fun deletingAZoneDeletesItsItemsIncludingNestedOnes() = runBlocking {
        val z = zone()
        val folder = item(z, type = ItemType.FOLDER)
        item(z, parent = folder)
        val other = zone("Other")
        item(other)
        db.zoneDao().deleteById(z)
        val remaining = db.itemDao().getAll()
        assertEquals(1, remaining.size)
        assertEquals(other, remaining.single().zoneId)
    }

    @Test
    fun deletingAFolderDeletesItsChildrenOnly() = runBlocking {
        val z = zone()
        val folder = item(z, type = ItemType.FOLDER)
        val child = item(z, parent = folder)
        val sibling = item(z)
        db.itemDao().deleteById(folder)
        assertEquals(listOf(sibling), db.itemDao().getAll().map { it.id })
        assertNull(db.itemDao().getById(child))
    }

    @Test
    fun deletingAZoneClearsTriggerTargetButKeepsTheTrigger() = runBlocking {
        val z = zone()
        val t = db.triggerDao().insert(TriggerEntity(name = "T", position = TriggerPosition.LEFT, targetZoneId = z))
        db.zoneDao().deleteById(z)
        val trigger = db.triggerDao().getById(t)
        assertNotNull(trigger)
        assertNull(trigger?.targetZoneId)
    }

    @Test
    fun defaultTriggerCannotBeDeletedButOthersCan() = runBlocking {
        val d = db.triggerDao().insert(TriggerEntity(name = "D", position = TriggerPosition.RIGHT, isDefault = true))
        val o = db.triggerDao().insert(TriggerEntity(name = "O", position = TriggerPosition.LEFT))
        assertEquals(0, db.triggerDao().deleteIfNotDefault(d))
        assertEquals(1, db.triggerDao().deleteIfNotDefault(o))
        assertEquals(listOf(d), db.triggerDao().getAll().map { it.id })
    }

    @Test
    fun nullParentMatchesOnlyTopLevelItems() = runBlocking {
        val z = zone()
        val folder = item(z, type = ItemType.FOLDER)
        val child = item(z, parent = folder)
        assertEquals(listOf(folder), db.itemDao().observeChildren(z, null).first().map { it.id })
        assertEquals(listOf(child), db.itemDao().observeChildren(z, folder).first().map { it.id })
        assertEquals(listOf(folder), db.itemDao().siblingIds(z, null))
    }

    @Test
    fun nextPositionIsScopedPerParent() = runBlocking {
        val z = zone()
        assertEquals(0, db.itemDao().nextPosition(z, null))
        val folder = db.itemDao().insert(ItemEntity(zoneId = z, position = 4, type = ItemType.FOLDER, label = "F"))
        assertEquals(5, db.itemDao().nextPosition(z, null))
        assertEquals(0, db.itemDao().nextPosition(z, folder))
    }

    @Test
    fun explicitIdsAreKeptOnInsert() = runBlocking {
        db.zoneDao().insertAll(listOf(ZoneEntity(id = 42, name = "A", colorArgb = 1, position = 0)))
        assertNotNull(db.zoneDao().getById(42))
    }

    @Test
    fun failedTransactionRollsBackEverything() = runBlocking {
        zone("keep")
        try {
            db.withTransaction {
                db.zoneDao().deleteAll()
                error("boom")
            }
        } catch (expected: IllegalStateException) {
            // expected
        }
        assertEquals(1, db.zoneDao().getAll().size)
        assertTrue(db.zoneDao().getAll().single().name == "keep")
    }

    @Test
    fun appCacheUpsertReplacesAndDeletesByPackage() = runBlocking {
        val dao = db.appCacheDao()
        dao.upsertAll(listOf(AppCacheEntity("p", "a", "Old"), AppCacheEntity("p", "b", "B"), AppCacheEntity("q", "a", "Q")))
        dao.upsertAll(listOf(AppCacheEntity("p", "a", "New")))
        assertEquals("New", dao.getAll().first { it.packageName == "p" && it.className == "a" }.label)
        dao.deletePackage("p")
        assertEquals(listOf("q"), dao.getAll().map { it.packageName })
    }
}
