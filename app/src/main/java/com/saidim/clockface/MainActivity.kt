package com.saidim.clockface

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.saidim.clockface.background.BackgroundSettingsActivity
import com.saidim.clockface.clock.ClockStylesActivity
import com.saidim.clockface.base.BaseActivity

class MainActivity : BaseActivity() {
    private val viewModel: ClockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupButtons()
    }

    private fun setupButtons() {
        findViewById<MaterialButton>(R.id.startButton).setOnClickListener {
            startClockDisplay()
        }

        findViewById<MaterialButton>(R.id.backgroundButton).setOnClickListener {
            openBackgroundSettings()
        }

        findViewById<MaterialButton>(R.id.clockButton).setOnClickListener {
            openClockStyles()
        }

        findViewById<MaterialButton>(R.id.settingsButton).setOnClickListener {
            openSettings()
        }
    }

    private fun startClockDisplay() {
        Intent(this, ClockDisplayActivity::class.java).apply {
            putExtra("is24Hour", viewModel.is24Hour.value)
            putExtra("showSeconds", viewModel.showSeconds.value)
            startActivity(this)
        }
    }

    private fun openBackgroundSettings() {
         Intent(this, BackgroundSettingsActivity::class.java).also(::startActivity)
    }

    private fun openClockStyles() {
        Intent(this, ClockStylesActivity::class.java).also(::startActivity)
    }

    private fun openSettings() {
        // TODO: Implement settings activity
        // Intent(this, SettingsActivity::class.java).also(::startActivity)
    }
}