package com.saidim.clockface.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppSettings(private val context: Context) {
    private object Keys {
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val ENABLE_LANDSCAPE = booleanPreferencesKey("enable_landscape")
        val ENABLE_BLURHASH = booleanPreferencesKey("enable_blurhash")
        val ENABLE_ANIMATIONS = booleanPreferencesKey("enable_animations")
        val CLOCK_STYLE = intPreferencesKey("clock_style")
        val BACKGROUND_INTERVAL = intPreferencesKey("background_interval")
        val SHOW_ANALOG_NUMBERS = booleanPreferencesKey("show_analog_numbers")
        val SHOW_ANALOG_TICKS = booleanPreferencesKey("show_analog_ticks")
        val SHOW_BINARY_LABELS = booleanPreferencesKey("show_binary_labels")
        val BINARY_COLOR = intPreferencesKey("binary_color")
        val USE_WORD_CASUAL = booleanPreferencesKey("use_word_casual")
    }

    val showSeconds: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.SHOW_SECONDS] ?: true }
    
    val use24Hour: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.USE_24_HOUR] ?: true }
    
    val enableLandscape: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.ENABLE_LANDSCAPE] ?: true }
    
    val enableBlurhash: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.ENABLE_BLURHASH] ?: true }
    
    val enableAnimations: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[Keys.ENABLE_ANIMATIONS] ?: true }
    
    val clockStyle: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[Keys.CLOCK_STYLE] ?: 0 }
    
    val backgroundInterval: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[Keys.BACKGROUND_INTERVAL] ?: 30 }

    suspend fun updateShowSeconds(show: Boolean) {
        context.dataStore.edit { preferences -> 
            preferences[Keys.SHOW_SECONDS] = show 
        }
    }

    suspend fun update24Hour(use24: Boolean) {
        context.dataStore.edit { preferences -> 
            preferences[Keys.USE_24_HOUR] = use24 
        }
    }

    suspend fun updateLandscape(enable: Boolean) {
        context.dataStore.edit { preferences -> 
            preferences[Keys.ENABLE_LANDSCAPE] = enable 
        }
    }

    suspend fun updateBlurhash(enable: Boolean) {
        context.dataStore.edit { preferences -> 
            preferences[Keys.ENABLE_BLURHASH] = enable 
        }
    }

    suspend fun updateAnimations(enable: Boolean) {
        context.dataStore.edit { preferences -> 
            preferences[Keys.ENABLE_ANIMATIONS] = enable 
        }
    }

    suspend fun updateClockStyle(style: Int) {
        context.dataStore.edit { preferences -> 
            preferences[Keys.CLOCK_STYLE] = style 
        }
    }

    suspend fun updateBackgroundInterval(minutes: Int) {
        context.dataStore.edit { preferences -> 
            preferences[Keys.BACKGROUND_INTERVAL] = minutes 
        }
    }

    suspend fun updateTimeFormat(is24Hour: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USE_24_HOUR] = is24Hour
        }
    }

    suspend fun updateAnalogNumbers(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SHOW_ANALOG_NUMBERS] = show
        }
    }

    suspend fun updateAnalogTicks(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SHOW_ANALOG_TICKS] = show
        }
    }

    suspend fun updateBinaryLabels(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SHOW_BINARY_LABELS] = show
        }
    }

    suspend fun updateBinaryColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.BINARY_COLOR] = color
        }
    }

    suspend fun updateWordCasual(casual: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USE_WORD_CASUAL] = casual
        }
    }
} 