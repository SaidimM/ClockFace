package com.saidim.clockface.settings

import ClockStyle
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.saidim.clockface.App
import com.saidim.clockface.background.BackgroundType
import com.saidim.clockface.background.model.BackgroundModel
import com.saidim.clockface.clock.syles.ClockStyleConfig
import com.saidim.clockface.clock.syles.ClockStyleConfigSerializer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppSettings {
    private val context = App.instance

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
        val CLOCK_STYLE_CONFIG = stringPreferencesKey("clock_style_config")
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
    val backgroundType = getPreference(Keys.BACKGROUND_TYPE, 0).map { BackgroundType.entries[it] }
    val binaryColor = getPreference(Keys.BINARY_COLOR, 0xFF000000.toInt())

    // String preferences
    val backgroundModel = flow {
        val json = getPreference(Keys.BACKGROUND_MODEL, "").first()
        val type = backgroundType.first()
        val model = when (type) {
            BackgroundType.COLOR -> Gson().fromJson(json, BackgroundModel.ColorModel::class.java)
            BackgroundType.IMAGE -> Gson().fromJson(json, BackgroundModel.ImageModel::class.java)
            BackgroundType.VIDEO -> Gson().fromJson(json, BackgroundModel.VideoModel::class.java)
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
    suspend fun updateBackgroundModel(backgroundModel: BackgroundModel) =
        setPreference(Keys.BACKGROUND_MODEL, backgroundModel.toJson())

    // Store configuration for current style
    suspend fun updateClockStyleConfig(style: ClockStyle, config: ClockStyleConfig) {
        setPreference(Keys.CLOCK_STYLE_CONFIG, ClockStyleConfigSerializer.serialize(config))
    }

    // Get configuration for current style
    fun getClockStyleConfig(style: ClockStyle): Flow<ClockStyleConfig> = flow {
        val json = getPreference(Keys.CLOCK_STYLE_CONFIG, "").first()
        val config = if (json.isEmpty()) {
            style.defaultConfig
        } else {
            try {
                ClockStyleConfigSerializer.deserialize(json, style)
            } catch (e: Exception) {
                style.defaultConfig
            }
        }
        emit(config)
    }
}
