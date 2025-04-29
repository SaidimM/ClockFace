package com.saidim.clockface.clock

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.saidim.clockface.clock.syles.ClockStyleConfig
import com.saidim.clockface.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ClockStyleEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings.instance

    private val _clockColor = MutableStateFlow(Color.White)
    val clockColor = _clockColor.asStateFlow()

    private val _clockFontFamily = MutableStateFlow("Roboto-Regular")
    val clockFontFamily = _clockFontFamily.asStateFlow()

    private val _clockSize = MutableStateFlow(1.0f)
    val clockSize = _clockSize.asStateFlow()

    private val _clockAnimation = MutableStateFlow(ClockAnimation.NONE)
    val clockAnimation = _clockAnimation.asStateFlow()

    val clockStyleConfig = appSettings.clockStyleConfig.asLiveData()

    init {
        viewModelScope.launch {
            // Load existing settings if available
            appSettings.clockStyleConfig.first().let { config ->
                _clockColor.value = Color(config.fontColor)
                _clockSize.value = config.fontSize
                _clockFontFamily.value = config.fontFamily
                _clockAnimation.value = config.animation
            }
        }
    }

    fun setClockColor(color: Color) {
        _clockColor.value = color
    }

    fun setClockFontFamily(fontFamily: String) {
        _clockFontFamily.value = fontFamily
    }

    fun setClockSize(size: Float) {
        _clockSize.value = size
    }

    fun setClockAnimation(animation: ClockAnimation) {
        _clockAnimation.value = animation
    }

    fun saveSettings() {
        viewModelScope.launch {
            // Create ClockStyleConfig with current settings
            val clockStyleConfig = ClockStyleConfig(
                fontColor = clockColor.value.toArgb(),
                fontSize = clockSize.value,
                fontFamily = clockFontFamily.value,
                animation = clockAnimation.value
            )

            // Save the config to AppSettings
            appSettings.updateClockStyleConfig(clockStyleConfig)
        }
    }

    // Get all font families using TypefaceUtil
    fun getFontFamilies(context: Context): List<Pair<String, String>> {
        val fontFamilies = TypefaceUtil.getFontFamilies(context)
        return fontFamilies.map { family ->
            family.displayName to family.familyName
        }
    }
    
    // Get available weights for a font using TypefaceUtil
    fun getAvailableWeights(context: Context, fontDisplayName: String, fontTypeface: String): List<String> {
        return TypefaceUtil.getAvailableWeights(context, fontTypeface)
    }
} 