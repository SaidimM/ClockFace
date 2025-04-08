package com.saidim.clockface

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import com.saidim.clockface.clock.ClockAnimation
import kotlin.math.max

/**
 * A utility class to animate individual characters in the clock display.
 * This provides smooth transitions for each character when it changes.
 */
class TimeTextAnimator(private val containerView: ViewGroup) {
    private val characterViews = mutableListOf<TextView>()
    private val animators = mutableMapOf<Int, ValueAnimator>()
    private var currentText = ""
    private var currentAnimationType: ClockAnimation = ClockAnimation.FADE
    private var textColor = 0
    private var textSize = 0f
    private var typeface: Typeface? = null
    
    init {
        // Configure the container as horizontal
        if (containerView is LinearLayout) {
            containerView.orientation = LinearLayout.HORIZONTAL
            containerView.gravity = Gravity.CENTER
        }
    }
    
    /**
     * Sets the animation type to use when characters change
     * 
     * @param animationType The type of animation to use for character changes
     */
    fun setAnimationType(animationType: ClockAnimation) {
        this.currentAnimationType = animationType
    }
    
    /**
     * Updates the style properties for all character views
     */
    fun updateTextProperties(color: Int, size: Float, font: Typeface?) {
        this.textColor = color
        this.textSize = size
        this.typeface = font
        
        // Update all existing character views
        characterViews.forEach { charView ->
            charView.setTextColor(color)
            charView.textSize = size
            charView.typeface = font
        }
    }
    
    /**
     * Animates the change from current text to the new text.
     * Only characters that actually change will be animated.
     * 
     * @param newText The new time text to display
     */
    fun animateTextChange(newText: String) {
        // If the text is the same, no need to animate
        if (newText == currentText) return
        
        Log.d("TimeTextAnimator", "Animating from '$currentText' to '$newText'")
        
        // Ensure we have enough character views
        ensureCharacterViews(newText.length)
        
        // For each character position, check if it changed and animate if needed
        val maxLength = max(currentText.length, newText.length)
        for (i in 0 until maxLength) {
            val oldChar = if (i < currentText.length) currentText[i] else ' '
            val newChar = if (i < newText.length) newText[i] else ' '
            
            // If this character changed, animate it
            if (oldChar != newChar) {
                Log.d("TimeTextAnimator", "Character at position $i changed: '$oldChar' to '$newChar'")
                animateCharacterChange(i, newChar.toString())
            }
        }
        
        // Update current text
        currentText = newText
    }
    
    /**
     * Makes sure we have enough TextView widgets for all characters
     */
    private fun ensureCharacterViews(count: Int) {
        // Create new character views if needed
        while (characterViews.size < count) {
            val charView = createCharacterView(containerView.context)
            containerView.addView(charView)
            characterViews.add(charView)
        }
        
        // Show only the views we need, hide the rest
        characterViews.forEachIndexed { index, view ->
            view.visibility = if (index < count) View.VISIBLE else View.GONE
        }
    }
    
    /**
     * Creates a new TextView for a single character
     */
    private fun createCharacterView(context: Context): TextView {
        return TextView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            setTextColor(textColor)
            textSize = this@TimeTextAnimator.textSize
            typeface = this@TimeTextAnimator.typeface
            // No padding to keep characters close together
            setPadding(0, 0, 0, 0)
        }
    }
    
    /**
     * Animates a change for a specific character position
     */
    private fun animateCharacterChange(position: Int, newChar: String) {
        // Cancel any ongoing animation for this position
        animators[position]?.cancel()
        
        // Get the appropriate view
        val charView = characterViews[position]
        
        // Apply the animation based on the current animation type
        when (currentAnimationType) {
            ClockAnimation.FADE -> animateCharacterFade(position, charView, newChar)
            ClockAnimation.PULSE -> animateCharacterPulse(position, charView, newChar)
            ClockAnimation.SLIDE -> animateCharacterSlide(position, charView, newChar)
            ClockAnimation.BOUNCE -> animateCharacterBounce(position, charView, newChar) 
            else -> {
                // No animation, just update the text
                charView.text = newChar
            }
        }
    }
    
    private fun animateCharacterFade(position: Int, charView: TextView, newChar: String) {
        val animator = ValueAnimator.ofFloat(1f, 0f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                charView.alpha = value
                
                // When fully transparent, change the text
                if (value <= 0.05f && charView.text.toString() != newChar) {
                    charView.text = newChar
                }
            }
            
            setupAnimatorListeners(position, charView)
        }
        
        animator.start()
        animators[position] = animator
    }
    
    private fun animateCharacterPulse(position: Int, charView: TextView, newChar: String) {
        val animator = ValueAnimator.ofFloat(1f, 0.7f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                charView.scaleX = value
                charView.scaleY = value
                
                // Change text at the smallest point
                if (value <= 0.71f && charView.text.toString() != newChar) {
                    charView.text = newChar
                }
            }
            
            setupAnimatorListeners(position, charView)
        }
        
        animator.start()
        animators[position] = animator
    }
    
    private fun animateCharacterSlide(position: Int, charView: TextView, newChar: String) {
        val animator = ValueAnimator.ofFloat(0f, -30f, 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                charView.translationY = value
                
                // Change text at midpoint
                if (value <= -29f && charView.text.toString() != newChar) {
                    charView.text = newChar
                }
            }
            
            setupAnimatorListeners(position, charView)
        }
        
        animator.start()
        animators[position] = animator
    }
    
    private fun animateCharacterBounce(position: Int, charView: TextView, newChar: String) {
        val animator = ValueAnimator.ofFloat(1f, 0.8f, 1.2f, 1f).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                if (value < 1f) {
                    // First half - squish horizontally
                    charView.scaleX = value
                    charView.scaleY = 2 - value // Stretch vertically
                    
                    // Change text at the most squished point
                    if (value <= 0.81f && charView.text.toString() != newChar) {
                        charView.text = newChar
                    }
                } else {
                    // Second half - bounce up then back to normal
                    charView.scaleX = value
                    charView.scaleY = 2 - value
                }
            }
            
            setupAnimatorListeners(position, charView)
        }
        
        animator.start()
        animators[position] = animator
    }
    
    private fun ValueAnimator.setupAnimatorListeners(position: Int, charView: TextView) {
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Reset any transformations
                charView.alpha = 1f
                charView.scaleX = 1f
                charView.scaleY = 1f
                charView.translationX = 0f
                charView.translationY = 0f
                animators.remove(position)
            }
            
            override fun onAnimationCancel(animation: Animator) {
                // Reset any transformations
                charView.alpha = 1f
                charView.scaleX = 1f
                charView.scaleY = 1f
                charView.translationX = 0f
                charView.translationY = 0f
                animators.remove(position)
            }
        })
    }
    
    /**
     * Immediately cancels all ongoing animations and resets the text views.
     */
    fun cancel() {
        // Cancel all animations
        animators.values.forEach { it.cancel() }
        animators.clear()
        
        // Reset all views
        characterViews.forEach { charView ->
            charView.alpha = 1f
            charView.scaleX = 1f
            charView.scaleY = 1f
            charView.translationX = 0f
            charView.translationY = 0f
        }
    }
    
    /**
     * Force-updates the text immediately with no animation
     */
    fun setTextWithoutAnimation(newText: String) {
        // Cancel any ongoing animations
        cancel()
        
        // Ensure we have enough character views
        ensureCharacterViews(newText.length)
        
        // Update each character
        for (i in newText.indices) {
            characterViews[i].text = newText[i].toString()
        }
        
        // Update current text
        currentText = newText
    }
} 