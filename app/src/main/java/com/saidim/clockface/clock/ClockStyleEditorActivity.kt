package com.saidim.clockface.clock

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.saidim.clockface.base.BaseActivity
import com.saidim.clockface.databinding.ActivityClockStyleEditorBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class ClockStyleEditorActivity : BaseActivity() {
    private val viewModel: ClockStyleEditorViewModel by viewModels()
    private lateinit var binding: ActivityClockStyleEditorBinding
    private lateinit var updateTimer: Timer

    companion object {
        const val EXTRA_STYLE = "extra_style"
        const val SHARED_ELEMENT_NAME = "clock_preview"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClockStyleEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val style = intent.getSerializableExtra(EXTRA_STYLE) as ClockStyle
        setupToolbar(style)
        setupPreview(style)
        setupControls(style)
        observeViewModel()
    }

    private fun setupToolbar(style: ClockStyle) {
        binding.topAppBar.apply {
            title = style.displayName
            setNavigationOnClickListener { finishAfterTransition() }
        }
    }

    private fun setupPreview(style: ClockStyle) {
        updateTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        binding.previewText.text = ClockStyleFormatter.formatTime(style)
                    }
                }
            }, 0, 1000)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.is24Hour.collect { is24Hour ->
                // Update preview if needed
            }
            // Observe other states...
        }
    }

    private fun setupControls(style: ClockStyle) {
        binding.controlsContainer.removeAllViews()

        lifecycleScope.launch {
            when (style) {
                ClockStyle.MINIMAL -> setupMinimalControls(binding.controlsContainer)
                ClockStyle.ANALOG -> setupAnalogControls(binding.controlsContainer)
                ClockStyle.WORD -> setupWordControls(binding.controlsContainer)
            }
        }
    }

    private suspend fun setupMinimalControls(container: LinearLayout) {
        container.addView(
            createSwitchPreference(
                "24-hour format",
                "Use 24-hour time format",
                viewModel.is24Hour.first()
            ) { checked -> viewModel.setTimeFormat(checked) })

        container.addView(
            createSwitchPreference(
                "Show seconds",
                "Display seconds in time",
                viewModel.showSeconds.first()
            ) { checked -> viewModel.setShowSeconds(checked) })
    }

    private suspend fun setupAnalogControls(container: LinearLayout) {
        container.addView(
            createSwitchPreference(
                "Show numbers",
                "Display hour numbers",
                viewModel.showAnalogNumbers.first()
            ) { checked -> viewModel.setShowAnalogNumbers(checked) })

        container.addView(
            createSwitchPreference(
                "Show ticks",
                "Display minute ticks",
                viewModel.showAnalogTicks.first()
            ) { checked -> viewModel.setShowAnalogTicks(checked) })
    }

    private suspend fun setupBinaryControls(container: LinearLayout) {
        container.addView(
            createSwitchPreference(
                "Show labels",
                "Display bit position labels",
                viewModel.showBinaryLabels.first()
            ) { checked -> viewModel.setShowBinaryLabels(checked) })

        container.addView(
            createColorPicker(
                "Active bit color",
                viewModel.binaryActiveColor.first()
            ) { color -> viewModel.setBinaryActiveColor(color) })
    }

    private suspend fun setupWordControls(container: LinearLayout) {
        container.addView(
            createSwitchPreference(
                "Use casual format",
                "Show time in casual language",
                viewModel.useWordCasual.first()
            ) { checked -> viewModel.setWordCasual(checked) })
    }

    private fun createSwitchPreference(
        title: String,
        subtitle: String,
        initialState: Boolean,
        onChanged: (Boolean) -> Unit
    ): View {
        return MaterialSwitch(this).apply {
            text = title
            isChecked = initialState
            setOnCheckedChangeListener { _, checked -> onChanged(checked) }
        }
    }

    private fun createColorPicker(
        title: String,
        initialColor: Int,
        onColorSelected: (Int) -> Unit
    ): View {
        return MaterialButton(this).apply {
            text = title
            setOnClickListener {
                ColorPickerDialog.Builder(this@ClockStyleEditorActivity)
                    .setTitle(title)
                    .setPositiveButton("Select", ColorEnvelopeListener { envelope, _ ->
                        onColorSelected(envelope.color)
                    })
                    .setNegativeButton("Cancel") { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    }
                    .attachAlphaSlideBar(false)
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateTimer.cancel()
    }
} 