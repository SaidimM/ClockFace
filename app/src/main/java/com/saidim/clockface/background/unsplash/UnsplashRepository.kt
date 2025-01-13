package com.saidim.clockface.background.unsplash

import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class UnsplashRepository {
    private val api: UnsplashApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder().build()

        val retrofit = Retrofit.Builder()
            .baseUrl(UnsplashApi.BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        api = retrofit.create(UnsplashApi::class.java)
    }

    suspend fun searchPhotos(
        query: String,
        page: Int = 1,
        perPage: Int = 30,
        orientation: String = "portrait"
    ): UnsplashSearchResultDto {
        return api.searchPhotos(
            query = query,
            page = page,
            perPage = perPage,
            orientation = orientation
        )
    }
} 