package com.saidim.clockface.background

import LogUtils
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.tabs.TabLayoutMediator
import com.saidim.clockface.R
import com.saidim.clockface.background.color.ColorSwatchAdapter
import com.saidim.clockface.background.color.GradientColorSettings
import com.saidim.clockface.background.model.BackgroundModel
import com.saidim.clockface.background.unsplash.TopicPagerAdapter
import com.saidim.clockface.background.unsplash.UnsplashTopics
import com.saidim.clockface.background.video.VideoAdapter
import com.saidim.clockface.base.BaseActivity
import com.saidim.clockface.databinding.ActivityBackgroundSettingsBinding
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
        setupColorControls()
        observeViewModel()
        observePreview()
        restoreBackgroundState()
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

    private fun setupColorControls() {
        binding.apply {
            // Setup color recycler view
            colorRecyclerView.apply {
                layoutManager = LinearLayoutManager(
                    this@BackgroundSettingsActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = ColorSwatchAdapter { color ->
                    viewModel.selectColor(color)
                    viewModel.colorModel.color = color
                }
            }

            // Setup gradient toggle
            gradientSwitch.isChecked = viewModel.colorModel.enableFluidColor
            gradientSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.colorModel.enableFluidColor = isChecked
            }

            // Submit default colors
            (colorRecyclerView.adapter as? ColorSwatchAdapter)?.submitList(
                listOf(
                    0xFFE57373.toInt(), // Red
                    0xFF81C784.toInt(), // Green
                    0xFF64B5F6.toInt(), // Blue
                    0xFFFFB74D.toInt(), // Orange
                    0xFF9575CD.toInt(), // Purple
                    0xFF4DB6AC.toInt(), // Teal
                    0xFFF06292.toInt(), // Pink
                    0xFFFFD54F.toInt()  // Yellow
                )
            )

            // Initially hide gradient controls
            gradientControls.visibility = View.GONE
            gradientControls.alpha = 0f
        }
    }

    private fun setupImageSourceControls() {
        // Clear existing tabs
        binding.tabLayout.removeAllTabs()
        // Add tabs for each topic
        UnsplashTopics.topics.forEach { topic ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().apply { text = topic })
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

            previewColor.isVisible = type == BackgroundType.COLOR
            previewImage.isVisible = type == BackgroundType.IMAGE
            previewVideo.isVisible = type == BackgroundType.VIDEO
        }
    }

    private fun observePreview() {
        viewModel.colorSelected = { showColorPreview(it) }
        viewModel.imageSelected = { showImagePreview(it) }
        viewModel.videoSelected = { showVideoPreview(it) }
        lifecycleScope.launch {
            viewModel.backgroundType.collect { type ->
                updateVisibility(type)
                if (type == BackgroundType.COLOR) {
                    setupColorPreview()
                }
            }
        }
        lifecycleScope.launch {
            viewModel.selectedImage.collect { image ->
                showImagePreview(image)
            }
        }
        lifecycleScope.launch {
            viewModel.selectedVideo.collect { video ->
                if (video != null) {
                    showVideoPreview(BackgroundModel.VideoModel(pixelVideo = video))
                } else {
                    showPlaceholder()
                }
            }
        }
    }

    private fun showColorPreview(colorModel: BackgroundModel.ColorModel) {
        binding.apply {
            previewColor.visibility = View.VISIBLE
            previewImage.visibility = View.GONE
            previewVideo.visibility = View.GONE
            previewColor.background = colorModel.getDrawable()
        }
    }

    private fun showImagePreview(image: BackgroundModel.ImageModel) {
        binding.apply {
            previewImage.visibility = View.VISIBLE
            previewVideo.visibility = View.GONE
            previewImage.load(image.imageUrl) { crossfade(true) }
        }
    }

    private fun showVideoPreview(video: BackgroundModel.VideoModel) {
        binding.apply {
            previewImage.visibility = View.GONE
            previewVideo.visibility = View.VISIBLE

            video.pixelVideo.getBestVideoFile()?.let { videoFile ->
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
        }
    }

    private fun setupColorPreview() {
        lifecycleScope.launch {
            viewModel.previewGradient.collect { gradientDrawable ->
                gradientDrawable?.let { binding.previewColor.background = it }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause video and gradient animations
        binding.previewVideo.apply {
            if (isPlaying) {
                pause()
            }
        }
        viewModel.previewGradient.value?.let { gradient ->
            gradient.updateSettings(gradient.settings.copy(isAnimated = false))
        }
        // Save current background state
        when (viewModel.backgroundType.value) {
            BackgroundType.COLOR -> viewModel.updateBackgroundModel(viewModel.colorModel)
            BackgroundType.IMAGE -> viewModel.updateBackgroundModel(viewModel.imageModel)
            BackgroundType.VIDEO -> viewModel.updateBackgroundModel(viewModel.videoModel)
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume video and gradient animations if needed
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

    private fun restoreBackgroundState() {
        lifecycleScope.launch {
            viewModel.backgroundType.collect { type ->
                // Update UI based on background type
                binding.backgroundTypeSegmentedButton.check(
                    when (type) {
                        BackgroundType.COLOR -> R.id.noneButton
                        BackgroundType.IMAGE -> R.id.imageButton
                        BackgroundType.VIDEO -> R.id.videoButton
                    }
                )
                updateVisibility(type)
            }
        }
    }
} 