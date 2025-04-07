package com.saidim.clockface.clock

import java.text.SimpleDateFormat
import java.util.*

object ClockStyleFormatter {
    fun formatTime(date: Date = Date()) = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
} 