package com.saidim.clockface.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.saidim.clockface.R

class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // View modes
    enum class ViewMode { GRID, SPECTRA, SLIDE }
    
    // UI Components
    private val segmentedControl: RadioGroup
    private val colorDisplayView: View
    private val opacitySeekBar: SeekBar
    private val opacityTextView: TextView
    private val hexTextView: TextView
    private val presetColorsLayout: LinearLayout
    
    // Color picker views
    private val gridView: ColorGridView
    private val spectraView: ColorSpectrumView
    private val slideView: ColorSlideView
    
    // Current state
    private var currentMode: ViewMode = ViewMode.SPECTRA
    private var currentColor: Int = Color.RED
    private var currentOpacity: Int = 255
    
    // Listener
    var onColorSelectedListener: ((color: Int) -> Unit)? = null

    init {
        // Inflate the layout
        inflate(context, R.layout.color_picker_view, this)
        
        // Find views
        segmentedControl = findViewById(R.id.segmentedControl)
        colorDisplayView = findViewById(R.id.colorDisplay)
        opacitySeekBar = findViewById(R.id.opacitySeekBar)
        opacityTextView = findViewById(R.id.opacityPercentage)
        hexTextView = findViewById(R.id.hexValue)
        presetColorsLayout = findViewById(R.id.presetColors)
        
        // Initialize color picker views
        gridView = findViewById(R.id.gridView)
        spectraView = findViewById(R.id.spectraView)
        slideView = findViewById(R.id.slideView)
        
        // Set up segmented control for switching modes
        segmentedControl.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.gridButton -> switchMode(ViewMode.GRID)
                R.id.spectraButton -> switchMode(ViewMode.SPECTRA)
                R.id.slideButton -> switchMode(ViewMode.SLIDE)
            }
        }
        
        // Set up opacity slider
        opacitySeekBar.max = 255
        opacitySeekBar.progress = 255
        opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentOpacity = progress
                updateOpacityText()
                updateCurrentColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Set up color selection callbacks
        gridView.onColorSelected = { updateColorWithoutAlpha(it) }
        spectraView.onColorSelected = { updateColorWithoutAlpha(it) }
        slideView.onColorSelected = { updateColorWithoutAlpha(it) }
        
        // Set up preset colors
        setupPresetColors()
        
        // Initial mode
        switchMode(ViewMode.SPECTRA)
        updateOpacityText()
        updateHexText()
    }
    
    private fun switchMode(mode: ViewMode) {
        currentMode = mode
        
        // Hide all views first
        gridView.visibility = View.GONE
        spectraView.visibility = View.GONE
        slideView.visibility = View.GONE
        
        // Show the selected view
        when (mode) {
            ViewMode.GRID -> gridView.visibility = View.VISIBLE
            ViewMode.SPECTRA -> spectraView.visibility = View.VISIBLE
            ViewMode.SLIDE -> slideView.visibility = View.VISIBLE
        }
    }
    
    private fun updateColorWithoutAlpha(color: Int) {
        // Preserve the current alpha value
        currentColor = Color.argb(
            currentOpacity,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
        
        updateColorDisplay()
        updateHexText()
        
        // Update the slide view with RGB values if in slide mode
        if (currentMode == ViewMode.SLIDE) {
            slideView.updateSliders(Color.red(color), Color.green(color), Color.blue(color), false)
        }
        
        // Notify listener
        onColorSelectedListener?.invoke(currentColor)
    }
    
    private fun updateCurrentColor() {
        // Update the color with current opacity
        currentColor = Color.argb(
            currentOpacity,
            Color.red(currentColor),
            Color.green(currentColor),
            Color.blue(currentColor)
        )
        
        updateColorDisplay()
        updateHexText()
        
        // Notify listener
        onColorSelectedListener?.invoke(currentColor)
    }
    
    private fun updateColorDisplay() {
        colorDisplayView.setBackgroundColor(currentColor)
    }
    
    private fun updateOpacityText() {
        val percentage = (currentOpacity * 100 / 255)
        opacityTextView.text = "$percentage%"
    }
    
    private fun updateHexText() {
        val hexColor = String.format("#%06X", 0xFFFFFF and currentColor)
        hexTextView.text = hexColor
    }
    
    private fun setupPresetColors() {
        val presetColors = listOf(
            Color.LTGRAY, Color.RED, Color.MAGENTA, Color.BLUE, Color.CYAN, Color.GREEN,
            Color.YELLOW, Color.DKGRAY
        )
        
        for (color in presetColors) {
            val colorView = View(context).apply {
                layoutParams = LayoutParams(48, 48).apply {
                    setMargins(8,8,8,8)
                }
                setBackgroundColor(color)
                setOnClickListener {
                    updateColorWithoutAlpha(color)
                }
            }
            presetColorsLayout.addView(colorView)
        }
        
        // Add "+" button for custom colors
        val addButton = View(context).apply {
            layoutParams = LayoutParams(48, 48).apply {
                setMargins(8,8,8,8)
            }
            background = ContextCompat.getDrawable(context, R.drawable.ic_add_circle)
            setOnClickListener {
                // Logic for adding a custom color
            }
        }
        presetColorsLayout.addView(addButton)
    }
    
    // Method to set color programmatically
    fun setColor(color: Int) {
        currentOpacity = Color.alpha(color)
        updateColorWithoutAlpha(color)
        opacitySeekBar.progress = currentOpacity
    }

    fun getCurrentColor() = currentColor
}

