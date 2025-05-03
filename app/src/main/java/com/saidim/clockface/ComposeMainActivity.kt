package com.saidim.clockface

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.saidim.clockface.background.ComposeBackgroundSettingsActivity
import com.saidim.clockface.clock.ClockStyleEditorActivity
import com.saidim.clockface.clock.ColorPickerActivity
import com.saidim.clockface.ui.theme.ClockFaceTheme

class ComposeMainActivity : ComponentActivity() {

    private val viewModel: ClockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        setContent {
            ClockFaceTheme { // Reuse the theme from ComposeClockDisplayActivity
                mainScreen(viewModel = viewModel)
            }
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.navigationBars()) // Hide status and navigation bars
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

@Composable
fun mainScreen(viewModel: ClockViewModel) {
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity
    val currentView = LocalView.current

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // how to get current activity object
            // val activity = LocalContext.current as? Activity
            // Start Button
            Button(onClick = {
                val intent = Intent(context, ComposeClockDisplayActivity::class.java)
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity!!, // Ensure activity is not null
                    currentView, // Use LocalView.current here
                    context.getString(R.string.clock_preview_transition)
                )
                // Need to actually start the activity with the options
                activity.startActivity(intent, options.toBundle())
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