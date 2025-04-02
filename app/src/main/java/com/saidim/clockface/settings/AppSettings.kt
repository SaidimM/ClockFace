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
        val CLOCK_STYLE = intPreferencesKey("clock_style")
        val BACKGROUND_TYPE = intPreferencesKey("background_type")
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

    // Int preferences
    val clockStyle = getPreference(Keys.CLOCK_STYLE, 0)
    val backgroundType = getPreference(Keys.BACKGROUND_TYPE, 0).map { BackgroundType.entries[it] }

    // String preferences
    val backgroundModel = flow {
        val json = getPreference(Keys.BACKGROUND_MODEL, "").first()
        val type = backgroundType.first()
        val gson = Gson()
        
        val model = if (json.isEmpty()) {
            // Return default model based on type
            when (type) {
                BackgroundType.COLOR -> BackgroundModel.ColorModel()
                BackgroundType.IMAGE -> BackgroundModel.ImageModel()
                BackgroundType.VIDEO -> BackgroundModel.VideoModel()
            }
        } else {
            try {
                // Deserialize to the correct model type
                when (type) {
                    BackgroundType.COLOR -> gson.fromJson(json, BackgroundModel.ColorModel::class.java)
                    BackgroundType.IMAGE -> gson.fromJson(json, BackgroundModel.ImageModel::class.java)
                    BackgroundType.VIDEO -> gson.fromJson(json, BackgroundModel.VideoModel::class.java)
                }
            } catch (e: Exception) {
                // If deserialization fails, return a default model
                when (type) {
                    BackgroundType.COLOR -> BackgroundModel.ColorModel()
                    BackgroundType.IMAGE -> BackgroundModel.ImageModel()
                    BackgroundType.VIDEO -> BackgroundModel.VideoModel()
                }
            }
        }
        emit(model)
    }

    // Update functions
    suspend fun updateClockStyle(value: Int) = setPreference(Keys.CLOCK_STYLE, value)
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
