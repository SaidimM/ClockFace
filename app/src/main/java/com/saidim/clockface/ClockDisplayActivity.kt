package com.saidim.clockface

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
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
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb

class ClockDisplayActivity : AppCompatActivity() {
    private val viewModel: ClockViewModel by viewModels()

    private var currentTimeText: String = ""
    private lateinit var timeTextAnimator: TimeTextAnimator
    private lateinit var rootView: View
    private lateinit var imageLoader: ImageLoader
    private lateinit var clockCharactersContainer: LinearLayout
    private val appSettings = AppSettings.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_clock_display)

        rootView = findViewById(R.id.root)
        
        // Set up a container for individual clock characters
        setupClockCharactersContainer()
        
        imageLoader = ImageLoader(this)

        setupObservers()
        setupLongPressMenu()
        observeBackground()
    }
    
    private fun setupClockCharactersContainer() {
        // Find the original clockText view to get its parent
        val originalClockText = findViewById<LinearLayout>(R.id.clockText)
        
        // Initialize the animator with the container
        timeTextAnimator = TimeTextAnimator(originalClockText)
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
                
                // Update the animator with new style properties
                timeTextAnimator.apply {
                    setAnimationType(config.animation)
                    updateTextProperties(
                        color = config.fontColor,
                        size = 64 * config.fontSize, // Base size multiplied by config size
                        font = TypefaceUtil.getTypefaceFromConfig(this@ClockDisplayActivity, config.fontFamily)
                    )
                }
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