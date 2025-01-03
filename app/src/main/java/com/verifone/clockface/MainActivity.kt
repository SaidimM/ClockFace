package com.verifone.clockface

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private val viewModel: ClockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val switch24Hour = findViewById<SwitchMaterial>(R.id.switch24Hour)
        val switchShowSeconds = findViewById<SwitchMaterial>(R.id.switchShowSeconds)
        val startButton = findViewById<MaterialButton>(R.id.startButton)

        viewModel.is24Hour.observe(this) { is24Hour ->
            switch24Hour.isChecked = is24Hour
        }

        viewModel.showSeconds.observe(this) { showSeconds ->
            switchShowSeconds.isChecked = showSeconds
        }

        switch24Hour.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTimeFormat(isChecked)
        }

        switchShowSeconds.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowSeconds(isChecked)
        }

        startButton.setOnClickListener {
            Intent(this, ClockDisplayActivity::class.java).apply {
                putExtra("is24Hour", viewModel.is24Hour.value)
                putExtra("showSeconds", viewModel.showSeconds.value)
                startActivity(this)
            }
        }
    }
}