/**
 * The Spectrum View component - shows a continuous color spectrum
 */
class ColorSpectrumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    private var currentX = 0f
    private var currentY = 0f
    private var spectrumRect = RectF()
    
    var onColorSelected: ((Int) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Update the drawing area
        val padding = 16f
        spectrumRect.set(padding, padding, w - padding, h - padding)
        
        // Create the spectrum shader
        val colors = intArrayOf(
            Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, 
            Color.BLUE, Color.MAGENTA, Color.RED
        )
        
        // Create horizontal rainbow gradient
        val horizontalShader = LinearGradient(
            spectrumRect.left, 0f, spectrumRect.right, 0f,
            colors, null, Shader.TileMode.CLAMP
        )
        
        // Create vertical white-to-black gradient
        val verticalShader = LinearGradient(
            0f, spectrumRect.top, 0f, spectrumRect.bottom,
            Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP
        )
        
        // Combine shaders
        paint.shader = ComposeShader(horizontalShader, verticalShader, PorterDuff.Mode.MULTIPLY)
        
        // Set initial position to center
        currentX = spectrumRect.centerX()
        currentY = spectrumRect.centerY()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw the spectrum rectangle
        canvas.drawRect(spectrumRect, paint)
        
        // Draw the selector circle
        canvas.drawCircle(currentX, currentY, 12f, selectorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                currentX = event.x.coerceIn(spectrumRect.left, spectrumRect.right)
                currentY = event.y.coerceIn(spectrumRect.top, spectrumRect.bottom)
                
                // Get the color at the touch position
                val color = getColorAtPosition(currentX, currentY)
                onColorSelected?.invoke(color)
                
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun getColorAtPosition(x: Float, y: Float): Int {
        // Map x coordinate to hue (0-360)
        val hue = ((x - spectrumRect.left) / spectrumRect.width() * 360f).toInt()
        
        // Map y coordinate to saturation and value
        val saturation = 1f
        val value = 1f - ((y - spectrumRect.top) / spectrumRect.height())
        
        // Convert HSV to RGB
        val hsv = floatArrayOf(hue.toFloat(), saturation, value)
        return Color.HSVToColor(hsv)
    }
}

/**
 * The Slide View component - shows RGB sliders
 */
class ColorSlideView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // UI Components
    private val redSeekBar: SeekBar
    private val greenSeekBar: SeekBar
    private val blueSeekBar: SeekBar
    private val redValueText: TextView
    private val greenValueText: TextView
    private val blueValueText: TextView
    
    // Current RGB values
    private var redValue: Int = 176
    private var greenValue: Int = 176
    private var blueValue: Int = 176
    
    // Callback
    var onColorSelected: ((Int) -> Unit)? = null
    
    // Flag to prevent circular updates
    private var isUpdatingFromExternalSource = false

    init {
        // Set orientation
        orientation = VERTICAL
        
        // Inflate layout
        inflate(context, R.layout.slide_view, this)
        
        // Find views
        redSeekBar = findViewById(R.id.redSeekBar)
        greenSeekBar = findViewById(R.id.greenSeekBar)
        blueSeekBar = findViewById(R.id.blueSeekBar)
        redValueText = findViewById(R.id.redValue)
        greenValueText = findViewById(R.id.greenValue)
        blueValueText = findViewById(R.id.blueValue)
        
        // Set up seekbars
        setupSeekBars()
        
        // Update initial values
        updateColorValues()
    }
    
    private fun setupSeekBars() {
        // Set max values to 255 (8-bit color)
        redSeekBar.max = 255
        greenSeekBar.max = 255
        blueSeekBar.max = 255
        
        // Set initial values
        redSeekBar.progress = redValue
        greenSeekBar.progress = greenValue
        blueSeekBar.progress = blueValue
        
        // Set up listeners
        val seekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (isUpdatingFromExternalSource) return
                
                when (seekBar) {
                    redSeekBar -> redValue = progress
                    greenSeekBar -> greenValue = progress
                    blueSeekBar -> blueValue = progress
                }
                
                updateColorValues()
                onColorSelected?.invoke(Color.rgb(redValue, greenValue, blueValue))
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        
        redSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        greenSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
        blueSeekBar.setOnSeekBarChangeListener(seekBarChangeListener)
    }
    
    private fun updateColorValues() {
        // Update text displays
        redValueText.text = redValue.toString()
        greenValueText.text = greenValue.toString()
        blueValueText.text = blueValue.toString()
        
        // Update slider gradients
        updateSliderGradients()
    }
    
    private fun updateSliderGradients() {
        // Red slider: gradient from 0 to 255 red
        val redColors = intArrayOf(
            Color.rgb(0, greenValue, blueValue),
            Color.rgb(255, greenValue, blueValue)
        )
        redSeekBar.progressDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, redColors
        )
        
        // Green slider: gradient from 0 to 255 green
        val greenColors = intArrayOf(
            Color.rgb(redValue, 0, blueValue),
            Color.rgb(redValue, 255, blueValue)
        )
        greenSeekBar.progressDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, greenColors
        )
        
        // Blue slider: gradient from 0 to 255 blue
        val blueColors = intArrayOf(
            Color.rgb(redValue, greenValue, 0),
            Color.rgb(redValue, greenValue, 255)
        )
        blueSeekBar.progressDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, blueColors
        )
    }
    
    // Method to update the sliders from an external source
    fun updateSliders(red: Int, green: Int, blue: Int, updateListeners: Boolean = true) {
        isUpdatingFromExternalSource = !updateListeners
        
        redValue = red
        greenValue = green
        blueValue = blue
        
        redSeekBar.progress = red
        greenSeekBar.progress = green
        blueSeekBar.progress = blue
        
        updateColorValues()
        
        isUpdatingFromExternalSource = false
    }
}

