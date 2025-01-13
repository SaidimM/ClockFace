package com.saidim.clockface.background.unsplash

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface UnsplashApi {
    companion object {
        private const val CLIENT_ID = "0E_aPev9Qt0iPOugsqoNrwrtJpbeIJ6a26KZFwok0EM"
        const val BASE_URL = "https://api.unsplash.com/"
    }

    @Headers("Authorization: Client-ID $CLIENT_ID")
    @GET("search/photos")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("order_by") orderByy: String = "latest",
        @Query("per_page") perPage: Int = 30,
        @Query("orientation") orientation: String = "portrait"
    ): UnsplashSearchResultDto

    @Headers("Authorization: Client-ID $CLIENT_ID")
    @GET("collections")
    suspend fun getCollections(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): List<UnsplashCollectionDto>

    @Headers("Authorization: Client-ID $CLIENT_ID")
    @GET("collections/{id}/photos")
    suspend fun getCollectionPhotos(
        @Path("id") collectionId: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 30
    ): List<UnsplashPhotoDto>
} 