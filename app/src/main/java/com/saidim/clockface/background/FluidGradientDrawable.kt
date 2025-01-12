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
import kotlin.math.sin

class FluidGradientDrawable : Drawable() {
    private val paint = Paint()
    private var time = 0f
    private val animator = ValueAnimator.ofFloat(0f, 2f * Math.PI.toFloat()).apply {
        duration = 10000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { 
            time = it.animatedValue as Float
            invalidateSelf()
        }
    }

    private val colors = intArrayOf(
        0xFF6200EE.toInt(),
        0xFF3700B3.toInt(),
        0xFF03DAC5.toInt()
    )

    init {
        animator.start()
    }

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        val y1 = height * 0.5f + (sin(time) * height * 0.2f)
        val y2 = height * 0.5f + (sin(time + Math.PI.toFloat()) * height * 0.2f)

        paint.shader = LinearGradient(
            0f, y1,
            width, y2,
            colors,
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