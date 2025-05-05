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
import com.saidim.clockface.background.unsplash.UnsplashTopicDto
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
    val activeBackgroundModel: BackgroundModel = colorModel, // Add active model, default to color
    val videos: List<PexelsVideo> = emptyList(),
    val topics: List<UnsplashTopicDto> = emptyList(),
    val unsplashPhotos: Map<String, List<UnsplashPhotoDto>> = emptyMap(), // Key is now Topic ID/Slug
    val videoSearchQuery: String = "",
    val isVideoLoading: Boolean = false,
    val isImageLoading: Map<String, Boolean> = emptyMap(), // Key is now Topic ID/Slug
    val isTopicListLoading: Boolean = false,
    val hasLoadedInitialVideos: Boolean = false
)

// 2. Define UI Events
sealed interface BackgroundSettingsEvent {
    data class SelectBackgroundType(val type: BackgroundType) : BackgroundSettingsEvent
    data class SelectColor(val color: Int) : BackgroundSettingsEvent
    data object LoadTopics : BackgroundSettingsEvent
    data class LoadTopicPhotos(val topicIdOrSlug: String) : BackgroundSettingsEvent
    data class SelectImage(val topicIdOrSlug: String, val photo: UnsplashPhotoDto) : BackgroundSettingsEvent
    data class SelectVideo(val video: PexelsVideo) : BackgroundSettingsEvent
    data class SearchVideos(val query: String) : BackgroundSettingsEvent
    data class UpdateVideoSearchQuery(val query: String) : BackgroundSettingsEvent
    data class ToggleGradient(val enabled: Boolean) : BackgroundSettingsEvent
    // Add other events as needed (e.g., for gradient direction)
}

class BackgroundSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val appSettings = AppSettings.instance
    private val pexelsRepository = PexelsVideoRepository()
    private val unsplashRepository = UnsplashRepository()

    private val _uiState = MutableStateFlow(BackgroundSettingsUiState())
    val uiState: StateFlow<BackgroundSettingsUiState> = _uiState.asStateFlow()

    // Keep track of which topic IDs/slugs have had photos loaded
    private val loadedTopicPhotoIds = mutableSetOf<String>()

    init {
        loadInitialState()
        handleEvent(BackgroundSettingsEvent.LoadTopics)
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val initialBackgroundType = appSettings.backgroundType.first()
            val initialBackgroundModel = appSettings.backgroundModel.first()

            _uiState.update { currentState ->
                currentState.copy(
                    selectedBackgroundType = initialBackgroundType,
                    colorModel = if (initialBackgroundModel is BackgroundModel.ColorModel) initialBackgroundModel else BackgroundModel.ColorModel(),
                    imageModel = if (initialBackgroundModel is BackgroundModel.ImageModel) initialBackgroundModel else BackgroundModel.ImageModel(),
                    videoModel = if (initialBackgroundModel is BackgroundModel.VideoModel) initialBackgroundModel else BackgroundModel.VideoModel(),
                    activeBackgroundModel = initialBackgroundModel
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
                    // Do NOT update activeBackgroundModel here
                    // Add similar logic for IMAGE type if needed
                }
                is BackgroundSettingsEvent.SelectColor -> {
                    val newColorModel = _uiState.value.colorModel.copy(color = event.color)
                    appSettings.updateBackgroundModel(newColorModel)
                    // Update both the specific model AND the active model
                    _uiState.update { it.copy(colorModel = newColorModel, activeBackgroundModel = newColorModel) }
                }
                is BackgroundSettingsEvent.LoadTopics -> {
                    if (_uiState.value.isTopicListLoading || _uiState.value.topics.isNotEmpty()) return@launch
                    _uiState.update { it.copy(isTopicListLoading = true) }
                    try {
                        val topics = unsplashRepository.listTopics(perPage = 20)
                        _uiState.update {
                            it.copy(topics = topics, isTopicListLoading = false)
                        }
                    } catch (e: Exception) {
                        Log.e("BackgroundSettingsVM", "Error loading Unsplash topics", e)
                        _uiState.update { it.copy(isTopicListLoading = false) }
                        // TODO: Show error message to user
                    }
                }
                is BackgroundSettingsEvent.LoadTopicPhotos -> {
                    val topicId = event.topicIdOrSlug
                    if (topicId in loadedTopicPhotoIds || _uiState.value.isImageLoading[topicId] == true) return@launch

                    _uiState.update { it.copy(isImageLoading = it.isImageLoading + (topicId to true)) }
                    loadedTopicPhotoIds.add(topicId)

                    try {
                        val photos = unsplashRepository.getTopicPhotos(topicId)
                        _uiState.update {
                            it.copy(
                                unsplashPhotos = it.unsplashPhotos + (topicId to photos),
                                isImageLoading = it.isImageLoading + (topicId to false)
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("BackgroundSettingsVM", "Error loading photos for topic $topicId", e)
                        _uiState.update { it.copy(isImageLoading = it.isImageLoading + (topicId to false)) }
                        loadedTopicPhotoIds.remove(topicId) // Allow retry on error
                        // TODO: Show error message to user
                    }
                }
                is BackgroundSettingsEvent.SelectImage -> {
                    val newImageModel = _uiState.value.imageModel.copy(
                        imageUrl = event.photo.urls.regular,
                        blurHash = event.photo.blurHash,
                        topicId = event.topicIdOrSlug
                    )
                    _uiState.update { it.copy(
                        imageModel = newImageModel,
                        activeBackgroundModel = newImageModel
                    ) }
                    saveCurrentBackgroundModel(newImageModel)
                }
                is BackgroundSettingsEvent.SelectVideo -> {
                    val newVideoModel = _uiState.value.videoModel.copy(
                        pixelVideo = event.video,
                        url = event.video.getBestVideoFile()?.link ?: ""
                    )
                    appSettings.updateBackgroundModel(newVideoModel)
                    // Update both the specific model AND the active model
                    _uiState.update { it.copy(videoModel = newVideoModel, activeBackgroundModel = newVideoModel) }
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
                is BackgroundSettingsEvent.ToggleGradient -> {
                    val newColorModel = _uiState.value.colorModel.copy(enableFluidColor = event.enabled)
                    appSettings.updateBackgroundModel(newColorModel)
                    // Also update active model if the current active model IS the color model
                    _uiState.update {
                        val updatedActiveModel = if (it.activeBackgroundModel is BackgroundModel.ColorModel) {
                            newColorModel
                        } else {
                            it.activeBackgroundModel
                        }
                        it.copy(colorModel = newColorModel, activeBackgroundModel = updatedActiveModel)
                    }
                }
            }
        }
    }

    private fun saveCurrentBackgroundModel(model: BackgroundModel) {
        // ... (existing save logic) ...
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