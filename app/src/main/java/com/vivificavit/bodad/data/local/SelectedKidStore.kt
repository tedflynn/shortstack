package com.vivificavit.bodad.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Remembers which kid's ledger was open, so the app reopens on it. */
interface SelectedKidStore {
    val lastSelectedKid: Flow<String?>
    suspend fun setLastSelectedKid(name: String)
}

/** SharedPreferences file used before DataStore; migrated on first read. */
private const val LEGACY_PREFS_NAME = "allowance_tracker_prefs"

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context -> listOf(SharedPreferencesMigration(context, LEGACY_PREFS_NAME)) }
)

class DataStoreSelectedKidStore(context: Context) : SelectedKidStore {
    private val dataStore = context.applicationContext.settingsDataStore

    override val lastSelectedKid: Flow<String?> =
        dataStore.data.map { it[KEY_LAST_SELECTED_KID] }

    override suspend fun setLastSelectedKid(name: String) {
        dataStore.edit { it[KEY_LAST_SELECTED_KID] = name }
    }

    private companion object {
        // Same name as the legacy SharedPreferences key, so the migration carries it over.
        val KEY_LAST_SELECTED_KID = stringPreferencesKey("last_selected_kid")
    }
}
