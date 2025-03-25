package com.saidim.clockface.clock

import ClockStyle
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import com.saidim.clockface.ui.theme.ClockFaceTheme
import android.view.View
import android.widget.TextView
import androidx.compose.ui.platform.LocalDensity

class ClockStylesActivity : ComponentActivity() {
    private val viewModel: ClockStylesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClockFaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ClockStylesScreen(
                        viewModel = viewModel,
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun getDisplayMediumTextSize(): Float {
    return with(LocalDensity.current) {
        MaterialTheme.typography.displayMedium.fontSize.value
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockStylesScreen(
    viewModel: ClockStylesViewModel,
    onNavigateBack: () -> Unit
) {
    val clockStyles by viewModel.clockStyles.collectAsState()
    val context = LocalContext.current
    var previewView by remember { mutableStateOf<View?>(null) }
    val textSize = getDisplayMediumTextSize()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clock Styles") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(clockStyles) { style ->
                ClockStyleItem(
                    style = style,
                    onStyleSelected = {
                        previewView?.let { view ->
                            val intent = Intent(context, ClockStyleEditorActivity::class.java).apply {
                                putExtra(ClockStyleEditorActivity.EXTRA_STYLE, style)
                            }
                            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                context as ComponentActivity,
                                Pair(view, ClockStyleEditorActivity.SHARED_ELEMENT_NAME)
                            )
                            context.startActivity(intent, options.toBundle())
                        }
                    },
                    onPreviewViewCreated = { view ->
                        previewView = view
                    },
                    textSize = textSize
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockStyleItem(
    style: ClockStyle,
    onStyleSelected: () -> Unit,
    onPreviewViewCreated: (View) -> Unit,
    textSize: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        onClick = onStyleSelected,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = style.displayName,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = style.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        transitionName = ClockStyleEditorActivity.SHARED_ELEMENT_NAME
                        onPreviewViewCreated(this)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                update = { textView ->
                    textView.text = ClockStyleFormatter.formatTime(style)
                    textView.textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
                    textView.textSize = textSize
                }
            )
        }
    }
} 