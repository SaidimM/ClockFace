package com.saidim.clockface.background

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saidim.clockface.background.unsplash.UnsplashCollection
import com.saidim.clockface.background.unsplash.UnsplashPhoto
import com.saidim.clockface.settings.AppSettings
import com.saidim.clockface.background.video.PexelsVideoRepository
import com.saidim.clockface.background.video.pexels.Video
import com.saidim.clockface.background.video.pexels.VideoFile
import com.saidim.clockface.utils.getBestVideoFile
import com.saidim.clockface.background.unsplash.UnsplashRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BackgroundSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings(application)
    private val backgroundSlideshow = BackgroundSlideshow(application, viewModelScope)
    private val pexelsRepository = PexelsVideoRepository()
    private val unsplashRepository = UnsplashRepository()

    private val _imageSource = MutableStateFlow(ImageSource.DEVICE)
    val imageSource: StateFlow<ImageSource> = _imageSource

    private val _selectedImages = MutableStateFlow<List<ImageItem>>(emptyList())
    val selectedImages: StateFlow<List<ImageItem>> = _selectedImages

    private val _interval = MutableStateFlow(30)
    val interval: StateFlow<Int> = _interval

    private val _backgroundType = MutableStateFlow(BackgroundType.NONE)
    val backgroundType: StateFlow<BackgroundType> = _backgroundType

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos

    var hasLoadedVideos = false
        private set

    private val _selectedVideo = MutableStateFlow<Video?>(null)
    val selectedVideo: StateFlow<Video?> = _selectedVideo

    private val _collections = MutableStateFlow<List<UnsplashCollection>>(emptyList())
    val collections: StateFlow<List<UnsplashCollection>> = _collections

    private val _collectionPhotos = MutableStateFlow<List<ImageItem>>(emptyList())
    val collectionPhotos: StateFlow<List<ImageItem>> = _collectionPhotos

    init {
        viewModelScope.launch {
            _interval.value = appSettings.backgroundInterval.first()
            loadCollections()
        }
    }

    private fun loadCollections() {
        viewModelScope.launch {
            try {
                _collections.value = unsplashRepository.getCollections()
            } catch (e: Exception) {
                Log.e("BackgroundSettingsVM", "Error loading collections", e)
            }
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

    fun addUnsplashPhotos(photos: List<UnsplashPhoto>) {
        val newImages = photos.map { ImageItem.UnsplashImage(it) }
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

    fun setBackgroundType(type: BackgroundType) {
        viewModelScope.launch {
            _backgroundType.value = type
            appSettings.updateBackgroundType(type.ordinal)
        }
    }

    fun loadPexelsVideos() {
        viewModelScope.launch {
            try {
                val videos = pexelsRepository.searchVideos("nature")
                val videoIds = videos.joinToString(", ") { it.id.toString() }
                Log.d(this.javaClass.simpleName, "Video IDs: $videoIds")
                _videos.value = videos
                hasLoadedVideos = true
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun selectVideo(video: Video) {
        viewModelScope.launch {
            _selectedVideo.value = video
            // Save selected video to preferences if needed
            video.getBestVideoFile()?.let { videoFile ->
                appSettings.updateVideoBackground(videoFile.link)
            }
        }
    }

    fun selectCollection(collection: UnsplashCollection) {
        viewModelScope.launch {
            // Load photos from collection and add them to selectedImages
            val photos = unsplashRepository.getCollectionPhotos(collection.id)
            addUnsplashPhotos(photos)
        }
    }

    fun loadCollectionPhotos(collectionId: String) {
        viewModelScope.launch {
            try {
                val photos = unsplashRepository.getCollectionPhotos(collectionId)
                _collectionPhotos.value = photos.map { photo ->
                    ImageItem.UnsplashImage(photo)
                }
            } catch (e: Exception) {
                Log.e("BackgroundSettingsVM", "Error loading collection photos", e)
            }
        }
    }
} 