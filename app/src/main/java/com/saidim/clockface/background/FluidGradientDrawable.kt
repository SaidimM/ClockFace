package com.saidim.clockface.background

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import com.saidim.clockface.background.color.GradientColorSettings
import kotlin.math.sin

class FluidGradientDrawable : Drawable() {
    private val paint = Paint()
    private var time = 0f
    internal var settings = GradientColorSettings(listOf(0xFF6200EE.toInt(), 0xFF3700B3.toInt()))
    
    private val DEFAULT_COLOR = 0xFF000000.toInt()

    private val animator = ValueAnimator.ofFloat(0f, 2f * Math.PI.toFloat()).apply {
        duration = settings.animationDuration
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { 
            time = it.animatedValue as Float
            invalidateSelf()
        }
    }

    fun updateSettings(newSettings: GradientColorSettings) {
        settings = newSettings
        if (settings.isAnimated) {
            animator.start()
        } else {
            animator.cancel()
        }
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        if (settings.isThreeLayer) {
            // Draw three animated gradient layers with different phases
            drawGradientLayer(canvas, 0f, 0.3f)
            drawGradientLayer(canvas, 2f * Math.PI.toFloat() / 3f, 0.3f)
            drawGradientLayer(canvas, 4f * Math.PI.toFloat() / 3f, 0.3f)
        } else {
            drawGradientLayer(canvas, 0f, 1f)
        }
    }

    private fun drawGradientLayer(canvas: Canvas, phase: Float, alpha: Float) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        val y1 = height * 0.5f + (sin(time + phase) * height * 0.2f)
        val y2 = height * 0.5f + (sin(time + phase + Math.PI.toFloat()) * height * 0.2f)

        // Ensure we have valid colors
        val gradientColors = when {
            settings.colors.isEmpty() -> listOf(DEFAULT_COLOR, DEFAULT_COLOR)
            settings.colors.size == 1 -> listOf(settings.colors[0], settings.colors[0])
            else -> settings.colors
        }.toIntArray()

        paint.alpha = (alpha * 255).toInt()
        paint.shader = LinearGradient(
            0f, y1,
            width, y2,
            gradientColors,
            null,
            Shader.TileMode.CLAMP
        )

        canvas.drawRect(bounds, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.OPAQUE
} 