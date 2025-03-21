package com.saidim.clockface.clock

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.saidim.clockface.R
import com.saidim.clockface.widgets.ColorPickerView

class ColorPickerActivity : AppCompatActivity() {
    
    private lateinit var colorPickerView: ColorPickerView
    private lateinit var colorPreview: View
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_picker)
        
        colorPickerView = findViewById(R.id.colorPicker)
        colorPreview = findViewById(R.id.colorPreview)
        
        // Set initial color
        colorPickerView.setColor(Color.parseColor("#FF4081"))
        
        // Listen for color changes
        colorPickerView.onColorSelectedListener = { color ->
            colorPreview.setBackgroundColor(color)
            // Format and display hex value with alpha
            val hexColor = String.format("#%08X", color)
            findViewById<TextView>(R.id.selectedColorText).text = hexColor
        }
        
        // Set up save button
        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("selected_color", colorPickerView.getCurrentColor())
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}