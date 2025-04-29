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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    // Store previous time to detect changes
    val previousTime = remember { mutableStateOf("") }

    // Use a counter to force recomposition and animation
    val animationCounter = remember { mutableStateOf(0) }

    // Detect time changes and update animation trigger
    LaunchedEffect(time) {
        if (time != previousTime.value) {
            Log.d("AnimClock", "Time changed: '$time' from '${previousTime.value}'. Incrementing counter.")
            animationCounter.value++
            previousTime.value = time
        }
    }

    Row(
        modifier = Modifier.wrapContentSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Render each character with animation
        time.forEachIndexed { index, char ->
            // Set a key that changes when the character changes
            key("${animationCounter.value}_${index}_${char}") {
                val shouldAnimate = index >= previousTime.value.length ||
                        (index < previousTime.value.length && char != previousTime.value[index])

                Log.d(
                    "AnimClock",
                    "Char '$char' at index $index: shouldAnimate=$shouldAnimate, counter=${animationCounter.value}"
                )

                AnimatedCharacter(
                    character = char.toString(),
                    color = color,
                    fontSize = fontSize,
                    fontFamily = fontFamily,
                    animationType = animationType,
                    shouldAnimate = shouldAnimate,
                    animationKey = animationCounter.value
                )
            }
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
    shouldAnimate: Boolean,
    animationKey: Int
) {
    // Animation values with initial values
    val alpha = remember { Animatable(initialValue = if (shouldAnimate) 0f else 1f) }
    val scale = remember { Animatable(initialValue = if (shouldAnimate) 0.5f else 1f) }
    val translateY = remember { Animatable(initialValue = if (shouldAnimate) 50f else 0f) }

    // Use animationKey to force re-execution of LaunchedEffect
    LaunchedEffect(animationKey, character) {
        Log.d(
            "AnimChar",
            "LaunchedEffect for '$character' (key=$animationKey): shouldAnimate=$shouldAnimate, type=$animationType"
        )
        if (shouldAnimate) {
            Log.d("AnimChar", "Animating '$character' with $animationType")
            when (animationType) {
                ClockAnimation.FADE -> {
                    alpha.snapTo(0f)
                    alpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
                }

                ClockAnimation.PULSE -> {
                    scale.snapTo(0.2f)
                    scale.animateTo(1.5f, tween(300))
                    scale.animateTo(1f, tween(200))
                }

                ClockAnimation.SLIDE -> {
                    translateY.snapTo(80f)
                    translateY.animateTo(0f, tween(500))
                }

                ClockAnimation.BOUNCE -> {
                    scale.snapTo(0.2f)
                    scale.animateTo(1.4f, tween(250))
                    scale.animateTo(0.8f, tween(100))
                    scale.animateTo(1.15f, tween(100))
                    scale.animateTo(0.95f, tween(100))
                    scale.animateTo(1f, tween(100))
                }

                else -> { /* No animation */
                }
            }
        } else {
            // Reset animations when not animating
            Log.d("AnimChar", "Resetting animation for '$character' (key=$animationKey)")
            alpha.snapTo(1f)
            scale.snapTo(1f)
            translateY.snapTo(0f)
        }
    }

    // Fixed width for digits to prevent layout shifts
    val isDigit = character.singleOrNull()?.isDigit() ?: false
    val fixedWidth = with(LocalDensity.current) {
        (fontSize.toPx() * 0.7f).toDp()
    }

    Box(
        modifier = Modifier
            .then(
                if (isDigit) {
                    Modifier.width(fixedWidth)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = character,
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(alpha.value)
                .scale(scale.value)
                .offset(y = translateY.value.dp)
        )
    }
}