package com.saidim.clockface.clock

import ClockStyle
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.saidim.clockface.ui.theme.ClockFaceTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import com.saidim.clockface.clock.ClockStyleEditorActivity.Companion.SHARED_ELEMENT_NAME

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
    val context = LocalContext.current
    val textSize = getDisplayLargeTextSize()

    // Collect state from ViewModel
    val clockColor by viewModel.clockColor.collectAsState()
    val clockFontFamily by viewModel.clockFontFamily.collectAsState()
    val clockSize by viewModel.clockSize.collectAsState()
    val clockAnimation by viewModel.clockAnimation.collectAsState()

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Preview section with applied settings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { context ->
                            TextView(context).apply {
                                transitionName = SHARED_ELEMENT_NAME
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        update = { textView ->
                            textView.text = currentTime
                            textView.textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
                            textView.textSize = textSize * clockSize
                            textView.setTextColor(clockColor.toArgb())

                            // Try to apply font family if available
                            try {
                                if (clockFontFamily != "Default") {
                                    textView.typeface = Typeface.create(clockFontFamily, Typeface.NORMAL)
                                }
                            } catch (e: Exception) {
                                // Fallback to default typeface if the requested one is not available
                            }
                        }
                    )
                }
            }

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
        Color.Black, Color.White, Color.Red, Color.Green, Color.Blue,
        Color.Yellow, Color.Cyan, Color.Magenta, MaterialTheme.colorScheme.primary
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
                        .background(color, shape = CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (currentColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(color) }
                )
            }

            item {
                IconButton(
                    onClick = { /* Open custom color picker dialog */ },
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
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
    val fonts = listOf("Default", "Roboto", "Montserrat", "Lato", "OpenSans", "SourceSansPro")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Font Style",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(fonts.size) { index ->
                val font = fonts[index]
                Card(
                    modifier = Modifier
                        .height(48.dp)
                        .clickable { onFontSelected(font) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFont == font)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = font,
                            style = MaterialTheme.typography.bodyMedium
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
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            "Clock Size",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Smaller")

            Slider(
                value = size,
                onValueChange = onSizeChanged,
                valueRange = 0.5f..2.0f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )

            Icon(Icons.Default.Add, contentDescription = "Larger")
        }

        Text(
            text = "Preview size: ${(size * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(Alignment.End)
        )
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
            items = ClockAnimation.values().map { it.displayName },
            selectedIndex = ClockAnimation.values().indexOf(selectedAnimation),
            onSelectedChanged = { index ->
                onAnimationSelected(ClockAnimation.values()[index])
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
