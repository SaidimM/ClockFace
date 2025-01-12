package com.saidim.clockface.background

import android.net.Uri
import com.saidim.clockface.background.unsplash.UnsplashPhoto

sealed class ImageItem {
    data class DeviceImage(val uri: Uri) : ImageItem() {
        override fun getUrl(): String = uri.toString()
        override fun getThumbnailUrl(): String = uri.toString()
    }

    data class UnsplashImage(val photo: UnsplashPhoto) : ImageItem() {
        override fun getUrl(): String = photo.urls.regular
        override fun getThumbnailUrl(): String = photo.urls.regular
    }

    abstract fun getUrl(): String
    abstract fun getThumbnailUrl(): String
} 