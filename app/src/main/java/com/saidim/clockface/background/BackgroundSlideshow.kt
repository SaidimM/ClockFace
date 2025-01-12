package com.saidim.clockface.background

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class BackgroundSlideshow(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _currentBackground = MutableLiveData<Drawable>()
    val currentBackground: LiveData<Drawable> = _currentBackground

    private var slideshowJob: Job? = null
    private var images = mutableListOf<String>()
    private var currentIndex = 0
    private var intervalMinutes = 30

    fun setImages(newImages: List<String>) {
        images.clear()
        images.addAll(newImages)
        currentIndex = 0
        startSlideshow()
    }

    fun setInterval(minutes: Int) {
        intervalMinutes = minutes
        startSlideshow()
    }

    private fun startSlideshow() {
        slideshowJob?.cancel()
        if (images.isEmpty()) return

        slideshowJob = scope.launch {
            while (isActive) {
                showNextImage()
                delay(TimeUnit.MINUTES.toMillis(intervalMinutes.toLong()))
            }
        }
    }

    private suspend fun showNextImage() {
        if (images.isEmpty()) return
        
        // Load image using Coil
        withContext(Dispatchers.IO) {
            try {
                val imageLoader = coil.ImageLoader(context)
                val request = coil.request.ImageRequest.Builder(context)
                    .data(images[currentIndex])
                    .target { drawable ->
                        _currentBackground.postValue(drawable)
                    }
                    .build()
                
                imageLoader.enqueue(request)
                
                currentIndex = (currentIndex + 1) % images.size
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun stop() {
        slideshowJob?.cancel()
    }
} 