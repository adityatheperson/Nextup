package com.adith.nextup

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.nextUpDataStore by preferencesDataStore(name = "nextup")

data class Settings(
    val count: Int = 3,
    val dismissed: Boolean = false,
    val activated: Boolean = false,
    val permissionRequested: Boolean = false,
    val generation: Long = 0,
)

class SettingsStore(private val context: Context) {
    private val count = intPreferencesKey("event_count")
    private val dismissed = booleanPreferencesKey("dismissed")
    private val activated = booleanPreferencesKey("activated")
    private val permissionRequested = booleanPreferencesKey("permission_requested")
    private val generation = longPreferencesKey("generation")

    suspend fun read(): Settings = context.nextUpDataStore.data.first().let {
        Settings((it[count] ?: 3).coerceIn(1, 5), it[dismissed] ?: false,
            it[activated] ?: false, it[permissionRequested] ?: false, it[generation] ?: 0)
    }

    suspend fun open() {
        context.nextUpDataStore.edit {
            it[dismissed] = false
            it[activated] = true
            it[generation] = (it[generation] ?: 0) + 1
        }
    }

    suspend fun dismiss(expectedGeneration: Long) {
        context.nextUpDataStore.edit {
            // A delayed delete intent from an earlier app session must not hide a newly opened agenda.
            if ((it[generation] ?: 0) == expectedGeneration) it[dismissed] = true
        }
    }

    suspend fun setCount(value: Int) { context.nextUpDataStore.edit { it[count] = value.coerceIn(1, 5) } }
    suspend fun markPermissionRequested() { context.nextUpDataStore.edit { it[permissionRequested] = true } }
}
