package com.saidim.clockface.background.video

import com.saidim.clockface.background.video.pexels.PexelsVideoResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface PexelsApi {
    @Headers("Authorization:563492ad6f917000010000015f6d7d5d858f4dc8967d0d9c3e1bdc25")
    @GET("videos/search")
    suspend fun searchVideos(
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 20,
        @Query("orientation") orientation: String = "landscape"
    ): PexelsVideoResponse
}