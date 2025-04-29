package com.saidim.clockface

import android.app.Application
import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.saidim.clockface.clock.ClockStyleFormatter
import com.saidim.clockface.settings.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ClockViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings.instance

    val backgroundType = appSettings.backgroundType

    private fun createGradientDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            colors = intArrayOf(color, color)
        }
    }

    private val _currentTime = MutableLiveData<String>()
    val currentTime: LiveData<String> = _currentTime

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
        val pattern = "HH:mm"
        _currentTime.value = SimpleDateFormat(pattern, Locale.getDefault()).format(currentDate)
    }


    // Get all font families from assets
    fun getFontFamilies(context: Context): List<Pair<String, String>> {
        return try {
            val assetManager = context.assets
            val fontDirs = assetManager.list("fonts") ?: emptyArray()

            fontDirs.filter { dir ->
                try {
                    val fontFiles = assetManager.list("fonts/$dir") ?: emptyArray()
                    fontFiles.isNotEmpty() && fontFiles.any { it.endsWith(".ttf") }
                } catch (e: Exception) {
                    false
                }
            }.map { dir ->
                val displayName = dir
                val fontId = dir.replace(" ", "")
                displayName to fontId
            }.sortedBy { it.first }
        } catch (e: Exception) {
            listOf(
                "Roboto" to "Roboto",
                "Lato" to "Lato",
                "Open Sans" to "OpenSans",
                "Raleway" to "Raleway",
                "Josefin Sans" to "JosefinSans"
            )
        }
    }
} 