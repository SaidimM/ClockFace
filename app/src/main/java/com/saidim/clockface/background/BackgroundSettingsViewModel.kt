package com.saidim.clockface.background

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saidim.clockface.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BackgroundSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings(application)
    private val backgroundSlideshow = BackgroundSlideshow(application, viewModelScope)

    private val _imageSource = MutableStateFlow(ImageSource.DEVICE)
    val imageSource: StateFlow<ImageSource> = _imageSource

    private val _selectedImages = MutableStateFlow<List<ImageItem>>(emptyList())
    val selectedImages: StateFlow<List<ImageItem>> = _selectedImages

    private val _interval = MutableStateFlow(30)
    val interval: StateFlow<Int> = _interval

    init {
        viewModelScope.launch {
            _interval.value = appSettings.backgroundInterval.first()
        }
    }

    fun setImageSource(source: ImageSource) {
        _imageSource.value = source
    }

    fun setInterval(minutes: Int) {
        viewModelScope.launch {
            _interval.value = minutes
            appSettings.updateBackgroundInterval(minutes)
            backgroundSlideshow.setInterval(minutes)
        }
    }

    fun addImages(uris: List<Uri>) {
        val newImages = uris.map { ImageItem.DeviceImage(it) }
        updateImages(newImages)
    }

    fun addUnsplashPhotos(photos: List<com.unsplash.pickerandroid.photopicker.data.UnsplashPhoto>) {
        val mappedPhotos = photos.map { photo ->
            UnsplashPhoto(
                urls = UnsplashPhoto.PhotoUrls(
                    regular = photo.urls.regular ?: ""
                ),
                user = UnsplashPhoto.User(
                    name = photo.user.name
                )
            )
        }
        val newImages = mappedPhotos.map { ImageItem.UnsplashImage(it) }
        updateImages(newImages)
    }

    private fun updateImages(newImages: List<ImageItem>) {
        val currentList = _selectedImages.value.toMutableList()
        currentList.addAll(newImages)
        _selectedImages.value = currentList

        // Update background slideshow
        val imageUrls = currentList.map { it.getUrl() }
        backgroundSlideshow.setImages(imageUrls)
    }

    fun removeImage(position: Int) {
        val currentList = _selectedImages.value.toMutableList()
        currentList.removeAt(position)
        _selectedImages.value = currentList

        // Update background slideshow
        val imageUrls = currentList.map { it.getUrl() }
        backgroundSlideshow.setImages(imageUrls)
    }
} 