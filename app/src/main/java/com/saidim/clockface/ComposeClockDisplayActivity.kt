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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.saidim.clockface.background.BackgroundType
import com.saidim.clockface.background.model.BackgroundModel
import com.saidim.clockface.clock.ClockAnimation
import com.saidim.clockface.clock.TypefaceUtil
import com.saidim.clockface.settings.AppSettings
import com.saidim.clockface.ui.theme.ClockFaceTheme
import kotlinx.coroutines.launch

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

    // Use AnimatedClockText if animation is enabled, otherwise use regular Text
    if (style.animation != ClockAnimation.NONE) {
        AnimatedClockText(
            time = time,
            color = androidx.compose.ui.graphics.Color(style.fontColor),
            fontSize = (64 * style.fontSize).sp,
            fontFamily = typeface?.let { FontFamily(it) } ?: FontFamily.Default,
            animationType = style.animation
        )
    } else {
        Text(
            text = time,
            color = androidx.compose.ui.graphics.Color(style.fontColor),
            fontSize = (64 * style.fontSize).sp,
            fontFamily = typeface?.let { FontFamily(it) } ?: FontFamily.Default
        )
    }
}

@Composable
fun AnimatedClockText(
    time: String,
    color: androidx.compose.ui.graphics.Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontFamily: FontFamily,
    animationType: ClockAnimation
) {
    // Previous state to detect changes
    val previousTime = remember { mutableStateOf("") }

    // Track changed positions to animate only those characters
    val changedPositions = remember(time, previousTime.value) {
        if (previousTime.value.isEmpty()) {
            // First render, mark all as changed
            time.indices.toSet()
        } else {
            // Find which characters have changed
            time.indices.filter { i ->
                i >= previousTime.value.length || time[i] != previousTime.value[i]
            }.toSet()
        }
    }

    // Update previous time for next comparison
    SideEffect {
        if (time != previousTime.value) {
            previousTime.value = time
        }
    }

    // Display characters in a row
    Row(
        modifier = Modifier.wrapContentSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Render each character with animation if needed
        time.forEachIndexed { index, char ->
            AnimatedCharacter(
                character = char.toString(),
                color = color,
                fontSize = fontSize,
                fontFamily = fontFamily,
                animationType = animationType,
                shouldAnimate = changedPositions.contains(index)
            )
        }
    }
}

@Composable
fun AnimatedCharacter(
    character: String,
    color: androidx.compose.ui.graphics.Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontFamily: FontFamily,
    animationType: ClockAnimation,
    shouldAnimate: Boolean
) {
    // Animation properties
    val animationSpec = tween<Float>(
        durationMillis = 300,
        easing = FastOutSlowInEasing
    )

    // Animation values
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(1f) }
    val translateY = remember { Animatable(0f) }

    // Trigger animation when character changes
    LaunchedEffect(character, shouldAnimate) {
        if (shouldAnimate) {
            when (animationType) {
                ClockAnimation.FADE -> {
                    // Fade out and in
                    alpha.snapTo(1f)
                    alpha.animateTo(0f, animationSpec)
                    alpha.animateTo(1f, animationSpec)
                }

                ClockAnimation.PULSE -> {
                    // Pulse effect (shrink and grow)
                    scale.snapTo(1f)
                    scale.animateTo(0.7f, animationSpec)
                    scale.animateTo(1f, animationSpec)
                }

                ClockAnimation.SLIDE -> {
                    // Slide up and down
                    translateY.snapTo(0f)
                    translateY.animateTo(-30f, animationSpec)
                    translateY.animateTo(0f, animationSpec)
                }

                ClockAnimation.BOUNCE -> {
                    // Bounce effect
                    scale.snapTo(1f)
                    // Squish
                    launch {
                        scale.animateTo(0.8f, animationSpec)
                        scale.animateTo(1.2f, animationSpec)
                        scale.animateTo(1f, animationSpec)
                    }
                }

                else -> { /* No animation */
                }
            }
        }
    }

    // Determine if we should use fixed width based on character
    val isDigit = character.singleOrNull()?.isDigit() ?: false
    // Calculate a reasonable fixed width for digits (using "0" as reference)
    val fixedWidth = with(androidx.compose.ui.platform.LocalDensity.current) {
        (fontSize.toPx() * 0.6f).toDp()
    }

    Text(
        text = character,
        color = color,
        fontSize = fontSize,
        fontFamily = fontFamily,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
            .alpha(alpha.value)
            .scale(scale.value)
            .offset(y = translateY.value.dp)
            // Apply fixed width only for digits, not for colons and other characters
            .then(
                if (isDigit) {
                    Modifier.width(fixedWidth)
                } else {
                    Modifier
                }
            )
            // Add a key to help Compose identify when the character changes
            .composed {
                val key = character + shouldAnimate
                remember(key) { this }
            }
    )
}