/**
 * The Grid View component - shows a color palette grid
 */
class ColorGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paletteColors = mutableListOf<Int>()
    private val cellSize = 48f
    private val gridSize = 10f
    
    var onColorSelected: ((Int) -> Unit)? = null

    init {
        // Generate color palette (8x10 grid)
        generateColorPalette()
    }
    
    private fun generateColorPalette() {
        // Generate a color palette similar to the one in the image
        // Create color gradients from light to dark for each hue
        val hues = listOf(0f, 30f, 60f, 120f, 180f, 210f, 240f, 270f, 300f, 330f)
        val saturations = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.6f, 0.7f, 0.85f, 1.0f)
        
        // Add colors to the palette
        for (saturation in saturations.reversed()) {
            for (hue in hues) {
                paletteColors.add(Color.HSVToColor(floatArrayOf(hue, saturation, 1.0f)))
            }
        }
        
        // Add grayscale colors on the right side
        val grayscaleBrightness = listOf(1.0f, 0.9f, 0.8f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f)
        for (brightness in grayscaleBrightness) {
            paletteColors.add(Color.rgb(
                (brightness * 255).toInt(),
                (brightness * 255).toInt(),
                (brightness * 255).toInt()
            ))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = (cellSize * gridSize).toInt() + paddingLeft + paddingRight
        val height = (cellSize * 8).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw the color grid
        var index = 0
        for (row in 0 until 8) {
            for (col in 0 until 10) {
                if (index < paletteColors.size) {
                    paint.color = paletteColors[index]
                    
                    val left = col * cellSize + paddingLeft
                    val top = row * cellSize + paddingTop
                    val right = left + cellSize
                    val bottom = top + cellSize
                    
                    canvas.drawRect(left, top, right, bottom, paint)
                    index++
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Calculate the grid cell
                val col = ((event.x - paddingLeft) / cellSize).toInt()
                val row = ((event.y - paddingTop) / cellSize).toInt()
                
                // Ensure valid grid position
                if (col in 0 until 10 && row in 0 until 8) {
                    val index = row * 10 + col
                    if (index < paletteColors.size) {
                        onColorSelected?.invoke(paletteColors[index])
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }
}