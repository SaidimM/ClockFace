package com.saidim.clockface

import android.widget.TextView
import android.animation.ValueAnimator
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan

class TimeTextAnimator(private val textView: TextView) {
    private var lastText = ""
    
    fun animateTextChange(newText: String) {
        if (lastText.isEmpty()) {
            textView.text = newText
            lastText = newText
            return
        }

        val spannableString = SpannableString(newText)
        val animator = ValueAnimator.ofFloat(0f, 1f)
        
        // Find which characters have changed
        val changedIndices = newText.indices.filter { i ->
            i >= lastText.length || newText[i] != lastText[i]
        }

        // Set initial alpha for changed characters
        changedIndices.forEach { i ->
            spannableString.setSpan(
                ForegroundColorSpan(Color.TRANSPARENT),
                i, i + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        textView.text = spannableString

        animator.duration = 150
        animator.addUpdateListener { animation ->
            val alpha = (animation.animatedValue as Float)
            val color = Color.argb(
                (alpha * 255).toInt(),
                255, 255, 255
            )

            val newSpannable = SpannableString(newText)
            changedIndices.forEach { i ->
                newSpannable.setSpan(
                    ForegroundColorSpan(color),
                    i, i + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            textView.text = newSpannable
        }

        animator.start()
        lastText = newText
    }
} 