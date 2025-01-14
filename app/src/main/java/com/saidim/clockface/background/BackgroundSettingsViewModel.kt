package com.saidim.clockface.background

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saidim.clockface.background.unsplash.UnsplashPhotoDto
import com.saidim.clockface.settings.AppSettings
import com.saidim.clockface.background.video.PexelsVideoRepository
import com.saidim.clockface.background.video.pexels.Video
import com.saidim.clockface.background.video.pexels.VideoFile
import com.saidim.clockface.utils.getBestVideoFile
import com.saidim.clockface.background.unsplash.UnsplashRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class BackgroundSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings(application)
    private val backgroundSlideshow = BackgroundSlideshow(application, viewModelScope)
    private val pexelsRepository = PexelsVideoRepository()
    private val unsplashRepository = UnsplashRepository()

    private val _selectedImages = MutableStateFlow<List<ImageItem>>(emptyList())
    val selectedImages: StateFlow<List<ImageItem>> = _selectedImages

    private val _backgroundType = MutableStateFlow(BackgroundType.COLOR)
    val backgroundType: StateFlow<BackgroundType> = _backgroundType

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    val videos: StateFlow<List<Video>> = _videos

    var hasLoadedVideos = false
        private set

    private val _selectedVideo = MutableStateFlow<Video?>(null)
    val selectedVideo: StateFlow<Video?> = _selectedVideo

    private val _collectionPhotos = MutableStateFlow<List<ImageItem>>(emptyList())
    val collectionPhotos: StateFlow<List<ImageItem>> = _collectionPhotos

    private val _currentTopic = MutableStateFlow<String?>(null)
    val currentTopic: StateFlow<String?> = _currentTopic

    private val _searchResults = MutableStateFlow<List<UnsplashPhotoDto>>(emptyList())
    val searchResults: StateFlow<List<UnsplashPhotoDto>> = _searchResults

    fun addImages(uris: List<Uri>) {
        val newImages = uris.map { ImageItem.DeviceImage(it) }
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

    fun loadPexelsVideos(topic: String) {
        viewModelScope.launch {
            try {
                val videos = pexelsRepository.searchVideos(topic)
                val videoIds = videos.joinToString(", ") { it.id.toString() }
                Log.d(this.javaClass.simpleName, "query: $topic Video IDs: $videoIds")
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

    fun searchPhotosByTopic(topic: String) = flow {
        try {
            val result = unsplashRepository.searchPhotos(topic)
            emit(result.results)
        } catch (e: Exception) {
            Log.e("BackgroundSettingsVM", "Error searching photos", e)
        }
    }
} 