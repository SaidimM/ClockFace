package com.saidim.clockface.background

import android.app.WallpaperManager
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BackgroundManager(private val context: Context) {
    private val _currentBackground = MutableLiveData<Drawable>()
    val currentBackground: LiveData<Drawable> = _currentBackground

    private val unsplashService = Retrofit.Builder()
        .baseUrl(UnsplashService.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(UnsplashService::class.java)

    private val imageLoader = ImageLoader.Builder(context)
        .crossfade(true)
        .build()

    suspend fun setLauncherWallpaper() {
        withContext(Dispatchers.IO) {
            val wallpaperManager = WallpaperManager.getInstance(context)
            val wallpaper = wallpaperManager.drawable
            _currentBackground.postValue(wallpaper)
        }
    }

    fun setGradientBackground() {
        val gradientDrawable = FluidGradientDrawable()
        _currentBackground.value = gradientDrawable
    }

    suspend fun setUnsplashWallpaper() {
        withContext(Dispatchers.IO) {
            try {
                val photo = unsplashService.getRandomPhoto()
                val request = ImageRequest.Builder(context)
                    .data(photo.urls.regular)
                    .target { drawable ->
                        _currentBackground.postValue(drawable)
                    }
                    .build()
                
                imageLoader.enqueue(request)
            } catch (e: Exception) {
                // Fallback to gradient background if the API call fails
                setGradientBackground()
            }
        }
    }
} 