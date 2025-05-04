package com.saidim.clockface.background

import LogUtils
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.saidim.clockface.R
import com.saidim.clockface.background.model.BackgroundModel
import com.saidim.clockface.background.unsplash.UnsplashPhotoDto
import com.saidim.clockface.background.unsplash.UnsplashTopics
import com.saidim.clockface.background.video.PexelsVideo
import com.saidim.clockface.ui.theme.ClockFaceTheme
import kotlinx.coroutines.launch

// --- Activity --- //
@OptIn(ExperimentalFoundationApi::class)
class ComposeBackgroundSettingsActivity : ComponentActivity() {
    private val viewModel: BackgroundSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            ClockFaceTheme {
                // Collect state and pass it down with the event handler
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                BackgroundSettingsScreen(
                    uiState = uiState,
                    onEvent = viewModel::handleEvent,
                    onNavigateBack = { finish() }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Note: Video pausing and gradient animations are handled in the Composable
        // We no longer need to explicitly save settings as they're saved when changed
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup is handled by Compose's lifecycle
    }
}

// --- Screen Composable (Stateless) --- //
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BackgroundSettingsScreen(
    uiState: BackgroundSettingsUiState,
    onEvent: (BackgroundSettingsEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Local state for Video Player (needed for AndroidView interaction)
    var isVideoPlaying by remember { mutableStateOf(false) }
    // Derived state for pager
    val imagePagerState = rememberPagerState(initialPage = 0) { UnsplashTopics.topics.size }
    // Scroll behavior for collapsing TopAppBar
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    // Define expanded and collapsed heights for the preview area
    // These might need adjustment based on your desired appearance
    val expandedHeight = 200.dp
    val collapsedHeight = TopAppBarDefaults.LargeAppBarCollapsedHeight // Standard collapsed height

    // Automatically load Unsplash topic when the pager page changes
    LaunchedEffect(imagePagerState.currentPage) {
        val currentTopic = UnsplashTopics.topics[imagePagerState.currentPage]
        // Trigger load only if not already loading/loaded
        if (uiState.unsplashPhotos[currentTopic] == null && uiState.isImageLoading[currentTopic] != true) {
            onEvent(BackgroundSettingsEvent.LoadUnsplashTopic(currentTopic))
        }
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {

        // Calculate current height based on scroll offset
        val currentTopAppBarHeight = lerp(expandedHeight, collapsedHeight, scrollBehavior.state.collapsedFraction)

        // Background Preview (drawn first, behind the Scaffold)
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(currentTopAppBarHeight) // Dynamic height
            // Add graphicsLayer transformations here later (alpha, scale, translationY)
        ) {
            BackgroundPreview(
                uiState = uiState,
                isVideoPlaying = isVideoPlaying
            ) { isPlaying ->
                isVideoPlaying = isPlaying // Update local state based on player callback
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.background_settings)) }, // Just the title now
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Use AutoMirrored
                                contentDescription = stringResource(R.string.cd_back) // Use string resource
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior, // Apply scroll behavior
                    // Make AppBar background transparent initially, fade in later if needed
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface // Or your desired collapsed color
                    )
                )
            },
            // Make Scaffold background transparent so Box behind is visible
            containerColor = Color.Transparent
        ) { paddingValues ->
            // Adjust padding for the content list
            val topPadding = paddingValues.calculateTopPadding()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPadding) // Use calculated padding
                    .padding(bottom = paddingValues.calculateBottomPadding())
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Background Type Selection Card
                    item {
                        BackgroundTypeSelector(selectedType = uiState.selectedBackgroundType) {
                            onEvent(BackgroundSettingsEvent.SelectBackgroundType(it))
                        }
                    }
                    // Content cards with weight
                    item {
                        // This will make the content expand to fill available space
                        Box(modifier = Modifier.fillParentMaxHeight()) {
                            when (uiState.selectedBackgroundType) {
                                BackgroundType.COLOR -> {
                                    ColorSettingsCard(
                                        colorModel = uiState.colorModel,
                                        onColorSelected = { color -> onEvent(BackgroundSettingsEvent.SelectColor(color)) },
                                        onGradientToggled = { enabled -> onEvent(BackgroundSettingsEvent.ToggleGradient(enabled)) }
                                    )
                                }
                                BackgroundType.IMAGE -> {
                                    ImageSettingsCard(
                                        topics = UnsplashTopics.topics,
                                        photosByTopic = uiState.unsplashPhotos,
                                        isLoadingByTopic = uiState.isImageLoading,
                                        pagerState = imagePagerState,
                                        onImageSelected = { topic, photo ->
                                            onEvent(BackgroundSettingsEvent.SelectImage(topic, photo))
                                        },
                                        coroutineScope = coroutineScope
                                    )
                                }
                                BackgroundType.VIDEO -> {
                                    VideoSettingsCard(
                                        videos = uiState.videos,
                                        isLoading = uiState.isVideoLoading,
                                        searchQuery = uiState.videoSearchQuery,
                                        onQueryChanged = { query -> onEvent(BackgroundSettingsEvent.UpdateVideoSearchQuery(query)) },
                                        onSearch = { query -> onEvent(BackgroundSettingsEvent.SearchVideos(query)) },
                                        onVideoSelected = { video -> onEvent(BackgroundSettingsEvent.SelectVideo(video)) },
                                        focusManager = focusManager
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Extracted Composables for better organization --- //

@Composable
fun BackgroundPreview(
    uiState: BackgroundSettingsUiState,
    isVideoPlaying: Boolean,
    onVideoPlaybackStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant) // Placeholder background
    ) {
        when (uiState.selectedBackgroundType) {
            BackgroundType.COLOR -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(color = Color(uiState.colorModel.color))
                )
            }
            BackgroundType.IMAGE -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uiState.imageModel.imageUrl.ifEmpty { R.drawable.placeholder }) // Placeholder if empty
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.cd_background_image_preview),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { PreviewLoadingIndicator() }
                )
            }
            BackgroundType.VIDEO -> {
                Box(Modifier.fillMaxSize()) {
                    val videoUrl = uiState.videoModel.url
                    if (videoUrl.isNotEmpty()) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoURI(Uri.parse(videoUrl))
                                    setOnPreparedListener { mediaPlayer ->
                                        mediaPlayer.isLooping = true
                                        mediaPlayer.setVolume(0f, 0f)
                                        start()
                                        onVideoPlaybackStateChanged(true)
                                    }
                                    setOnErrorListener { _, what, extra ->
                                        LogUtils.d("Video error: what=$what, extra=$extra")
                                        onVideoPlaybackStateChanged(false)
                                        true
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.Center),
                            onRelease = { videoView ->
                                videoView.stopPlayback()
                                onVideoPlaybackStateChanged(false)
                            }
                        )

                        if (!isVideoPlaying) {
                            PreviewLoadingIndicator()
                        }
                    } else {
                        // Show placeholder text if no video URL
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_video_selected))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewLoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun BackgroundTypeSelector(selectedType: BackgroundType, onTypeSelected: (BackgroundType) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.background_type_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            SegmentedButtonRow(selectedType = selectedType, onTypeSelected = onTypeSelected)
        }
    }
}

