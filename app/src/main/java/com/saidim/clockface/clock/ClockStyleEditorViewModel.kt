package com.saidim.clockface.clock

import ClockStyle
import android.app.Application
import android.content.Context
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
            // Load other preferences...
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
    
    // Get available weights for a font
    fun getAvailableWeights(context: Context, fontDisplayName: String, fontTypeface: String): List<String> {
        val fontStyles = listOf("Light", "Regular", "Medium", "Bold", "Black")
        return fontStyles.filter { style ->
            try {
                val fontPath = "fonts/$fontDisplayName/$fontTypeface-$style.ttf"
                context.assets.open(fontPath).use { it.close(); true }
            } catch (e: Exception) {
                false
            }
        }
    }
} 