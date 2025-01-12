package com.saidim.clockface.clock

import java.text.SimpleDateFormat
import java.util.*

object ClockStyleFormatter {
    fun formatTime(style: ClockStyle, date: Date = Date()): String {
        return when (style) {
            ClockStyle.MINIMAL -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            ClockStyle.ANALOG -> "🕐" // Placeholder for analog preview
            ClockStyle.WORD -> toWordTime(date)
        }
    }

    private fun toBinaryTime(date: Date): String {
        val hour = SimpleDateFormat("HH", Locale.getDefault()).format(date).toInt()
        val minute = SimpleDateFormat("mm", Locale.getDefault()).format(date).toInt()
        return "${hour.toString(2).padStart(5, '0')}\n${minute.toString(2).padStart(6, '0')}"
    }

    private fun toWordTime(date: Date): String {
        val hour = SimpleDateFormat("h", Locale.getDefault()).format(date).toInt()
        val minute = SimpleDateFormat("mm", Locale.getDefault()).format(date).toInt()
        
        val hourWord = when (hour) {
            1 -> "one" 
            2 -> "two"
            // ... add other hours
            12 -> "twelve"
            else -> hour.toString()
        }

        return when (minute) {
            0 -> "$hourWord o'clock"
            15 -> "quarter past $hourWord"
            30 -> "half past $hourWord"
            45 -> "quarter to ${if (hour == 12) "one" else (hour + 1)}"
            else -> "$minute past $hourWord"
        }
    }
} 