@Composable
fun SegmentedButtonRow(selectedType: BackgroundType, onTypeSelected: (BackgroundType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val options = BackgroundType.entries.toList()
        options.forEachIndexed { index, type ->
            val label = when (type) {
                BackgroundType.COLOR -> stringResource(R.string.background_type_color)
                BackgroundType.IMAGE -> stringResource(R.string.background_type_image)
                BackgroundType.VIDEO -> stringResource(R.string.background_type_video)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (selectedType == type) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable { onTypeSelected(type) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selectedType == type) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }

            if (index < options.size - 1) {
                Divider(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
fun ColorSettingsCard(
    colorModel: BackgroundModel.ColorModel,
    onColorSelected: (Int) -> Unit,
    onGradientToggled: (Boolean) -> Unit
    // Add onDirectionChanged lambda if needed
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp), // Consistent padding
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.select_colors),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            ColorSwatches(selectedColor = colorModel.color, onColorSelected = onColorSelected)
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            GradientSwitch(enabled = colorModel.enableFluidColor, onToggle = onGradientToggled)
            // Add Gradient Direction controls here if implemented
            AnimatedVisibility(visible = colorModel.enableFluidColor) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.gradient_direction), // Placeholder
                        style = MaterialTheme.typography.bodyMedium
                    )
                    // Gradient direction controls would go here
                }
            }
        }
    }
}

