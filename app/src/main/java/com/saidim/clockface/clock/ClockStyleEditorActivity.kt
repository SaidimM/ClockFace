package com.saidim.clockface.clock

import ClockStyle
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.saidim.clockface.clock.ClockStyleEditorActivity.Companion.SHARED_ELEMENT_NAME
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
        val style = intent.getSerializableExtra(EXTRA_STYLE) as ClockStyle

        // Ensure only Minimal style is edited
        if (style != ClockStyle.MINIMAL) {
            finish()
            return
        }

        setContent {
            ClockFaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ClockStyleEditorScreen(
                        style = style,
                        viewModel = viewModel,
                        onNavigateBack = { finishAfterTransition() }
                    )
                }
            }
        }

        setupPreview(style)
    }

    private fun setupPreview(style: ClockStyle) {
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
    style: ClockStyle,
    viewModel: ClockStyleEditorViewModel,
    onNavigateBack: () -> Unit
) {
    var currentTime by remember { mutableStateOf(ClockStyleFormatter.formatTime(style)) }
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
    val is24Hour by viewModel.is24Hour.collectAsState()
    val showSeconds by viewModel.showSeconds.collectAsState()

    // Parse the font family and style
    val parts = clockFontFamily.split("-")
    val currentTypeface = parts.getOrNull(0) ?: "Roboto"
    val currentStyle = parts.getOrNull(1) ?: "Regular"

    // Get the context for asset loading
    val context = LocalContext.current

    // Reference to the preview TextView for direct updates
    val previewTextRef = remember { mutableStateOf<TextView?>(null) }

    LaunchedEffect(Unit) {
        Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    currentTime = ClockStyleFormatter.formatTime(style)
                }
            }, 0, 1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(style.displayName) },
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
                            textView.textSize = textSize * clockSize
                            textView.setTextColor(clockColor.toArgb())

                            // Apply font family and style
                            try {
                                // Find the display name for the font directory
                                val fontFamilies = viewModel.getFontFamilies(context)
                                val fontDisplayName = fontFamilies.find { it.second == currentTypeface }?.first ?: "Roboto"

                                val fontPath = "fonts/$fontDisplayName/$currentTypeface-$currentStyle.ttf"
                                try {
                                    val typeface = Typeface.createFromAsset(context.assets, fontPath)
                                    textView.typeface = typeface
                                } catch (e: Exception) {
                                    // Check available weights
                                    val availableWeights = viewModel.getAvailableWeights(context, fontDisplayName, currentTypeface)

                                    // Try to use Regular or first available weight as fallback
                                    if (availableWeights.isNotEmpty()) {
                                        val fallbackStyle = if (availableWeights.contains("Regular"))
                                            "Regular" else availableWeights.first()
                                        val fallbackPath = "fonts/$fontDisplayName/$currentTypeface-$fallbackStyle.ttf"
                                        try {
                                            val fallbackTypeface = Typeface.createFromAsset(context.assets, fallbackPath)
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

                // Style-specific settings
                Text(
                    text = "Clock Settings",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Only Minimal style controls
                MinimalControls(viewModel)

                // Add save button
                Button(
                    onClick = { viewModel.saveSettings() },
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
fun MinimalControls(viewModel: ClockStyleEditorViewModel) {
    val is24Hour by viewModel.is24Hour.collectAsState()
    val showSeconds by viewModel.showSeconds.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        SwitchPreference(
            title = "24-hour format",
            subtitle = "Use 24-hour time format",
            checked = is24Hour,
            onCheckedChange = { viewModel.setTimeFormat(it) }
        )

        SwitchPreference(
            title = "Show seconds",
            subtitle = "Display seconds in time",
            checked = showSeconds,
            onCheckedChange = { viewModel.setShowSeconds(it) }
        )
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

    // Dynamically load font families from assets
    val fontFamilies = remember {
        val assetManager = context.assets
        try {
            // Get all directories in the fonts folder
            val fontDirs = assetManager.list("fonts") ?: emptyArray()

            // Create pairs of display name and ID for each font family
            fontDirs.filter { dir ->
                // search for .ttf and .otf files in the directory
                // to determine if it's a valid font family
                try {
                    val fontFiles = assetManager.list("fonts/$dir") ?: emptyArray()
                    fontFiles.any { it.endsWith(".ttf") || it.endsWith(".otf") }
                } catch (e: Exception) {
                    false
                }
            }.map { dir ->
                // Create display name and ID for the font
                val displayName = dir
                val fontId = dir.replace(" ", "")
                LogUtils.d("FontFamily", "Found font family: $displayName")
                displayName to fontId
            }.sortedBy { it.first } // Sort alphabetically by display name
        } catch (e: Exception) {
            // Fallback to default list if there's an error
            listOf(
                "Roboto" to "Roboto",
                "Lato" to "Lato",
                "Raleway" to "Raleway",
                "Josefin Sans" to "JosefinSans"
            )
        }
    }

    // Font weights/styles available
    val fontStyles = listOf(
        "Thin",
        "Light",
        "Regular",
        "Bold",
        "Black"
    )

    // Extract typeface and style from selected font
    val parts = selectedFont.split("-")
    val currentTypeface = parts.getOrNull(0) ?: "Roboto"
    val currentStyle = parts.getOrNull(1) ?: "Regular"

    // Find display name for current typeface
    val currentDisplayName = fontFamilies.find { it.second == currentTypeface }?.first ?: "Roboto"

    // Check for available weights for the current font
    val availableWeights = remember(currentTypeface) {
        fontStyles.filter { style ->
            try {
                // Try .ttf first, then .otf if that fails
                val ttfPath = "fonts/$currentDisplayName/$currentTypeface-$style.ttf"
                val otfPath = "fonts/$currentDisplayName/$currentTypeface-$style.otf"

                try {
                    context.assets.open(ttfPath).use { it.close(); true }
                } catch (e: Exception) {
                    // Try .otf if .ttf fails
                    context.assets.open(otfPath).use { it.close(); true }
                }
            } catch (e: Exception) {
                false
            }
        }
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
                val (displayName, fontId) = fontFamilies[index]

                Card(
                    modifier = Modifier
                        .height(48.dp)
                        .clickable {
                            // When selecting a new font, try to keep the current weight
                            // or fall back to Regular if not available
                            val newStyle = if (availableWeights.contains(currentStyle))
                                currentStyle else "Regular"
                            onFontSelected("$fontId-$newStyle")
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentTypeface == fontId)
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
                                    text = displayName
                                    textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
                                    textSize = 16f
                                }
                            },
                            update = { textView ->
                                try {
                                    // Format the path using display name for directory and ID for filename
                                    val fontPath = "fonts/$displayName/$fontId-Regular.ttf"
                                    val typeface = Typeface.createFromAsset(context.assets, fontPath)
                                    textView.typeface = typeface
                                    textView.text = displayName
                                } catch (e: Exception) {
                                    // Fallback to default
                                    textView.typeface = Typeface.DEFAULT
                                    textView.text = displayName
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

        // Slidable weight selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
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

            // Use active weights or all weights with disabled appearance
            val displayedWeights = fontStyles

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                // Visual weight indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Visual representation of font weights
                    displayedWeights.forEach { style ->
                        val isAvailable = availableWeights.contains(style)
                        val isSelected = style == currentStyle
                        val weight = when(style) {
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
    }
}

@Composable
fun SizeAdjustmentSlider(
    size: Float,
    onSizeChanged: (Float) -> Unit
) {
    val sizeOptions = listOf(0.5f, 0.75f, 1.0f, 1.5f, 2.0f)
    val sizeLabels = listOf("XS", "S", "M", "L", "XL")

    // Find the closest size option to the current value
    val currentSizeIndex = remember(size) {
        val closestIndex = sizeOptions.indexOfFirst { it >= size }
        if (closestIndex == -1) sizeOptions.size - 1 else closestIndex
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            "Clock Size",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Size indicator
        Text(
            text = "Size: ${sizeLabels[currentSizeIndex]}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Custom discrete slider implementation
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            sizeOptions.forEachIndexed { index, option ->
                val isSelected = size == option
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSizeChanged(option) }
                ) {
                    // Size option circle
                    Box(
                        modifier = Modifier
                            .size(24.dp + (index * 6).dp) // Gradually increase size
                            .background(
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    )

                    // Size label
                    Text(
                        text = sizeLabels[index],
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
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


