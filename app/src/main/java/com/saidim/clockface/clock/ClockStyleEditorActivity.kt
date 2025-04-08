package com.saidim.clockface.clock

import android.graphics.Typeface
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.saidim.clockface.clock.ClockStyleEditorActivity.Companion.SHARED_ELEMENT_NAME
import com.saidim.clockface.clock.syles.ClockStyleConfig
import com.saidim.clockface.ui.theme.ClockFaceTheme
import java.util.*

class ClockStyleEditorActivity : ComponentActivity() {
    private val viewModel: ClockStyleEditorViewModel by viewModels()
    private lateinit var updateTimer: Timer

    companion object {
        const val EXTRA_STYLE = "extra_style"
        const val SHARED_ELEMENT_NAME = "clock_preview"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupPreview()
        observe()
    }

    private fun observe() {
        viewModel.clockStyleConfig.observe(this) { config ->
            setContent {
                ClockFaceTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ClockStyleEditorScreen(
                            config,
                            viewModel = viewModel,
                            onNavigateBack = { finishAfterTransition() }
                        )
                    }
                }
            }
        }
    }

    private fun setupPreview() {
        updateTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    // Preview updates will be handled by Compose state
                }
            }, 0, 1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateTimer.cancel()
    }
}

@Composable
private fun getDisplayLargeTextSize(): Float {
    return with(LocalDensity.current) {
        MaterialTheme.typography.displayLarge.fontSize.value
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockStyleEditorScreen(
    config: ClockStyleConfig,
    viewModel: ClockStyleEditorViewModel,
    onNavigateBack: () -> Unit
) {
    var currentTime by remember { mutableStateOf(ClockStyleFormatter.formatTime()) }
    val textSize = getDisplayLargeTextSize()

    // Get screen dimensions
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val ratio = screenWidth / screenHeight

    // Collect state from ViewModel
    val clockColor by viewModel.clockColor.collectAsState()
    val clockFontFamily by viewModel.clockFontFamily.collectAsState()
    val clockSize by viewModel.clockSize.collectAsState()
    val clockAnimation by viewModel.clockAnimation.collectAsState()

    // Parse the font family and style
    val parts = clockFontFamily.split("-")
    val currentTypeface = parts.getOrNull(0) ?: "Roboto"
    val currentStyle = parts.getOrNull(1) ?: "Regular"

    // Get the context for asset loading
    val context = LocalContext.current

    // Reference to the preview TextView for direct updates
    val previewTextRef = remember { mutableStateOf<TextView?>(null) }

    // Update time every second
    LaunchedEffect(Unit) {
        Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    currentTime = ClockStyleFormatter.formatTime()
                }
            }, 0, 1000)
        }
    }

    // Calculate the actual size multiplier to use for animated previews
    var currentSizeMultiplier by remember { mutableStateOf(clockSize) }

    // Animate size transitions
    LaunchedEffect(clockSize) {
        val anim = android.animation.ValueAnimator.ofFloat(currentSizeMultiplier, clockSize).apply {
            duration = 300
            addUpdateListener { valueAnimator ->
                currentSizeMultiplier = valueAnimator.animatedValue as Float
            }
        }
        anim.start()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clock Config") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Fixed Preview section at the top
            val previewWidth =
                screenWidth - padding.calculateStartPadding(LayoutDirection.Ltr) - padding.calculateEndPadding(
                    LayoutDirection.Ltr
                )
            val previewHeight = previewWidth * 0.4f // Reduced height ratio

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            TextView(context).apply {
                                gravity = android.view.Gravity.CENTER
                                transitionName = SHARED_ELEMENT_NAME
                                previewTextRef.value = this
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        update = { textView ->
                            // Update content
                            textView.text = currentTime
                            textView.textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
                            textView.textSize = textSize * currentSizeMultiplier
                            textView.setTextColor(clockColor.toArgb())

                            // Apply font family and style
                            try {
                                // Find the display name for the font directory
                                val fontFamilies = viewModel.getFontFamilies(context)
                                val fontDisplayName =
                                    fontFamilies.find { it.second == currentTypeface }?.first ?: "Roboto"

                                val fontPath = "fonts/$fontDisplayName/$currentTypeface-$currentStyle.ttf"
                                try {
                                    val typeface = Typeface.createFromAsset(context.assets, fontPath)
                                    textView.typeface = typeface
                                } catch (e: Exception) {
                                    // Check available weights
                                    val availableWeights =
                                        viewModel.getAvailableWeights(context, fontDisplayName, currentTypeface)

                                    // Try to use Regular or first available weight as fallback
                                    if (availableWeights.isNotEmpty()) {
                                        val fallbackStyle = if (availableWeights.contains("Regular"))
                                            "Regular" else availableWeights.first()
                                        val fallbackPath = "fonts/$fontDisplayName/$currentTypeface-$fallbackStyle.ttf"
                                        try {
                                            val fallbackTypeface =
                                                Typeface.createFromAsset(context.assets, fallbackPath)
                                            textView.typeface = fallbackTypeface
                                        } catch (e: Exception) {
                                            textView.typeface = Typeface.DEFAULT
                                        }
                                    } else {
                                        textView.typeface = Typeface.DEFAULT
                                    }
                                }
                            } catch (e: Exception) {
                                // Fallback to default typeface
                                textView.typeface = Typeface.DEFAULT
                            }

                            // Apply animations if needed
                            when (clockAnimation) {
                                ClockAnimation.FADE -> {
                                    textView.alpha = 0f
                                    textView.animate().alpha(1f).setDuration(500).start()
                                }

                                ClockAnimation.PULSE -> {
                                    textView.scaleX = 0.8f
                                    textView.scaleY = 0.8f
                                    textView.animate().scaleX(1f).scaleY(1f).setDuration(300).start()
                                }

                                else -> {
                                    // No animation or other animations
                                }
                            }
                        }
                    )
                }
            }

            // Scrollable content below the fixed preview
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = previewHeight + 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Common style settings
                Text(
                    text = "Style Settings",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Color Picker
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ColorPickerSection(
                            currentColor = clockColor,
                            onColorSelected = { viewModel.setClockColor(it) }
                        )
                    }
                }

                // Font Style Selector
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        FontStyleSelector(
                            selectedFont = clockFontFamily,
                            onFontSelected = { viewModel.setClockFontFamily(it) }
                        )
                    }
                }

                // Size Adjustment Slider
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    SizeAdjustmentSlider(
                        size = clockSize,
                        onSizeChanged = { viewModel.setClockSize(it) }
                    )
                }

                // Animation Style Selector
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ClockAnimationSelector(
                            selectedAnimation = clockAnimation,
                            onAnimationSelected = { viewModel.setClockAnimation(it) }
                        )
                    }
                }
                // Add save button
                Button(
                    onClick = { viewModel.saveSettings().also { onNavigateBack() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text("Save Settings")
                }

                // Add some bottom padding for better scrolling experience
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}


@Composable
fun SwitchPreference(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun ColorPickerSection(
    currentColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colors = listOf(
        Color(0xFFE57373.toInt()), // Red
        Color(0xFF81C784.toInt()), // Green
        Color(0xFF64B5F6.toInt()), // Blue
        Color(0xFFFFB74D.toInt()), // Orange
        Color(0xFF9575CD.toInt()), // Purple
        Color(0xFF4DB6AC.toInt()), // Teal
        Color(0xFFF06292.toInt()), // Pink
        Color(0xFFFFD54F.toInt()), // Yellow
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Clock Color",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(colors.size) { index ->
                val color = colors[index]
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color, shape = RoundedCornerShape(12.dp))
                        .border(
                            width = 2.dp,
                            color = if (currentColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onColorSelected(color) }
                )
            }

            item {
                IconButton(
                    onClick = { /* Open custom color picker dialog */ },
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Custom color"
                    )
                }
            }
        }
    }
}

