package com.saidim.clockface.background

enum class BackgroundType(val displayName: String, val description: String) {
    COLOR("Color", "Display without background"),
    IMAGE("Single Image", "Display one static image"),
    VIDEO("Video Background", "Play video in background")
} 