package com.saidim.clockface.background

import android.app.Application
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.saidim.clockface.background.color.GradientColorSettings
import com.saidim.clockface.background.model.BackgroundModel
import com.saidim.clockface.background.unsplash.UnsplashPhotoDto
import com.saidim.clockface.background.unsplash.UnsplashRepository
import com.saidim.clockface.background.video.PexelsVideo
import com.saidim.clockface.background.video.PexelsVideoRepository
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

    var colorModel = BackgroundModel.ColorModel()
    var imageModel = BackgroundModel.ImageModel()
    var videoModel = BackgroundModel.VideoModel()

    var colorSelected: (BackgroundModel.ColorModel) -> Unit = {}
    var imageSelected: (BackgroundModel.ImageModel) -> Unit = {}
    var videoSelected: (BackgroundModel.VideoModel) -> Unit = {}

    private val _backgroundType = MutableStateFlow(BackgroundType.COLOR)
    val backgroundType: StateFlow<BackgroundType> = _backgroundType

    private val _videos = MutableStateFlow<List<PexelsVideo>>(emptyList())
    val videos: StateFlow<List<PexelsVideo>> = _videos

    var hasLoadedVideos = false

    private val _searchResults = MutableStateFlow<List<UnsplashPhotoDto>>(emptyList())
    val searchResults: StateFlow<List<UnsplashPhotoDto>> = _searchResults

    private val _previewGradient = MutableStateFlow<FluidGradientDrawable?>(null)
    val previewGradient: StateFlow<FluidGradientDrawable?> = _previewGradient

    private val _isGradientEnabled = MutableStateFlow(false)
    val isGradientEnabled: StateFlow<Boolean> = _isGradientEnabled

    init {
        viewModelScope.launch {
            val backgroundModel = appSettings.backgroundModel.first()
            // Load saved background type and model
            val gson = Gson()
            _backgroundType.value = appSettings.backgroundType.first().apply {
                when (this) {
                    BackgroundType.COLOR -> colorModel = (backgroundModel as BackgroundModel.ColorModel).apply { colorSelected(this) }
                    BackgroundType.IMAGE -> imageModel = (backgroundModel as BackgroundModel.ImageModel).apply { imageSelected(this) }
                    BackgroundType.VIDEO -> videoModel = (backgroundModel as BackgroundModel.VideoModel).apply { videoSelected(this) }
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
            colorModel.color = color
            updateColorBackground()
            updateBackgroundModel(colorModel)
            colorSelected(colorModel)
        }
    }

    fun selectImage(newImage: BackgroundModel.ImageModel) {
        viewModelScope.launch {
            imageModel.imageUrl = newImage.imageUrl
            updateBackgroundModel(imageModel)
            imageSelected(imageModel)
        }
    }

    fun selectVideo(video: PexelsVideo) {
        viewModelScope.launch {
            videoModel.pixelVideo = video
            // Set the URL from the best video file available
            video.getBestVideoFile()?.let { videoFile ->
                videoModel.url = videoFile.link
            }
            updateBackgroundModel(videoModel)
            videoSelected(videoModel)
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
        val type = if (model is BackgroundModel.ColorModel) BackgroundType.COLOR
        else if (model is BackgroundModel.ImageModel) BackgroundType.IMAGE else BackgroundType.VIDEO
        viewModelScope.launch {
            appSettings.updateBackgroundType(type.ordinal)
            appSettings.updateBackgroundModel(model)
        }
    }
} 