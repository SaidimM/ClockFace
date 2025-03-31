package com.saidim.clockface.clock

import ClockStyle
import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saidim.clockface.clock.syles.ClockStyleConfig
import com.saidim.clockface.settings.AppSettings
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
        )
        appSettings.updateClockStyleConfig(ClockStyle.MINIMAL, config)
    }
} 