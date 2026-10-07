package com.example.edgering.data.repo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.edgering.data.db.EdgeRingDatabase
import com.example.edgering.data.db.TestDb
import com.example.edgering.data.db.geometry
import com.example.edgering.data.model.ItemType
import com.example.edgering.data.model.TriggerGeometry
import com.example.edgering.data.model.TriggerPosition
import com.example.edgering.data.model.TriggerRules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RepositoriesTest {
    private lateinit var db: EdgeRingDatabase
    private lateinit var zones: ZoneRepository
    private lateinit var triggers: TriggerRepository
    private lateinit var items: ItemRepository

    @Before
    fun setUp() {
        db = TestDb.create()
        zones = ZoneRepository(db)
        triggers = TriggerRepository(db)
        items = ItemRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun app(label: String) = ItemDraft(ItemType.APP, label, target = "pkg/$label")

    // --- zones ---

    @Test
    fun zonesAreAppendedInOrderAndNamesAreTrimmed() = runBlocking {
        zones.add("  One ", 1)
        zones.add("Two", 2)
        val all = zones.observeAll().first()
        assertEquals(listOf("One", "Two"), all.map { it.name })
        assertEquals(listOf(0, 1), all.map { it.position })
    }

    @Test
    fun blankZoneNameIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { zones.add("   ", 1) } }
    }

    @Test
    fun zoneReorderAppliesAValidPermutationAndRejectsAnInvalidOne() = runBlocking {
        val a = zones.add("A", 0)
        val b = zones.add("B", 0)
        val c = zones.add("C", 0)
        assertTrue(zones.reorder(listOf(c, a, b)))
        assertEquals(listOf(c, a, b), zones.observeAll().first().map { it.id })
        assertFalse(zones.reorder(listOf(a, b)))
        assertFalse(zones.reorder(listOf(a, a, b, c)))
        assertEquals(listOf(c, a, b), zones.observeAll().first().map { it.id })
    }

    @Test
    fun editAndDeleteReportMissingZones() = runBlocking {
        val a = zones.add("A", 0)
        assertTrue(zones.edit(a, "Renamed", 7))
        assertEquals("Renamed", zones.observeAll().first().single().name)
        assertFalse(zones.edit(999, "x", 0))
        assertTrue(zones.delete(a))
        assertFalse(zones.delete(a))
    }

    // --- triggers ---

    @Test
    fun ensureDefaultIsIdempotent() = runBlocking {
        val first = triggers.ensureDefault("Default")
        val second = triggers.ensureDefault("Other name")
        assertEquals(first, second)
        val all = triggers.observeAll().first()
        assertEquals(1, all.size)
        assertTrue(all.single().isDefault)
        assertEquals(TriggerPosition.RIGHT, all.single().position)
        assertEquals(TriggerGeometry(), all.single().geometry())
    }

    @Test
    fun defaultTriggerCannotBeDeleted() = runBlocking {
        val d = triggers.ensureDefault("Default")
        val extra = triggers.add("Extra", TriggerPosition.LEFT)
        assertFalse(triggers.delete(d))
        assertTrue(triggers.delete(extra))
        assertFalse(triggers.delete(extra))
        assertEquals(listOf(d), triggers.observeAll().first().map { it.id })
    }

    @Test
    fun triggerGeometryIsClampedAndUnknownTargetIsDropped() = runBlocking {
        val id = triggers.add("T", TriggerPosition.TOP, TriggerGeometry(5f, 500, 9f), targetZoneId = 123)
        val t = triggers.observeAll().first().single { it.id == id }
        assertTrue(TriggerRules.isValid(t.geometry()))
        assertNull(t.targetZoneId)
    }

    @Test
    fun triggerEditKeepsTheDefaultFlagAndValidatesTarget() = runBlocking {
        val d = triggers.ensureDefault("Default")
        val z = zones.add("Z", 0)
        assertTrue(triggers.edit(d, "Renamed", TriggerPosition.BOTTOM, TriggerGeometry(0.5f, 20, 0.1f), z, enabled = false))
        val t = triggers.observeAll().first().single()
        assertTrue(t.isDefault)
        assertEquals("Renamed", t.name)
        assertEquals(z, t.targetZoneId)
        assertFalse(t.enabled)
        assertTrue(triggers.edit(d, "Renamed", TriggerPosition.BOTTOM, TriggerGeometry(), 999, enabled = true))
        assertNull(triggers.observeAll().first().single().targetZoneId)
        assertFalse(triggers.edit(555, "x", TriggerPosition.LEFT, TriggerGeometry(), null, true))
    }

    // --- items ---

    @Test
    fun itemsGetSequentialPositionsPerContainer() = runBlocking {
        val z = zones.add("Z", 0)
        val a = items.add(z, null, app("a"))
        val b = items.add(z, null, app("b"))
        val folder = items.add(z, null, ItemDraft(ItemType.FOLDER, "F"))
        val child = items.add(z, folder, app("c"))
        val top = items.observeChildren(z, null).first()
        assertEquals(listOf(a, b, folder), top.map { it.id })
        assertEquals(listOf(0, 1, 2), top.map { it.position })
        assertEquals(listOf(child), items.observeChildren(z, folder).first().map { it.id })
        assertEquals(0, items.observeChildren(z, folder).first().single().position)
    }

    @Test
    fun addRejectsMissingZoneAndInvalidParents() = runBlocking {
        val z1 = zones.add("Z1", 0)
        val z2 = zones.add("Z2", 0)
        val plain = items.add(z1, null, app("plain"))!!
        val folder = items.add(z1, null, ItemDraft(ItemType.FOLDER, "F"))!!
        assertNull(items.add(999, null, app("x")))
        assertNull(items.add(z1, 999, app("x")))
        assertNull(items.add(z1, plain, app("x")))
        assertNull(items.add(z2, folder, app("x")))
        assertNotNull(items.add(z1, folder, app("ok")))
    }

    @Test
    fun itemLabelIsTrimmedTruncatedAndNeverBlank() = runBlocking {
        val z = zones.add("Z", 0)
        val id = items.add(z, null, app("  ${"x".repeat(300)}  "))!!
        assertEquals(128, items.observeChildren(z, null).first().single { it.id == id }.label.length)
        assertThrows(IllegalArgumentException::class.java) { runBlocking { items.add(z, null, app(" ")) } }
        Unit
    }

    @Test
    fun itemReorderIsScopedToSiblingsAndRejectsForeignIds() = runBlocking {
        val z = zones.add("Z", 0)
        val a = items.add(z, null, app("a"))!!
        val b = items.add(z, null, app("b"))!!
        val folder = items.add(z, null, ItemDraft(ItemType.FOLDER, "F"))!!
        val child = items.add(z, folder, app("c"))!!
        assertTrue(items.reorder(z, null, listOf(folder, b, a)))
        assertEquals(listOf(folder, b, a), items.observeChildren(z, null).first().map { it.id })
        assertFalse(items.reorder(z, null, listOf(folder, b, child)))
        assertFalse(items.reorder(z, null, listOf(folder, b)))
        assertEquals(listOf(folder, b, a), items.observeChildren(z, null).first().map { it.id })
    }

    @Test
    fun editChangesFieldsAndDeleteRemovesFolderContents() = runBlocking {
        val z = zones.add("Z", 0)
        val folder = items.add(z, null, ItemDraft(ItemType.FOLDER, "F"))!!
        val child = items.add(z, folder, app("c"))!!
        assertTrue(items.edit(child, ItemDraft(ItemType.URL, "Site", target = "https://example.org")))
        assertEquals("Site", items.observeChildren(z, folder).first().single().label)
        assertFalse(items.edit(999, app("x")))
        assertTrue(items.delete(folder))
        assertTrue(items.observeChildren(z, folder).first().isEmpty())
    }
}
