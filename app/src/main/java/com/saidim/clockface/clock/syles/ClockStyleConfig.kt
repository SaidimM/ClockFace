package com.saidim.clockface.clock.syles

import android.graphics.Color
import android.graphics.Typeface

sealed class ClockStyleConfig {
    data class MinimalConfig(
        val is24Hour: Boolean = true,
        val showSeconds: Boolean = true,
        val fontColor: Int = Color.WHITE,
        val fontSize: FontSize = FontSize.MEDIUM,
        val typefaceStyle: String = "sans-serif"
    ) : ClockStyleConfig() {
        enum class FontSize(val scale: Float) {
            SMALL(0.75f),
            MEDIUM(1.0f),
            LARGE(1.5f)
        }
    }

    data class AnalogConfig(
        val showNumbers: Boolean = true,
        val showTicks: Boolean = true,
        val hourHandColor: Int = Color.WHITE,
        val minuteHandColor: Int = Color.WHITE,
        val secondHandColor: Int = Color.RED,
        val numbersColor: Int = Color.WHITE,
        val ticksColor: Int = Color.WHITE
    ) : ClockStyleConfig()

    data class WordConfig(
        val useCasualLanguage: Boolean = true,
        val textColor: Int = Color.WHITE,
        val fontScale: Float = 1.0f,
        val useUpperCase: Boolean = false
    ) : ClockStyleConfig()
}