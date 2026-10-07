package com.example.edgering.data.backup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.edgering.data.db.EdgeRingDatabase
import com.example.edgering.data.db.TestDb
import com.example.edgering.data.model.AppSettings
import com.example.edgering.data.model.ItemType
import com.example.edgering.data.model.LaunchMode
import com.example.edgering.data.model.SettingKeys
import com.example.edgering.data.model.TriggerPosition
import com.example.edgering.data.repo.ItemDraft
import com.example.edgering.data.repo.ItemRepository
import com.example.edgering.data.repo.SettingsRepository
import com.example.edgering.data.repo.TriggerRepository
import com.example.edgering.data.repo.ZoneRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BackupRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var db: EdgeRingDatabase
    private lateinit var settings: SettingsRepository
    private lateinit var backup: BackupRepository

    @Before
    fun setUp() {
        db = TestDb.create()
        settings = SettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "s.preferences_pb") })
        backup = BackupRepository(db, settings)
    }

    @After
    fun tearDown() {
        db.close()
        scope.cancel()
    }

    /** Snapshot of all user data, for "nothing changed" assertions. */
    private suspend fun snapshot() =
        listOf(db.zoneDao().getAll(), db.triggerDao().getAll(), db.itemDao().getAll(), settings.current())

    private suspend fun seed() {
        val zones = ZoneRepository(db)
        val z = zones.add("Seeded", 5)
        TriggerRepository(db).ensureDefault("Default")
        val folder = ItemRepository(db).add(z, null, ItemDraft(ItemType.FOLDER, "Folder"))
        ItemRepository(db).add(z, folder, ItemDraft(ItemType.APP, "App", target = "a/b"))
        settings.update { it.copy(gridColumns = 6, launchMode = LaunchMode.TOUCH_THEN_TAP) }
    }

    private fun assertRejected(result: ImportResult, code: BackupIssueCode) {
        assertTrue("was $result", result is ImportResult.Rejected)
        assertTrue((result as ImportResult.Rejected).issues.any { it.code == code })
    }

    @Test
    fun exportThenImportReproducesTheData() = runBlocking {
        seed()
        val before = snapshot()
        val text = backup.export()
        // Wipe by importing a different valid file, then restore from the export.
        assertTrue(backup.import(BackupCodec.encode(TestBackups.valid())) is ImportResult.Success)
        val result = backup.import(text)
        assertTrue("was $result", result is ImportResult.Success)
        assertEquals(before, snapshot())
    }

    @Test
    fun importReplacesExistingDataAndKeepsIdsAndNesting() = runBlocking {
        seed()
        val result = backup.import(BackupCodec.encode(TestBackups.valid())) as ImportResult.Success
        assertEquals(2, result.zones)
        assertEquals(2, result.triggers)
        assertEquals(3, result.items)
        assertTrue(result.settingsApplied)
        assertEquals(listOf(1L, 2L), db.zoneDao().getAll().map { it.id })
        assertEquals(1L, db.itemDao().getById(2)?.parentItemId)
        assertEquals(5, settings.current().gridColumns)
        assertEquals(LaunchMode.SINGLE_TOUCH, settings.current().launchMode)
    }

    @Test
    fun invalidFileLeavesEverythingUntouched() = runBlocking {
        seed()
        val before = snapshot()
        val bad = TestBackups.valid().copy(items = TestBackups.valid().items.map { it.copy(zoneId = 99) })
        assertRejected(backup.import(BackupCodec.encode(bad)), BackupIssueCode.MISSING_ZONE)
        assertEquals(before, snapshot())
    }

    @Test
    fun malformedAndNewerVersionFilesLeaveEverythingUntouched() = runBlocking {
        seed()
        val before = snapshot()
        assertRejected(backup.import("{ this is not json"), BackupIssueCode.MALFORMED)
        assertRejected(backup.import(""), BackupIssueCode.MALFORMED)
        val newer = TestBackups.valid().copy(schemaVersion = BackupFile.CURRENT_VERSION + 1)
        assertRejected(backup.import(BackupCodec.encode(newer)), BackupIssueCode.UNSUPPORTED_VERSION)
        assertEquals(before, snapshot())
    }

    @Test
    fun masterSwitchIsNeitherExportedNorImported() = runBlocking {
        settings.update { it.copy(serviceEnabled = true) }
        assertFalse(backup.export().contains(SettingKeys.SERVICE_ENABLED))
        backup.import(BackupCodec.encode(TestBackups.valid()))
        assertTrue(settings.current().serviceEnabled)
        settings.update { it.copy(serviceEnabled = false) }
        backup.import(BackupCodec.encode(TestBackups.valid()))
        assertFalse(settings.current().serviceEnabled)
    }

    @Test
    fun storageErrorIsReportedAndLeavesTheDatabaseUntouched() = runBlocking {
        seed()
        val before = snapshot()
        val failing = BackupRepository(db, settings) { _ -> throw IllegalStateException("disk full") }
        assertEquals(ImportResult.StorageError, failing.import(BackupCodec.encode(TestBackups.valid())))
        assertEquals(before, snapshot())
    }

    @Test
    fun partialWritesInsideAFailedTransactionAreRolledBack() = runBlocking {
        seed()
        val before = snapshot()
        val half = BackupRepository(db, settings) { block ->
            db.withTransaction {
                block()
                throw IllegalStateException("fails after all writes")
            }
        }
        assertEquals(ImportResult.StorageError, half.import(BackupCodec.encode(TestBackups.valid())))
        assertEquals(before, snapshot())
    }

    @Test
    fun importedTriggersKeepTheirTargetsAndDefaultFlag() = runBlocking {
        backup.import(BackupCodec.encode(TestBackups.valid()))
        val triggers = db.triggerDao().getAll()
        assertTrue(triggers.first().isDefault)
        assertEquals(TriggerPosition.LEFT, triggers[1].position)
        assertEquals(2L, triggers[1].targetZoneId)
        assertEquals(AppSettings().copy(gridColumns = 5, launchMode = LaunchMode.SINGLE_TOUCH), settings.current())
    }
}
