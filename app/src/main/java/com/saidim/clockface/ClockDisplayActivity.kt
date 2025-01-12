package com.saidim.clockface

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import coil.ImageLoader

class ClockDisplayActivity : AppCompatActivity() {
    private val viewModel: ClockViewModel by viewModels()
    private var currentTimeText: String = ""
    private lateinit var timeTextAnimator: TimeTextAnimator
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
    }

    private fun setupObservers() {
        viewModel.currentTime.observe(this) { time ->
            if (time != currentTimeText) {
                currentTimeText = time
                timeTextAnimator.animateTextChange(time)
            }
        }
    }

    private fun setupLongPressMenu() {
    }

    companion object {
        private const val UNSPLASH_PHOTO_PICKER_REQUEST_CODE = 1
    }
} 