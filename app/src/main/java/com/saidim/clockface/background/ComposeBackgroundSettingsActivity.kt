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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.saidim.clockface.R
import com.saidim.clockface.background.model.BackgroundModel
import com.saidim.clockface.background.unsplash.UnsplashTopics
import com.saidim.clockface.background.video.PexelsVideo
import com.saidim.clockface.ui.theme.ClockFaceTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
class ComposeBackgroundSettingsActivity : ComponentActivity() {
    private val viewModel: BackgroundSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        setContent {
            ClockFaceTheme {
                BackgroundSettingsScreen(viewModel = viewModel, onNavigateBack = { finish() })
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundSettingsScreen(
    viewModel: BackgroundSettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val backgroundType by viewModel.backgroundType.collectAsStateWithLifecycle(BackgroundType.COLOR)
    val videos by viewModel.videos.collectAsStateWithLifecycle(emptyList())
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    
    var isVideoPlaying by remember { mutableStateOf(false) }
    
    // Load popular videos by default when video type is selected
    LaunchedEffect(backgroundType) {
        if (backgroundType == BackgroundType.VIDEO && !viewModel.hasLoadedVideos) {
            viewModel.loadPexelsVideos("popular")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.background_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Preview Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    when (backgroundType) {
                        BackgroundType.COLOR -> {
                            val colorModel = viewModel.colorModel
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = Color(colorModel.color),
                                    )
                            )
                        }
                        BackgroundType.IMAGE -> {
                            val imageModel = viewModel.imageModel
                            
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageModel.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Background Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                loading = {
                                    Box(Modifier.fillMaxSize()) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            )
                        }
                        BackgroundType.VIDEO -> {
                            val videoModel = viewModel.videoModel
                            
                            Box(Modifier.fillMaxSize()) {
                                videoModel.pixelVideo.getBestVideoFile()?.let { videoFile ->
                                    // Update the url property in the VideoModel for persistence
                                    videoModel.url = videoFile.link
                                    
                                    AndroidView(
                                        factory = { ctx ->
                                            VideoView(ctx).apply {
                                                setVideoURI(Uri.parse(videoFile.link))
                                                setOnPreparedListener { mediaPlayer ->
                                                    mediaPlayer.isLooping = true
                                                    mediaPlayer.setVolume(0f, 0f)
                                                    start()
                                                    isVideoPlaying = true
                                                }
                                                setOnErrorListener { _, what, extra ->
                                                    LogUtils.d("Video error: what=$what, extra=$extra")
                                                    isVideoPlaying = false
                                                    true
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .align(Alignment.Center),
                                        onRelease = { videoView ->
                                            videoView.stopPlayback()
                                        }
                                    )
                                    
                                    if (!isVideoPlaying) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                } ?: run {
                                    // Show placeholder if no video file
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Text(
                                            "No video selected",
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Background Type Selection
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
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
                        
                        // Segmented button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            val options = listOf(
                                BackgroundType.COLOR to stringResource(R.string.background_type_color),
                                BackgroundType.IMAGE to stringResource(R.string.background_type_image),
                                BackgroundType.VIDEO to stringResource(R.string.background_type_video)
                            )
                            
                            options.forEachIndexed { index, (type, label) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(
                                            if (backgroundType == type) 
                                                MaterialTheme.colorScheme.primary
                                            else 
                                                MaterialTheme.colorScheme.surface
                                        )
                                        .clickable { 
                                            viewModel.setBackgroundType(type)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (backgroundType == type)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                if (index < options.size - 1) {
                                    Divider(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .fillMaxHeight()
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Color Source Card
            item {
                AnimatedVisibility(
                    visible = backgroundType == BackgroundType.COLOR,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
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
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Color swatches
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                val colors = listOf(
                                    0xFFE57373.toInt(), // Red
                                    0xFF81C784.toInt(), // Green
                                    0xFF64B5F6.toInt(), // Blue
                                    0xFFFFB74D.toInt(), // Orange
                                    0xFF9575CD.toInt(), // Purple
                                    0xFF4DB6AC.toInt(), // Teal
                                    0xFFF06292.toInt(), // Pink
                                    0xFFFFD54F.toInt()  // Yellow
                                )
                                
                                items(colors) { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color(color))
                                            .clickable {
                                                viewModel.selectColor(color)
                                            }
                                            .border(
                                                width = 2.dp,
                                                color = if (viewModel.colorModel.color == color)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    Color.Transparent,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 16.dp))
                            
                            // Gradient switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.enable_gradient),
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Switch(
                                    checked = viewModel.colorModel.enableFluidColor,
                                    onCheckedChange = { isChecked ->
                                        viewModel.colorModel.enableFluidColor = isChecked
                                        viewModel.updateBackgroundModel(viewModel.colorModel)
                                    }
                                )
                            }
                            
                            AnimatedVisibility(visible = viewModel.colorModel.enableFluidColor) {
                                Column(
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.gradient_direction),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    
                                    // Gradient direction controls would go here
                                }
                            }
                        }
                    }
                }
            }
            
            // Video Source Card
            item {
                AnimatedVisibility(
                    visible = backgroundType == BackgroundType.VIDEO,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            var searchQuery by remember { mutableStateOf("") }
                            var isLoading by remember { mutableStateOf(false) }
                            
                            // Search field
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text(stringResource(R.string.video_source_title)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onSearch = {
                                        isLoading = true
                                        focusManager.clearFocus()
                                        viewModel.loadPexelsVideos(searchQuery)
                                    }
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Loading indicator or video grid
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp) // Fixed height for the grid container
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .align(Alignment.Center)
                                    )
                                }
                                
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize() // Use fillMaxSize inside the Box with fixed height
                                ) {
                                    items(videos) { video ->
                                        VideoThumbnail(
                                            video = BackgroundModel.VideoModel(pixelVideo = video),
                                            onClick = {
                                                viewModel.selectVideo(video)
                                            }
                                        )
                                    }
                                }
                                
                                LaunchedEffect(videos) {
                                    isLoading = false
                                }
                            }
                        }
                    }
                }
            }
            
            // Image Source Card
            item {
                AnimatedVisibility(
                    visible = backgroundType == BackgroundType.IMAGE,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp) // Fixed height for the column containing pager
                        ) {
                            val topics = UnsplashTopics.topics
                            val pageCount = topics.size
                            val pagerState = rememberPagerState(initialPage = 0) { pageCount }
                            
                            ScrollableTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                indicator = { tabPositions ->
                                    TabRowDefaults.Indicator(
                                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                    )
                                }
                            ) {
                                topics.forEachIndexed { index, topic ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        },
                                        text = { Text(topic) }
                                    )
                                }
                            }
                            
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.weight(1f) // Use weight instead of fixed height
                            ) { page ->
                                // UnsplashImagesGrid would go here - simplified for now
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Unsplash images for topic: ${topics[page]}")
                                    // In a full implementation, we would load images from Unsplash
                                    // and display them in a grid
                                }
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
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = video.pixelVideo.thumbnail,
                contentDescription = "Video thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Optional duration or other info overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${video.pixelVideo.duration}s",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun Modifier.border(width: androidx.compose.ui.unit.Dp, color: Color, shape: androidx.compose.ui.graphics.Shape) = 
    this.then(Modifier.drawBehind {
        drawIntoCanvas { canvas ->
            val paint = androidx.compose.ui.graphics.Paint().apply {
                this.color = color
                this.isAntiAlias = true
                this.style = androidx.compose.ui.graphics.PaintingStyle.Stroke
                this.strokeWidth = width.toPx()
            }
            canvas.drawPath(
                androidx.compose.ui.graphics.Path().apply {
                    addOutline(shape.createOutline(size, layoutDirection, this@drawBehind))
                },
                paint
            )
        }
    }) 