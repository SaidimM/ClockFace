package com.verifone.clockface

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView

class ClockDisplayActivity : AppCompatActivity() {
    private val viewModel: ClockViewModel by viewModels()
    private var currentTimeText: String = ""
    private lateinit var timeTextAnimator: TimeTextAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_clock_display)
        
        window.setDecorFitsSystemWindows(false)
        
        val clockText = findViewById<TextView>(R.id.clockText)
        timeTextAnimator = TimeTextAnimator(clockText)

        viewModel.currentTime.observe(this) { time ->
            if (time != currentTimeText) {
                currentTimeText = time
                timeTextAnimator.animateTextChange(time)
            }
        }

        // Get settings from intent
        intent.getBooleanExtra("is24Hour", true).let {
            viewModel.setTimeFormat(it)
        }
        intent.getBooleanExtra("showSeconds", true).let {
            viewModel.setShowSeconds(it)
        }
    }
} 