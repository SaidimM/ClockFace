package com.saidim.clockface.background.color

enum class GradientDirection {
    TOP_BOTTOM,
    LEFT_RIGHT,
    DIAGONAL
}

data class GradientColorSettings(
    val colors: List<Int>,
    val direction: GradientDirection = GradientDirection.TOP_BOTTOM,
    val isAnimated: Boolean = false,
    val animationDuration: Long = 10000,
    val isThreeLayer: Boolean = false
)