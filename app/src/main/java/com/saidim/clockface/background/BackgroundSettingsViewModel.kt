package com.saidim.clockface.background

import android.app.Application
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saidim.clockface.background.color.GradientColorSettings
import com.saidim.clockface.background.color.GradientDirection
import com.saidim.clockface.background.model.BackgroundModel
import com.saidim.clockface.background.unsplash.UnsplashPhotoDto
import com.saidim.clockface.settings.AppSettings
import com.saidim.clockface.background.video.PexelsVideoRepository
import com.saidim.clockface.background.video.pexels.Video
import com.saidim.clockface.utils.getBestVideoFile
import com.saidim.clockface.background.unsplash.UnsplashRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class BackgroundSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings.instance
    private val backgroundSlideshow = BackgroundSlideshow(application, viewModelScope)
    private val pexelsRepository = PexelsVideoRepository()
    private val unsplashRepository = UnsplashRepository()
    private var backgroundModel = appSettings.backgroundType

    val colorModel = BackgroundModel.ColorModel()
    val imageModel = BackgroundModel.ImageModel()
    val videoModel = BackgroundModel.VideoModel()

    private val _selectedImage = MutableStateFlow<ImageItem>(ImageItem.DeviceImage(Uri.EMPTY))
    val selectedImage: StateFlow<ImageItem> = _selectedImage

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

    private val _selectedColor = MutableStateFlow<Int>(Color.BLACK)
    val selectedColor: StateFlow<Int> = _selectedColor

    private val _gradientDirection = MutableStateFlow(GradientDirection.TOP_BOTTOM)
    val gradientDirection: StateFlow<GradientDirection> = _gradientDirection

    private val _previewGradient = MutableStateFlow<FluidGradientDrawable?>(null)
    val previewGradient: StateFlow<FluidGradientDrawable?> = _previewGradient

    private val _isGradientEnabled = MutableStateFlow(false)
    val isGradientEnabled: StateFlow<Boolean> = _isGradientEnabled

    private val DEFAULT_COLOR = 0xFF000000.toInt()

    init {
        _previewGradient.value = FluidGradientDrawable()
    }

    fun selectImage(newImages: ImageItem) {
        _selectedImage.value = newImages
        imageModel.imageUrl = newImages.getUrl()
        updateBackgroundModel(imageModel)
    }

    fun setBackgroundType(type: BackgroundType) {
        viewModelScope.launch {
            _backgroundType.value = type
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
            videoModel.url = video.url
            // Save selected video to preferences if needed
            video.getBestVideoFile()?.let { videoFile ->
                appSettings.updateVideoBackground(videoFile.link)
            }
            updateBackgroundModel(videoModel)
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

    fun updateGradientSettings(settings: GradientColorSettings) {
        viewModelScope.launch {
            _selectedColor.value = Color.BLACK
            _gradientDirection.value = settings.direction
            _previewGradient.value?.updateSettings(settings)
        }
    }

    fun selectColor(color: Int) {
        viewModelScope.launch {
            _selectedColor.value = color
            updateColorBackground()
            updateBackgroundModel(colorModel)
        }
    }

    private fun updateColorBackground() {
        val settings = if (_isGradientEnabled.value) {
            val color = _selectedColor.value
            GradientColorSettings(
                colors = listOf(color),
                direction = _gradientDirection.value,
                isAnimated = false,
                isThreeLayer = false
            )
        } else {
            val color = _selectedColor.value
            GradientColorSettings(
                colors = listOf(color, color),
                isAnimated = false,
                isThreeLayer = false
            )
        }
        updateGradientSettings(settings)
    }

    fun updateBackgroundModel(model: BackgroundModel) {
        val type = if (model is BackgroundModel.ColorModel) BackgroundType.COLOR else if (model is BackgroundModel.ImageModel) BackgroundType.IMAGE else BackgroundType.VIDEO
        viewModelScope.launch {
            appSettings.updateBackgroundType(type.ordinal)
            appSettings.updateBackgroundModel(model)
        }
    }
} 