package com.saidim.clockface.background.video

import com.saidim.clockface.background.video.pexels.PexelsVideoResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface PexelsApi {
    companion object {
        private const val ACCESS_KEY = "563492ad6f917000010000015f6d7d5d858f4dc8967d0d9c3e1bdc25"
    }

    @Headers("Authorization:$ACCESS_KEY")
    @GET("/videos/search")
    suspend fun searchVideos(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("orientation") orientation: String = "landscape"
    ): PexelsVideoResponse


    @Headers("Authorization:$ACCESS_KEY")
    @GET("videos/popular")
    suspend fun popularVideos(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("min_width") minWidth: Int = 2048,
    ): PexelsVideoResponse
}