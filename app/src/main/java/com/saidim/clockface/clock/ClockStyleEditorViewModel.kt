package com.saidim.clockface.clock

import android.app.Application
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saidim.clockface.settings.AppSettings
import com.saidim.clockface.clock.syles.ClockStyleConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ClockStyleEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings.instance

    private val _is24Hour = MutableStateFlow(true)
    val is24Hour: StateFlow<Boolean> = _is24Hour

    private val _showSeconds = MutableStateFlow(true)
    val showSeconds: StateFlow<Boolean> = _showSeconds

    private val _showAnalogNumbers = MutableStateFlow(true)
    val showAnalogNumbers: StateFlow<Boolean> = _showAnalogNumbers

    private val _showAnalogTicks = MutableStateFlow(true)
    val showAnalogTicks: StateFlow<Boolean> = _showAnalogTicks

    private val _showBinaryLabels = MutableStateFlow(true)
    val showBinaryLabels: StateFlow<Boolean> = _showBinaryLabels

    private val _binaryActiveColor = MutableStateFlow(Color.Green)
    val binaryActiveColor: StateFlow<Color> = _binaryActiveColor

    private val _useWordCasual = MutableStateFlow(true)
    val useWordCasual: StateFlow<Boolean> = _useWordCasual

    private val _minimalFontColor = MutableStateFlow(Color.White)
    val minimalFontColor: StateFlow<Color> = _minimalFontColor

    private val _minimalFontSize = MutableStateFlow(ClockStyleConfig.MinimalConfig.FontSize.MEDIUM)
    val minimalFontSize: StateFlow<ClockStyleConfig.MinimalConfig.FontSize> = _minimalFontSize

    private val _minimalTypefaceStyle = MutableStateFlow("sans-serif")
    val minimalTypefaceStyle: StateFlow<String> = _minimalTypefaceStyle

    private val _clockColor = MutableStateFlow(Color.White)
    val clockColor = _clockColor.asStateFlow()

    private val _clockFontFamily = MutableStateFlow("Default")
    val clockFontFamily = _clockFontFamily.asStateFlow()

    private val _clockSize = MutableStateFlow(1.0f)
    val clockSize = _clockSize.asStateFlow()

    private val _clockAnimation = MutableStateFlow(ClockAnimation.NONE)
    val clockAnimation = _clockAnimation.asStateFlow()

    init {
        viewModelScope.launch {
            _is24Hour.value = appSettings.use24Hour.first()
            _showSeconds.value = appSettings.showSeconds.first()
            // Load other preferences...
        }
    }

    fun setTimeFormat(is24Hour: Boolean) {
        viewModelScope.launch {
            _is24Hour.value = is24Hour
            appSettings.update24Hour(is24Hour)
        }
    }

    fun setShowSeconds(show: Boolean) {
        viewModelScope.launch {
            _showSeconds.value = show
            appSettings.updateShowSeconds(show)
        }
    }

    fun setShowAnalogNumbers(show: Boolean) {
        viewModelScope.launch {
            _showAnalogNumbers.value = show
            // Save to settings
        }
    }

    fun setShowAnalogTicks(show: Boolean) {
        viewModelScope.launch {
            _showAnalogTicks.value = show
            // Save to settings
        }
    }

    fun setShowBinaryLabels(show: Boolean) {
        viewModelScope.launch {
            _showBinaryLabels.value = show
            // Save to settings
        }
    }

    fun setBinaryActiveColor(color: Color) {
        viewModelScope.launch {
            _binaryActiveColor.value = color
            // Save to settings
        }
    }

    fun setWordCasual(casual: Boolean) {
        viewModelScope.launch {
            _useWordCasual.value = casual
            // Save to settings
        }
    }

    fun setMinimalFontColor(color: Color) {
        viewModelScope.launch {
            _minimalFontColor.value = color
            updateMinimalConfig()
        }
    }

    fun setMinimalFontSize(size: ClockStyleConfig.MinimalConfig.FontSize) {
        viewModelScope.launch {
            _minimalFontSize.value = size
            updateMinimalConfig()
        }
    }

    fun setMinimalTypeface(typefaceStyle: String) {
        viewModelScope.launch {
            _minimalTypefaceStyle.value = typefaceStyle
            updateMinimalConfig()
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
            // Save all settings to datastore or preferences
        }
    }

    private suspend fun updateMinimalConfig() {
        val config = ClockStyleConfig.MinimalConfig(
            is24Hour = _is24Hour.value,
            showSeconds = _showSeconds.value,
            fontColor = _minimalFontColor.value.toArgb(),
            fontSize = _minimalFontSize.value,
            typefaceStyle = _minimalTypefaceStyle.value
        )
        appSettings.updateClockStyleConfig(ClockStyle.MINIMAL, config)
    }
} 