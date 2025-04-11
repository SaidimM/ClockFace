package com.saidim.clockface

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.saidim.clockface.background.BackgroundType
import com.saidim.clockface.background.model.BackgroundModel
import com.saidim.clockface.clock.TypefaceUtil
import com.saidim.clockface.settings.AppSettings
import com.saidim.clockface.ui.theme.ClockFaceTheme

class ComposeClockDisplayActivity : ComponentActivity() {

    private val viewModel: ClockViewModel by viewModels()
    private val appSettings = AppSettings.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ClockFaceTheme { // Assuming you have a theme defined
                clockScreen(viewModel = viewModel, appSettings = appSettings)
            }
        }
    }
}

@Composable
fun clockScreen(viewModel: ClockViewModel, appSettings: AppSettings) {
    val backgroundType by viewModel.backgroundType.collectAsState(initial = BackgroundType.COLOR)
    val backgroundModel by viewModel.backgroundModel.collectAsState(initial = BackgroundModel.ColorModel(Color.BLACK))
    val currentTime by viewModel.currentTime.observeAsState(initial = "") // Use observeAsState for LiveData
    val clockStyle by appSettings.clockStyleConfig.collectAsState(initial = com.saidim.clockface.clock.syles.ClockStyleConfig()) // Provide default initial value

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background Layer
        backgroundLayer(backgroundType, backgroundModel)

        // Clock Text Layer
        clockText(
            time = currentTime,
            style = clockStyle
        )
    }
}

@Composable
fun backgroundLayer(
    type: BackgroundType,
    model: BackgroundModel?
) {
    when (type) {
        BackgroundType.COLOR -> {
            val colorInt = (model as? BackgroundModel.ColorModel)?.color ?: Color.BLACK
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color(colorInt))
            )
        }

        BackgroundType.IMAGE -> {
            val imageUrl = (model as? BackgroundModel.ImageModel)?.imageUrl ?: ""
            AsyncImage(
                model = imageUrl,
                contentDescription = "Background Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                // Consider adding placeholder and error drawables/colors
                // placeholder = painterResource(id = R.drawable.placeholder),
                // error = painterResource(id = R.drawable.error)
            )
        }

        BackgroundType.VIDEO -> {
            val videoUrl = (model as? BackgroundModel.VideoModel)?.url ?: ""
            videoBackground(videoUrl)
        }
    }
}

@Composable
fun videoBackground(videoUrl: String) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                try {
                    setVideoPath(videoUrl)
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = true
                        mediaPlayer.setVolume(0f, 0f) // Mute video
                        start()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e("ComposeClockDisplay", "VideoView Error: what=$what, extra=$extra")
                        // Handle error, maybe show a fallback background
                        true // Indicate the error was handled
                    }
                } catch (e: Exception) {
                    Log.e("ComposeClockDisplay", "Error setting video path: $videoUrl", e)
                    // Handle error
                }
            }
        },
        update = { videoView ->
            // Restart or update video if URL changes (and it's not the same)
            if (videoView.tag != videoUrl) {
                videoView.tag = videoUrl // Store current URL to prevent unnecessary resets
                try {
                    videoView.stopPlayback() // Stop previous video if any
                    videoView.setVideoPath(videoUrl)
                    videoView.start()
                } catch (e: Exception) {
                    Log.e("ComposeClockDisplay", "Error updating video path: $videoUrl", e)
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}


@Composable
fun clockText(
    time: String,
    style: com.saidim.clockface.clock.syles.ClockStyleConfig
) {
    val context = LocalContext.current
    val typeface = remember(style.fontFamily) {
        try {
            TypefaceUtil.getTypefaceFromConfig(context, style.fontFamily)
        } catch (e: Exception) {
            Log.w("ComposeClockDisplay", "Failed to load typeface for ${style.fontFamily}, using default", e)
            Typeface.DEFAULT // Fallback
        }
    }

    Text(
        text = time,
        color = androidx.compose.ui.graphics.Color(style.fontColor),
        fontSize = (64 * style.fontSize).sp, // Base size * multiplier
        fontFamily = typeface?.let { FontFamily(it) } ?: FontFamily.Default
    )
}