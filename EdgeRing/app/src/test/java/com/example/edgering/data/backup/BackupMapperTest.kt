package com.example.edgering.data.backup

import com.example.edgering.data.db.ItemEntity
import com.example.edgering.data.model.AppSettings
import com.example.edgering.data.model.ItemType
import com.example.edgering.data.model.LaunchMode
import com.example.edgering.data.model.SettingKeys
import com.example.edgering.data.model.TriggerPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupMapperTest {
    @Test
    fun entitiesRoundTripThroughTheFileModel() {
        val entities = BackupMapper.toEntities(TestBackups.valid())
        val file = BackupMapper.toFile(entities.zones, entities.triggers, entities.items, AppSettings())
        // Same data (items come back parents-first, so compare as sets keyed by id).
        val original = TestBackups.valid()
        assertEquals(original.zones, file.zones)
        assertEquals(original.triggers, file.triggers)
        assertEquals(original.items.associateBy { it.id }, file.items.associateBy { it.id })
    }

    @Test
    fun itemsAreOrderedParentsFirstEvenWhenTheFileListsChildrenFirst() {
        val items = BackupMapper.toEntities(TestBackups.valid()).items
        val seen = HashSet<Long>()
        for (item in items) {
            item.parentItemId?.let { assertTrue("parent ${it} before child ${item.id}", it in seen) }
            seen.add(item.id)
        }
        assertEquals(3, items.size)
    }

    @Test
    fun parentsFirstHandlesDeepChainsInReverseOrder() {
        val chain = (1L..30L).map { BackupItem(it, 1, if (it == 1L) null else it - 1, 0, "FOLDER", "F$it") }
        val sorted = BackupMapper.parentsFirst(chain.reversed())
        assertEquals((1L..30L).toList(), sorted.map { it.id })
    }

    @Test
    fun enumsAndFlagsMapCorrectly() {
        val e = BackupMapper.toEntities(TestBackups.valid())
        assertEquals(TriggerPosition.LEFT, e.triggers[1].position)
        assertFalse(e.triggers[1].enabled)
        assertTrue(e.triggers[0].isDefault)
        assertEquals(ItemType.FOLDER, e.items.first { it.id == 1L }.type)
        assertNull(e.items.first { it.id == 1L }.parentItemId)
    }

    @Test
    fun customIconIsNotPartOfBackupVersion1() {
        val withIcon = ItemEntity(id = 5, zoneId = 1, position = 0, type = ItemType.APP, label = "A", customIcon = "icon_5.png")
        val file = BackupMapper.toFile(emptyList(), emptyList(), listOf(withIcon), AppSettings())
        assertNull(BackupMapper.toEntities(file).items.single().customIcon)
    }

    @Test
    fun masterSwitchIsNeverExported() {
        val file = BackupMapper.toFile(emptyList(), emptyList(), emptyList(), AppSettings(serviceEnabled = true, launchMode = LaunchMode.SINGLE_TOUCH))
        assertFalse(SettingKeys.SERVICE_ENABLED in file.settings)
        assertEquals("SINGLE_TOUCH", file.settings[SettingKeys.LAUNCH_MODE])
    }

    @Test
    fun namesAreTrimmedOnImport() {
        val v = TestBackups.valid()
        val e = BackupMapper.toEntities(v.copy(zones = listOf(v.zones[0].copy(name = "  Social  "), v.zones[1])))
        assertEquals("Social", e.zones[0].name)
    }
}
