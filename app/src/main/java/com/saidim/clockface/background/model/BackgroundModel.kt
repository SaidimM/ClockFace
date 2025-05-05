package com.saidim.clockface.background.model

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import com.google.gson.Gson
import com.saidim.clockface.background.video.PexelsVideo

sealed class BackgroundModel {
    data class ColorModel(
        var color: Int = Color.BLACK,
        var enableFluidColor: Boolean = false,
    ) : BackgroundModel() {
        override fun toJson(): String = Gson().toJson(this)
        fun getDrawable() = ColorDrawable(color)
    }

    data class ImageModel(
        var imageUrl: String = "",
        var enableSlides: Boolean = false,
        var enableAnimation: Boolean = false,
        var blurHash: String = "",
        var topicId: String = "",
        var featuredImages: List<String> = emptyList()
    ) : BackgroundModel() {
        override fun toJson(): String = Gson().toJson(this)
    }

    data class VideoModel(
        var url: String = "",
        var pixelVideo: PexelsVideo = PexelsVideo()
    ) : BackgroundModel() {
        override fun toJson(): String = Gson().toJson(this)
    }

    abstract fun toJson(): String
}