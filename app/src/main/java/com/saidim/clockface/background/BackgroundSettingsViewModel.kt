package com.saidim.clockface.background

import android.app.Application
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saidim.clockface.background.color.GradientColorSettings
import com.saidim.clockface.background.color.GradientDirection
import com.saidim.clockface.background.model.BackgroundModel
import com.saidim.clockface.background.unsplash.UnsplashPhotoDto
import com.saidim.clockface.background.unsplash.UnsplashRepository
import com.saidim.clockface.background.video.PexelsVideoRepository
import com.saidim.clockface.background.video.PexelsVideo
import com.saidim.clockface.background.video.pexels.Video
import com.saidim.clockface.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class BackgroundSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings.instance
    private val pexelsRepository = PexelsVideoRepository()
    private val unsplashRepository = UnsplashRepository()

    val colorModel = BackgroundModel.ColorModel()
    val imageModel = BackgroundModel.ImageModel()
    val videoModel = BackgroundModel.VideoModel()

    var colorSelected: (BackgroundModel.ColorModel) -> Unit = {}
    var imageSelected: (BackgroundModel.ImageModel) -> Unit = {}
    var videoSelected: (BackgroundModel.VideoModel) -> Unit = {}

    private val _selectedImage = MutableStateFlow<BackgroundModel.ImageModel>(BackgroundModel.ImageModel())
    val selectedImage: StateFlow<BackgroundModel.ImageModel> = _selectedImage

    private val _backgroundType = MutableStateFlow(BackgroundType.COLOR)
    val backgroundType: StateFlow<BackgroundType> = _backgroundType

    private val _videos = MutableStateFlow<List<PexelsVideo>>(emptyList())
    val videos: StateFlow<List<PexelsVideo>> = _videos

    var hasLoadedVideos = false
        private set

    private val _selectedVideo = MutableStateFlow<PexelsVideo?>(null)
    val selectedVideo: StateFlow<PexelsVideo?> = _selectedVideo

    private val _searchResults = MutableStateFlow<List<UnsplashPhotoDto>>(emptyList())
    val searchResults: StateFlow<List<UnsplashPhotoDto>> = _searchResults

    private val _gradientColors = MutableStateFlow(
        listOf(
            0xFF6200EE.toInt(),
            0xFF3700B3.toInt(),
            0xFF03DAC5.toInt()
        )
    )
    val gradientColors: StateFlow<List<Int>> = _gradientColors

    private val _previewGradient = MutableStateFlow<FluidGradientDrawable?>(null)
    val previewGradient: StateFlow<FluidGradientDrawable?> = _previewGradient

    private val _isGradientEnabled = MutableStateFlow(false)
    val isGradientEnabled: StateFlow<Boolean> = _isGradientEnabled

    private val DEFAULT_COLOR = 0xFF000000.toInt()

    init {
        viewModelScope.launch {
            // Load saved background type and model
            _backgroundType.value = appSettings.backgroundType.first()

            // Load saved background model
            appSettings.backgroundModel.first()?.let { model ->
                when (model) {
                    is BackgroundModel.ColorModel -> {
                        colorModel.color = model.color
                        updateColorBackground()
                    }

                    is BackgroundModel.ImageModel -> {
                        // Load image preview
                        _selectedImage.value = imageModel
                    }

                    is BackgroundModel.VideoModel -> {
                        videoModel.url = model.url
                        // Load PexelsVideo preview
                        loadPexelsVideos("popular") // Load default videos
                    }
                }
            }
        }
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
                Log.d(this.javaClass.simpleName, "query: $topic PexelsVideo IDs: $videoIds")
                _videos.value = videos
                hasLoadedVideos = true
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun selectColor(color: Int) {
        viewModelScope.launch {
            updateColorBackground()
            updateBackgroundModel(colorModel)
        }
    }

    fun selectImage(newImage: BackgroundModel.ImageModel) {
        _selectedImage.value = newImage
        imageModel.imageUrl = newImage.imageUrl
        updateBackgroundModel(imageModel)
    }

    fun selectVideo(video: PexelsVideo) {
        viewModelScope.launch {
            videoModel.pixelVideo = video
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
            _previewGradient.value?.updateSettings(settings)
        }
    }

    private fun updateColorBackground() {
        val settings = if (_isGradientEnabled.value) {
            GradientColorSettings(
                colors = listOf(Color.BLACK, colorModel.color),
                isAnimated = false,
                isThreeLayer = false
            )
        } else {
            GradientColorSettings(
                colors = listOf(Color.BLACK, colorModel.color),
                isAnimated = false,
                isThreeLayer = false
            )
        }
        updateGradientSettings(settings)
    }

    fun updateBackgroundModel(model: BackgroundModel) {
        val type =
            if (model is BackgroundModel.ColorModel) BackgroundType.COLOR else if (model is BackgroundModel.ImageModel) BackgroundType.IMAGE else BackgroundType.VIDEO
        viewModelScope.launch {
            appSettings.updateBackgroundType(type.ordinal)
            appSettings.updateBackgroundModel(model)
        }
        when (type) {
            BackgroundType.COLOR -> colorSelected(model as BackgroundModel.ColorModel)
            BackgroundType.IMAGE -> imageSelected(model as BackgroundModel.ImageModel)
            BackgroundType.VIDEO -> videoSelected(model as BackgroundModel.VideoModel)
        }
    }
} 