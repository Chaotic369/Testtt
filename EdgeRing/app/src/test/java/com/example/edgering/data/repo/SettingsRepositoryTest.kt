package com.example.edgering.data.repo

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.edgering.data.model.AppSettings
import com.example.edgering.data.model.GridDirection
import com.example.edgering.data.model.IconSize
import com.example.edgering.data.model.SettingKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() = scope.cancel()

    private fun newStore(name: String = "settings.preferences_pb") =
        PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, name) }

    @Test
    fun emptyStoreGivesDefaults() = runBlocking {
        assertEquals(AppSettings(), SettingsRepository(newStore()).current())
    }

    @Test
    fun updateChangesOnlyWhatTheTransformChanges() = runBlocking {
        val repo = SettingsRepository(newStore())
        repo.update { it.copy(gridColumns = 6, iconSize = IconSize.LARGE) }
        repo.update { it.copy(gridDirection = GridDirection.BOTTOM_TO_TOP) }
        val s = repo.current()
        assertEquals(6, s.gridColumns)
        assertEquals(IconSize.LARGE, s.iconSize)
        assertEquals(GridDirection.BOTTOM_TO_TOP, s.gridDirection)
        assertFalse(s.hideAzIndex)
    }

    @Test
    fun aSecondRepositoryOnTheSameStoreSeesStoredValues() = runBlocking {
        val store = newStore("shared.preferences_pb")
        SettingsRepository(store).update { it.copy(gridColumns = 7) }
        assertEquals(7, SettingsRepository(store).current().gridColumns)
    }

    @Test
    fun replaceDropsUnknownAndOldKeys() = runBlocking {
        val store = newStore()
        store.edit { it[stringPreferencesKey("legacy_key")] = "x" }
        val repo = SettingsRepository(store)
        repo.update { it.copy(gridColumns = 2) }
        repo.replace(AppSettings(gridColumns = 3))
        assertEquals(3, repo.current().gridColumns)
        val keys = store.edit { }.asMap().keys.map { it.name }.toSet()
        assertEquals(AppSettings().toMap().keys, keys)
    }

    @Test
    fun corruptStoredValueFallsBackToTheDefault() = runBlocking {
        val store = newStore()
        store.edit { it[stringPreferencesKey(SettingKeys.GRID_COLUMNS)] = "banana" }
        assertEquals(AppSettings.DEFAULT_COLUMNS, SettingsRepository(store).current().gridColumns)
    }
}