@Composable
fun ColorSwatches(selectedColor: Int, onColorSelected: (Int) -> Unit) {
    // Define colors list outside LazyRow scope but within the Composable
    val colors = remember {
        listOf(
            0xFFE57373, 0xFF81C784, 0xFF64B5F6, 0xFFFFB74D, // Reds, Greens, Blues, Oranges
            0xFF9575CD, 0xFF4DB6AC, 0xFFF06292, 0xFFFFD54F, // Purples, Teals, Pinks, Yellows
            0xFFFFFFFF, 0xFF616161, 0xFF000000             // White, Grey, Black
        ).map { it.toInt() }
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        // Pass the pre-defined list
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(color))
                    .clickable { onColorSelected(color) }
                    .border(
                        width = 3.dp,
                        color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
            )
        }
    }
}

@Composable
fun GradientSwitch(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.enable_gradient),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageSettingsCard(
    topics: List<String>,
    photosByTopic: Map<String, List<UnsplashPhotoDto>>,
    isLoadingByTopic: Map<String, Boolean>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onImageSelected: (String, UnsplashPhotoDto) -> Unit,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) { // Bounds check
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
                        )
                    }
                },
                edgePadding = 0.dp // Remove default padding
            ) {
                topics.forEachIndexed { index, topic ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(topic, style = MaterialTheme.typography.bodyMedium) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f) // Give Pager a fixed height
                    .fillMaxWidth()
            ) { page ->
                val currentTopic = topics[page]
                val photos = photosByTopic[currentTopic]
                val isLoading = isLoadingByTopic[currentTopic] ?: false

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        PreviewLoadingIndicator()
                    } else if (photos != null) {
                        if (photos.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                                Text(stringResource(R.string.no_images_found, currentTopic))
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(photos, key = { it.id }) { photo ->
                                    ImageThumbnail(photo = photo) {
                                        onImageSelected(currentTopic, photo)
                                    }
                                }
                            }
                        }
                    } else {
                        // Placeholder before loading starts, or if error occurred (handled in VM)
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                            Text(stringResource(R.string.loading_topic, currentTopic))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageThumbnail(photo: UnsplashPhotoDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1.5f) // Make thumbnails square
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp) // Slightly rounded corners
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photo.urls.thumb)
                .crossfade(true)
                .build(),
            contentDescription = photo.user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { Box(Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) } // Simple placeholder
        )
    }
}

@Composable
fun VideoSettingsCard(
    videos: List<PexelsVideo>,
    isLoading: Boolean,
    searchQuery: String,
    onQueryChanged: (String) -> Unit,
    onSearch: (String) -> Unit,
    onVideoSelected: (PexelsVideo) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChanged,
                label = { Text(stringResource(R.string.video_source_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onSearch(searchQuery)
                    }
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoading) {
                    PreviewLoadingIndicator()
                } else {
                    if (videos.isEmpty() && searchQuery.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_videos_found, searchQuery))
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(vertical = 8.dp), // Padding for grid itself
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(videos, key = { it.id }) { video ->
                                VideoThumbnail(
                                    video = BackgroundModel.VideoModel(pixelVideo = video),
                                    onClick = { onVideoSelected(video) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoThumbnail(video: BackgroundModel.VideoModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = video.pixelVideo.thumbnail,
                contentDescription = stringResource(R.string.cd_video_thumbnail),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Optional duration overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${video.pixelVideo.duration}s",
                    style = MaterialTheme.typography.labelSmall, // Smaller label
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}