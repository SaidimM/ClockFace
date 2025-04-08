package com.saidim.clockface

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView

/**
 * A utility class to animate text changes in the clock display.
 * This provides smooth transitions between time updates.
 */
class TimeTextAnimator(private val textView: TextView) {
    private var currentAnimator: ValueAnimator? = null
    private var isAnimating = false
    
    /**
     * Animates the change from current text to the new text.
     * Uses a simple fade out/fade in animation to avoid jarring changes.
     * 
     * @param newText The new time text to display
     */
    fun animateTextChange(newText: String) {
        // If we're already animating, cancel that animation
        currentAnimator?.cancel()
        
        // If the text view already shows the new text, do nothing
        if (textView.text == newText) return
        
        // If the text view is not visible, just set the text without animation
        if (textView.alpha == 0f) {
            textView.text = newText
            return
        }
        
        // Create and configure the animation
        val animator = ValueAnimator.ofFloat(1f, 0f, 1f).apply {
            duration = 300 // Total animation duration
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                textView.alpha = value
                
                // When fully transparent, change the text
                if (value <= 0.05f && textView.text != newText) {
                    textView.text = newText
                }
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    isAnimating = true
                }
                
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                    currentAnimator = null
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    isAnimating = false
                    textView.alpha = 1f  // Ensure visibility is restored
                    currentAnimator = null
                }
            })
        }
        
        // Start the animation
        animator.start()
        currentAnimator = animator
    }
    
    /**
     * Immediately cancels any ongoing animation and resets the text view.
     */
    fun cancel() {
        currentAnimator?.cancel()
        textView.alpha = 1f
    }
} 