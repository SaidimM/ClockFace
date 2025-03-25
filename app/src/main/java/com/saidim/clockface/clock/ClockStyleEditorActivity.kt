package com.saidim.clockface.clock

import ClockStyle
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
    val lifecycleScope = rememberCoroutineScope()

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
            // Preview section
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
                            textView.textSize = textSize
                        }
                    )
                }
            }

            // Controls section
            when (style) {
                ClockStyle.MINIMAL -> MinimalControls(viewModel)
                ClockStyle.ANALOG -> AnalogControls(viewModel)
                ClockStyle.WORD -> WordControls(viewModel)
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
fun AnalogControls(viewModel: ClockStyleEditorViewModel) {
    val showNumbers by viewModel.showAnalogNumbers.collectAsState()
    val showTicks by viewModel.showAnalogTicks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        SwitchPreference(
            title = "Show numbers",
            subtitle = "Display hour numbers",
            checked = showNumbers,
            onCheckedChange = { viewModel.setShowAnalogNumbers(it) }
        )

        SwitchPreference(
            title = "Show ticks",
            subtitle = "Display minute ticks",
            checked = showTicks,
            onCheckedChange = { viewModel.setShowAnalogTicks(it) }
        )
    }
}

@Composable
fun WordControls(viewModel: ClockStyleEditorViewModel) {
    val useCasual by viewModel.useWordCasual.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        SwitchPreference(
            title = "Use casual format",
            subtitle = "Show time in casual language",
            checked = useCasual,
            onCheckedChange = { viewModel.setWordCasual(it) }
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