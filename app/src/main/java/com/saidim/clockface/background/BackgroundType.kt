package com.saidim.clockface.background

enum class BackgroundType(val displayName: String, val description: String) {
    NONE("Blank", "Display without background"),
    IMAGE("Single Image", "Display one static image"),
    SLIDES("Image Slideshow", "Cycle through multiple images"),
    VIDEO("Video Background", "Play video in background")
} 