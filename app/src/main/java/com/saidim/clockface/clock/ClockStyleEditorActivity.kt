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

        // Preview Section
        contentContainer.addView(createSectionTitle("Preview"))
        contentContainer.addView(createPreviewCard(style))

        // Time Format Section
        contentContainer.addView(createSectionTitle("Time Format"))
        
        // Time format settings group
        val timeFormatContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        timeFormatContainer.addView(
            createSwitchPreference(
                "24-hour format",
                "Switch between 12-hour and 24-hour time display",
                viewModel.is24Hour.first()
            ) { checked -> viewModel.setTimeFormat(checked) }
        )

        timeFormatContainer.addView(
            createSwitchPreference(
                "Show seconds",
                "Display seconds alongside hours and minutes",
                viewModel.showSeconds.first()
            ) { checked -> viewModel.setShowSeconds(checked) }
        )

        contentContainer.addView(timeFormatContainer)

        // Typography Section
        contentContainer.addView(createSectionTitle("Typography"))
        
        // Font size radio group
        contentContainer.addView(createFontSizeRadioGroup(
            viewModel.minimalFontSize.first()
        ) { size -> viewModel.setMinimalFontSize(size) })

        // Get current typeface
        val currentTypeface = viewModel.minimalTypefaceStyle.first()

        // Typeface Card
        contentContainer.addView(createTypefaceCard(
            currentTypeface
        ) { typeface -> 
            lifecycleScope.launch {
                viewModel.setMinimalTypeface(typeface)
            }
        })

        // Add content to scroll view
        scrollView.addView(contentContainer)

        // Add scroll view to main container
        container.addView(scrollView)
    }

    private fun createPreviewCard(style: ClockStyle): View {
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
                setMargins(24, 8, 24, 16)
            }
            updatePadding(left = 24, top = 20, right = 24, bottom = 24)

            // Preview Title
            addView(MaterialTextView(context).apply {
                text = "Live Preview"
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                alpha = 0.87f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
            })

            // Update preview when settings change
            lifecycleScope.launch {
                launch {
                    viewModel.is24Hour.collect { is24Hour ->
                        binding.previewText.text = ClockStyleFormatter.formatTime(style)
                    }
                }
                launch {
                    viewModel.showSeconds.collect { showSeconds ->
                        binding.previewText.text = ClockStyleFormatter.formatTime(style)
                    }
                }
                launch {
                    viewModel.minimalFontSize.collect { fontSize ->
                        binding.previewText.setTextAppearance(
                            when (fontSize) {
                                ClockStyleConfig.MinimalConfig.FontSize.SMALL -> com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium
                                ClockStyleConfig.MinimalConfig.FontSize.MEDIUM -> com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge
                                else -> com.google.android.material.R.style.TextAppearance_Material3_DisplaySmall
                            }
                        )
                    }
                }
                launch {
                    viewModel.minimalTypefaceStyle.collect { typefaceStyle ->
                        binding.previewText.typeface = Typeface.create(typefaceStyle, Typeface.NORMAL)
                    }
                }
            }
        }
    }

    private fun createSectionTitle(title: String): View {
        return MaterialTextView(this).apply {
            text = title
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
            alpha = 0.87f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(24, 40, 24, 16)
            }
        }
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

    private fun createFontSizeRadioGroup(
        initialSize: ClockStyleConfig.MinimalConfig.FontSize,
        onSizeSelected: (ClockStyleConfig.MinimalConfig.FontSize) -> Unit
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
                setMargins(24, 8, 24, 16)
            }
            updatePadding(left = 24, top = 20, right = 24, bottom = 24)

            // Label and Control in one row
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                weightSum = 3f

                // Label
                addView(MaterialTextView(context).apply {
                    text = "Font Size"
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                    alpha = 0.87f
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    gravity = Gravity.CENTER_VERTICAL
                })

                // Radio button group
                addView(RadioGroup(context).apply {
                    orientation = RadioGroup.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        2f
                    )

                    val sizes = listOf(
                        ClockStyleConfig.MinimalConfig.FontSize.SMALL to "S",
                        ClockStyleConfig.MinimalConfig.FontSize.MEDIUM to "M",
                        ClockStyleConfig.MinimalConfig.FontSize.LARGE to "L"
                    )

                    sizes.forEachIndexed { index, (size, label) ->
                        addView(MaterialRadioButton(context).apply {
                            id = index + 1
                            text = label
                            isChecked = size == initialSize
                            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                            buttonTintList = ColorStateList.valueOf(
                                MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.BLACK)
                            )
                            layoutParams = RadioGroup.LayoutParams(
                                RadioGroup.LayoutParams.WRAP_CONTENT,
                                RadioGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                marginEnd = 16
                            }
                        })
                    }

                    setOnCheckedChangeListener { _, checkedId ->
                        val selectedSize = when (checkedId) {
                            1 -> ClockStyleConfig.MinimalConfig.FontSize.SMALL
                            2 -> ClockStyleConfig.MinimalConfig.FontSize.MEDIUM
                            else -> ClockStyleConfig.MinimalConfig.FontSize.LARGE
                        }
                        onSizeSelected(selectedSize)
                    }
                })
            })
        }
    }

    private fun createTypefaceCard(
        currentTypeface: String,
        onTypefaceSelected: (String) -> Unit
    ): View {
        return MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(24, 8, 24, 16)
            }
            elevation = resources.getDimension(R.dimen.m3_card_elevation)
            setCardBackgroundColor(SurfaceColors.SURFACE_2.getColor(context))
            radius = resources.getDimension(R.dimen.m3_card_corner_radius)

            // Content container
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                updatePadding(left = 24, top = 20, right = 24, bottom = 24)

                // Title
                addView(MaterialTextView(context).apply {
                    text = "Typeface"
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                    alpha = 0.87f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 16)
                    }
                })

                // Font Family Section
                addView(MaterialTextView(context).apply {
                    text = "Font Family"
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
                    alpha = 0.6f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 8)
                    }
                })

                // Font Family Scroll Container
                addView(HorizontalScrollView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 24
                    }
                    isHorizontalScrollBarEnabled = false

                    // Font Family Chip Group
                    val familyChipGroup = com.google.android.material.chip.ChipGroup(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        isSingleSelection = true
                        isSelectionRequired = true
                    }

                    // Get system fonts
                    val systemFonts = listOf(
                        "sans-serif",
                        "serif",
                        "monospace",
                        "casual",
                        "cursive"
                    )

                    // Add font family chips
                    systemFonts.forEach { family ->
                        familyChipGroup.addView(com.google.android.material.chip.Chip(
                            ContextThemeWrapper(context, com.google.android.material.R.style.Widget_Material3_Chip_Filter)
                        ).apply {
                            text = family.split('-')
                                .joinToString(" ") { 
                                    it.capitalize(Locale.getDefault()) 
                                }
                            typeface = Typeface.create(family, Typeface.NORMAL)
                            isCheckable = true
                            isChecked = currentTypeface.startsWith(family)
                            setOnCheckedChangeListener { _, isChecked ->
                                if (isChecked) {
                                    val newTypeface = "$family${currentTypeface.substringAfter(family.substringBefore('-'), "")}"
                                    onTypefaceSelected(newTypeface)
                                }
                            }
                        })
                    }
                    addView(familyChipGroup)
                })

                // Font Style Section
                addView(MaterialTextView(context).apply {
                    text = "Font Style"
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
                    alpha = 0.6f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, 8)
                    }
                })

                // Style Chip Group
                val styleChipGroup = com.google.android.material.chip.ChipGroup(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    isSingleSelection = true
                    isSelectionRequired = true
                }

                // Get current family and style
                val currentFamily = currentTypeface.substringBefore('-')
                val currentStyle = currentTypeface.substringAfter(currentFamily, "")

                // Add style options
                listOf(
                    Triple("Regular", "", "Aa"),
                    Triple("Light", "-light", "Aa"),
                    Triple("Medium", "-medium", "Aa"),
                    Triple("Bold", "-bold", "Aa"),
                    Triple("Black", "-black", "Aa")
                ).filter { (_, suffix, _) ->
                    try {
                        val testTypeface = Typeface.create("$currentFamily$suffix", Typeface.NORMAL)
                        testTypeface != Typeface.DEFAULT || currentFamily == "sans-serif"
                    } catch (e: Exception) {
                        false
                    }
                }.forEach { (name, suffix, preview) ->
                    styleChipGroup.addView(com.google.android.material.chip.Chip(
                        ContextThemeWrapper(context, com.google.android.material.R.style.Widget_Material3_Chip_Filter)
                    ).apply {
                        text = preview
                        contentDescription = name
                        typeface = Typeface.create("$currentFamily$suffix", Typeface.NORMAL)
                        isCheckable = true
                        isChecked = suffix == currentStyle
                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                onTypefaceSelected("$currentFamily$suffix")
                            }
                        }
                    })
                }

                addView(styleChipGroup)
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

    private fun createColorPicker(
        title: String,
        initialColor: Int,
        onColorSelected: (Int) -> Unit
    ): View {
        return MaterialButton(this).apply {
            text = title
            setOnClickListener {

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateTimer.cancel()
    }
} 