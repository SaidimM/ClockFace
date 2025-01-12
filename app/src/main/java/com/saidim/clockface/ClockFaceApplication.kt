package com.saidim.clockface

import android.app.Application
import com.unsplash.pickerandroid.photopicker.UnsplashPhotoPicker

class ClockFaceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Unsplash Photo Picker with your client ID
        UnsplashPhotoPicker.init(
            this, // Application context
            "0E_aPev9Qt0iPOugsqoNrwrtJpbeIJ6a26KZFwok0EM",
            "K0H8UTyDBMvVydHRLe4zuQr3X-7KB3ZOs6oUVt3D9u0"
        )
    }
} 