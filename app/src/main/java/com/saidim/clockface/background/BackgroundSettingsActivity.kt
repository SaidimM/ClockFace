package com.saidim.clockface.background

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.slider.Slider
import com.saidim.clockface.R
import com.unsplash.pickerandroid.photopicker.UnsplashPhotoPicker
import com.unsplash.pickerandroid.photopicker.presentation.UnsplashPickerActivity
import kotlinx.coroutines.launch
import com.saidim.clockface.base.BaseActivity

class BackgroundSettingsActivity : BaseActivity() {
    private val viewModel: BackgroundSettingsViewModel by viewModels()
    private lateinit var selectedImagesAdapter: SelectedImagesAdapter
    
    private val pickImages = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
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

    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val photos = result.data?.getParcelableArrayListExtra<com.unsplash.pickerandroid.photopicker.data.UnsplashPhoto>(UnsplashPickerActivity.EXTRA_PHOTOS)
            if (photos != null) {
                viewModel.addUnsplashPhotos(photos)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_background_settings)

        setupToolbar()
        setupImageSourceToggle()
        setupIntervalSlider()
        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupToolbar() {
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar).setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupImageSourceToggle() {
        findViewById<MaterialButtonToggleGroup>(R.id.imageSourceToggle).addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.setImageSource(when (checkedId) {
                    R.id.deviceButton -> ImageSource.DEVICE
                    R.id.unsplashButton -> ImageSource.UNSPLASH
                    else -> ImageSource.DEVICE
                })
            }
        }
    }

    private fun setupIntervalSlider() {
        findViewById<Slider>(R.id.intervalSlider).addOnChangeListener { _, value, _ ->
            viewModel.setInterval(value.toInt())
            findViewById<android.widget.TextView>(R.id.intervalText).text = 
                "Change interval: ${value.toInt()} minutes"
        }
    }

    private fun setupRecyclerView() {
        selectedImagesAdapter = SelectedImagesAdapter { position ->
            viewModel.removeImage(position)
        }
        findViewById<RecyclerView>(R.id.selectedImagesRecyclerView).apply {
            layoutManager = GridLayoutManager(this@BackgroundSettingsActivity, 2)
            adapter = selectedImagesAdapter
        }
    }

    private fun setupFab() {
        findViewById<ExtendedFloatingActionButton>(R.id.addImagesFab).setOnClickListener {
            when (viewModel.imageSource.value) {
                ImageSource.DEVICE -> launchDeviceImagePicker()
                ImageSource.UNSPLASH -> launchUnsplashPicker()
            }
        }
    }

    private fun launchDeviceImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickImages.launch(intent)
    }

    private fun launchUnsplashPicker() {
        val intent = UnsplashPickerActivity.getStartingIntent(this, true)
        pickMultipleMedia.launch(intent)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.selectedImages.collect { images ->
                selectedImagesAdapter.submitList(images)
            }
        }
    }
} 