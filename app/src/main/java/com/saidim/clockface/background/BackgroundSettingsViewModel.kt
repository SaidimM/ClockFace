package com.saidim.clockface.background

import android.app.Application
import android.graphics.Color
import android.util.Log
import androidx.compose.ui.graphics.toArgb
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// 1. Define UI State
data class BackgroundSettingsUiState(
    val selectedBackgroundType: BackgroundType = BackgroundType.COLOR,
    val colorModel: BackgroundModel.ColorModel = BackgroundModel.ColorModel(),
    val imageModel: BackgroundModel.ImageModel = BackgroundModel.ImageModel(),
    val videoModel: BackgroundModel.VideoModel = BackgroundModel.VideoModel(),
    val videos: List<PexelsVideo> = emptyList(),
    val unsplashPhotos: Map<String, List<UnsplashPhotoDto>> = emptyMap(), // Map of topic to photos
    val videoSearchQuery: String = "",
    val isVideoLoading: Boolean = false,
    val isImageLoading: Map<String, Boolean> = emptyMap(), // Map of topic to loading state
    val hasLoadedInitialVideos: Boolean = false
)

// 2. Define UI Events
sealed interface BackgroundSettingsEvent {
    data class SelectBackgroundType(val type: BackgroundType) : BackgroundSettingsEvent
    data class SelectColor(val color: Int) : BackgroundSettingsEvent
    data class SelectImage(val topic: String, val photo: UnsplashPhotoDto) : BackgroundSettingsEvent
    data class SelectVideo(val video: PexelsVideo) : BackgroundSettingsEvent
    data class SearchVideos(val query: String) : BackgroundSettingsEvent
    data class UpdateVideoSearchQuery(val query: String) : BackgroundSettingsEvent
    data class LoadUnsplashTopic(val topic: String) : BackgroundSettingsEvent
    data class ToggleGradient(val enabled: Boolean) : BackgroundSettingsEvent
    // Add other events as needed (e.g., for gradient direction)
}

class BackgroundSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings.instance
    private val pexelsRepository = PexelsVideoRepository()
    private val unsplashRepository = UnsplashRepository()

    private val _uiState = MutableStateFlow(BackgroundSettingsUiState())
    val uiState: StateFlow<BackgroundSettingsUiState> = _uiState.asStateFlow()

    // Keep track of which topics have been loaded
    private val loadedUnsplashTopics = mutableSetOf<String>()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val initialBackgroundType = appSettings.backgroundType.first()
            val initialBackgroundModel = appSettings.backgroundModel.first()

            _uiState.update { currentState ->
                currentState.copy(
                    selectedBackgroundType = initialBackgroundType,
                    colorModel = if (initialBackgroundModel is BackgroundModel.ColorModel) initialBackgroundModel else currentState.colorModel,
                    imageModel = if (initialBackgroundModel is BackgroundModel.ImageModel) initialBackgroundModel else currentState.imageModel,
                    videoModel = if (initialBackgroundModel is BackgroundModel.VideoModel) initialBackgroundModel else currentState.videoModel
                )
            }
            // Auto-load popular videos if video type is initially selected
            if (initialBackgroundType == BackgroundType.VIDEO && !_uiState.value.hasLoadedInitialVideos) {
                handleEvent(BackgroundSettingsEvent.SearchVideos("popular"))
            }
        }
    }

    // 3. Handle Events
    fun handleEvent(event: BackgroundSettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is BackgroundSettingsEvent.SelectBackgroundType -> {
                    appSettings.updateBackgroundType(event.type.ordinal)
                    _uiState.update { it.copy(selectedBackgroundType = event.type) }
                    // Load initial data if switching to a type for the first time
                    if (event.type == BackgroundType.VIDEO && !_uiState.value.hasLoadedInitialVideos) {
                        handleEvent(BackgroundSettingsEvent.SearchVideos("popular"))
                    }
                    // Add similar logic for IMAGE type if needed
                }
                is BackgroundSettingsEvent.SelectColor -> {
                    val newColorModel = _uiState.value.colorModel.copy(color = event.color)
                    appSettings.updateBackgroundModel(newColorModel)
                    _uiState.update { it.copy(colorModel = newColorModel) }
                }
                is BackgroundSettingsEvent.SelectImage -> {
                     val newImageModel = _uiState.value.imageModel.copy(imageUrl = event.photo.urls.regular)
                    appSettings.updateBackgroundModel(newImageModel)
                    _uiState.update { it.copy(imageModel = newImageModel) }
                }
                is BackgroundSettingsEvent.SelectVideo -> {
                    val newVideoModel = _uiState.value.videoModel.copy(
                        pixelVideo = event.video,
                        url = event.video.getBestVideoFile()?.link ?: ""
                    )
                    appSettings.updateBackgroundModel(newVideoModel)
                    _uiState.update { it.copy(videoModel = newVideoModel) }
                }
                 is BackgroundSettingsEvent.SearchVideos -> {
                     if (event.query.isBlank()) return@launch
                     _uiState.update { it.copy(isVideoLoading = true, videoSearchQuery = event.query) }
                     try {
                         val videos = pexelsRepository.searchVideos(event.query)
                         _uiState.update {
                             it.copy(
                                 videos = videos,
                                 isVideoLoading = false,
                                 hasLoadedInitialVideos = it.hasLoadedInitialVideos || event.query == "popular" // Mark initial load done
                             )
                         }
                     } catch (e: Exception) {
                         Log.e("BackgroundSettingsVM", "Error searching videos", e)
                         _uiState.update { it.copy(isVideoLoading = false) }
                         // TODO: Show error message to user
                     }
                 }
                is BackgroundSettingsEvent.UpdateVideoSearchQuery -> {
                    _uiState.update { it.copy(videoSearchQuery = event.query) }
                }
                is BackgroundSettingsEvent.LoadUnsplashTopic -> {
                    if (event.topic in loadedUnsplashTopics || _uiState.value.isImageLoading[event.topic] == true) return@launch

                    _uiState.update { it.copy(isImageLoading = it.isImageLoading + (event.topic to true)) }
                    loadedUnsplashTopics.add(event.topic) // Mark as loading/loaded

                     try {
                         val photos = unsplashRepository.searchPhotos(event.topic).results
                         _uiState.update {
                             it.copy(
                                 unsplashPhotos = it.unsplashPhotos + (event.topic to photos),
                                 isImageLoading = it.isImageLoading + (event.topic to false)
                             )
                         }
                     } catch (e: Exception) {
                         Log.e("BackgroundSettingsVM", "Error loading Unsplash topic ${event.topic}", e)
                         _uiState.update { it.copy(isImageLoading = it.isImageLoading + (event.topic to false)) }
                          loadedUnsplashTopics.remove(event.topic) // Allow retry on error
                         // TODO: Show error message to user
                     }
                 }
                is BackgroundSettingsEvent.ToggleGradient -> {
                     val newColorModel = _uiState.value.colorModel.copy(enableFluidColor = event.enabled)
                    appSettings.updateBackgroundModel(newColorModel)
                    _uiState.update { it.copy(colorModel = newColorModel) }
                }
            }
        }
    }

    fun setBackgroundType(type: BackgroundType) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedBackgroundType = type) }
            appSettings.updateBackgroundType(type.ordinal)
        }
    }

    fun loadPexelsVideos(topic: String) {
        viewModelScope.launch {
            try {
                val videos = pexelsRepository.searchVideos(topic)
                val videoIds = videos.joinToString(", ") { it.id.toString() }
                Log.d(this.javaClass.simpleName, "query: $topic PexelsVideo IDs: $videoIds")
                _uiState.update { it.copy(videos = videos) }
            } catch (e: Exception) {
                // Handle error
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