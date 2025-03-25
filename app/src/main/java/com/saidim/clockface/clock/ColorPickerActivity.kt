package com.saidim.clockface.clock

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.saidim.clockface.ui.theme.ClockFaceTheme
import com.saidim.clockface.widgets.ColorPickerView

class ColorPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClockFaceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ColorPickerScreen(
                        onColorSelected = { color ->
                            val resultIntent = Intent().apply {
                                putExtra("selected_color", color)
                            }
                            setResult(Activity.RESULT_OK, resultIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerScreen(
    onColorSelected: (Int) -> Unit
) {
    var selectedColor by remember { mutableStateOf(Color.parseColor("#FF4081")) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Color Picker") },
                navigationIcon = {
                    IconButton(onClick = { (context as? Activity)?.finish() }) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Color preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(androidx.compose.ui.graphics.Color(selectedColor))
            )

            // Color picker
            AndroidView(
                factory = { context ->
                    ColorPickerView(context).apply {
                        setColor(selectedColor)
                        onColorSelectedListener = { color ->
                            selectedColor = color
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

            // Color hex value
            Text(
                text = String.format("#%08X", selectedColor),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            // Save button
            Button(
                onClick = { onColorSelected(selectedColor) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Color")
            }
        }
    }
}