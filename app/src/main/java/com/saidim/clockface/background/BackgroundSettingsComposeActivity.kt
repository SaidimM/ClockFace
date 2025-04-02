package com.saidim.clockface.background

import LogUtils
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.saidim.clockface.background.color.GradientColorSettings
import com.saidim.clockface.background.model.BackgroundModel
import com.saidim.clockface.background.unsplash.UnsplashPhotoDto
import com.saidim.clockface.background.unsplash.UnsplashTopics
import com.saidim.clockface.background.video.PexelsVideo
import kotlinx.coroutines.launch

class BackgroundSettingsComposeActivity : ComponentActivity() {
    private val viewModel: BackgroundSettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            BackgroundSettingsScreen(
                viewModel = viewModel,
                onNavigateUp = { finish() }
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // Stop gradient animations
        viewModel.previewGradient.value?.let { gradient ->
            gradient.updateSettings(gradient.settings.copy(isAnimated = false))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BackgroundSettingsScreen(
    viewModel: BackgroundSettingsViewModel,
    onNavigateUp: () -> Unit
) {
    val backgroundType by viewModel.backgroundType.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Background Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Background Type Selection (SegmentedButton equivalent)
            BackgroundTypeSelector(
                selectedType = backgroundType,
                onTypeSelected = { viewModel.setBackgroundType(it) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Background Preview
            BackgroundPreview(
                backgroundType = backgroundType,
                viewModel = viewModel
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Background Settings based on type
            when (backgroundType) {
                BackgroundType.COLOR -> ColorSettings(
                    viewModel = viewModel,
                    onColorSelected = { viewModel.selectColor(it) }
                )
                
                BackgroundType.IMAGE -> ImageSettings(
                    viewModel = viewModel,
                    onImageSelected = { viewModel.selectImage(it) }
                )
                
                BackgroundType.VIDEO -> VideoSettings(
                    videos = videos,
                    onVideoSearch = { viewModel.loadPexelsVideos(it) },
                    onVideoSelected = { viewModel.selectVideo(it) },
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun BackgroundTypeSelector(
    selectedType: BackgroundType,
    onTypeSelected: (BackgroundType) -> Unit
) {
    val types = BackgroundType.values()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        types.forEach { type ->
            val isSelected = type == selectedType
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTypeSelected(type) }
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (type) {
                        BackgroundType.COLOR -> "Color"
                        BackgroundType.IMAGE -> "Image"
                        BackgroundType.VIDEO -> "Video"
                    },
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BackgroundPreview(
    backgroundType: BackgroundType,
    viewModel: BackgroundSettingsViewModel
) {
    var colorModel by remember { mutableStateOf<BackgroundModel.ColorModel?>(null) }
    var imageModel by remember { mutableStateOf<BackgroundModel.ImageModel?>(null) }
    var videoModel by remember { mutableStateOf<BackgroundModel.VideoModel?>(null) }
    
    // Set up callbacks to receive selected backgrounds
    LaunchedEffect(Unit) {
        viewModel.colorSelected = { colorModel = it }
        viewModel.imageSelected = { imageModel = it }
        viewModel.videoSelected = { videoModel = it }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (backgroundType) {
                BackgroundType.COLOR -> {
                    colorModel?.let { model ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(model.color))
                        )
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
                
                BackgroundType.IMAGE -> {
                    imageModel?.let { model ->
                        if (model.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = model.imageUrl,
                                contentDescription = "Background Image",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No image selected", color = Color.White)
                            }
                        }
                    }
                }
                
                BackgroundType.VIDEO -> {
                    // Video preview is trickier in Compose
                    // In a real implementation, you might use AndroidView with VideoView
                    // or ExoPlayer's PlayerView
                    videoModel?.let { model ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (model.url.isNotEmpty()) 
                                    "Video selected: ${model.pixelVideo.id}" 
                                else 
                                    "No video selected",
                                color = Color.White
                            )
                        }
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No video selected", color = Color.White)
                    }
                }
            }
            
            // Loading indicator if needed
            if (false) { // Replace with actual loading state
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun ColorSettings(
    viewModel: BackgroundSettingsViewModel,
    onColorSelected: (Int) -> Unit
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
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select a Color",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(colors) { color ->
                    val isSelected = viewModel.colorModel.color == color
                    
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Gradient Animation",
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
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageSettings(
    viewModel: BackgroundSettingsViewModel,
    onImageSelected: (BackgroundModel.ImageModel) -> Unit
) {
    val topics = UnsplashTopics.topics
    val pagerState = rememberPagerState(pageCount = { topics.size })
    val coroutineScope = rememberCoroutineScope()
    val currentTopic = topics[pagerState.currentPage]
    
    val searchResults = remember { mutableStateOf<List<UnsplashPhotoDto>>(emptyList()) }
    
    // Fetch photos when the tab changes
    LaunchedEffect(currentTopic) {
        viewModel.searchPhotosByTopic(currentTopic).collect { results ->
            searchResults.value = results
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select an Image",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // TabLayout equivalent
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 0.dp
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
            
            // ViewPager equivalent
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) { page ->
                if (searchResults.value.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchResults.value) { photo ->
                            AsyncImage(
                                model = photo.urls.small,
                                contentDescription = "Photo by ${photo.user.name}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onImageSelected(
                                            BackgroundModel.ImageModel(
                                                imageUrl = photo.urls.regular
                                            )
                                        )
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSettings(
    videos: List<PexelsVideo>,
    onVideoSearch: (String) -> Unit,
    onVideoSelected: (PexelsVideo) -> Unit,
    viewModel: BackgroundSettingsViewModel
) {
    val searchQuery = remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select a Video",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search field
            OutlinedTextField(
                value = searchQuery.value,
                onValueChange = { searchQuery.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search videos...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onVideoSearch(searchQuery.value)
                        focusManager.clearFocus()
                    }
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (videos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(videos) { video ->
                        // Video thumbnail
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f/9f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.DarkGray)
                                .clickable { onVideoSelected(video) },
                            contentAlignment = Alignment.Center
                        ) {
                            // You would use AsyncImage with video.image here in a real implementation
                            // For now, we'll just display the video ID
                            Text(
                                text = "Video ${video.id}",
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            // Load default videos if none loaded
            LaunchedEffect(Unit) {
                if (!viewModel.hasLoadedVideos) {
                    onVideoSearch("popular")
                }
            }
        }
    }
} 