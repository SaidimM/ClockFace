package com.saidim.clockface.settings

import android.content.Context
import android.graphics.Color
import android.provider.CalendarContract.Colors
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.saidim.clockface.App
import com.saidim.clockface.background.BackgroundType
import com.saidim.clockface.background.model.BackgroundModel
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppSettings {
    private val context = App.instance
    private val moshi = Moshi.Builder().build()

    companion object {
        val instance by lazy { AppSettings() }
    }

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
        val BACKGROUND_TYPE = intPreferencesKey("background_type")
        val VIDEO_BACKGROUND = stringPreferencesKey("video_background")
        val BACKGROUND_MODEL = stringPreferencesKey("background_model")
    }

    // Generic functions for getting and setting preferences
    private fun <T> getPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }

    private suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    // Boolean preferences
    val showSeconds = getPreference(Keys.SHOW_SECONDS, false)
    val use24Hour = getPreference(Keys.USE_24_HOUR, false)
    val enableLandscape = getPreference(Keys.ENABLE_LANDSCAPE, true)
    val showAnalogNumbers = getPreference(Keys.SHOW_ANALOG_NUMBERS, true)
    val showAnalogTicks = getPreference(Keys.SHOW_ANALOG_TICKS, true)
    val useWordCasual = getPreference(Keys.USE_WORD_CASUAL, false)

    // Int preferences
    val clockStyle = getPreference(Keys.CLOCK_STYLE, 0)
    val backgroundInterval = getPreference(Keys.BACKGROUND_INTERVAL, 30)
    val backgroundType = getPreference(Keys.BACKGROUND_TYPE, 0)
    val binaryColor = getPreference(Keys.BINARY_COLOR, 0xFF000000.toInt())

    // String preferences
    val backgroundModel = flow {
        val json = getPreference(Keys.BACKGROUND_MODEL, "").first()
        val type = backgroundType.first()
        val model = when (type) {
            BackgroundType.COLOR.ordinal -> moshi.adapter(BackgroundModel.ColorModel::class.java).fromJson(json)
            BackgroundType.IMAGE.ordinal -> moshi.adapter(BackgroundModel.ColorModel::class.java).fromJson(json)
            BackgroundType.VIDEO.ordinal -> moshi.adapter(BackgroundModel.ColorModel::class.java).fromJson(json)
            else -> BackgroundModel.ColorModel()
        }
        emit(model)
    }

    // Update functions
    suspend fun updateShowSeconds(value: Boolean) = setPreference(Keys.SHOW_SECONDS, value)
    suspend fun update24Hour(value: Boolean) = setPreference(Keys.USE_24_HOUR, value)
    suspend fun updateLandscape(value: Boolean) = setPreference(Keys.ENABLE_LANDSCAPE, value)
    suspend fun updateBlurhash(value: Boolean) = setPreference(Keys.ENABLE_BLURHASH, value)
    suspend fun updateAnimations(value: Boolean) = setPreference(Keys.ENABLE_ANIMATIONS, value)
    suspend fun updateClockStyle(value: Int) = setPreference(Keys.CLOCK_STYLE, value)
    suspend fun updateBackgroundInterval(value: Int) = setPreference(Keys.BACKGROUND_INTERVAL, value)
    suspend fun updateAnalogNumbers(value: Boolean) = setPreference(Keys.SHOW_ANALOG_NUMBERS, value)
    suspend fun updateAnalogTicks(value: Boolean) = setPreference(Keys.SHOW_ANALOG_TICKS, value)
    suspend fun updateBinaryLabels(value: Boolean) = setPreference(Keys.SHOW_BINARY_LABELS, value)
    suspend fun updateBinaryColor(value: Int) = setPreference(Keys.BINARY_COLOR, value)
    suspend fun updateWordCasual(value: Boolean) = setPreference(Keys.USE_WORD_CASUAL, value)
    suspend fun updateBackgroundType(value: Int) = setPreference(Keys.BACKGROUND_TYPE, value)
    suspend fun updateVideoBackground(value: String) = setPreference(Keys.VIDEO_BACKGROUND, value)
    suspend fun updateBackgroundModel(backgroundModel: BackgroundModel) =
        setPreference(Keys.BACKGROUND_MODEL, backgroundModel.toJson())
}
