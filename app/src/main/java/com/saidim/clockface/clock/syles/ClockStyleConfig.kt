package com.saidim.clockface.clock.syles

import android.graphics.Color

// Renamed from MinimalConfig, no longer sealed
data class ClockStyleConfig(
    val fontColor: Int = Color.WHITE,
    val fontSize: Float = 1.0f,
    val fontFamily: String = "Roboto-Regular"
)