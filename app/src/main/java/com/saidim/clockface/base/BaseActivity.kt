package com.saidim.clockface.base

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import android.content.res.Configuration
import com.google.android.material.color.MaterialColors

abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupStatusBar()
    }

    private fun setupStatusBar() {
        window.statusBarColor = MaterialColors.getColor(
            this, 
            com.google.android.material.R.attr.colorSurface, 
            Color.WHITE
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = resources.configuration.uiMode and 
                Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
        }
    }
} 