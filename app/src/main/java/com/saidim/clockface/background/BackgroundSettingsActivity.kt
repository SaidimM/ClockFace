package com.saidim.clockface.background

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.EditorInfo.IME_ACTION_GO
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import com.google.android.material.tabs.TabLayoutMediator
import com.saidim.clockface.R
import com.saidim.clockface.background.unsplash.TopicPagerAdapter
import com.saidim.clockface.background.unsplash.UnsplashTopics
import com.saidim.clockface.background.video.VideoAdapter
import com.saidim.clockface.background.video.pexels.Video
import com.saidim.clockface.base.BaseActivity
import com.saidim.clockface.databinding.ActivityBackgroundSettingsBinding
import com.saidim.clockface.utils.getBestVideoFile
import kotlinx.coroutines.launch

class BackgroundSettingsActivity : BaseActivity() {
    private val binding by lazy { ActivityBackgroundSettingsBinding.inflate(layoutInflater) }
    private val viewModel: BackgroundSettingsViewModel by viewModels()
    private var topicPagerAdapter: TopicPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupToolbar()
        setupBackgroundTypeSelection()
        setupImageSourceControls()
        setupVideoControls()
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
                    R.id.noneButton -> BackgroundType.COLOR
                    R.id.imageButton -> BackgroundType.IMAGE
                    R.id.videoButton -> BackgroundType.VIDEO
                    else -> BackgroundType.COLOR
                }
                viewModel.setBackgroundType(type)
            }
        }
    }

    private fun setupImageSourceControls() {
        // Clear existing tabs
        binding.tabLayout.removeAllTabs()
        // Add tabs for each topic
        UnsplashTopics.topics.forEach { topic ->
            binding.tabLayout.addTab(
                binding.tabLayout.newTab().apply {
                    text = topic
                }
            )
        }

        // Setup ViewPager
        topicPagerAdapter = TopicPagerAdapter(
            topics = UnsplashTopics.topics,
            fragmentActivity = this
        )
        
        binding.viewPager.apply {
            adapter = topicPagerAdapter
            offscreenPageLimit = 2  // Cache 2 pages on each side
            isUserInputEnabled = true // Enable swiping between topics
            isNestedScrollingEnabled = true // Enable nested scrolling
        }

        // Connect TabLayout with ViewPager using TabLayoutMediator
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = UnsplashTopics.topics[position]
        }.attach()
    }

    private fun setupVideoControls() {
        binding.videoRecyclerView.apply {
            layoutManager = GridLayoutManager(this@BackgroundSettingsActivity, 2)
            adapter = VideoAdapter { video -> viewModel.selectVideo(video) }
        }

        // Handle both Enter key and IME action
        binding.videoSourceEdit.apply {
            setOnEditorActionListener { textView, actionId, event ->
                when {
                    // Handle IME_ACTION_SEARCH or IME_ACTION_DONE
                    actionId == EditorInfo.IME_ACTION_SEARCH || 
                    actionId == EditorInfo.IME_ACTION_DONE -> {
                        LogUtils.d("EditorInfo.IME_ACTION_SEARCH pressed")
                        handleVideoSearch(textView.text.toString())
                        true
                    }
                    // Handle Enter key press
                    event?.keyCode == KeyEvent.KEYCODE_ENTER && 
                    event.action == KeyEvent.ACTION_DOWN -> {
                        LogUtils.d("KeyEvent.KEYCODE_ENTER pressed")
                        handleVideoSearch(textView.text.toString())
                        true
                    }
                    else -> false
                }
            }
            
            // Set IME options to show search action
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }

        // Load popular videos by default when video type is selected
        if (!viewModel.hasLoadedVideos) {
            viewModel.loadPexelsVideos("popular")
        }
    }

    private fun handleVideoSearch(query: String) {
        binding.videoSourceEdit.clearFocus()
        hideKeyboard(binding.videoSourceEdit)
        viewModel.loadPexelsVideos(query)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.videos.collect { videos ->
                LogUtils.d(videos.toString())
                (binding.videoRecyclerView.adapter as? VideoAdapter)?.submitList(videos)
            }
        }
    }

    private fun updateVisibility(type: BackgroundType) {
        binding.apply {
            imageSourceCard.isVisible = type == BackgroundType.IMAGE
            videoSourceCard.isVisible = type == BackgroundType.VIDEO
            colorSourceCard.isVisible = type == BackgroundType.COLOR

            previewPlaceholder.isVisible = type == BackgroundType.COLOR
            previewImage.isVisible = type == BackgroundType.IMAGE
            previewVideo.isVisible = type == BackgroundType.VIDEO

            if (type == BackgroundType.VIDEO) {
                viewModel.loadPexelsVideos("")
            }
        }
    }

    private fun observePreview() {
        lifecycleScope.launch { viewModel.backgroundType.collect { type -> updateVisibility(type) } }
        lifecycleScope.launch {
            viewModel.selectedImages.collect { images ->
                if (images.isNotEmpty()) {
                    showImagePreview(images.first())
                } else {
                    showPlaceholder()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.selectedVideo.collect { video ->
                if (video != null) {
                    showVideoPreview(video)
                } else {
                    showPlaceholder()
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
        topicPagerAdapter?.clearFragments()
        topicPagerAdapter = null
        binding.previewVideo.apply {
            stopPlayback()
            setOnPreparedListener(null)
            setOnErrorListener(null)
        }
    }
} 