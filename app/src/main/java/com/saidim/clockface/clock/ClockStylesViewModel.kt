package com.saidim.clockface.clock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saidim.clockface.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ClockStylesViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings(application)
    
    private val _clockStyles = MutableStateFlow<List<ClockStyle>>(ClockStyle.entries)
    val clockStyles: StateFlow<List<ClockStyle>> = _clockStyles

    private val _selectedStyle = MutableStateFlow<ClockStyle>(ClockStyle.MINIMAL)
    val selectedStyle: StateFlow<ClockStyle> = _selectedStyle

    init {
        viewModelScope.launch {
            val savedStyleIndex = appSettings.clockStyle.first()
            _selectedStyle.value = try {
                ClockStyle.entries[savedStyleIndex]
            } catch (e: ArrayIndexOutOfBoundsException) {
                ClockStyle.MINIMAL // Fallback to default style
            }
        }
    }

    fun setClockStyle(style: ClockStyle) {
        viewModelScope.launch {
            _selectedStyle.value = style
            appSettings.updateClockStyle(style.ordinal)
        }
    }
} 