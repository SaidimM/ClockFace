package com.saidim.clockface.clock

import android.app.Application
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saidim.clockface.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ClockStyleEditorViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings(application)

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

    private val _binaryActiveColor = MutableStateFlow(Color.GREEN)
    val binaryActiveColor: StateFlow<Int> = _binaryActiveColor

    private val _useWordCasual = MutableStateFlow(true)
    val useWordCasual: StateFlow<Boolean> = _useWordCasual

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
            appSettings.updateTimeFormat(is24Hour)
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

    fun setBinaryActiveColor(color: Int) {
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
} 