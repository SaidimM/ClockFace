package com.saidim.clockface.clock

import ClockStyle
import android.R.attr.maxHeight
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.saidim.clockface.base.BaseActivity
import com.saidim.clockface.databinding.ActivityClockStyleEditorBinding
import com.saidim.clockface.clock.syles.ClockStyleConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import android.widget.ArrayAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import androidx.appcompat.view.ContextThemeWrapper
import com.saidim.clockface.R
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.elevation.SurfaceColors
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import android.widget.ScrollView
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.RadioGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.card.MaterialCardView

class ClockStyleEditorActivity : BaseActivity() {
    private val viewModel: ClockStyleEditorViewModel by viewModels()
    private lateinit var binding: ActivityClockStyleEditorBinding
    private lateinit var updateTimer: Timer
    private lateinit var fontFamilyDrawer: BottomSheetDialog

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
        // Get the current style from intent
        val style = intent.getSerializableExtra(EXTRA_STYLE) as ClockStyle

        // Main container with vertical scroll support
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, 32)
        }

        // Add content to scroll view
        scrollView.addView(contentContainer)

        // Add scroll view to main container
        container.addView(scrollView)
    }

    private fun createSwitchPreference(
        title: String,
        subtitle: String,
        initialState: Boolean,
        onChanged: (Boolean) -> Unit
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = MaterialShapeDrawable(
                ShapeAppearanceModel.builder()
                    .setAllCornerSizes(28f)
                    .build()
            ).apply {
                fillColor = ColorStateList.valueOf(SurfaceColors.SURFACE_2.getColor(context))
                elevation = resources.getDimension(R.dimen.m3_card_elevation)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(24, 8, 24, 8)
            }
            updatePadding(left = 24, top = 20, right = 24, bottom = 20)
            
            addView(MaterialSwitch(context).apply {
                text = title
                isChecked = initialState
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                setOnCheckedChangeListener { _, checked -> onChanged(checked) }
            })
            
            addView(MaterialTextView(context).apply {
                text = subtitle
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                alpha = 0.6f
                updatePadding(top = 4)
            })
        }
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

    private suspend fun setupWordControls(container: LinearLayout) {
        container.addView(
            createSwitchPreference(
                "Use casual format",
                "Show time in casual language",
                viewModel.useWordCasual.first()
            ) { checked -> viewModel.setWordCasual(checked) })
    }

    override fun onDestroy() {
        super.onDestroy()
        updateTimer.cancel()
    }
} 