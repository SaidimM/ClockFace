package com.saidim.clockface

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.load
import com.saidim.clockface.background.BackgroundType
import com.saidim.clockface.background.model.BackgroundModel
import com.saidim.clockface.clock.ClockAnimation
import com.saidim.clockface.clock.TypefaceUtil
import com.saidim.clockface.clock.syles.ClockStyleConfig
import com.saidim.clockface.settings.AppSettings
import kotlinx.coroutines.launch
import android.graphics.Typeface
import android.util.Log
import android.graphics.Color
import android.view.animation.AlphaAnimation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import androidx.compose.ui.graphics.toArgb

class ClockDisplayActivity : AppCompatActivity() {
    private val viewModel: ClockViewModel by viewModels()

    private var currentTimeText: String = ""
    private lateinit var timeTextAnimator: TimeTextAnimator
    private lateinit var rootView: View
    private lateinit var imageLoader: ImageLoader
    private val appSettings = AppSettings.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_clock_display)

        rootView = findViewById(R.id.root)
        val clockText = findViewById<TextView>(R.id.clockText)
        timeTextAnimator = TimeTextAnimator(clockText)
        imageLoader = ImageLoader(this)

        setupObservers()
        setupLongPressMenu()
        observeBackground()

        // Get settings from intent
        intent.getBooleanExtra("is24Hour", true).let {
            viewModel.setTimeFormat(it)
        }
        intent.getBooleanExtra("showSeconds", true).let {
            viewModel.setShowSeconds(it)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.currentTime.observe(this@ClockDisplayActivity) { time ->
                if (time != currentTimeText) {
                    currentTimeText = time
                    timeTextAnimator.animateTextChange(time)
                }
            }

            // Observe the single clock style config directly
            appSettings.clockStyleConfig.collect { config ->
                Log.d("ClockDisplayActivity", "Applying config: $config")
                val clockText = findViewById<TextView>(R.id.clockText)
                
                // Apply the single ClockStyleConfig
                clockText.apply {
                    // Update content
                    textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
                    setTextColor(config.fontColor)
                    textSize = 32f * config.fontSize // Base size multiplied by config size
                    
                    // Apply typeface from config
                    typeface = TypefaceUtil.getTypefaceFromConfig(context, config.fontFamily)
                    
                    // Apply animation based on config
                    applyClockAnimation(this, config.animation)
                }
            }
        }
    }
    
    private fun applyClockAnimation(textView: TextView, animation: ClockAnimation) {
        // Clear any existing animations
        textView.clearAnimation()
        
        when (animation) {
            ClockAnimation.FADE -> {
                val fadeAnim = AlphaAnimation(0.0f, 1.0f).apply {
                    duration = 500
                    fillAfter = true
                }
                textView.startAnimation(fadeAnim)
            }
            ClockAnimation.PULSE -> {
                val pulseAnim = ScaleAnimation(
                    0.8f, 1.0f, 0.8f, 1.0f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                ).apply {
                    duration = 300
                    fillAfter = true
                }
                textView.startAnimation(pulseAnim)
            }
            ClockAnimation.SLIDE -> {
                val slideAnim = TranslateAnimation(
                    -50f, 0f, 0f, 0f
                ).apply {
                    duration = 300
                    fillAfter = true
                }
                textView.startAnimation(slideAnim)
            }
            ClockAnimation.BOUNCE -> {
                val bounceAnim = ScaleAnimation(
                    0.8f, 1.0f, 1.2f, 1.0f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f
                ).apply {
                    duration = 500
                    fillAfter = true
                }
                textView.startAnimation(bounceAnim)
            }
            else -> {
                // No animation (ClockAnimation.NONE)
            }
        }
    }

    private fun setupLongPressMenu() {
    }

    private fun observeBackground() {
        lifecycleScope.launch {
            viewModel.backgroundType.collect { type ->
                when (type) {
                    BackgroundType.COLOR -> setupColorBackground()
                    BackgroundType.IMAGE -> setupImageBackground()
                    BackgroundType.VIDEO -> setupVideoBackground()
                }
            }
        }
    }

    private fun setupColorBackground() {
        val previewColor = findViewById<ImageView>(R.id.previewColor)
        val previewImage = findViewById<ImageView>(R.id.previewImage)
        val previewVideo = findViewById<VideoView>(R.id.previewVideo)

        lifecycleScope.launch {
            viewModel.backgroundModel.collect { model ->
                if (model is BackgroundModel.ColorModel) {
                    previewColor.visibility = View.VISIBLE
                    previewImage.visibility = View.GONE
                    previewVideo.visibility = View.GONE
                    
                    try {
                        previewColor.background = ColorDrawable(model.color)
                    } catch (e: Exception) {
                        Log.e("ClockDisplayActivity", "Error setting color background: ${e.message}")
                        // Set a default color in case of error
                        previewColor.background = ColorDrawable(Color.BLACK)
                    }
                } else {
                    // If we received the wrong type of model, log an error
                    Log.e("ClockDisplayActivity", "Expected ColorModel but got ${model::class.java.simpleName}")
                }
            }
        }
    }

    private fun setupImageBackground() {
        val previewColor = findViewById<ImageView>(R.id.previewColor)
        val previewImage = findViewById<ImageView>(R.id.previewImage)
        val previewVideo = findViewById<VideoView>(R.id.previewVideo)

        lifecycleScope.launch {
            viewModel.backgroundModel.collect { model ->
                if (model is BackgroundModel.ImageModel) {
                    previewColor.visibility = View.GONE
                    previewImage.visibility = View.VISIBLE
                    previewVideo.visibility = View.GONE

                    try {
                        if (model.imageUrl.isNotEmpty()) {
                            previewImage.load(model.imageUrl) { 
                                crossfade(true)
                                error(ColorDrawable(Color.GRAY))
                            }
                        } else {
                            // If imageUrl is empty, show a placeholder
                            previewImage.setImageDrawable(ColorDrawable(Color.GRAY))
                        }
                    } catch (e: Exception) {
                        Log.e("ClockDisplayActivity", "Error loading image: ${e.message}")
                        // Show a placeholder in case of error
                        previewImage.setImageDrawable(ColorDrawable(Color.GRAY))
                    }
                } else {
                    // If we received the wrong type of model, log an error
                    Log.e("ClockDisplayActivity", "Expected ImageModel but got ${model::class.java.simpleName}")
                }
            }
        }
    }

    private fun setupVideoBackground() {
        val previewColor = findViewById<ImageView>(R.id.previewColor)
        val previewImage = findViewById<ImageView>(R.id.previewImage)
        val previewVideo = findViewById<VideoView>(R.id.previewVideo)

        lifecycleScope.launch {
            viewModel.backgroundModel.collect { model ->
                if (model is BackgroundModel.VideoModel) {
                    previewColor.visibility = View.GONE
                    previewImage.visibility = View.GONE
                    previewVideo.visibility = View.VISIBLE

                    try {
                        previewVideo.apply {
                            setVideoPath(model.url)
                            setOnPreparedListener { mediaPlayer ->
                                mediaPlayer.isLooping = true
                                mediaPlayer.setVolume(0f, 0f)
                                start()
                            }
                            setOnErrorListener { _, what, extra ->
                                Log.e("ClockDisplayActivity", "Video error: what=$what, extra=$extra")
                                // Fall back to a solid color background
                                previewVideo.visibility = View.GONE
                                previewColor.visibility = View.VISIBLE
                                true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ClockDisplayActivity", "Error setting up video: ${e.message}")
                        // Fall back to a solid color background
                        previewVideo.visibility = View.GONE
                        previewColor.visibility = View.VISIBLE
                    }
                } else {
                    // If we received the wrong type of model, hide the video view
                    previewVideo.visibility = View.GONE
                    Log.e("ClockDisplayActivity", "Expected VideoModel but got ${model::class.java.simpleName}")
                }
            }
        }
    }

    companion object {
        private const val UNSPLASH_PHOTO_PICKER_REQUEST_CODE = 1
    }
} 