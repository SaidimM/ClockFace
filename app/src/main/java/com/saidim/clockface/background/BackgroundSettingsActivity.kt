package com.saidim.clockface.background

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.saidim.clockface.R
import com.saidim.clockface.background.video.VideoAdapter
import com.saidim.clockface.base.BaseActivity
import com.saidim.clockface.databinding.ActivityBackgroundSettingsBinding
import kotlinx.coroutines.launch
import android.view.View
import coil.load
import com.saidim.clockface.utils.getBestVideoFile
import android.util.Log
import com.saidim.clockface.background.video.pexels.Video
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.saidim.clockface.background.unsplash.UnsplashCollection

class BackgroundSettingsActivity : BaseActivity() {
    private val binding by lazy { ActivityBackgroundSettingsBinding.inflate(layoutInflater) }
    private val viewModel: BackgroundSettingsViewModel by viewModels()

    private val pickImages = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.clipData?.let { clipData ->
                val imageUris = mutableListOf<Uri>()
                for (i in 0 until clipData.itemCount) {
                    imageUris.add(clipData.getItemAt(i).uri)
                }
                viewModel.addImages(imageUris)
            } ?: result.data?.data?.let { uri ->
                viewModel.addImages(listOf(uri))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()
        setupBackgroundTypeSelection()
        setupImageSourceControls()
        setupVideoControls()
        setupSlideshowControls()
        observeViewModel()
        observePreview()
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener { finish() }
    }

    private fun setupBackgroundTypeSelection() {
        binding.backgroundTypeSegmentedButton.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val type = when (checkedId) {
                    R.id.noneButton -> BackgroundType.NONE
                    R.id.imageButton -> BackgroundType.IMAGE
                    R.id.videoButton -> BackgroundType.VIDEO
                    else -> BackgroundType.NONE
                }
                viewModel.setBackgroundType(type)
            }
        }
    }

    private fun setupImageSourceControls() {
        lifecycleScope.launch {
            viewModel.collections.collect { collections ->
                if (collections.isNotEmpty()) {
                    setupCollectionTabs(collections)
                }
            }
        }
    }

    private fun setupCollectionTabs(collections: List<UnsplashCollection>) {
        // Clear existing tabs
        binding.tabLayout.removeAllTabs()
        
        // Add tab for each collection
        collections.forEach { collection ->
            binding.tabLayout.addTab(
                binding.tabLayout.newTab().apply {
                    text = collection.title
                    tag = collection.id
                }
            )
        }

        // Setup ViewPager
        val pagerAdapter = UnsplashCollectionPagerAdapter(
            collections = collections,
            fragmentActivity = this
        )
        binding.viewPager.apply {
            adapter = pagerAdapter
            // Disable swiping to prevent accidental switches
            isUserInputEnabled = false
        }

        // Connect TabLayout with ViewPager
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.viewPager.currentItem = tab.position
                // Load photos from selected collection
                val collectionId = tab.tag as String
                viewModel.loadCollectionPhotos(collectionId)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Select first collection by default
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
    }

    private fun setupVideoControls() {
        binding.videoRecyclerView.apply {
            layoutManager = GridLayoutManager(this@BackgroundSettingsActivity, 2)
            adapter = VideoAdapter { video ->
                viewModel.selectVideo(video)
            }
        }
    }

    private fun setupSlideshowControls() {
        binding.intervalSlider.addOnChangeListener { _, value, _ ->
            viewModel.setInterval(value.toInt())
            binding.intervalText.text = getString(R.string.interval_format, value.toInt())
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.videos.collect { videos ->
                (binding.videoRecyclerView.adapter as? VideoAdapter)?.submitList(videos)
            }
        }
    }

    private fun updateVisibility(type: BackgroundType) {
        binding.apply {
            imageSourceCard.isVisible = type == BackgroundType.IMAGE || type == BackgroundType.SLIDES
            videoSourceCard.isVisible = type == BackgroundType.VIDEO
            slideshowSettingsCard.isVisible = type == BackgroundType.SLIDES

            if (type == BackgroundType.VIDEO && !viewModel.hasLoadedVideos) {
                viewModel.loadPexelsVideos()
            }
        }
    }

    private fun observePreview() {
        lifecycleScope.launch {
            viewModel.backgroundType.collect { type ->
                updateVisibility(type)
                when (type) {
                    BackgroundType.NONE -> {
                        binding.previewImage.visibility = View.GONE
                        binding.previewVideo.visibility = View.GONE
                        binding.previewPlaceholder.visibility = View.VISIBLE
                    }
                    BackgroundType.SLIDES,
                    BackgroundType.IMAGE -> {
                        viewModel.selectedImages.collect { images ->
                            if (images.isNotEmpty()) {
                                showImagePreview(images.first())
                            } else {
                                showPlaceholder()
                            }
                        }
                    }
                    BackgroundType.VIDEO -> {
                        viewModel.selectedVideo.collect { video ->
                            if (video != null) {
                                showVideoPreview(video)
                            } else {
                                showPlaceholder()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showImagePreview(image: ImageItem) {
        binding.apply {
            previewImage.visibility = View.VISIBLE
            previewVideo.visibility = View.GONE
            previewPlaceholder.visibility = View.GONE
            
            previewImage.load(image.getUrl()) {
                crossfade(true)
            }
        }
    }

    private fun showVideoPreview(video: Video) {
        binding.apply {
            previewImage.visibility = View.GONE
            previewVideo.visibility = View.VISIBLE
            previewPlaceholder.visibility = View.GONE
            
            video.getBestVideoFile()?.let { videoFile ->
                previewVideo.apply {
                    setVideoPath(videoFile.link)
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = true
                        mediaPlayer.setVolume(0f, 0f) // Mute the video
                        start()
                    }
                    setOnErrorListener { _, what, extra ->
                        Log.e("VideoPreview", "Error playing video: what=$what, extra=$extra")
                        showPlaceholder()
                        true
                    }
                }
            } ?: showPlaceholder()
        }
    }

    private fun showPlaceholder() {
        binding.apply {
            previewImage.visibility = View.GONE
            previewVideo.visibility = View.GONE
            previewPlaceholder.visibility = View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        binding.previewVideo.apply {
            if (isPlaying) {
                pause()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.previewVideo.apply {
            if (visibility == View.VISIBLE && !isPlaying) {
                start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.previewVideo.apply {
            stopPlayback()
            setOnPreparedListener(null)
            setOnErrorListener(null)
        }
    }
} 