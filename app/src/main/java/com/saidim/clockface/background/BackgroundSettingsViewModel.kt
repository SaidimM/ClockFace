package com.saidim.clockface.background

import android.app.Application
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saidim.clockface.background.color.GradientColorSettings
import com.saidim.clockface.background.color.GradientDirection
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

    private val _currentColor = MutableStateFlow(0xFF000000.toInt())
    val currentColor: StateFlow<Int> = _currentColor

    private val _gradientColors = MutableStateFlow(listOf(
        0xFF6200EE.toInt(),
        0xFF3700B3.toInt(),
        0xFF03DAC5.toInt()
    ))
    val gradientColors: StateFlow<List<Int>> = _gradientColors

    private val _gradientSettings = MutableStateFlow(
        GradientColorSettings(gradientColors.value)
    )
    val gradientSettings: StateFlow<GradientColorSettings> = _gradientSettings

    private val _selectedColors = MutableStateFlow<List<Int>>(listOf())
    val selectedColors: StateFlow<List<Int>> = _selectedColors

    private val _gradientDirection = MutableStateFlow(GradientDirection.TOP_BOTTOM)
    val gradientDirection: StateFlow<GradientDirection> = _gradientDirection

    private val _previewGradient = MutableStateFlow<FluidGradientDrawable?>(null)
    val previewGradient: StateFlow<FluidGradientDrawable?> = _previewGradient

    private val _isGradientEnabled = MutableStateFlow(false)
    val isGradientEnabled: StateFlow<Boolean> = _isGradientEnabled

    private val DEFAULT_COLOR = 0xFF000000.toInt()

    init {
        _previewGradient.value = FluidGradientDrawable()
        _selectedColors.value = listOf(DEFAULT_COLOR)
    }

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

    fun setColor(color: Int) {
        viewModelScope.launch {
            _currentColor.value = color
            _gradientColors.value = listOf(color, adjustColor(color, 0.8f), adjustColor(color, 0.6f))
            updateGradientSettings(_gradientSettings.value.copy(colors = _gradientColors.value))
        }
    }

    fun updateGradientSettings(settings: GradientColorSettings) {
        viewModelScope.launch {
            _selectedColors.value = settings.colors
            _gradientDirection.value = settings.direction
            _previewGradient.value?.updateSettings(settings)
        }
    }

    private fun adjustColor(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= factor
        return Color.HSVToColor(hsv)
    }

    fun addSelectedColor(color: Int) {
        viewModelScope.launch {
            val currentColors = _selectedColors.value.toMutableList()
            if (_isGradientEnabled.value && currentColors.size < 2) {
                currentColors.add(color)
            } else {
                currentColors.clear()
                currentColors.add(color)
            }
            _selectedColors.value = currentColors
            updateColorBackground()
        }
    }

    fun updateGradientDirection(direction: GradientDirection) {
        viewModelScope.launch {
            _gradientDirection.value = direction
            updateColorBackground()
        }
    }

    private fun updateColorBackground() {
        val settings = if (_isGradientEnabled.value) {
            val colors = _selectedColors.value
            GradientColorSettings(
                colors = when {
                    colors.isEmpty() -> listOf(DEFAULT_COLOR, DEFAULT_COLOR)
                    colors.size == 1 -> listOf(colors[0], adjustColor(colors[0], 0.8f))
                    else -> colors
                },
                direction = _gradientDirection.value,
                isAnimated = false,
                isThreeLayer = false
            )
        } else {
            val color = _selectedColors.value.firstOrNull() ?: DEFAULT_COLOR
            GradientColorSettings(
                colors = listOf(color, color),
                isAnimated = false,
                isThreeLayer = false
            )
        }
        updateGradientSettings(settings)
    }

    fun setGradientEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isGradientEnabled.value = enabled
            updateColorBackground()
        }
    }
} 