package com.saidim.clockface.background.video

import android.util.Log
import com.google.gson.GsonBuilder
import com.saidim.clockface.background.video.pexels.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class PexelsVideoRepository {
    private val api: PexelsApi

    init {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.pexels.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        api = retrofit.create(PexelsApi::class.java)
    }

    suspend fun searchVideos(query: String, perPage: Int = 20): List<Video> {
        return try {
            Log.i("PexelsVideoRepository", "start get data")
            val data = withContext(Dispatchers.IO) { api.searchVideos(query, perPage) }
            Log.i("PexelsVideoRepository", data.toString())
            data.videos
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("PexelsVideoRepository", e.message.toString())
            emptyList()
        }
    }
} 