@Composable
fun FontStyleSelector(
    selectedFont: String,
    onFontSelected: (String) -> Unit
) {
    val context = LocalContext.current

    // Use TypefaceUtil to load font families
    val fontFamilies = remember {
        TypefaceUtil.getFontFamilies(context)
    }

    // Extract typeface and style from selected font
    val parts = selectedFont.split("-")
    val currentTypeface = parts.getOrNull(0) ?: "Roboto"
    val currentStyle = parts.getOrNull(1) ?: "Regular"

    // Get available weights for the current font
    val availableWeights = remember(currentTypeface) {
        TypefaceUtil.getAvailableWeights(context, currentTypeface)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Typeface selection
        Text(
            "Font Typeface",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Display each font family in its own font
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(fontFamilies.size) { index ->
                val fontFamily = fontFamilies[index]

                Card(
                    modifier = Modifier
                        .height(48.dp)
                        .clickable {
                            // When selecting a new font, try to keep the current weight
                            // or fall back to Regular if not available
                            val weights = TypefaceUtil.getAvailableWeights(context, fontFamily.familyName)
                            val newStyle = if (weights.contains(currentStyle))
                                currentStyle else "Regular"
                            onFontSelected("${fontFamily.familyName}-$newStyle")
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentTypeface == fontFamily.familyName)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Load and display text in the actual font
                        AndroidView(
                            factory = { context ->
                                TextView(context).apply {
                                    text = fontFamily.displayName
                                    textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
                                    textSize = 16f
                                }
                            },
                            update = { textView ->
                                try {
                                    // Get a typeface for this font family
                                    val typeface = TypefaceUtil.getTypefaceFromFamilyAndStyle(
                                        context, 
                                        fontFamily.familyName, 
                                        "Regular"
                                    ) ?: Typeface.DEFAULT
                                    
                                    textView.typeface = typeface
                                    textView.text = fontFamily.displayName
                                } catch (e: Exception) {
                                    // Fallback to default
                                    textView.typeface = Typeface.DEFAULT
                                    textView.text = fontFamily.displayName
                                }
                            }
                        )
                    }
                }
            }
        }

        // Font style/weight selection - horizontal slider
        Text(
            "Font Weight",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Current weight indicator with count of available weights
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentStyle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Visual representation of font weights
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Get common font weights to display
            val commonWeights = listOf("Thin", "Light", "Regular", "Bold", "Black")
            
            // Determine which weights to display - either available weights or common weights with some disabled
            val displayWeights = if (availableWeights.size >= 3) {
                availableWeights
            } else {
                commonWeights
            }
            
            displayWeights.forEach { style ->
                val isAvailable = availableWeights.contains(style)
                val isSelected = style == currentStyle
                val weight = when (style) {
                    "Thin" -> androidx.compose.ui.text.font.FontWeight.Thin
                    "Light" -> androidx.compose.ui.text.font.FontWeight.Light
                    "Regular" -> androidx.compose.ui.text.font.FontWeight.Normal
                    "Medium" -> androidx.compose.ui.text.font.FontWeight.Medium
                    "Bold" -> androidx.compose.ui.text.font.FontWeight.Bold
                    "Black" -> androidx.compose.ui.text.font.FontWeight.Black
                    else -> androidx.compose.ui.text.font.FontWeight.Normal
                }

                Text(
                    text = "A",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = weight
                    ),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isAvailable -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    },
                    modifier = Modifier
                        .padding(4.dp)
                        .clickable(enabled = isAvailable) {
                            if (isAvailable && !isSelected) {
                                onFontSelected("$currentTypeface-$style")
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun SizeAdjustmentSlider(
    size: Float,
    onSizeChanged: (Float) -> Unit
) {
    val sizeOptions = listOf(0.5f, 0.75f, 1.0f, 1.5f, 2.0f)
    val sizeLabels = listOf("XS", "S", "M", "L", "XL")

    // Find the closest size option index
    val sliderPosition = remember(size) {
        sizeOptions.indexOf(size).coerceAtLeast(0).toFloat()
    }

    // Slider state to handle interaction
    var sliderPositionState by remember(sliderPosition) {
        mutableStateOf(sliderPosition)
    }

    // Current size preview text
    val currentSize = sizeOptions[sliderPositionState.toInt()]
    val currentLabel = sizeLabels[sliderPositionState.toInt()]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header and current value indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Clock Size",
                style = MaterialTheme.typography.titleMedium
            )

            // Size value chip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "$currentLabel (${currentSize}x)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Slider with custom steps
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            // Material3 Slider
            Slider(
                value = sliderPositionState,
                onValueChange = { newValue ->
                    sliderPositionState = newValue
                    // Update size immediately for better feedback
                    val selectedIndex = newValue.toInt().coerceIn(0, sizeOptions.size - 1)
                    onSizeChanged(sizeOptions[selectedIndex])
                },
                valueRange = 0f..(sizeOptions.size - 1).toFloat(),
                steps = sizeOptions.size - 2, // steps = items - 1 - 1 (for valuRange)
                modifier = Modifier.fillMaxWidth()
            )

            // Size labels below slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                sizeLabels.forEachIndexed { index, label ->
                    val isSelected = index == sliderPositionState.toInt()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Dot indicator
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 6.dp)
                                .background(
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                        )

                        // Size label
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected)
                                androidx.compose.ui.text.font.FontWeight.Bold
                            else
                                androidx.compose.ui.text.font.FontWeight.Normal,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // Numeric value
                        Text(
                            text = "${sizeOptions[index]}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClockAnimationSelector(
    selectedAnimation: ClockAnimation,
    onAnimationSelected: (ClockAnimation) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Animation Style",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        RadioGroup(
            items = ClockAnimation.entries.map { it.displayName },
            selectedIndex = ClockAnimation.entries.indexOf(selectedAnimation),
            onSelectedChanged = { index ->
                onAnimationSelected(ClockAnimation.entries[index])
            }
        )
    }
}

@Composable
fun RadioGroup(
    items: List<String>,
    selectedIndex: Int,
    onSelectedChanged: (Int) -> Unit
) {
    Column {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = index == selectedIndex,
                        onClick = { onSelectedChanged(index) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = index == selectedIndex,
                    onClick = null
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

enum class ClockAnimation(val displayName: String) {
    NONE("None"),
    FADE("Fade"),
    SLIDE("Slide"),
    BOUNCE("Bounce"),
    PULSE("Pulse")
}


