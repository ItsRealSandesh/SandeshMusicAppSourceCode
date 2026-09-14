package com.sandeshmusic.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

class ThemePreferencesRepository(private val context: Context) {

    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val modeName = preferences[THEME_MODE_KEY] ?: ThemeMode.DARK.name
            try {
                ThemeMode.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                ThemeMode.DARK
            }
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
}
