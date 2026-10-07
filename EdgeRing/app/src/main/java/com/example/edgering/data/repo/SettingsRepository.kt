package com.example.edgering.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.edgering.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Settings on Preferences DataStore. Every value is a string (see [AppSettings]); parsing and
 * defaults live in [AppSettings.fromMap], so unknown or corrupt entries are harmless.
 */
class SettingsRepository(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.settingsDataStore)

    val settings: Flow<AppSettings> = store.data.map { AppSettings.fromMap(it.toStringMap()) }

    suspend fun current(): AppSettings = settings.first()

    /** Atomic read-modify-write. */
    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs -> write(prefs, transform(AppSettings.fromMap(prefs.toStringMap()))) }
    }

    /** Replaces all settings with [new] (used by backup import). */
    suspend fun replace(new: AppSettings) {
        store.edit { prefs ->
            prefs.clear()
            write(prefs, new)
        }
    }

    private fun write(prefs: MutablePreferences, settings: AppSettings) {
        settings.toMap().forEach { (key, value) -> prefs[stringPreferencesKey(key)] = value }
    }

    private fun Preferences.toStringMap(): Map<String, String> =
        asMap().entries.associate { (key, value) -> key.name to value.toString() }
}
