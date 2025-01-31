package com.saidim.clockface

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.saidim.clockface.clock.ClockStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.saidim.clockface.clock.ClockStyleFormatter
import com.saidim.clockface.settings.AppSettings
import android.graphics.drawable.GradientDrawable
import com.saidim.clockface.background.BackgroundType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ClockViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings.instance
    
    val backgroundType = appSettings.backgroundType
        .stateIn(viewModelScope, SharingStarted.Eagerly, BackgroundType.COLOR)

    private fun createGradientDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            colors = intArrayOf(color, color)
        }
    }

    private val _currentTime = MutableLiveData<String>()
    val currentTime: LiveData<String> = _currentTime

    private val _is24Hour = MutableLiveData(true)
    val is24Hour: LiveData<Boolean> = _is24Hour

    private val _showSeconds = MutableLiveData(true)
    val showSeconds: LiveData<Boolean> = _showSeconds

    private val _clockStyle = MutableLiveData<ClockStyle>(ClockStyle.MINIMAL)
    val clockStyle: LiveData<ClockStyle> = _clockStyle

    val backgroundModel = AppSettings.instance.backgroundModel

    init {
        startClock()
    }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                updateTime()
                delay(1000)
            }
        }
    }

    private fun updateTime() {
        val currentDate = Date()
        _currentTime.value = when (clockStyle.value) {
            ClockStyle.MINIMAL -> {
                val pattern = when {
                    showSeconds.value == true -> if (is24Hour.value == true) "HH:mm:ss" else "hh:mm:ss a"
                    else -> if (is24Hour.value == true) "HH:mm" else "hh:mm a"
                }
                SimpleDateFormat(pattern, Locale.getDefault()).format(currentDate)
            }
            else -> ClockStyleFormatter.formatTime(clockStyle.value ?: ClockStyle.MINIMAL, currentDate)
        }
    }

    fun setTimeFormat(is24Hour: Boolean) {
        _is24Hour.value = is24Hour
    }

    fun setShowSeconds(show: Boolean) {
        _showSeconds.value = show
    }

    fun setClockStyle(style: ClockStyle) {
        _clockStyle.value = style
    }
} 