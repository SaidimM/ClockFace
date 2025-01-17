package com.saidim.clockface.background.model

import android.graphics.Color
import com.squareup.moshi.Moshi

sealed class BackgroundModel {
    data class ColorModel(
//        val colors: MutableList<Int> = mutableListOf(),
        var color: Int = Color.BLACK,
        var enableFluidColor: Boolean = false,
    ) : BackgroundModel() {
        override fun toJson(): String = Moshi.Builder().build().adapter(ColorModel::class.java).toJson(this)
    }

    data class ImageModel(
//        var images: MutableList<String> = mutableListOf(),
        var imageUrl: String = "",
        var enableSlides: Boolean = false,
        var enableAnimation: Boolean = false,
    ) : BackgroundModel() {
        override fun toJson(): String = Moshi.Builder().build().adapter(ImageModel::class.java).toJson(this)
    }

    data class VideoModel(
        var url: String = "",
    ) : BackgroundModel() {
        override fun toJson(): String = Moshi.Builder().build().adapter(VideoModel::class.java).toJson(this)
    }

    abstract fun toJson(): String
}