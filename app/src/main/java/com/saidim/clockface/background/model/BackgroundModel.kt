package com.saidim.clockface.background.model

import com.squareup.moshi.Moshi

sealed class BackgroundModel {
    data class ColorModel(
        var colors: List<Int> = listOf(),
        var enableFluidColor: Boolean = false,
    ) : BackgroundModel() {
        override fun toJson(): String = Moshi.Builder().build().adapter(ColorModel::class.java).toJson(this)
    }

    data class ImageModel(
        var url: String,
        var enableSlides: Boolean = false,
        var enableAnimation: Boolean = false,
    ) : BackgroundModel() {
        override fun toJson(): String = Moshi.Builder().build().adapter(ImageModel::class.java).toJson(this)
    }

    data class VideoModel(
        var url: String,
    ) : BackgroundModel() {
        override fun toJson(): String = Moshi.Builder().build().adapter(VideoModel::class.java).toJson(this)
    }

    abstract fun toJson(): String
}