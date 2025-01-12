package com.saidim.clockface

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.saidim.clockface.background.BackgroundManager
import com.unsplash.pickerandroid.photopicker.UnsplashPhotoPicker
import com.unsplash.pickerandroid.photopicker.data.UnsplashPhoto
import com.unsplash.pickerandroid.photopicker.presentation.UnsplashPickerActivity
import kotlinx.coroutines.launch
import coil.ImageLoader
import coil.request.ImageRequest

class ClockDisplayActivity : AppCompatActivity() {
    private val viewModel: ClockViewModel by viewModels()
    private var currentTimeText: String = ""
    private lateinit var timeTextAnimator: TimeTextAnimator
    private lateinit var backgroundManager: BackgroundManager
    private lateinit var rootView: View
    private lateinit var imageLoader: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_clock_display)
        
        window.setDecorFitsSystemWindows(false)
        
        rootView = findViewById(R.id.root)
        val clockText = findViewById<TextView>(R.id.clockText)
        timeTextAnimator = TimeTextAnimator(clockText)
        backgroundManager = BackgroundManager(this)
        imageLoader = ImageLoader(this)

        setupObservers()
        setupLongPressMenu()
        
        // Get settings from intent
        intent.getBooleanExtra("is24Hour", true).let {
            viewModel.setTimeFormat(it)
        }
        intent.getBooleanExtra("showSeconds", true).let {
            viewModel.setShowSeconds(it)
        }

        // Initialize Unsplash Photo Picker
        UnsplashPhotoPicker.init(
            application,
            "0E_aPev9Qt0iPOugsqoNrwrtJpbeIJ6a26KZFwok0EM",
            "K0H8UTyDBMvVydHRLe4zuQr3X-7KB3ZOs6oUVt3D9u0"
        )
    }

    private fun setupObservers() {
        viewModel.currentTime.observe(this) { time ->
            if (time != currentTimeText) {
                currentTimeText = time
                timeTextAnimator.animateTextChange(time)
            }
        }

        backgroundManager.currentBackground.observe(this) { drawable ->
            rootView.background = drawable
        }
    }

    private fun setupLongPressMenu() {
        rootView.setOnLongClickListener { view ->
            showBackgroundMenu(view)
            true
        }
    }

    private fun showBackgroundMenu(view: View) {
        PopupMenu(this, view).apply {
            menu.add(0, 1, 0, "System Wallpaper")
            menu.add(0, 2, 0, "Fluid Gradient")
            menu.add(0, 3, 0, "Unsplash Daily")

            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> lifecycleScope.launch { 
                        backgroundManager.setLauncherWallpaper() 
                    }
                    2 -> backgroundManager.setGradientBackground()
                    3 -> launchUnsplashPicker()
                }
                true
            }
            show()
        }
    }

    private fun launchUnsplashPicker() {
        val intent = UnsplashPickerActivity.getStartingIntent(this, true)
        startActivityForResult(intent, UNSPLASH_PHOTO_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UNSPLASH_PHOTO_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
            val photos: List<UnsplashPhoto>? = data?.getParcelableArrayListExtra(UnsplashPickerActivity.EXTRA_PHOTOS)
            photos?.firstOrNull()?.let { photo ->
                val request = ImageRequest.Builder(this)
                    .data(photo.urls.regular)
                    .target { drawable ->
                        rootView.background = drawable
                    }
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    companion object {
        private const val UNSPLASH_PHOTO_PICKER_REQUEST_CODE = 1
    }
} 