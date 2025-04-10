package com.saidim.clockface

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saidim.clockface.background.ComposeBackgroundSettingsActivity
import com.saidim.clockface.clock.ClockStyleEditorActivity
import com.saidim.clockface.clock.ColorPickerActivity
import com.saidim.clockface.ui.theme.ClockFaceTheme

class ComposeMainActivity : ComponentActivity() {

    private val viewModel: ClockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClockFaceTheme { // Reuse the theme from ComposeClockDisplayActivity
                mainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun mainScreen(viewModel: ClockViewModel) {
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Start Button
            Button(onClick = {
                val intent = Intent(context, ComposeClockDisplayActivity::class.java).apply {
                    // Pass relevant data if needed, similar to original MainActivity
                    // Using observeAsState would be better here if the state is needed reactively
                    // For simplicity, accessing value directly for one-time read
                    putExtra(MainActivity.EXTRA_IS_24_HOUR, viewModel.is24Hour.value != false)
                    putExtra(MainActivity.EXTRA_SHOW_SECONDS, viewModel.showSeconds.value != false)
                }
                // TODO: Implement shared element transition if desired
                context.startActivity(intent)
            }) {
                Text("Start Clock") // Use appropriate string resource later
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Background Settings Button
            Button(onClick = {
                context.startActivity(Intent(context, ComposeBackgroundSettingsActivity::class.java))
            }) {
                Text("Background Settings") // Use appropriate string resource later
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Clock Styles Button
            Button(onClick = {
                context.startActivity(Intent(context, ClockStyleEditorActivity::class.java))
            }) {
                Text("Clock Styles") // Use appropriate string resource later
            }

            Spacer(modifier = Modifier.height(16.dp))

            // General Settings Button (originally ColorPickerActivity)
            Button(onClick = {
                // Assuming ColorPickerActivity is still the intended settings target
                context.startActivity(Intent(context, ColorPickerActivity::class.java))
            }) {
                Text("Settings") // Use appropriate string resource later
            }
        }
    }
}