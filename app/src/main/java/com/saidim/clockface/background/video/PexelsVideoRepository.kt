package com.saidim.clockface.background.video

import android.util.Log
import com.google.gson.GsonBuilder
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

    suspend fun searchVideos(query: String = ""): List<PexelsVideo> {
        return try {
            LogUtils.d("searchVideos, query: $query")
            val data = if (query.isEmpty()) withContext(Dispatchers.IO) { api.popularVideos() }
            else withContext(Dispatchers.IO) { api.searchVideos(query = query) }
            LogUtils.d(data.toString())
            data.videos.filter { it.height < it.width }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("PexelsVideoRepository", e.message.toString())
            emptyList()
        }
    }
} 