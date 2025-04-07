package com.saidim.clockface

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.app.ActivityOptionsCompat
import com.saidim.clockface.background.ComposeBackgroundSettingsActivity
import com.saidim.clockface.base.BaseActivity
import com.saidim.clockface.clock.ClockStylesActivity
import com.saidim.clockface.clock.ColorPickerActivity
import com.saidim.clockface.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel: ClockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupButtons()
    }

    private fun setupButtons() {
        binding.startButton.apply {
            setOnClickListener {
                navigateToClockDisplay()
            }
        }

        binding.backgroundButton.setOnClickListener {
            openBackgroundSettings()
        }

        binding.clockButton.setOnClickListener {
            openClockStyles()
        }

        binding.settingsButton.setOnClickListener {
            openSettings()
        }
    }

    private fun navigateToClockDisplay() {
        val intent = Intent(this, ClockDisplayActivity::class.java).apply {
            putExtra(EXTRA_IS_24_HOUR, viewModel.is24Hour.value)
            putExtra(EXTRA_SHOW_SECONDS, viewModel.showSeconds.value)
        }

        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            binding.startButton,
            getString(R.string.clock_preview_transition)
        )

        startActivity(intent, options.toBundle())
    }

    private fun openBackgroundSettings() {
        startActivity(Intent(this, ComposeBackgroundSettingsActivity::class.java))
    }

    private fun openClockStyles() {
        startActivity(Intent(this, ClockStylesActivity::class.java))
    }

    private fun openSettings() {
        startActivity(Intent(this, ColorPickerActivity::class.java))
    }

    companion object {
        const val EXTRA_IS_24_HOUR = "extra_is_24_hour"
        const val EXTRA_SHOW_SECONDS = "extra_show_seconds"
    }
}